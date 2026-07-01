package com.gargara.launcher;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import org.json.JSONObject;

import java.time.OffsetDateTime;

public class DiscordPresence {

    private static final long CLIENT_ID = 1521064597010780232L;
    private static IPCClient client;
    private static boolean running = false;
    private static OffsetDateTime startTime;

    public static void start() {
        if (CLIENT_ID == 1521064597010780232L) {
            // Aslında 1521064597010780232L kullanıcı tarafından girilen ID.
            // Sadece null veya -1 gibi geçersiz bir id var mı diye kontrol edebilirdik.
        }

        try {
            client = new IPCClient(CLIENT_ID);
            client.setListener(new IPCListener() {
                @Override
                public void onReady(IPCClient client) {
                    System.out.println("Discord RPC Hazır!");
                    running = true;
                    startTime = OffsetDateTime.now();
                    updatePresence("Launcher'da", "Giriş Bekleniyor");
                }

                @Override
                public void onClose(IPCClient client, JSONObject json) {
                    running = false;
                }

                @Override
                public void onDisconnect(IPCClient client, Throwable t) {
                    running = false;
                }
            });
            client.connect();
        } catch (Exception e) {
            System.err.println("Discord RPC Başlatılamadı: " + e.getMessage());
        }
    }

    public static void updatePresence(String state, String details) {
        if (!running || client == null) return;

        try {
            RichPresence.Builder builder = new RichPresence.Builder();
            builder.setState(state);
            builder.setDetails(details);
            builder.setStartTimestamp(startTime);
            builder.setLargeImage("logo", "Gargara Sunucusu");
            
            client.sendRichPresence(builder.build());
        } catch (Exception e) {
            System.err.println("Discord Durumu güncellenemedi: " + e.getMessage());
        }
    }

    public static void stop() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {}
            running = false;
        }
    }
}
