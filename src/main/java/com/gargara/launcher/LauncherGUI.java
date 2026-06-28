package com.gargara.launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class LauncherGUI extends JFrame {

    private JTextField usernameField;
    private JButton playButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    public LauncherGUI() {
        setTitle("Gargara Launcher");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Pencere ve görev çubuğu ikonu (logo.png)
        try {
            java.net.URL iconURL = getClass().getResource("/logo.png");
            if (iconURL != null) {
                setIconImage(new ImageIcon(iconURL).getImage());
            }
        } catch (Exception ignored) {}

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Logo Alanı
        JLabel logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            java.net.URL imgURL = getClass().getResource("/logo.png");
            if (imgURL != null) {
                ImageIcon icon = new ImageIcon(imgURL);
                Image img = icon.getImage();
                Image newimg = img.getScaledInstance(150, 150, java.awt.Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(newimg));
            } else {
                logoLabel.setText("GARGARA SUNUCUSU");
                logoLabel.setFont(new Font("Arial", Font.BOLD, 28));
                logoLabel.setForeground(new Color(41, 128, 185));
            }
        } catch (Exception e) {
            logoLabel.setText("GARGARA SUNUCUSU");
        }
        mainPanel.add(logoLabel, BorderLayout.NORTH);

        // Orta Panel (Kullanıcı Adı Girişi)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(3, 1, 5, 5));

        JLabel infoLabel = new JLabel("Kullanıcı Adı (Oyundaki isminiz):");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(infoLabel);

        usernameField = new JTextField();
        usernameField.setHorizontalAlignment(JTextField.CENTER);
        usernameField.setFont(new Font("Arial", Font.BOLD, 16));
        centerPanel.add(usernameField);

        // Alt Panel (İlerleme Çubuğu ve Durum)
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("Giriş bekleniyor...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        progressPanel.add(progressBar, BorderLayout.NORTH);
        progressPanel.add(statusLabel, BorderLayout.SOUTH);
        centerPanel.add(progressPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Oyna Butonu
        playButton = new JButton("OYNA");
        playButton.setFont(new Font("Arial", Font.BOLD, 20));
        playButton.setBackground(new Color(46, 204, 113)); // Yeşil oyna butonu
        playButton.setForeground(Color.WHITE);
        playButton.setPreferredSize(new Dimension(100, 50));
        playButton.addActionListener(this::onPlay);
        mainPanel.add(playButton, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void onPlay(ActionEvent e) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lütfen bir kullanıcı adı girin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }

        playButton.setEnabled(false);
        usernameField.setEnabled(false);
        
        // Klasör ~/.gargara_launcher olacak
        String path = LauncherLogic.getDefaultMinecraftDir();

        Thread installThread = new Thread(() -> {
            try {
                LauncherLogic.launchGame(username, path, new LauncherLogic.ProgressCallback() {
                    @Override
                    public void updateProgress(int percentage, String status) {
                        SwingUtilities.invokeLater(() -> {
                            if (percentage < 0) {
                                progressBar.setIndeterminate(true);
                                progressBar.setStringPainted(false);
                            } else {
                                progressBar.setIndeterminate(false);
                                progressBar.setStringPainted(true);
                                progressBar.setValue(percentage);
                            }
                            statusLabel.setText(status);
                        });
                    }

                    @Override
                    public void onGameExit() {
                        SwingUtilities.invokeLater(() -> {
                            playButton.setEnabled(true);
                            usernameField.setEnabled(true);
                            playButton.setText("OYNA");
                            statusLabel.setText("Oyun kapandı. Yeniden oynamak için tıklayın.");
                            progressBar.setValue(0);
                        });
                    }
                });
                
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Oyun arka planda çalışıyor...");
                    playButton.setText("OYUNDA");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Bir hata oluştu:\n" + ex.getMessage(), 
                        "Hata", JOptionPane.ERROR_MESSAGE);
                    playButton.setEnabled(true);
                    usernameField.setEnabled(true);
                    statusLabel.setText("Hata oluştu.");
                    playButton.setText("TEKRAR DENE");
                });
                ex.printStackTrace();
            }
        });
        installThread.start();
    }
}
