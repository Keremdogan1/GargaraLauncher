package com.gargara.launcher;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ThemeManager {
    
    public static final String THEME_DARK = "Karanlık Tema";
    public static final String THEME_LIGHT = "Aydınlık Tema";
    public static final String THEME_SYSTEM = "Sistem Teması";

    public static void applyTheme(Window window) {
        String currentTheme = SettingsManager.get("theme", THEME_SYSTEM);
        boolean isDark;

        if (THEME_SYSTEM.equals(currentTheme)) {
            isDark = isSystemDarkTheme();
        } else {
            isDark = THEME_DARK.equals(currentTheme);
        }

        try {
            if (isDark) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            
            // Re-apply custom properties after LookAndFeel change
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            Color primaryColor = new Color(41, 128, 185);
            UIManager.put("ProgressBar.foreground", primaryColor);

            if (window != null) {
                SwingUtilities.updateComponentTreeUI(window);
                
                // If there are other open windows (like DebugConsole), update them too
                for (Window w : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(w);
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
    }

    public static boolean isSystemDarkTheme() {
        try {
            Process process = Runtime.getRuntime().exec("reg query HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize /v AppsUseLightTheme");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("0x0")) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }
}
