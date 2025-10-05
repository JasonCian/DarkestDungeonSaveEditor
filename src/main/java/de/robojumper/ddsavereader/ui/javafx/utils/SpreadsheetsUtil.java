package de.robojumper.ddsavereader.ui.javafx.utils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ProgressIndicator;

import de.robojumper.ddsavereader.BuildConfig;
import de.robojumper.ddsavereader.i18n.Messages;
import de.robojumper.ddsavereader.spreadsheets.SpreadsheetsService;
import de.robojumper.ddsavereader.ui.javafx.StateManager;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.auth.oauth2.Credential;

/**
 * JavaFX版本的Google Sheets集成工具
 */
public class SpreadsheetsUtil {
    
    /**
     * 显示Google Sheets设置对话框
     */
    public static void showSpreadsheetsDialog(StateManager state) {
        // 检查是否已设置认证
        if (!SpreadsheetsService.isCredentialsConfigured()) {
            showCredentialsSetupDialog();
            return;
        }
        
        // 获取表格ID
        TextInputDialog dialog = new TextInputDialog(state.getLastSheetID());
        dialog.setTitle(Messages.getString("dialog.setSpreadsheetId", "Set Spreadsheet ID"));
        dialog.setHeaderText(Messages.getString("dialog.enterSpreadsheetId", "Enter Google Sheets ID"));
        dialog.setContentText("Spreadsheet ID:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String sheetId = result.get().trim();
            state.setLastSheetID(sheetId);
            
            // 启动表格更新任务
            startSpreadsheetsSync(sheetId, state);
        }
    }
    
    private static void showCredentialsSetupDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Messages.getString("error.spreadsheetNotSetup", "Spreadsheet Setup Required"));
        alert.setHeaderText("Google Sheets integration is not configured");
        alert.setContentText("To use Google Sheets integration, you need to:\\n" +
            "1. Set up Google API credentials\\n" +
            "2. Enable the Google Sheets API\\n" +
            "3. Place the credentials file (client_secret.json) in the application directory\\n\\n" +
            "Would you like to open the setup instructions?");
        
        ButtonType okButton = new ButtonType(Messages.getString("button.ok", "OK"));
        ButtonType helpButton = new ButtonType(Messages.getString("button.goToReadme", "View Instructions"));
        
        alert.getButtonTypes().setAll(okButton, helpButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == helpButton) {
            try {
                Desktop.getDesktop().browse(new URI(BuildConfig.GITHUB_URL + "/blob/master/README.md#spreadsheets"));
            } catch (java.io.IOException | java.net.URISyntaxException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to open browser");
                errorAlert.setContentText("Could not open the instructions page: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }
    
    private static void startSpreadsheetsSync(String sheetId, StateManager state) {
        // 创建进度对话框
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle(Messages.getString("status.running", "Running"));
        progressAlert.setHeaderText("Setting up Google Sheets sync...");
        progressAlert.setContentText("Initializing connection and starting sync process.");
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressAlert.getDialogPane().setContent(progressIndicator);
        progressAlert.getButtonTypes().clear();
        progressAlert.show();
        
        Task<SpreadsheetsService.SheetUpdater> setupTask = new Task<SpreadsheetsService.SheetUpdater>() {
            @Override
            protected SpreadsheetsService.SheetUpdater call() throws Exception {
                // 在后台线程中设置Google Sheets连接
                final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
                Credential cred = SpreadsheetsService.getCredentials(HTTP_TRANSPORT);
                
                if (cred == null) {
                    throw new Exception("Failed to obtain Google API credentials");
                }
                
                // 创建表格更新器
                SpreadsheetsService.SheetUpdater updater = SpreadsheetsService.makeUpdaterRunnable(
                    sheetId, state.getSaveDir(), cred, HTTP_TRANSPORT);
                
                return updater;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    progressAlert.close();
                    
                    SpreadsheetsService.SheetUpdater updater = getValue();
                    
                    // 启动定期同步
                    startPeriodicSync(updater, state);
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText("Google Sheets sync started");
                    successAlert.setContentText("The spreadsheet will be updated automatically every 2 minutes.\\n" +
                        "You can continue using the application normally.");
                    successAlert.showAndWait();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressAlert.close();
                    
                    Throwable exception = getException();
                    String errorMessage = exception.getMessage();
                    
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Setup Failed");
                    errorAlert.setHeaderText("Failed to setup Google Sheets sync");
                    
                    if (errorMessage.contains("credentials")) {
                        errorAlert.setContentText("Authentication failed. Please check your credentials file:\\n" +
                            "1. Make sure client_secret.json is in the application directory\\n" +
                            "2. Verify the file contains valid Google API credentials\\n" +
                            "3. Ensure you have enabled the Google Sheets API");
                    } else {
                        errorAlert.setContentText("Error: " + errorMessage);
                    }
                    
                    errorAlert.showAndWait();
                });
            }
        };
        
        Thread thread = new Thread(setupTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    private static void startPeriodicSync(SpreadsheetsService.SheetUpdater updater, StateManager state) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // 每2分钟同步一次
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (updater.isRunning()) {
                    updater.run();
                }
            } catch (Exception e) {
                System.err.println("Error during periodic sync: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.MINUTES);
        
        // 将调度器保存到State中，以便应用程序关闭时可以停止它
        state.setSpreadsheetScheduler(scheduler);
    }
    
    /**
     * 检查Google Sheets是否已配置
     */
    public static boolean isSpreadsheetsConfigured() {
        return SpreadsheetsService.isCredentialsConfigured();
    }
}