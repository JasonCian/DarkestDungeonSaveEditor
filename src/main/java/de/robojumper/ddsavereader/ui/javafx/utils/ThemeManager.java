package de.robojumper.ddsavereader.ui.javafx.utils;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

/**
 * 主题管理器
 * 负责应用程序主题的切换和保存
 */
public class ThemeManager {
    
    private static final String THEME_PREFERENCE_KEY = "application.theme";
    private static final String LIGHT_THEME = "light";
    private static final String DARK_THEME = "dark";
    
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    
    /**
     * 应用主题到场景
     * @param scene 要应用主题的场景
     * @param isDark 是否为深色主题
     */
    public static void applyTheme(Scene scene, boolean isDark) {
        if (isDark) {
            scene.getRoot().getStyleClass().add("dark-theme");
        } else {
            scene.getRoot().getStyleClass().remove("dark-theme");
        }
        saveThemePreference(isDark);
    }
    
    /**
     * 切换主题
     * @param scene 要切换主题的场景
     */
    public static void toggleTheme(Scene scene) {
        boolean isDarkTheme = scene.getRoot().getStyleClass().contains("dark-theme");
        applyTheme(scene, !isDarkTheme);
    }
    
    /**
     * 获取保存的主题偏好
     * @return 是否为深色主题
     */
    public static boolean getSavedThemePreference() {
        String theme = prefs.get(THEME_PREFERENCE_KEY, LIGHT_THEME);
        return DARK_THEME.equals(theme);
    }
    
    /**
     * 保存主题偏好
     * @param isDark 是否为深色主题
     */
    private static void saveThemePreference(boolean isDark) {
        prefs.put(THEME_PREFERENCE_KEY, isDark ? DARK_THEME : LIGHT_THEME);
    }
    
    /**
     * 应用保存的主题
     * @param scene 要应用主题的场景
     */
    public static void applySavedTheme(Scene scene) {
        applyTheme(scene, getSavedThemePreference());
    }
}