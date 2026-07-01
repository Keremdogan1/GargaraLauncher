package com.gargara.launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class LauncherGUI extends JFrame {
    
    public static final String LAUNCHER_VERSION = "v3.2.5";

    private JTextField usernameField;
    private JButton playButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel serverStatusLabel;
    private JLabel logoLabel;
    private List<Image> slideImages = new ArrayList<>();
    private int currentSlideIndex = 0;
    private Timer slideshowTimer;

    public LauncherGUI() {
        setTitle("Gargara Launcher v" + UpdateManager.CURRENT_VERSION);
        setSize(800, 550);
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

        // Check for extras.flag created by Inno Setup
        File extrasFlag = new File(LauncherLogic.getDefaultMinecraftDir(), "extras.flag");
        if (extrasFlag.exists()) {
            SettingsManager.set("downloadExtras", "true");
            extrasFlag.delete();
        }

        initUI();
        ThemeManager.applyTheme(this);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Logo Alanı
        logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setPreferredSize(new Dimension(800, 250));
        
        try {
            java.net.URL imgURL = getClass().getResource("/logo.png");
            if (imgURL != null) {
                ImageIcon icon = new ImageIcon(imgURL);
                Image img = icon.getImage();
                Image newimg = img.getScaledInstance(-1, 250, java.awt.Image.SCALE_SMOOTH);
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

        // Orta Panel
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel infoLabel = new JLabel("Kullanıcı Adı (Oyundaki isminiz):");
        infoLabel.setFont(new Font("Arial", Font.BOLD, 18));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(infoLabel);

        usernameField = new JTextField(SettingsManager.get("username", ""));
        usernameField.setFont(new Font("Arial", Font.BOLD, 16));
        usernameField.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(usernameField);

        // Alt Panel (İlerleme Çubuğu ve Durum)
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        
        serverStatusLabel = new JLabel("Sunucu Durumu: Sorgulanıyor...");
        serverStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        serverStatusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("Giriş bekleniyor...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        progressPanel.add(serverStatusLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(statusLabel, BorderLayout.SOUTH);
        centerPanel.add(progressPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Sunucu Durumunu Anlık Sorgula (Her 30 saniyede bir)
        Timer pingerTimer = new Timer(30000, e -> pingServerStatus());
        pingerTimer.setInitialDelay(0); // İlk açılışta hemen çalışsın
        pingerTimer.start();

        // Alt Butonlar Paneli
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setOpaque(false);
        
        // Ayarlar Butonu
        JButton settingsButton = new JButton("Ayarlar");
        settingsButton.setFont(new Font("Arial", Font.BOLD, 14));
        settingsButton.setBackground(new Color(149, 165, 166));
        settingsButton.setForeground(Color.WHITE);
        settingsButton.setPreferredSize(new Dimension(100, 50));
        settingsButton.addActionListener(e -> {
            SettingsDialog dialog = new SettingsDialog(this);
            dialog.setVisible(true);
        });
        
        // Oyna Butonu
        playButton = new JButton("OYNA");
        playButton.setFont(new Font("Arial", Font.BOLD, 20));
        playButton.setBackground(new Color(46, 204, 113)); // Yeşil oyna butonu
        playButton.setForeground(Color.WHITE);
        playButton.setPreferredSize(new Dimension(100, 50));
        playButton.addActionListener(this::onPlay);
        
        JLabel versionLabel = new JLabel(LAUNCHER_VERSION);
        versionLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        versionLabel.setForeground(Color.GRAY);
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        versionLabel.setPreferredSize(new Dimension(100, 50));

        bottomPanel.add(settingsButton, BorderLayout.WEST);
        bottomPanel.add(playButton, BorderLayout.CENTER);
        bottomPanel.add(versionLabel, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Haberler Paneli
        JEditorPane newsPane = new JEditorPane();
        newsPane.setEditable(false);
        newsPane.setContentType("text/html");
        newsPane.setText("<html><body style='font-family:Arial; padding:5px;'><h3>Sunucu Haberleri</h3><p>Haberler yükleniyor...</p></body></html>");
        JScrollPane newsScroll = new JScrollPane(newsPane);
        newsScroll.setPreferredSize(new Dimension(250, 0));
        newsScroll.setBorder(BorderFactory.createTitledBorder("Duyurular & Haberler"));
        mainPanel.add(newsScroll, BorderLayout.EAST);

        add(mainPanel);

        // Arka planda haberleri çek
        fetchNews(newsPane);

        // Arka planda slayt gösterisini başlat
        startSlideshow();
    }

    private void fetchNews(JEditorPane newsPane) {
        new Thread(() -> {
            try {
                String driveId = "1uForYvzbvogIn9wyETpWsBeWAmSCSNSS";
                if (!driveId.equals("BURAYA_HABERLER_TXT_ID_GELECEK")) {
                    File tempNews = File.createTempFile("gargara_news", ".html");
                    LauncherLogic.downloadFromGoogleDrive(driveId, tempNews, null);
                    if (tempNews.exists()) {
                        String newsContent = java.nio.file.Files.readString(tempNews.toPath());
                        SwingUtilities.invokeLater(() -> newsPane.setText(newsContent));
                        tempNews.delete();
                    }
                } else {
                    SwingUtilities.invokeLater(() -> newsPane.setText("<html><body style='font-family:Arial; padding:5px;'>Haber sistemi ayarlanmadı.</body></html>"));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> newsPane.setText("<html><body style='font-family:Arial; padding:5px; color:red;'>Haberler alınamadı.</body></html>"));
            }
        }).start();
    }

    private void startSlideshow() {
        new Thread(() -> {
            try {
                String baseDir = LauncherLogic.getDefaultMinecraftDir();
                File slidesDir = new File(baseDir, "slides");
                File slidesZip = new File(baseDir, "slides.zip");

                // Eğer slides klasörü yoksa veya içi boşsa, zip'i indir ve çıkar
                if (!slidesDir.exists() || slidesDir.listFiles() == null || slidesDir.listFiles().length == 0) {
                    if (!slidesDir.exists()) slidesDir.mkdirs();
                    
                    LauncherLogic.downloadFromGoogleDrive("1qlCXyuGXLm2HW6nxUOibxIqqy0QHkLr1", slidesZip, null);
                    LauncherLogic.extractZip(slidesZip, slidesDir);
                    slidesZip.delete();
                }

                // Resimleri belleğe yükle
                File[] files = slidesDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File f : files) {
                        if (f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".jpg")) {
                            Image img = ImageIO.read(f);
                            if (img != null) {
                                slideImages.add(img);
                            }
                        }
                    }
                }

                // Resimler varsa Timer başlat
                if (!slideImages.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        updateSlide();
                        slideshowTimer = new Timer(3000, e -> {
                            currentSlideIndex = (currentSlideIndex + 1) % slideImages.size();
                            updateSlide();
                        });
                        slideshowTimer.start();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateSlide() {
        Image img = slideImages.get(currentSlideIndex);
        Image newimg = img.getScaledInstance(-1, 250, java.awt.Image.SCALE_SMOOTH);
        logoLabel.setIcon(new ImageIcon(newimg));
    }

    private void onPlay(ActionEvent e) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lütfen bir kullanıcı adı girin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]{3,16}$")) {
            JOptionPane.showMessageDialog(this, "Kullanıcı adı sadece İngilizce harfler, rakamlar ve alt çizgi (_) içerebilir!\nUzunluk: 3-16 karakter.\n(Lütfen Türkçe karakter kullanmayın: ğ, ü, ş, ı, ö, ç vs.)", "Geçersiz Kullanıcı Adı", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SettingsManager.set("username", username);
        
        playButton.setEnabled(false);
        usernameField.setEnabled(false);
        
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
                    public void onGameLaunch() {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Oyun arka planda çalışıyor...");
                            playButton.setText("OYUNDA");
                            DiscordPresence.updatePresence("Oyunda", "Gargara Sunucusunda");
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
                            DiscordPresence.updatePresence("Launcher'da", "Bekliyor");
                        });
                    }
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
                    DiscordPresence.updatePresence("Launcher'da", "Hata Oluştu");
                });
                ex.printStackTrace();
            }
        });
        installThread.start();
    }

    private void pingServerStatus() {
        ServerPinger.pingServer("130.61.80.148:25565", (online, current, max) -> {
            SwingUtilities.invokeLater(() -> {
                if (online) {
                    serverStatusLabel.setText("● Çevrimiçi: " + current + " / " + max + " Oyuncu");
                    serverStatusLabel.setForeground(new Color(46, 204, 113));
                } else {
                    serverStatusLabel.setText("● Sunucu Kapalı veya Ulaşılamıyor");
                    serverStatusLabel.setForeground(Color.RED);
                }
            });
        });
    }
}
