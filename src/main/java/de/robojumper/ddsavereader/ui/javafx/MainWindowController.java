package de.robojumper.ddsavereader.ui.javafx;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import de.robojumper.ddsavereader.BuildConfig;
import de.robojumper.ddsavereader.i18n.Messages;
import de.robojumper.ddsavereader.ui.javafx.StateManager;
import de.robojumper.ddsavereader.ui.javafx.StateManager.SaveFile;
import de.robojumper.ddsavereader.ui.javafx.StateManager.Status;
import de.robojumper.ddsavereader.ui.javafx.dialogs.DataPathsDialog;
import de.robojumper.ddsavereader.ui.javafx.dialogs.LanguageSelectionDialog;
import de.robojumper.ddsavereader.ui.javafx.dialogs.FixedSmartSaveEditorDialog;
import de.robojumper.ddsavereader.ui.javafx.utils.UpdateCheckUtil;
import de.robojumper.ddsavereader.ui.javafx.utils.SpreadsheetsUtil;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * JavaFX主窗口控制器
 * 提供现代化的用户界面和更好的用户体验
 */
public class MainWindowController implements Initializable {

    @FXML
    private VBox root;
    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu fileMenu, toolsMenu, helpMenu;
    @FXML
    private MenuItem exitMenuItem, openBackupDirMenuItem, generateNamesMenuItem;
    @FXML
    private MenuItem spreadsheetsMenuItem, languageMenuItem, aboutMenuItem, saveEditorMenuItem;

    @FXML
    private TextField savePathField;
    @FXML
    private Button browseSavePathButton, makeBackupButton, restoreBackupButton;
    @FXML
    private Label saveFileStatusLabel;

    @FXML
    private TabPane fileTabPane;
    @FXML
    private Button discardChangesButton, saveAllButton, reloadAllButton;
    @FXML
    private Label errorLabel, saveStatusLabel;
    @FXML
    private Button updateAvailableButton;

    private Stage primaryStage;
    private StateManager state = new StateManager();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComponents();
        initializeState();
        checkForUpdates();
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    private void setupComponents() {
        // 设置菜单项文本
        fileMenu.setText(Messages.getString("menu.file"));
        exitMenuItem.setText(Messages.getString("menu.file.exit"));
        openBackupDirMenuItem.setText(Messages.getString("menu.file.openBackupDirectory"));

        toolsMenu.setText(Messages.getString("menu.tools"));
        generateNamesMenuItem.setText(Messages.getString("menu.tools.generateNameFile"));
        spreadsheetsMenuItem.setText(Messages.getString("menu.tools.spreadsheets"));
        saveEditorMenuItem.setText(Messages.getString("menu.tools.saveEditor"));

        helpMenu.setText(Messages.getString("menu.help"));
        languageMenuItem.setText("Language / 语言");
        aboutMenuItem.setText(Messages.getString("menu.help.about"));

        // 设置按钮和标签文本
        browseSavePathButton.setText(Messages.getString("button.browse"));
        makeBackupButton.setText(Messages.getString("button.makeBackup"));
        restoreBackupButton.setText(Messages.getString("button.loadBackup"));
        discardChangesButton.setText(Messages.getString("button.discardFileChanges"));
        saveAllButton.setText(Messages.getString("button.saveAllChanges"));
        reloadAllButton.setText(Messages.getString("button.reloadAll"));
        updateAvailableButton.setText(Messages.getString("button.newUpdateAvailable"));

        // 初始状态
        savePathField.setEditable(false);
        updateAvailableButton.setVisible(false);
        spreadsheetsMenuItem.setDisable(true);

        // 绑定事件处理器
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // 文件菜单事件
        exitMenuItem.setOnAction(this::handleExit);
        openBackupDirMenuItem.setOnAction(this::handleOpenBackupDirectory);

        // 工具菜单事件
        generateNamesMenuItem.setOnAction(this::handleGenerateNames);
        spreadsheetsMenuItem.setOnAction(this::handleSpreadsheets);
        saveEditorMenuItem.setOnAction(this::handleSaveEditor);

        // 帮助菜单事件
        languageMenuItem.setOnAction(this::handleLanguageSelection);
        aboutMenuItem.setOnAction(this::handleAbout);

        // 按钮事件
        browseSavePathButton.setOnAction(this::handleBrowseSavePath);
        makeBackupButton.setOnAction(this::handleMakeBackup);
        restoreBackupButton.setOnAction(this::handleRestoreBackup);
        discardChangesButton.setOnAction(this::handleDiscardChanges);
        saveAllButton.setOnAction(this::handleSaveAll);
        reloadAllButton.setOnAction(this::handleReloadAll);
        updateAvailableButton.setOnAction(this::handleUpdateAvailable);

        // Tab切换事件
        fileTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            updateSaveStatus();
        });
    }

    private void initializeState() {
        state.init(this::onStateChange);

        // 如果是首次运行，显示数据路径对话框
        if (!state.sawGameDataPopup()) {
            showDataPathsDialog(true);
            state.setSawGameDataPopup(true);
        }

        updateSaveDirectory();
        updateFiles();
        updateSaveStatus();
    }

    private void onStateChange(String fileName) {
        Platform.runLater(() -> {
            updateSaveStatus();
            updateTabStatus(fileName);
        });
    }

    // 事件处理方法
    @FXML
    private void handleExit(ActionEvent event) {
        handleExitEvent(null);
    }

    public void handleExit(WindowEvent event) {
        handleExitEvent(event);
    }

    private void handleExitEvent(WindowEvent event) {
        if (confirmLoseChanges()) {
            state.save();
            Platform.exit();
        } else if (event != null) {
            event.consume(); // 阻止窗口关闭
        }
    }

    private void handleOpenBackupDirectory(ActionEvent event) {
        try {
            java.awt.Desktop.getDesktop().open(new File(state.getBackupPath()));
        } catch (Exception e) {
            showError("Error opening backup directory", e.getMessage());
        }
    }

    private void handleBrowseSavePath(ActionEvent event) {
        if (confirmLoseChanges()) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(Messages.getString("dialog.chooseSaveDirectory"));

            String currentDir = state.getSaveDir();
            if (currentDir != null && !currentDir.isEmpty()) {
                File initial = new File(currentDir);
                if (initial.exists()) {
                    chooser.setInitialDirectory(initial);
                }
            }

            File selectedDir = chooser.showDialog(primaryStage);
            if (selectedDir != null) {
                state.setSaveDir(selectedDir.getAbsolutePath());
                updateSaveDirectory();
                updateFiles();
            }
        }
    }

    private void handleMakeBackup(ActionEvent event) {
        if (state.getSaveStatus() != Status.ERROR) {
            TextInputDialog dialog = new TextInputDialog(
                    new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date()));
            dialog.setTitle(Messages.getString("dialog.chooseBackupName"));
            dialog.setHeaderText(Messages.getString("dialog.chooseBackupName"));
            dialog.setContentText("Backup name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                String cleanName = name.replaceAll("[:\\\\/*?|<>]", "_");
                if (state.hasBackup(cleanName)) {
                    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmDialog.setTitle(Messages.getString("dialog.backupAlreadyExistsTitle"));
                    confirmDialog.setHeaderText(Messages.getString("dialog.backupAlreadyExists", cleanName));

                    Optional<ButtonType> confirmResult = confirmDialog.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() != ButtonType.OK) {
                        return;
                    }
                }
                state.makeBackup(cleanName);
                updateBackupButtons();
            });
        }
    }

    private void handleRestoreBackup(ActionEvent event) {
        if (state.getSaveStatus() != Status.ERROR && state.hasAnyBackups() && confirmLoseChanges()) {
            ChoiceDialog<String> dialog = new ChoiceDialog<>();
            dialog.getItems().addAll(state.getBackupNames());
            dialog.setSelectedItem(dialog.getItems().get(0));
            dialog.setTitle(Messages.getString("dialog.restoreBackupTitle"));
            dialog.setHeaderText(Messages.getString("dialog.restoreBackup"));

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(backup -> {
                try {
                    state.restoreBackup(backup);
                    state.loadFiles();
                    updateFiles();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    // 显示错误对话框
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("恢复备份失败");
                    alert.setContentText("无法恢复备份: " + e.getMessage());
                    alert.showAndWait();
                }
            });
        }
    }

    private void handleDiscardChanges(ActionEvent event) {
        Tab selectedTab = fileTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getUserData() instanceof String) {
            String fileName = (String) selectedTab.getUserData();
            SaveFile saveFile = state.getSaveFile(fileName);

            // 找到对应的CodeArea并重置内容
            if (selectedTab.getContent() instanceof ScrollPane) {
                ScrollPane scrollPane = (ScrollPane) selectedTab.getContent();
                if (scrollPane.getContent() instanceof CodeArea) {
                    CodeArea codeArea = (CodeArea) scrollPane.getContent();
                    codeArea.replaceText(saveFile.getOriginalContents());
                    codeArea.moveTo(0);
                }
            }
        }
    }

    private void handleSaveAll(ActionEvent event) {
        if (state.canSave()) {
            // 在后台线程中保存
            Task<Void> saveTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    state.saveChanges();
                    return null;
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        state.loadFiles();
                        updateFiles();
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        showError("Save Failed", "Failed to save changes: " + getException().getMessage());
                    });
                }
            };

            new Thread(saveTask).start();
        }
    }

    private void handleReloadAll(ActionEvent event) {
        if (confirmLoseChanges()) {
            state.loadFiles();
            updateFiles();
        }
    }

    private void handleGenerateNames(ActionEvent event) {
        if (confirmLoseChanges()) {
            showDataPathsDialog(false);
            // 不需要调用loadFiles，DataPathsDialog只是用于生成名称文件
            updateFiles();
        }
    }

    private void handleSpreadsheets(ActionEvent event) {
        if (state.getSaveStatus() != Status.ERROR) {
            SpreadsheetsUtil.showSpreadsheetsDialog(state);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(Messages.getString("error.title", "Error"));
            alert.setHeaderText(Messages.getString("error.cannotUseSpreadsheets", "Cannot use spreadsheets"));
            alert.setContentText(Messages.getString("error.fixSaveErrorsFirst",
                    "Please fix save file errors before using spreadsheets."));
            alert.showAndWait();
        }
    }

    private void handleSaveEditor(ActionEvent event) {
        try {
            // 创建修复版智能存档编辑器
            FixedSmartSaveEditorDialog controller = new FixedSmartSaveEditorDialog();

            // 初始化控制器
            controller.initialize(null, null);

            // 创建新窗口
            Stage dialogStage = controller.createDialog();
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);

            // 设置状态管理器
            controller.setState(state);

            // 显示对话框
            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText("无法打开存档编辑器");
            alert.setContentText("错误详情: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleLanguageSelection(ActionEvent event) {
        LanguageSelectionDialog dialog = new LanguageSelectionDialog(primaryStage);
        if (dialog.showAndWait()) {
            dialog.saveLanguagePreference();

            // 刷新界面文本
            refreshUITexts();

            // 刷新文件标签页标题
            refreshTabTitles();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Language / 语言");
            alert.setHeaderText(null);
            alert.setContentText(Messages.getString("dialog.languageChanged",
                    "Language has been changed successfully!\n语言已成功更改！\n\nSome text may require restart to take effect.\n某些文本可能需要重启才能生效。"));
            alert.showAndWait();
        }
    }

    /**
     * 刷新界面中的所有文本
     */
    private void refreshUITexts() {
        // 重新设置菜单项文本
        fileMenu.setText(Messages.getString("menu.file"));
        exitMenuItem.setText(Messages.getString("menu.file.exit"));
        openBackupDirMenuItem.setText(Messages.getString("menu.file.openBackupDirectory"));

        toolsMenu.setText(Messages.getString("menu.tools"));
        generateNamesMenuItem.setText(Messages.getString("menu.tools.generateNameFile"));
        spreadsheetsMenuItem.setText(Messages.getString("menu.tools.spreadsheets"));
        saveEditorMenuItem.setText(Messages.getString("menu.tools.saveEditor"));

        helpMenu.setText(Messages.getString("menu.help"));
        aboutMenuItem.setText(Messages.getString("menu.help.about"));

        // 重新设置按钮和标签文本
        browseSavePathButton.setText(Messages.getString("button.browse"));
        makeBackupButton.setText(Messages.getString("button.makeBackup"));
        restoreBackupButton.setText(Messages.getString("button.loadBackup"));
        discardChangesButton.setText(Messages.getString("button.discardFileChanges"));
        saveAllButton.setText(Messages.getString("button.saveAllChanges"));
        reloadAllButton.setText(Messages.getString("button.reloadAll"));
        updateAvailableButton.setText(Messages.getString("button.newUpdateAvailable"));

        // 刷新状态标签
        updateSaveStatus();
    }

    /**
     * 刷新标签页标题
     */
    private void refreshTabTitles() {
        for (Tab tab : fileTabPane.getTabs()) {
            if (tab.getUserData() instanceof String) {
                String fileName = (String) tab.getUserData();
                // 保持文件名不变，只是确保标签页能正确显示
                tab.setText(fileName);
            }
        }
    }

    private void handleAbout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Messages.getString("dialog.aboutTitle"));
        alert.setHeaderText(BuildConfig.DISPLAY_NAME);
        alert.setContentText(String.format("Version: %s\nGitHub: %s",
                BuildConfig.VERSION, BuildConfig.GITHUB_URL));
        alert.showAndWait();
    }

    private void handleUpdateAvailable(ActionEvent event) {
        de.robojumper.ddsavereader.updatechecker.UpdateChecker.Release release = UpdateCheckUtil.getLatestRelease();
        if (release != null) {
            UpdateCheckUtil.showUpdateAvailableDialog(release);
        }
    }

    // 界面更新方法
    private void updateSaveDirectory() {
        savePathField.setText(state.getSaveDir());
        updateSaveStatus();
    }

    private void updateFiles() {
        fileTabPane.getTabs().clear();

        for (SaveFile file : state.getSaveFiles()) {
            Tab tab = new Tab();
            tab.setText((file.isChanged() ? "*" : "") + file.getName());
            tab.setUserData(file.getName());

            // 创建代码编辑区域
            CodeArea codeArea = new CodeArea();
            codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
            codeArea.replaceText(file.getOriginalContents());

            // 监听文本变化
            codeArea.textProperty().addListener((obs, oldText, newText) -> {
                file.setContents(newText);
                Platform.runLater(() -> {
                    tab.setText((file.isChanged() ? "*" : "") + file.getName());
                    updateSaveStatus();
                });
            });

            ScrollPane scrollPane = new ScrollPane(codeArea);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            tab.setContent(scrollPane);

            fileTabPane.getTabs().add(tab);
        }
    }

    private void updateTabStatus(String fileName) {
        for (Tab tab : fileTabPane.getTabs()) {
            if (fileName.equals(tab.getUserData())) {
                SaveFile file = state.getSaveFile(fileName);
                tab.setText((file.isChanged() ? "*" : "") + file.getName());
                break;
            }
        }
    }

    private void updateSaveStatus() {
        boolean hasChanges = state.anyChanges();
        boolean canSave = state.canSave();

        saveAllButton.setDisable(!canSave);
        discardChangesButton.setDisable(!hasChanges);

        Tab selectedTab = fileTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getUserData() instanceof String) {
            String fileName = (String) selectedTab.getUserData();
            SaveFile file = state.getSaveFile(fileName);
            errorLabel.setText(file.canSaveFile() ? "" : file.getErrorReason());
        } else {
            errorLabel.setText("");
        }
    }

    private void updateBackupButtons() {
        makeBackupButton.setDisable(state.getSaveStatus() == Status.ERROR);
        restoreBackupButton.setDisable(!state.hasAnyBackups());
    }

    private String getColorForFile(SaveFile file) {
        if (!file.canSaveFile())
            return "red";
        if (file.isChanged())
            return "orange";
        return "black";
    }

    private boolean confirmLoseChanges() {
        if (state.getNumUnsavedChanges() > 0) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.getString("message.confirmLoseChangesTitle"));
            alert.setHeaderText(Messages.getString("message.confirmLoseChanges"));

            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }
        return true;
    }

    private void checkForUpdates() {
        UpdateCheckUtil.checkForUpdatesSilently(new UpdateCheckUtil.UpdateCheckCallback() {
            @Override
            public void onUpdateAvailable(de.robojumper.ddsavereader.updatechecker.UpdateChecker.Release release) {
                Platform.runLater(() -> {
                    updateAvailableButton.setVisible(true);
                    updateAvailableButton.setText(Messages.getString("button.newUpdateAvailable", "Update Available"));
                });
            }

            @Override
            public void onNoUpdateAvailable() {
                // 静默处理，不显示任何信息
            }

            @Override
            public void onError(Exception e) {
                // 静默处理错误，不影响用户体验
                System.err.println("Update check failed: " + e.getMessage());
            }
        });
    }

    private void showDataPathsDialog(boolean skipInsteadOfCancel) {
        DataPathsDialog.showDialog(primaryStage, state.getGameDir(), state.getModsDir(), state, skipInsteadOfCancel);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}