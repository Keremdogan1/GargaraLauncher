package com.gargara.launcher;

import org.to2mbn.jmccc.mcdownloader.MinecraftDownloader;
import org.to2mbn.jmccc.mcdownloader.MinecraftDownloaderBuilder;
import org.to2mbn.jmccc.option.LaunchOption;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.option.ServerInfo;
import org.to2mbn.jmccc.auth.OfflineAuthenticator;
import org.to2mbn.jmccc.mcdownloader.download.concurrent.CallbackAdapter;
import org.to2mbn.jmccc.version.Version;
import org.to2mbn.jmccc.launch.LauncherBuilder;
import org.to2mbn.jmccc.launch.Launcher;
import org.to2mbn.jmccc.launch.ProcessListener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LauncherLogic {

    public static final String MC_VERSION = "1.20.1";
    public static final String FABRIC_VERSION = "0.17.2";
    public static final String FABRIC_VERSION_ID = "fabric-loader-" + FABRIC_VERSION + "-" + MC_VERSION;
    public static final String MODS_DRIVE_FILE_ID = "1kKMhQ4JDM3gf-XeS0VGmAwAsWwqK1W-j";

    public static String getDefaultMinecraftDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        if (os.contains("win")) {
            return System.getenv("APPDATA") + "\\.gargara_launcher";
        } else if (os.contains("mac")) {
            return userHome + "/Library/Application Support/gargara_launcher";
        } else {
            return userHome + "/.gargara_launcher";
        }
    }

    public static void launchGame(String username, String customPath, ProgressCallback callback) throws Exception {
        File mcDir = new File(customPath);
        if (!mcDir.exists()) {
            mcDir.mkdirs();
        }
        MinecraftDirectory dir = new MinecraftDirectory(customPath);

        // 1. Modları İndir ve Kur (Google Drive veya gömülü)
        callback.updateProgress(5, "Modlar kontrol ediliyor...");
        downloadAndInstallMods(mcDir, callback);

        // 2. Fabric Installer'ı indir ve çalıştır
        callback.updateProgress(20, "Oyun motoru (Fabric) ayarlanıyor...");
        setupFabric(mcDir);

        // 3. Oyun Dosyalarını İndir (kayan animasyonlu)
        callback.updateProgress(-1, "Oyun dosyaları indiriliyor... (Bu işlem biraz sürebilir)");
        downloadGame(dir, callback);

        // ReplayMod ayarlarını yap (otomatik kaydı kapat)
        configureReplayMod(mcDir);

        // 4. Oyunu Başlat
        callback.updateProgress(90, "Oyun başlatılıyor...");
        startGame(dir, username);
        
        callback.updateProgress(100, "Oyun açıldı. Pencereyi kapatabilirsiniz.");
    }

    private static void configureReplayMod(File mcDir) {
        try {
            File configDir = new File(mcDir, "config");
            if (!configDir.exists()) configDir.mkdirs();
            
            File replayConfig = new File(configDir, "replaymod.json");
            if (!replayConfig.exists()) {
                // Replay mod otomatik kayıt özelliğini kapatıyoruz
                String json = "{\n  \"recording\": {\n    \"autoStartServer\": false,\n    \"autoStartSingleplayer\": false\n  }\n}";
                Files.writeString(replayConfig.toPath(), json);
            }
        } catch (Exception ignored) {}
    }

    private static void downloadAndInstallMods(File mcDir, ProgressCallback callback) throws Exception {
        File modsDir = new File(mcDir, "mods");
        File markerFile = new File(mcDir, ".mods_installed");

        // Modlar zaten kuruluysa atla
        if (modsDir.exists() && modsDir.listFiles() != null
                && modsDir.listFiles().length > 0 && markerFile.exists()) {
            callback.updateProgress(10, "Modlar zaten kurulu, atlanıyor...");
            return;
        }

        File modsZip;

        // Önce JAR içinde gömülü mods.zip var mı kontrol et (offline dağıtım)
        InputStream embeddedMods = LauncherLogic.class.getResourceAsStream("/mods.zip");
        if (embeddedMods != null) {
            callback.updateProgress(8, "Gömülü modlar çıkartılıyor...");
            modsZip = new File(mcDir, "mods_temp.zip");
            try (FileOutputStream fos = new FileOutputStream(modsZip)) {
                byte[] buffer = new byte[65536];
                int len;
                while ((len = embeddedMods.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            } finally {
                embeddedMods.close();
            }
        } else {
            // Google Drive'dan indir
            callback.updateProgress(5, "Modlar Google Drive'dan indiriliyor...");
            modsZip = new File(mcDir, "mods_download.zip");
            if (!modsZip.exists()) {
                downloadFromGoogleDrive(MODS_DRIVE_FILE_ID, modsZip, callback);
            }
        }

        // ZIP'i mods klasörüne çıkart
        callback.updateProgress(25, "Modlar kurulum klasörüne çıkartılıyor...");
        if (!modsDir.exists()) {
            modsDir.mkdirs();
        }
        extractZip(modsZip, modsDir);

        // Kurulum tamamlandı işareti
        Files.writeString(markerFile.toPath(), "v1");

        // Geçici zip dosyasını temizle
        modsZip.delete();
        callback.updateProgress(28, "Modlar başarıyla kuruldu!");
    }

    private static void downloadFromGoogleDrive(String fileId, File outputFile, ProgressCallback callback) throws Exception {
        String downloadUrl = "https://drive.usercontent.google.com/download?id=" + fileId
                + "&export=download&authuser=0&confirm=t";

        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            conn.disconnect();
            throw new Exception("Google Drive'dan indirme başarısız. HTTP: " + responseCode
                    + "\nDosyanın herkese açık paylaşıldığından emin olun.");
        }

        long totalSize = conn.getContentLengthLong();

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[65536]; // 64KB buffer
            long downloaded = 0;
            int bytesRead;
            long lastUIUpdate = System.currentTimeMillis();

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                long now = System.currentTimeMillis();
                if (now - lastUIUpdate > 250) {
                    lastUIUpdate = now;
                    String downloadedMB = String.format("%.1f", downloaded / 1048576.0);
                    if (totalSize > 0) {
                        int percent = (int) (5 + (downloaded * 20 / totalSize));
                        String totalMB = String.format("%.1f", totalSize / 1048576.0);
                        callback.updateProgress(percent,
                                "Modlar indiriliyor: " + downloadedMB + " MB / " + totalMB + " MB");
                    } else {
                        callback.updateProgress(-1, "Modlar indiriliyor: " + downloadedMB + " MB");
                    }
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void extractZip(File zipFile, File destDir) throws Exception {
        Path destPath = destDir.toPath().toRealPath();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                Path newFilePath = newFile.toPath().normalize();

                // Zip Slip güvenlik kontrolü
                if (!newFilePath.startsWith(destPath)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void setupFabric(File mcDir) throws Exception {
        File fabricVersionDir = new File(mcDir, "versions/" + FABRIC_VERSION_ID);
        if (new File(fabricVersionDir, FABRIC_VERSION_ID + ".json").exists()) {
            return; // Zaten kurulu
        }

        File installerJar = new File(mcDir, "fabric-installer.jar");
        if (!installerJar.exists()) {
            try (InputStream in = new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar").openStream();
                 FileOutputStream out = new FileOutputStream(installerJar)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }

        // Fabric Installer'ı çalıştır
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        ProcessBuilder pb = new ProcessBuilder(
                javaBin, "-jar", installerJar.getAbsolutePath(),
                "client", "-dir", mcDir.getAbsolutePath(),
                "-mcversion", MC_VERSION, "-loader", FABRIC_VERSION, "-noprofile"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();
    }

    private static void downloadGame(MinecraftDirectory dir, ProgressCallback callback) throws Exception {
        MinecraftDownloader downloader = MinecraftDownloaderBuilder.buildDefault();

        downloader.downloadIncrementally(dir, FABRIC_VERSION_ID, new CallbackAdapter<Version>() {
            @Override
            public void done(Version result) {
                callback.updateProgress(85, "Oyun dosyaları hazır!");
            }

            @Override
            public void failed(Throwable e) {
                // Hata .get() tarafından yakalanacak
            }
        }).get();
    }

    private static void startGame(MinecraftDirectory dir, String username) throws Exception {
        Launcher mc = LauncherBuilder.buildDefault();
        LaunchOption option = new LaunchOption(FABRIC_VERSION_ID, new OfflineAuthenticator(username), dir);
        
        // Bellek ayarları - 4 GB RAM tahsisi (mod paketi için gerekli)
        option.setMaxMemory(4096);
        option.setMinMemory(1024);
        
        // Doğrudan sunucuya bağlanma
        option.setServerInfo(new ServerInfo("130.61.80.148", 25565));

        mc.launch(option, new ProcessListener() {
            @Override
            public void onLog(String log) {
                System.out.println("[Minecraft] " + log);
            }

            @Override
            public void onErrorLog(String log) {
                System.err.println("[Minecraft ERR] " + log);
            }

            @Override
            public void onExit(int code) {
                System.out.println("Minecraft kapandı. Çıkış kodu: " + code);
                System.exit(0); // Minecraft kapanınca Launcher'ın arka plan işlemlerini tamamen bitir
            }
        });
    }

    public interface ProgressCallback {
        void updateProgress(int percentage, String status);
    }
}
