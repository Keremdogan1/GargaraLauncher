package com.gargara.launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerPinger {

    public interface PingCallback {
        void onResult(boolean online, int currentPlayers, int maxPlayers);
    }

    public static void pingServer(String ip, PingCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.mcsrvstat.us/3/" + ip);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "GargaraLauncher/1.0");

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    String json = response.toString();
                    
                    // Basit JSON Parse
                    boolean online = json.contains("\"online\": true") || json.contains("\"online\":true");
                    int onlinePlayers = 0;
                    int maxPlayers = 0;

                    if (online && json.contains("\"players\":")) {
                        try {
                            String pOnlineStr = extractJsonValue(json, "\"online\":");
                            String pMaxStr = extractJsonValue(json, "\"max\":");
                            onlinePlayers = Integer.parseInt(pOnlineStr);
                            maxPlayers = Integer.parseInt(pMaxStr);
                        } catch (Exception ignored) {}
                    }
                    
                    callback.onResult(online, onlinePlayers, maxPlayers);
                } else {
                    callback.onResult(false, 0, 0);
                }
            } catch (Exception e) {
                callback.onResult(false, 0, 0);
            }
        }).start();
    }

    private static String extractJsonValue(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return "0";
        index += key.length();
        
        StringBuilder val = new StringBuilder();
        boolean started = false;
        for (int i = index; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) {
                val.append(c);
                started = true;
            } else if (started) {
                break;
            }
        }
        return val.length() > 0 ? val.toString() : "0";
    }
}
