package de.robojumper.ddsavereader.ui.javafx.dialogs;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import de.robojumper.ddsavereader.file.DsonTypes;
import de.robojumper.ddsavereader.i18n.Messages;
import de.robojumper.ddsavereader.ui.javafx.StateManager;
import de.robojumper.ddsavereader.util.ReadNames;
import de.robojumper.ddsavereader.util.Helpers;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

/**
 * JavaFX版本的数据路径对话框
 * 用于设置游戏数据目录和模组目录，生成名称文件
 */
public class DataPathsDialog {

    private Stage dialogStage;
    private TextField gameDirField;
    private TextField modsDirField;
    private Button generateButton;
    private Button cancelButton;
    private Button skipButton;

    private String gameDir;
    private String modsDir;
    private StateManager state;
    private boolean skipInsteadOfCancel;
    private boolean result = false;

    public DataPathsDialog(Window owner, String gameDir, String modsDir, StateManager state,
            boolean skipInsteadOfCancel) {
        this.gameDir = gameDir;
        this.modsDir = modsDir;
        this.state = state;
        this.skipInsteadOfCancel = skipInsteadOfCancel;

        initializeDialog(owner);
    }

    private void initializeDialog(Window owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setTitle(Messages.getString("dataPaths.title", "Data Paths Setup"));
        dialogStage.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // 说明文本
        Label descLabel1 = new Label(Messages.getString("dataPaths.description1",
                "This tool generates a names file that helps decode hashed values in save files."));
        Label descLabel2 = new Label(Messages.getString("dataPaths.description2",
                "For example, the hash for \"jester\" is: " + DsonTypes.stringHash("jester")));
        Label descLabel3 = new Label(Messages.getString("dataPaths.description3",
                "You need to specify the game data directory and optionally the mods directory."));
        Label descLabel4 = new Label(Messages.getString("dataPaths.description4",
                "The game directory should contain files like 'heroes', 'monsters', etc."));

        descLabel1.setWrapText(true);
        descLabel2.setWrapText(true);
        descLabel3.setWrapText(true);
        descLabel4.setWrapText(true);

        VBox descBox = new VBox(5, descLabel1, descLabel2, descLabel3, descLabel4);

        // 分隔线
        Separator separator = new Separator();

        // 游戏目录设置
        GridPane pathGrid = new GridPane();
        pathGrid.setHgap(10);
        pathGrid.setVgap(10);

        Label gameDirLabel = new Label(Messages.getString("dataPaths.gameDirectory", "Game Directory:"));
        gameDirField = new TextField(gameDir);
        gameDirField.setPrefColumnCount(40);
        Button browseGameDirButton = new Button(Messages.getString("button.browse", "Browse..."));
        browseGameDirButton.setOnAction(e -> browseGameDirectory());

        Label modsDirLabel = new Label(Messages.getString("dataPaths.modsDirectory", "Mods Directory (optional):"));
        modsDirField = new TextField(modsDir);
        modsDirField.setPrefColumnCount(40);
        Button browseModsDirButton = new Button(Messages.getString("button.browse", "Browse..."));
        browseModsDirButton.setOnAction(e -> browseModsDirectory());

        pathGrid.add(gameDirLabel, 0, 0);
        pathGrid.add(gameDirField, 1, 0);
        pathGrid.add(browseGameDirButton, 2, 0);

        pathGrid.add(modsDirLabel, 0, 1);
        pathGrid.add(modsDirField, 1, 1);
        pathGrid.add(browseModsDirButton, 2, 1);

        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center-right;");

        generateButton = new Button(Messages.getString("dataPaths.generateNames", "Generate Names"));
        generateButton.setDefaultButton(true);
        generateButton.setOnAction(e -> generateNames());

        if (skipInsteadOfCancel) {
            skipButton = new Button(Messages.getString("button.skip", "Skip"));
            skipButton.setOnAction(e -> {
                result = true;
                dialogStage.close();
            });
            buttonBox.getChildren().addAll(skipButton, generateButton);
        } else {
            cancelButton = new Button(Messages.getString("button.cancel", "Cancel"));
            cancelButton.setCancelButton(true);
            cancelButton.setOnAction(e -> {
                result = false;
                dialogStage.close();
            });
            buttonBox.getChildren().addAll(cancelButton, generateButton);
        }

        // 组装界面
        root.getChildren().addAll(descBox, separator, pathGrid, buttonBox);

        Scene scene = new Scene(root);
        // 加载CSS样式表，如果找不到则使用默认样式
        try {
            scene.getStylesheets().add(getClass().getResource("/css/enhanced-editor.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS file not found, using default styling for DataPathsDialog");
        }
        dialogStage.setScene(scene);
    }

    private void browseGameDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Messages.getString("dataPaths.chooseGameDirectory", "Choose Game Directory"));

        if (!gameDirField.getText().isEmpty()) {
            File current = new File(gameDirField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        }

        File selectedDir = chooser.showDialog(dialogStage);
        if (selectedDir != null) {
            gameDirField.setText(selectedDir.getAbsolutePath());
        }
    }

    private void browseModsDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Messages.getString("dataPaths.chooseModsDirectory", "Choose Mods Directory"));

        if (!modsDirField.getText().isEmpty()) {
            File current = new File(modsDirField.getText());
            if (current.exists()) {
                chooser.setInitialDirectory(current);
            }
        }

        File selectedDir = chooser.showDialog(dialogStage);
        if (selectedDir != null) {
            modsDirField.setText(selectedDir.getAbsolutePath());
        }
    }

    private void generateNames() {
        String gameDirectory = gameDirField.getText().trim();
        String modsDirectory = modsDirField.getText().trim();

        if (gameDirectory.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(Messages.getString("error.title", "Error"));
            alert.setHeaderText(Messages.getString("dataPaths.gameDirectoryRequired", "Game directory is required"));
            alert.setContentText(Messages.getString("dataPaths.pleaseSelectGameDirectory",
                    "Please select the game directory to continue."));
            alert.showAndWait();
            return;
        }

        // 禁用按钮，显示进度
        generateButton.setDisable(true);
        if (cancelButton != null)
            cancelButton.setDisable(true);
        if (skipButton != null)
            skipButton.setDisable(true);

        // 创建简单的进度对话框
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initOwner(dialogStage);
        progressStage.setTitle("正在生成名称文件...");
        progressStage.setResizable(false);

        VBox progressBox = new VBox(10);
        progressBox.setPadding(new Insets(20));
        progressBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label progressLabel = new Label("正在生成名称文件，请稍候...");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(-1); // 无限进度条

        progressBox.getChildren().addAll(progressLabel, progressIndicator);

        Scene progressScene = new Scene(progressBox, 300, 120);
        progressStage.setScene(progressScene);
        progressStage.show();

        // 在后台线程中生成名称文件
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    System.out.println("开始生成名称文件...");

                    // 更新状态中的目录设置
                    state.setGameDir(gameDirectory);
                    if (!modsDirectory.isEmpty()) {
                        state.setModsDir(modsDirectory);
                    }

                    // 生成名称文件
                    String[] dirs = modsDirectory.isEmpty() ? new String[] { gameDirectory }
                            : new String[] { gameDirectory, modsDirectory };

                    System.out.println("调用ReadNames.main，目录：" + java.util.Arrays.toString(dirs));
                    ReadNames.main(dirs);
                    System.out.println("名称文件生成完成");

                    return null;
                } catch (Exception e) {
                    System.err.println("生成名称文件时发生异常：" + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void succeeded() {
                System.out.println("Task succeeded - 进入succeeded回调");
                javafx.application.Platform.runLater(() -> {
                    try {
                        System.out.println("关闭进度对话框");
                        progressStage.close();

                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("成功");
                        successAlert.setHeaderText("名称文件生成完成");
                        successAlert.setContentText("名称文件已成功生成，将用于解码哈希值。");
                        successAlert.showAndWait();

                        result = true;
                        dialogStage.close();
                    } catch (Exception e) {
                        System.err.println("在succeeded回调中发生异常：" + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }

            @Override
            protected void failed() {
                System.out.println("Task failed - 进入failed回调");
                javafx.application.Platform.runLater(() -> {
                    try {
                        progressStage.close();

                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("错误");
                        errorAlert.setHeaderText("名称文件生成失败");

                        Throwable exception = getException();
                        String errorMessage = exception != null ? exception.getMessage() : "未知错误";
                        errorAlert.setContentText("生成名称文件时发生错误：" + errorMessage);
                        errorAlert.showAndWait();

                        // 重新启用按钮
                        generateButton.setDisable(false);
                        if (cancelButton != null)
                            cancelButton.setDisable(false);
                        if (skipButton != null)
                            skipButton.setDisable(false);
                    } catch (Exception e) {
                        System.err.println("在failed回调中发生异常：" + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public boolean showAndWait() {
        dialogStage.showAndWait();
        return result;
    }

    public static void showDialog(Window owner, String gameDir, String modsDir, StateManager state,
            boolean skipInsteadOfCancel) {
        DataPathsDialog dialog = new DataPathsDialog(owner, gameDir, modsDir, state, skipInsteadOfCancel);
        dialog.showAndWait();
    }

    /**
     * 从缓存的名称文件更新DsonTypes
     */
    public static void updateFromDataFile() {
        File cachedNameFile = new File(Helpers.DATA_DIR, "names_cache.txt");
        if (!cachedNameFile.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(cachedNameFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                DsonTypes.offerName(line);
            }
        } catch (IOException e) {
            // 忽略错误
            System.err.println("Warning: Could not read cached names file: " + e.getMessage());
        }
    }
}