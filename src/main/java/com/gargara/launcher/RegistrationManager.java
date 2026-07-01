package com.gargara.launcher;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class RegistrationManager {

    private static final String FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSfJUYHlXGA66qyxAsx9kReLYjXCupICjj8D0qjVEWZSoEDh1w/formResponse";

    public static boolean submitForm(String name, String year, String month, String day, String email) {
        try {
            URL url = new URL(FORM_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            Map<String, String> arguments = new HashMap<>();
            arguments.put("entry.559002138", name);
            arguments.put("entry.1840540319_year", year);
            arguments.put("entry.1840540319_month", month);
            arguments.put("entry.1840540319_day", day);
            arguments.put("entry.1210381155", email);

            StringJoiner sj = new StringJoiner("&");
            for(Map.Entry<String,String> entry : arguments.entrySet())
                sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" 
                     + URLEncoder.encode(entry.getValue(), "UTF-8"));
            
            byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;
            conn.setRequestProperty("Content-Length", String.valueOf(length));
            
            try(OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }
            
            int responseCode = conn.getResponseCode();
            return responseCode == 200 || responseCode == 302; // Redirects/200 OK typically mean success for forms
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
