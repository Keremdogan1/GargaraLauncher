import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;

public class DownloadDependency {
    public static void main(String[] args) throws Exception {
        String downloadUrl = "https://github.com/MinnDevelopment/java-discord-rpc/releases/download/v2.0.2/java-discord-rpc-2.0.2.jar";
        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setInstanceFollowRedirects(true);
        System.out.println("Response Code: " + conn.getResponseCode());
        
        File libDir = new File("lib");
        if(!libDir.exists()) libDir.mkdirs();
        
        File outFile = new File(libDir, "java-discord-rpc-2.0.2.jar");
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("Downloaded to: " + outFile.getAbsolutePath());
    }
}
