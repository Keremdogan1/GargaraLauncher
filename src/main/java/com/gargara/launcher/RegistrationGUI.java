package com.gargara.launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RegistrationGUI extends JFrame {

    private JTextField nameField;
    private JTextField emailField;
    private JComboBox<String> dayCombo;
    private JComboBox<String> monthCombo;
    private JComboBox<String> yearCombo;
    private JButton submitButton;

    public RegistrationGUI() {
        setTitle("Gargara Sunucusu Kayıt v" + UpdateManager.CURRENT_VERSION);
        setSize(550, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        try {
            java.net.URL iconURL = getClass().getResource("/logo.png");
            if (iconURL != null) {
                setIconImage(new ImageIcon(iconURL).getImage());
            }
        } catch (Exception ignored) {}

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Header
        JLabel headerLabel = new JLabel("Hoş Geldiniz!");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setForeground(new Color(41, 128, 185));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        
        formPanel.add(new JLabel("Size hitap edebilmemiz için isminizi öğrenebilir miyiz?"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Sizinle iletişime geçebilmemiz için e-posta adresiniz?"));
        emailField = new JTextField();
        formPanel.add(emailField);

        formPanel.add(new JLabel("Doğum Tarihiniz?"));
        JPanel datePanel = new JPanel(new GridLayout(1, 3, 5, 0));
        
        String[] days = new String[31];
        for(int i=0; i<31; i++) days[i] = String.format("%02d", i+1);
        dayCombo = new JComboBox<>(days);

        String[] months = new String[12];
        for(int i=0; i<12; i++) months[i] = String.format("%02d", i+1);
        monthCombo = new JComboBox<>(months);

        String[] years = new String[80];
        int currentYear = 2026;
        for(int i=0; i<80; i++) years[i] = String.valueOf(currentYear - i);
        yearCombo = new JComboBox<>(years);

        datePanel.add(dayCombo);
        datePanel.add(monthCombo);
        datePanel.add(yearCombo);
        formPanel.add(datePanel);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Submit Button
        submitButton = new JButton("Kayıt Ol ve Devam Et");
        submitButton.setFont(new Font("Arial", Font.BOLD, 16));
        submitButton.setBackground(new Color(46, 204, 113));
        submitButton.setForeground(Color.WHITE);
        submitButton.setPreferredSize(new Dimension(100, 45));
        submitButton.addActionListener(this::onSubmit);
        
        mainPanel.add(submitButton, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void onSubmit(ActionEvent e) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String day = (String) dayCombo.getSelectedItem();
        String month = (String) monthCombo.getSelectedItem();
        String year = (String) yearCombo.getSelectedItem();

        if (name.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lütfen tüm alanları doldurun.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Kaydediliyor...");

        new Thread(() -> {
            boolean success = RegistrationManager.submitForm(name, year, month, day, email);
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    SettingsManager.setRegistered(true);
                    SettingsManager.set("username", name);
                    
                    LauncherGUI launcher = new LauncherGUI();
                    launcher.setVisible(true);
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Kayıt olurken bir hata oluştu. Lütfen internet bağlantınızı kontrol edin.", "Hata", JOptionPane.ERROR_MESSAGE);
                    submitButton.setEnabled(true);
                    submitButton.setText("Kayıt Ol ve Devam Et");
                }
            });
        }).start();
    }
}
