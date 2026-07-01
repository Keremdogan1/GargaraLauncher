package com.gargara.launcher;

import java.io.*;
import java.util.Properties;

public class SettingsManager {
    private static final String SETTINGS_FILE = LauncherLogic.getDefaultMinecraftDir() + File.separator + "settings.properties";
    private static Properties properties = new Properties();

    static {
        loadSettings();
    }

    private static void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveSettings() {
        File file = new File(SETTINGS_FILE);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, "Gargara Launcher Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static void set(String key, String value) {
        properties.setProperty(key, value);
        saveSettings();
    }

    public static boolean isRegistered() {
        return "true".equals(get("registered", "false"));
    }

    public static void setRegistered(boolean registered) {
        set("registered", String.valueOf(registered));
    }
}
