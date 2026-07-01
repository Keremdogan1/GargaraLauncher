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
    public static final String EXTRAS_DRIVE_FILE_ID = "1dijZpP2KYTLfJSFLKtjEgmNAVrnC-SD5";

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

        // 1.5. Görsel Paketleri (Shader & Texture) Kontrol Et
        if ("true".equals(SettingsManager.get("downloadExtras", "false"))) {
            callback.updateProgress(15, "Görsel paketler kontrol ediliyor...");
            downloadAndInstallExtras(mcDir, callback);
        }

        // 2. Fabric Installer'ı indir ve çalıştır
        callback.updateProgress(20, "Oyun motoru (Fabric) ayarlanıyor...");
        setupFabric(mcDir);

        // 3. Oyun Dosyalarını İndir (kayan animasyonlu)
        callback.updateProgress(-1, "Oyun dosyaları indiriliyor... (Bu işlem biraz sürebilir)");
        downloadGame(dir, callback);

        // Özel ayarları uygula (Replay mod silme, GUI boyutu 3 yapma)
        applyCustomGameSettings(mcDir);

        // 4. Oyunu Başlat
        callback.updateProgress(90, "Oyun başlatılıyor...");
        startGame(dir, username, callback);
        
        callback.updateProgress(100, "Oyun açıldı. Arkada çalışıyor...");
    }

    private static void applyCustomGameSettings(File mcDir) {
        // 1. Replay Mod'u tamamen sil
        File modsDir = new File(mcDir, "mods");
        if (modsDir.exists()) {
            File[] files = modsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().toLowerCase().contains("replaymod")) {
                        f.delete();
                        System.out.println("Replay Mod silindi: " + f.getName());
                    }
                }
            }
        }

        // 2. GUI Boyutunu 3 yap (options.txt) ve Kaynak Paketleri
        try {
            File optionsFile = new File(mcDir, "options.txt");
            boolean extrasEnabled = "true".equals(SettingsManager.get("downloadExtras", "false"));
            String resPacksLine = "resourcePacks:[\"vanilla\",\"file/Better-Leaves-9.5.zip\",\"file/Yuushya Foliage Addon 1.3.zip\",\"fabric\"]";
            
            if (!optionsFile.exists()) {
                StringBuilder sb = new StringBuilder();
                sb.append("guiScale:3\n");
                sb.append("key_key.zoomify.zoom:key.keyboard.c\n");
                sb.append("key_key.journeymap.chatposition:key.keyboard.insert\n");
                if (extrasEnabled) {
                    sb.append(resPacksLine).append("\n");
                }
                Files.writeString(optionsFile.toPath(), sb.toString());
            } else {
                java.util.List<String> lines = Files.readAllLines(optionsFile.toPath());
                boolean foundGui = false, foundZoom = false, foundJmChat = false, foundRes = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("guiScale:")) {
                        lines.set(i, "guiScale:3");
                        foundGui = true;
                    }
                    if (lines.get(i).startsWith("key_key.zoomify.zoom:")) {
                        lines.set(i, "key_key.zoomify.zoom:key.keyboard.c");
                        foundZoom = true;
                    }
                    if (lines.get(i).startsWith("key_key.journeymap.chatposition:")) {
                        lines.set(i, "key_key.journeymap.chatposition:key.keyboard.insert");
                        foundJmChat = true;
                    }
                    if (extrasEnabled && lines.get(i).startsWith("resourcePacks:")) {
                        // if they haven't modified it manually to include these yet, we force it ONCE.
                        // Actually, if it's there we just overwrite it to ensure they are active.
                        // Or we can just set it. The user wants them active.
                        lines.set(i, resPacksLine);
                        foundRes = true;
                    }
                }
                if (!foundGui) lines.add("guiScale:3");
                if (!foundZoom) lines.add("key_key.zoomify.zoom:key.keyboard.c");
                if (!foundJmChat) lines.add("key_key.journeymap.chatposition:key.keyboard.insert");
                if (extrasEnabled && !foundRes) lines.add(resPacksLine);
                Files.write(optionsFile.toPath(), lines);
            }
        } catch (Exception ignored) {}

        // 3. Sunucuyu Multiplayer listesine otomatik ekle (servers.dat)
        try {
            File serversDat = new File(mcDir, "servers.dat");
            if (!serversDat.exists()) {
                String b64 = "H4sIAAAAAAAA/+NiYOBkYC9OLSpLLSrmYmBgYORgYMlLzE1lEHBPLEpPLEpUCC7NK00uLS7lYGDKLGAQNjQ20DMz1LMw0DM0sbAyMjU1M2VgAADK7Ky1RwAAAA==";
                byte[] data = java.util.Base64.getDecoder().decode(b64);
                Files.write(serversDat.toPath(), data);
            }
        } catch (Exception ignored) {}
    }

    // Bu ID, icinde sadece "v2" gibi bir surum yazan kucuk bir metin dosyasinin ID'sidir
    public static final String MODS_VERSION_DRIVE_ID = "1uxxYC_u50dHKPXfgvYvpXDZBGMS39g77";

    private static void downloadAndInstallMods(File mcDir, ProgressCallback callback) throws Exception {
        File modsDir = new File(mcDir, "mods");
        File markerFile = new File(mcDir, ".mods_version.txt");

        callback.updateProgress(5, "Mod versiyonu kontrol ediliyor...");
        String localVersion = "";
        if (markerFile.exists()) {
            localVersion = Files.readString(markerFile.toPath()).trim();
        }

        String remoteVersion = "v1"; // Varsayılan
        // GDrive'dan güncel versiyon numarasını çek
        if (!MODS_VERSION_DRIVE_ID.equals("BURAYA_VERSION_TXT_ID_GELECEK")) {
            File tempVersionFile = new File(mcDir, "temp_version.txt");
            try {
                downloadFromGoogleDrive(MODS_VERSION_DRIVE_ID, tempVersionFile, null);
                if (tempVersionFile.exists()) {
                    remoteVersion = Files.readString(tempVersionFile.toPath()).trim();
                    tempVersionFile.delete();
                }
            } catch (Exception e) {
                System.err.println("Mod versiyonu kontrol edilemedi, lokal versiyon kullanilacak.");
                remoteVersion = localVersion.isEmpty() ? "v1" : localVersion;
            }
        }

        // Modlar zaten kuruluysa ve versiyon aynıysa atla
        if (modsDir.exists() && modsDir.listFiles() != null
                && modsDir.listFiles().length > 0 && localVersion.equals(remoteVersion)) {
            callback.updateProgress(10, "Modlar güncel (" + localVersion + "), atlanıyor...");
            return;
        }

        callback.updateProgress(8, "Yeni mod paketi bulundu! Eski modlar siliniyor...");
        if (modsDir.exists()) {
            File[] oldMods = modsDir.listFiles();
            if (oldMods != null) {
                for (File f : oldMods) f.delete();
            }
        } else {
            modsDir.mkdirs();
        }

        File modsZip;

        // Önce JAR içinde gömülü mods.zip var mı kontrol et (offline dağıtım)
        InputStream embeddedMods = LauncherLogic.class.getResourceAsStream("/mods.zip");
        if (embeddedMods != null && localVersion.isEmpty()) { // Sadece hiç kurulmamışsa gömülüyü kullan
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
            callback.updateProgress(5, "Yeni modlar indiriliyor (Bu işlem boyutuna göre sürebilir)...");
            modsZip = new File(mcDir, "mods_download.zip");
            downloadFromGoogleDrive(MODS_DRIVE_FILE_ID, modsZip, callback);
        }

        // ZIP'i mods klasörüne çıkart
        callback.updateProgress(45, "Modlar kurulum klasörüne çıkartılıyor...");
        extractZip(modsZip, modsDir);

        // Ekstra modları mods klasörüne kopyala
        File extraModsDir = new File(mcDir, "extra_mods");
        if (extraModsDir.exists()) {
            callback.updateProgress(48, "Ekstra modlarınız ekleniyor...");
            File[] extras = extraModsDir.listFiles();
            if (extras != null) {
                for (File f : extras) {
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        java.nio.file.Files.copy(f.toPath(), new File(modsDir, f.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }

        // Kurulum tamamlandı işareti
        Files.writeString(markerFile.toPath(), remoteVersion);

        // Geçici zip dosyasını temizle
        modsZip.delete();
        callback.updateProgress(50, "Modlar başarıyla güncellendi!");
    }

    private static void downloadAndInstallExtras(File mcDir, ProgressCallback callback) throws Exception {
        File extrasMarker = new File(mcDir, ".extras_downloaded.flag");
        // Sadece bir kere indirsin, her girişte indirmesin.
        if (extrasMarker.exists()) {
            return;
        }

        callback.updateProgress(15, "Görsel paketler indiriliyor (Bu işlem boyutuna göre sürebilir)...");
        File extrasZip = new File(mcDir, "extras_download.zip");
        try {
            downloadFromGoogleDrive(EXTRAS_DRIVE_FILE_ID, extrasZip, callback);
            callback.updateProgress(45, "Görsel paketler kuruluyor...");
            extractZip(extrasZip, mcDir);
            
            // Eğer optionsshaders.txt varsa ve photon.zip aktifse kapatalım
            // Kullanıcı kapalı gelsin dedi.
            File shadersConfig = new File(mcDir, "optionsshaders.txt");
            if (!shadersConfig.exists()) {
                Files.writeString(shadersConfig.toPath(), "shaderPack=OFF\n");
            } else {
                java.util.List<String> lines = Files.readAllLines(shadersConfig.toPath());
                boolean foundShader = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("shaderPack=")) {
                        lines.set(i, "shaderPack=OFF");
                        foundShader = true;
                    }
                }
                if (!foundShader) lines.add("shaderPack=OFF");
                Files.write(shadersConfig.toPath(), lines);
            }
            
            Files.writeString(extrasMarker.toPath(), "done");
        } catch (Exception e) {
            System.err.println("Görsel paketler indirilemedi: " + e.getMessage());
        } finally {
            if (extrasZip.exists()) {
                extrasZip.delete();
            }
        }
    }

    public static void downloadFromGoogleDrive(String fileId, File outputFile, ProgressCallback callback) throws Exception {
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
                        int percent = (int) (5 + (downloaded * 40 / totalSize));
                        String totalMB = String.format("%.1f", totalSize / 1048576.0);
                        if (callback != null) {
                            callback.updateProgress(percent,
                                "İndiriliyor: " + downloadedMB + " MB / " + totalMB + " MB");
                        }
                    } else {
                        if (callback != null) {
                            callback.updateProgress(-1, "İndiriliyor: " + downloadedMB + " MB");
                        }
                    }
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    public static void extractZip(File zipFile, File destDir) throws Exception {
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
        callback.updateProgress(60, "Oyun dosyaları paralel indiriliyor... (Bu işlem birkaç dakika sürebilir)");
        // JMCCC varsayılan olarak havuz kullanır. Maksimum hızı zorlamak için özel bir oluşturucu kullanıyoruz.
        MinecraftDownloader downloader = MinecraftDownloaderBuilder.create().build();

        downloader.downloadIncrementally(dir, FABRIC_VERSION_ID, new CallbackAdapter<Version>() {
            @Override
            public void done(Version result) {
                callback.updateProgress(90, "Oyun dosyaları hazır!");
            }

            @Override
            public void failed(Throwable e) {
                // Hata .get() tarafından yakalanacak
            }
        }).get();
    }

    private static void startGame(MinecraftDirectory dir, String username, ProgressCallback callback) throws Exception {
        Launcher mc = LauncherBuilder.buildDefault();
        LaunchOption option = new LaunchOption(FABRIC_VERSION_ID, new OfflineAuthenticator(username), dir);
        
        java.util.List<String> gameLogs = new java.util.ArrayList<>();
        
        // Bellek ayarları
        String ramStr = SettingsManager.get("ram", "4096");
        int maxRam = 4096;
        try { maxRam = Integer.parseInt(ramStr); } catch (Exception ignored) {}
        
        option.setMaxMemory(maxRam);
        option.setMinMemory(1024);
        // Doğrudan sunucuya bağlanma (Kırmızı ekran riski olsa da direkt bağlantı için)
        option.extraMinecraftArguments().add("--quickPlayMultiplayer");
        option.extraMinecraftArguments().add("130.61.80.148:25565");

        mc.launch(option, new ProcessListener() {
            @Override
            public void onLog(String log) {
                System.out.println("[Minecraft] " + log);
                synchronized(gameLogs) {
                    if (gameLogs.size() > 200) gameLogs.remove(0);
                    gameLogs.add("[LOG] " + log);
                }
            }

            @Override
            public void onErrorLog(String log) {
                System.err.println("[Minecraft ERR] " + log);
                synchronized(gameLogs) {
                    if (gameLogs.size() > 200) gameLogs.remove(0);
                    gameLogs.add("[ERR] " + log);
                }
            }

            @Override
            public void onExit(int code) {
                System.out.println("Minecraft kapandı. Çıkış kodu: " + code);
                
                if (code != 0) {
                    StringBuilder crashReport = new StringBuilder("Oyun çöktü! (Çıkış Kodu: " + code + ")\n\nSon 200 satır log:\n");
                    synchronized(gameLogs) {
                        for (String l : gameLogs) crashReport.append(l).append("\n");
                    }
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.JTextArea ta = new javax.swing.JTextArea(crashReport.toString(), 20, 60);
                        ta.setEditable(false);
                        javax.swing.JOptionPane.showMessageDialog(null, new javax.swing.JScrollPane(ta), "Minecraft Çöktü", javax.swing.JOptionPane.ERROR_MESSAGE);
                    });
                }
                
                callback.onGameExit(); // Oyuna çıkış yapıldığını GUI'ye bildir
            }
        });
        
        callback.onGameLaunch();
    }

    public interface ProgressCallback {
        void updateProgress(int percentage, String status);
        void onGameLaunch();
        void onGameExit();
    }
}
