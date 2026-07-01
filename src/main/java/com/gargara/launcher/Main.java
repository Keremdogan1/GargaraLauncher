package com.gargara.launcher;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        
        Color primaryColor = new Color(41, 128, 185);
        UIManager.put("ProgressBar.foreground", primaryColor);
        
        DebugConsole.initialize();
        DiscordPresence.start();
        
        System.out.println("Gargara Launcher Başlatılıyor...");
        
        SwingUtilities.invokeLater(() -> {
            if (SettingsManager.isRegistered()) {
                LauncherGUI gui = new LauncherGUI();
                gui.setVisible(true);
            } else {
                RegistrationGUI gui = new RegistrationGUI();
                gui.setVisible(true);
            }
            
            // Arka planda güncelleme kontrolü yap
            new Thread(() -> UpdateManager.checkUpdate()).start();
        });
    }
}
