package me.nether.forgiagent.utils;

import me.nether.forgiagent.Installer;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NetworkUtils {

    public static String getResponseBody(String request_url) {
        try {
            URL url = new URL(request_url);
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String body = IOUtils.toString(in, encoding);
            return body;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static void downloadUsingStream(String urlStr, File file) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
        long completeFileSize = httpConnection.getContentLength();

        BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
        byte[] data = new byte[1024];
        long downloadedFileSize = 0;
        int x = 0;
        while ((x = in.read(data, 0, 1024)) >= 0) {
            downloadedFileSize += x;

            // calculate progress
            final int currentProgress = (int) (((float) downloadedFileSize / (float) completeFileSize) * completeFileSize);

            // update progress bar
            SwingUtilities.invokeLater(() -> {
                Installer.instance.progressBar.setMaximum((int) completeFileSize);
                Installer.instance.progressBar.setValue(currentProgress);
                Installer.instance.downloadStatus.setText(String.format("(%s / %s KB) %s", currentProgress / 1000, completeFileSize / 1000, file.getName()));
            });

            bout.write(data, 0, x);
        }
        bout.close();
        in.close();
    }

    public static void downloadUsingNIO(String urlStr, File file) throws IOException {
        URL url = new URL(urlStr);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

    public static Function<String, Integer> getFileSize = (link) -> {
        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            int lenght = conn.getContentLength();
            conn.disconnect();
            return lenght;
        } catch (Exception e) {
            return -1;
        }
    };

    public static Function<String, Boolean> isOnline = (url) -> {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    };

    public static Function<String, List<String>> readFileLinesFull = (link) -> {
        List<String> lines = new ArrayList<String>();
        try {
            URL url = new URL(link);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
        } catch (Exception e) {
        }
        return lines;
    };

    private static final String USER_AGENT = "Mozilla/5.0";

    public static void makeGitHubRequest(String link) {
        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestProperty("GET", "/repos/:dariopassarello/:forgiamodpacktest/contents/:tree/master/mods");
//			conn.getInputStream();
            String encoding = conn.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String body = IOUtils.toString(conn.getInputStream(), encoding);

//            System.out.println(body);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
