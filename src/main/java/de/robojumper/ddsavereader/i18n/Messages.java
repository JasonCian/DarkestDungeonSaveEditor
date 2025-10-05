package de.robojumper.ddsavereader.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 国际化消息管理类
 * 用于管理应用程序的多语言支持
 */
public class Messages {
    
    private static final String BUNDLE_NAME = "de.robojumper.ddsavereader.i18n.messages";
    private static ResourceBundle resourceBundle;
    
    static {
        // 默认使用系统语言，如果系统是中文则使用中文，否则使用英文
        Locale locale = Locale.getDefault();
        if (locale.getLanguage().equals("zh")) {
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.SIMPLIFIED_CHINESE);
        } else {
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
        }
    }
    
    /**
     * 获取指定键的本地化消息
     * @param key 消息键
     * @return 本地化后的消息
     */
    public static String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            // 如果找不到键，返回键本身作为后备
            return key;
        }
    }
    
    /**
     * 获取格式化的本地化消息
     * @param key 消息键
     * @param args 格式化参数
     * @return 格式化后的本地化消息
     */
    public static String getString(String key, Object... args) {
        try {
            String message = resourceBundle.getString(key);
            return String.format(message, args);
        } catch (Exception e) {
            return key;
        }
    }
    
    /**
     * 设置语言环境
     * @param locale 语言环境
     */
    public static void setLocale(Locale locale) {
        resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }
    
    /**
     * 获取当前语言环境
     * @return 当前语言环境
     */
    public static Locale getCurrentLocale() {
        return resourceBundle.getLocale();
    }
}