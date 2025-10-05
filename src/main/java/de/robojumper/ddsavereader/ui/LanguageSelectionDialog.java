package de.robojumper.ddsavereader.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.robojumper.ddsavereader.i18n.Messages;

/**
 * 语言选择对话框
 * 允许用户选择应用程序的显示语言
 */
public class LanguageSelectionDialog extends JDialog {
    
    private static final String PREF_LANGUAGE = "language";
    private static final Preferences prefs = Preferences.userNodeForPackage(LanguageSelectionDialog.class);
    
    private JComboBox<LanguageOption> languageComboBox;
    private boolean confirmed = false;
    
    /**
     * 语言选项类
     */
    private static class LanguageOption {
        private final String code;
        private final String displayName;
        
        public LanguageOption(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
        
        public String getCode() {
            return code;
        }
    }
    
    public LanguageSelectionDialog(JFrame parent) {
        super(parent, "Language / 语言", true);
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // 创建语言选项
        LanguageOption[] languages = {
            new LanguageOption("en", "English"),
            new LanguageOption("zh_CN", "中文 (简体)")
        };
        
        languageComboBox = new JComboBox<>(languages);
        
        // 设置当前选择的语言
        String savedLanguage = prefs.get(PREF_LANGUAGE, "auto");
        if ("auto".equals(savedLanguage)) {
            // 自动检测系统语言
            String systemLang = Locale.getDefault().getLanguage();
            if ("zh".equals(systemLang)) {
                languageComboBox.setSelectedIndex(1); // 中文
            } else {
                languageComboBox.setSelectedIndex(0); // 英文
            }
        } else {
            for (int i = 0; i < languages.length; i++) {
                if (languages[i].getCode().equals(savedLanguage)) {
                    languageComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        
        // 主面板
        JPanel mainPanel = new JPanel(new FlowLayout());
        mainPanel.add(new JLabel("Select Language / 选择语言:"));
        mainPanel.add(languageComboBox);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton okButton = new JButton("OK / 确定");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });
        
        JButton cancelButton = new JButton("Cancel / 取消");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    /**
     * 显示语言选择对话框
     * @return 是否确认选择
     */
    public boolean showDialog() {
        setVisible(true);
        return confirmed;
    }
    
    /**
     * 获取选择的语言代码
     */
    public String getSelectedLanguage() {
        LanguageOption selected = (LanguageOption) languageComboBox.getSelectedItem();
        return selected != null ? selected.getCode() : "en";
    }
    
    /**
     * 保存语言选择
     */
    public void saveLanguagePreference() {
        prefs.put(PREF_LANGUAGE, getSelectedLanguage());
    }
    
    /**
     * 获取保存的语言偏好
     */
    public static String getSavedLanguagePreference() {
        return prefs.get(PREF_LANGUAGE, "auto");
    }
    
    /**
     * 根据保存的偏好设置语言
     */
    public static void initializeLanguage() {
        String savedLanguage = getSavedLanguagePreference();
        
        if ("auto".equals(savedLanguage)) {
            // 使用系统默认语言
            return;
        }
        
        Locale locale;
        switch (savedLanguage) {
            case "zh_CN":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "en":
            default:
                locale = Locale.ENGLISH;
                break;
        }
        
        Messages.setLocale(locale);
    }
}