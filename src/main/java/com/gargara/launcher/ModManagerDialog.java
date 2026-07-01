package com.gargara.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModManagerDialog extends JDialog {
    
    private List<JCheckBox> modCheckboxes = new ArrayList<>();
    private List<File> modFiles = new ArrayList<>();

    public ModManagerDialog(JDialog parent) {
        super(parent, "Görsel Mod Yöneticisi", true);
        setSize(400, 500);
        setLocationRelativeTo(parent);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Aktif Etmek İstediğiniz Modları Seçin");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JButton openFolderBtn = new JButton("Ekstra Mod Klasörünü Aç");
        openFolderBtn.setBackground(new Color(155, 89, 182));
        openFolderBtn.setForeground(Color.WHITE);
        openFolderBtn.addActionListener(e -> {
            try {
                String mcDir = SettingsManager.get("minecraftDir", System.getenv("APPDATA") + "\\.gargara_launcher");
                File extraDir = new File(mcDir, "extra_mods");
                if (!extraDir.exists()) extraDir.mkdirs();
                Desktop.getDesktop().open(extraDir);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(openFolderBtn, BorderLayout.SOUTH);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        String mcDir = SettingsManager.get("minecraftDir", System.getenv("APPDATA") + "\\.gargara_launcher");
        File extraModsDir = new File(mcDir, "extra_mods");
        
        if (extraModsDir.exists() && extraModsDir.isDirectory()) {
            File[] files = extraModsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".jar") || name.endsWith(".jar.disabled")) {
                        boolean isEnabled = name.endsWith(".jar");
                        String displayName = isEnabled ? f.getName() : f.getName().replace(".disabled", "");
                        
                        JCheckBox cb = new JCheckBox(displayName, isEnabled);
                        modCheckboxes.add(cb);
                        modFiles.add(f);
                        listPanel.add(cb);
                    }
                }
            }
        }

        if (modCheckboxes.isEmpty()) {
            listPanel.add(new JLabel("Ekstra mod bulunamadı. (extra_mods klasörü boş)"));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JButton saveBtn = new JButton("Kaydet ve Kapat");
        saveBtn.setBackground(new Color(46, 204, 113));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> saveMods());
        mainPanel.add(saveBtn, BorderLayout.SOUTH);

        ThemeManager.applyTheme(this);
        add(mainPanel);
    }

    private void saveMods() {
        for (int i = 0; i < modCheckboxes.size(); i++) {
            JCheckBox cb = modCheckboxes.get(i);
            File f = modFiles.get(i);
            
            boolean shouldBeEnabled = cb.isSelected();
            boolean isCurrentlyEnabled = f.getName().toLowerCase().endsWith(".jar");
            
            if (shouldBeEnabled && !isCurrentlyEnabled) {
                File newFile = new File(f.getParent(), f.getName().replace(".disabled", ""));
                f.renameTo(newFile);
            } else if (!shouldBeEnabled && isCurrentlyEnabled) {
                File newFile = new File(f.getParent(), f.getName() + ".disabled");
                f.renameTo(newFile);
            }
        }
        dispose();
    }
}
