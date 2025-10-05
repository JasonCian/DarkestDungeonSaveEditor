package de.robojumper.ddsavereader.ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import de.robojumper.ddsavereader.BuildConfig;
import de.robojumper.ddsavereader.i18n.Messages;

import java.io.IOException;

/**
 * 现代化的JavaFX主应用程序
 * 替换旧的Swing界面，提供更好的用户体验
 */
public class DDSaveEditorApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 初始化语言设置
        de.robojumper.ddsavereader.ui.javafx.dialogs.LanguageSelectionDialog.initializeLanguage();
        
        try {
            // 加载FXML布局
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();
            
            // 获取控制器并传递Stage引用
            MainWindowController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);
            
            // 创建场景
            Scene scene = new Scene(root, 1200, 800);
            
            // 加载CSS样式
            try {
                scene.getStylesheets().add(getClass().getResource("/css/enhanced-editor.css").toExternalForm());
            } catch (Exception e) {
                System.out.println("CSS文件未找到，使用默认样式");
            }
            
            // 设置窗口属性
            primaryStage.setTitle(BuildConfig.DISPLAY_NAME + " " + BuildConfig.VERSION);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // 使用默认的Java图标，不需要自定义图标
            
            // 设置关闭事件处理
            primaryStage.setOnCloseRequest(event -> {
                controller.handleExit(event);
            });
            
            primaryStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            // JavaFX界面加载失败，直接退出
            Platform.exit();
            System.err.println("Failed to load JavaFX interface!");
            System.exit(1);
        }
    }
    
    @Override
    public void stop() throws Exception {
        // 应用程序关闭时的清理工作
        super.stop();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}