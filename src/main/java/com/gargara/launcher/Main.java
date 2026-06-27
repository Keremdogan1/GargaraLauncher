package com.gargara.launcher;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Tema ayarları
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        
        // Ana renkleri ayarla
        Color primaryColor = new Color(41, 128, 185);
        UIManager.put("ProgressBar.foreground", primaryColor);
        
        SwingUtilities.invokeLater(() -> {
            LauncherGUI gui = new LauncherGUI();
            gui.setVisible(true);
        });
    }
}
