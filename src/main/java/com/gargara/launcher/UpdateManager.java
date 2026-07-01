package com.gargara.launcher;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {
    public static final String CURRENT_VERSION = LauncherGUI.LAUNCHER_VERSION.replace("v", "");
    private static final String API_URL = "https://api.github.com/repos/Keremdogan1/GargaraLauncher/releases";

    public static void checkUpdate() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                // Basit bir sekilde json array icindeki objeleri bolelim (Gson kullanmadigimiz icin)
                int releaseIdx = 0;
                while ((releaseIdx = json.indexOf("\"tag_name\"", releaseIdx)) != -1) {
                    int colonIndex = json.indexOf(":", releaseIdx);
                    int quoteStart = json.indexOf("\"", colonIndex);
                    int quoteEnd = json.indexOf("\"", quoteStart + 1);
                    if (quoteStart != -1 && quoteEnd != -1) {
                        String tagName = json.substring(quoteStart + 1, quoteEnd);
                        if (!tagName.startsWith("mods-") && !tagName.startsWith("extras-")) {
                            // Bu bir Launcher surumu
                            String strippedTag = tagName.replace("v", "");
                            if (!strippedTag.equals(CURRENT_VERSION)) {
                                // Exe dosyasini sadece bu release blogu icinde ara
                                int nextRelease = json.indexOf("\"tag_name\"", quoteEnd);
                                if (nextRelease == -1) nextRelease = json.length();
                                String releaseBlock = json.substring(quoteEnd, nextRelease);
                                String downloadUrl = extractExeDownloadUrl(releaseBlock);
                                if (downloadUrl != null) {
                                    showUpdateDialog(strippedTag, downloadUrl);
                                    return; // Buldugumuz ilk launcher guncellemesini sorduk
                                }
                            } else {
                                // Eger en son launcher surumu su anki surumse guncelleme yok
                                return;
                            }
                        }
                    }
                    releaseIdx = quoteEnd;
                }
            }
        } catch (Exception e) {
            System.err.println("Guncelleme kontrolu basarisiz: " + e.getMessage());
        }
    }

    private static String extractExeDownloadUrl(String block) {
        String searchKey = "\"browser_download_url\"";
        int idx = 0;
        while ((idx = block.indexOf(searchKey, idx)) != -1) {
            int colonIndex = block.indexOf(":", idx);
            int quoteStart = block.indexOf("\"", colonIndex);
            int quoteEnd = block.indexOf("\"", quoteStart + 1);
            if (quoteStart != -1 && quoteEnd != -1) {
                String url = block.substring(quoteStart + 1, quoteEnd);
                if (url.toLowerCase().endsWith(".exe")) {
                    return url;
                }
            }
            idx = quoteEnd;
        }
        return null;
    }

    private static void showUpdateDialog(String newVersion, String downloadUrl) {
        int choice = JOptionPane.showConfirmDialog(null,
                "Yeni bir Gargara Launcher sürümü bulundu! (v" + newVersion + ")\nŞu anki sürüm: v" + CURRENT_VERSION + "\n\nŞimdi indirip kurmak ister misiniz?",
                "Güncelleme Mevcut!",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            downloadAndUpdate(downloadUrl);
        }
    }

    private static void downloadAndUpdate(String downloadUrl) {
        JFrame progressFrame = new JFrame("Güncelleniyor...");
        progressFrame.setSize(400, 100);
        progressFrame.setLocationRelativeTo(null);
        progressFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        progressFrame.setLayout(new java.awt.BorderLayout(10, 10));

        JLabel infoLabel = new JLabel("Yeni sürüm indiriliyor, lütfen bekleyin...", SwingConstants.CENTER);
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        progressFrame.add(infoLabel, java.awt.BorderLayout.NORTH);
        progressFrame.add(progressBar, java.awt.BorderLayout.CENTER);
        progressFrame.setVisible(true);

        new Thread(() -> {
            try {
                File tempFile = File.createTempFile("GargaraSetup_Update", ".exe");
                
                HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                conn.setInstanceFollowRedirects(true);
                long totalSize = conn.getContentLengthLong();

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[65536];
                    long downloaded = 0;
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;
                        if (totalSize > 0) {
                            int percent = (int) ((downloaded * 100) / totalSize);
                            SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
                        }
                    }
                }

                // Çalıştır ve çık!
                infoLabel.setText("Kurulum başlatılıyor...");
                // UAC (Yönetici İzni) tetiklenmesi için cmd /c ile başlatıyoruz.
                new ProcessBuilder("cmd", "/c", "start", "", tempFile.getAbsolutePath()).start();
                System.exit(0);

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    progressFrame.dispose();
                    JOptionPane.showMessageDialog(null, "Güncelleme indirilirken hata oluştu:\n" + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
}
