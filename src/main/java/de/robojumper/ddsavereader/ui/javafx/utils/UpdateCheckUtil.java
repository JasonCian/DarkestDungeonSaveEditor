package de.robojumper.ddsavereader.ui.javafx.utils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import de.robojumper.ddsavereader.BuildConfig;
import de.robojumper.ddsavereader.i18n.Messages;
import de.robojumper.ddsavereader.updatechecker.UpdateChecker;
import de.robojumper.ddsavereader.updatechecker.UpdateChecker.Release;
import de.robojumper.ddsavereader.updatechecker.Version;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;

/**
 * JavaFX版本的更新检查工具
 * 在后台检查是否有新版本可用
 */
public class UpdateCheckUtil {
    
    private static Release latestRelease;
    
    public interface UpdateCheckCallback {
        void onUpdateAvailable(Release release);
        void onNoUpdateAvailable();
        void onError(Exception e);
    }
    
    /**
     * 异步检查更新
     */
    public static void checkForUpdatesAsync(UpdateCheckCallback callback) {
        Task<Release> task = new Task<Release>() {
            @Override
            protected Release call() throws Exception {
                return UpdateChecker.getLatestRelease();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    Release release = getValue();
                    latestRelease = release;
                    
                    if (release != null) {
                        Version current = new Version(BuildConfig.VERSION);
                        if (release.version.compareTo(current) > 0) {
                            callback.onUpdateAvailable(release);
                        } else {
                            callback.onNoUpdateAvailable();
                        }
                    } else {
                        callback.onNoUpdateAvailable();
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    callback.onError(new Exception(getException()));
                });
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * 显示更新可用对话框
     */
    public static void showUpdateAvailableDialog(Release release) {
        if (release == null) return;
        
        Version current = new Version(BuildConfig.VERSION);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Messages.getString("dialog.updateAvailableTitle", "Update Available"));
        alert.setHeaderText(Messages.getString("dialog.updateAvailableMessage", 
            "A new version is available!\nCurrent: %s\nLatest: %s", 
            current.toString(), release.version.toString()));
        alert.setContentText("Would you like to visit the releases page?");
        
        ButtonType visitButton = new ButtonType(Messages.getString("button.goToReleases", "Visit Releases"));
        ButtonType cancelButton = new ButtonType(Messages.getString("button.cancel", "Cancel"));
        
        alert.getButtonTypes().setAll(visitButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == visitButton) {
            try {
                Desktop.getDesktop().browse(new URI(release.htmlUrl));
            } catch (Exception e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to open browser");
                errorAlert.setContentText("Could not open the releases page: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }
    
    /**
     * 静默检查更新（不显示对话框，只返回结果）
     */
    public static void checkForUpdatesSilently(UpdateCheckCallback callback) {
        checkForUpdatesAsync(new UpdateCheckCallback() {
            @Override
            public void onUpdateAvailable(Release release) {
                callback.onUpdateAvailable(release);
            }
            
            @Override
            public void onNoUpdateAvailable() {
                // 静默模式下不显示"无更新"消息
                callback.onNoUpdateAvailable();
            }
            
            @Override
            public void onError(Exception e) {
                // 静默模式下不显示错误消息
                callback.onError(e);
            }
        });
    }
    
    public static Release getLatestRelease() {
        return latestRelease;
    }
}