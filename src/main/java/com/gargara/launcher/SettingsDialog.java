package com.gargara.launcher;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {
    
    private JComboBox<String> ramComboBox;

    private JComboBox<String> themeComboBox;
    private JCheckBox extrasCheckBox;
    private JFrame parentFrame;

    public SettingsDialog(JFrame parent) {
        super(parent, "Gargara Ayarları", true);
        this.parentFrame = parent;
        setSize(350, 450);
        setLocationRelativeTo(parent);
        setResizable(false);
        initUI();
        ThemeManager.applyTheme(this);
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridLayout(11, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // RAM Settings
        JLabel ramLabel = new JLabel("Oyuna Verilecek RAM Miktarı:");
        ramLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(ramLabel);

        String[] ramOptions = {"2 GB", "3 GB", "4 GB", "6 GB", "8 GB", "12 GB", "16 GB"};
        ramComboBox = new JComboBox<>(ramOptions);
        String currentRamStr = SettingsManager.get("ram", "4096");
        int currentRam = Integer.parseInt(currentRamStr);
        String currentRamGB = (currentRam / 1024) + " GB";
        ramComboBox.setSelectedItem(currentRamGB);
        panel.add(ramComboBox);

        // Theme Settings
        JLabel themeLabel = new JLabel("Tema Seçimi:");
        themeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(themeLabel);

        String[] themeOptions = {ThemeManager.THEME_SYSTEM, ThemeManager.THEME_DARK, ThemeManager.THEME_LIGHT};
        themeComboBox = new JComboBox<>(themeOptions);
        themeComboBox.setSelectedItem(SettingsManager.get("theme", ThemeManager.THEME_SYSTEM));
        panel.add(themeComboBox);

        // Extras Download Settings
        extrasCheckBox = new JCheckBox("Görsel Paketleri (Shader & Kaynak) İndir ve Kur");
        extrasCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
        extrasCheckBox.setSelected(SettingsManager.get("downloadExtras", "false").equals("true"));
        panel.add(extrasCheckBox);

        // Mod Manager Button
        JButton modManagerBtn = new JButton("Görsel Mod Yöneticisi (extra_mods)");
        modManagerBtn.setBackground(new Color(230, 126, 34)); // Orange
        modManagerBtn.setForeground(Color.WHITE);
        modManagerBtn.addActionListener(e -> {
            new ModManagerDialog(this).setVisible(true);
        });
        panel.add(modManagerBtn);

        // Repair Button
        JButton repairBtn = new JButton("Oyun Dosyalarını Onar");
        repairBtn.setBackground(new Color(192, 57, 43)); // Red
        repairBtn.setForeground(Color.WHITE);
        repairBtn.addActionListener(e -> repairFiles());
        panel.add(repairBtn);

        // Debug Console Button
        JButton debugButton = new JButton("Geliştirici Konsolunu Aç");
        debugButton.setBackground(new Color(44, 62, 80));
        debugButton.setForeground(Color.WHITE);
        debugButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> DebugConsole.showConsole());
        });
        panel.add(debugButton);

        // Save Button
        JButton saveButton = new JButton("Kaydet");
        saveButton.setBackground(new Color(41, 128, 185));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> saveSettings());
        panel.add(saveButton);

        add(panel);
    }

    private void repairFiles() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Oyun dosyaları silinip sıfırdan yüklenecek (Kayıtlarınız ve modlarınız güvende). Emin misiniz?", 
            "Onarım Aracı", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String mcDirStr = SettingsManager.get("minecraftDir", System.getenv("APPDATA") + "\\.gargara_launcher");
                java.io.File mcDir = new java.io.File(mcDirStr);
                
                deleteFolder(new java.io.File(mcDir, "versions"));
                deleteFolder(new java.io.File(mcDir, "libraries"));
                deleteFolder(new java.io.File(mcDir, "assets"));
                
                JOptionPane.showMessageDialog(this, "Onarım tamamlandı! Lütfen 'Oyna' butonuna basarak dosyaların yeniden indirilmesini bekleyin.");
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteFolder(java.io.File folder) {
        if (folder.exists()) {
            java.io.File[] files = folder.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isDirectory()) deleteFolder(f);
                    else f.delete();
                }
            }
            folder.delete();
        }
    }

    private void saveSettings() {
        String selectedRam = (String) ramComboBox.getSelectedItem();
        if (selectedRam != null) {
            int gb = Integer.parseInt(selectedRam.replace(" GB", ""));
            int mb = gb * 1024;
            SettingsManager.set("ram", String.valueOf(mb));
        }
        
        String selectedTheme = (String) themeComboBox.getSelectedItem();
        if (selectedTheme != null) {
            SettingsManager.set("theme", selectedTheme);
            ThemeManager.applyTheme(parentFrame);
        }
        
        SettingsManager.set("downloadExtras", extrasCheckBox.isSelected() ? "true" : "false");
        
        dispose();
    }
}
