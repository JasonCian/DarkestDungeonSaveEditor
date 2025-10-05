package de.robojumper.ddsavereader.ui.javafx.dialogs;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import de.robojumper.ddsavereader.i18n.Messages;

import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * JavaFX版本的语言选择对话框
 * 允许用户选择应用程序的显示语言
 */
public class LanguageSelectionDialog {
    
    private static final String LANGUAGE_PREFERENCE_KEY = "selected.language";
    private static final Preferences prefs = Preferences.userNodeForPackage(LanguageSelectionDialog.class);
    
    private Stage dialogStage;
    private ComboBox<LanguageItem> languageComboBox;
    private boolean result = false;
    
    // 支持的语言
    private static final LanguageItem[] LANGUAGES = {
        new LanguageItem("English", "en", "US"),
        new LanguageItem("中文", "zh", "CN")
    };
    
    public LanguageSelectionDialog(Window owner) {
        initializeDialog(owner);
    }
    
    private void initializeDialog(Window owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setTitle("Language Selection / 语言选择");
        dialogStage.setResizable(false);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        // 说明标签
        Label instructionLabel = new Label("Please select your preferred language / 请选择您的首选语言:");
        instructionLabel.setStyle("-fx-font-weight: bold;");
        
        // 语言选择下拉框
        languageComboBox = new ComboBox<>(FXCollections.observableArrayList(LANGUAGES));
        languageComboBox.setPrefWidth(300);
        languageComboBox.setCellFactory(listView -> new LanguageListCell());
        languageComboBox.setButtonCell(new LanguageListCell());
        
        // 设置当前选中的语言
        String savedLanguage = getSavedLanguageCode();
        for (LanguageItem item : LANGUAGES) {
            if (item.getCode().equals(savedLanguage)) {
                languageComboBox.getSelectionModel().select(item);
                break;
            }
        }
        
        // 如果没有找到保存的语言，默认选择第一个
        if (languageComboBox.getSelectionModel().getSelectedItem() == null) {
            languageComboBox.getSelectionModel().selectFirst();
        }
        
        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center-right;");
        
        Button okButton = new Button("OK / 确定");
        okButton.setDefaultButton(true);
        okButton.setOnAction(e -> {
            result = true;
            dialogStage.close();
        });
        
        Button cancelButton = new Button("Cancel / 取消");
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> {
            result = false;
            dialogStage.close();
        });
        
        buttonBox.getChildren().addAll(cancelButton, okButton);
        
        // 组装界面
        root.getChildren().addAll(instructionLabel, languageComboBox, buttonBox);
        
        Scene scene = new Scene(root);
        // 使用存在的CSS文件，或者移除CSS加载以避免错误
        try {
            scene.getStylesheets().add(getClass().getResource("/css/enhanced-editor.css").toExternalForm());
        } catch (Exception e) {
            // 如果CSS文件不存在，继续执行，不影响功能
            System.out.println("CSS file not found, using default styling");
        }
        dialogStage.setScene(scene);
    }
    
    public boolean showAndWait() {
        dialogStage.showAndWait();
        return result;
    }
    
    public void saveLanguagePreference() {
        LanguageItem selectedItem = languageComboBox.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String languageCode = selectedItem.getCode();
            prefs.put(LANGUAGE_PREFERENCE_KEY, languageCode);
            
            // 设置JVM的默认语言环境
            Locale newLocale = selectedItem.getLocale();
            Locale.setDefault(newLocale);
            
            // 更新Messages类的ResourceBundle
            Messages.setLocale(newLocale);
            
            System.out.println("Language changed to: " + selectedItem.getDisplayName() + " (" + languageCode + ")");
        }
    }
    
    public static String getSavedLanguageCode() {
        return prefs.get(LANGUAGE_PREFERENCE_KEY, "en");
    }
    
    public static void initializeLanguage() {
        String savedLanguage = getSavedLanguageCode();
        
        // 根据保存的语言设置设置JVM的默认语言环境
        for (LanguageItem item : LANGUAGES) {
            if (item.getCode().equals(savedLanguage)) {
                Locale newLocale = item.getLocale();
                Locale.setDefault(newLocale);
                
                // 更新Messages类的ResourceBundle
                Messages.setLocale(newLocale);
                
                System.out.println("Initialized language: " + item.getDisplayName() + " (" + savedLanguage + ")");
                break;
            }
        }
    }
    
    // 语言项目类
    private static class LanguageItem {
        private final String displayName;
        private final String languageCode;
        private final String countryCode;
        
        public LanguageItem(String displayName, String languageCode, String countryCode) {
            this.displayName = displayName;
            this.languageCode = languageCode;
            this.countryCode = countryCode;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getCode() {
            return languageCode;
        }
        
        public Locale getLocale() {
            return new Locale(languageCode, countryCode);
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // 自定义单元格渲染器
    private static class LanguageListCell extends ListCell<LanguageItem> {
        @Override
        protected void updateItem(LanguageItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getDisplayName());
            }
        }
    }
}