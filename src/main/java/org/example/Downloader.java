package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader {

    Logger logger;

    public Downloader() {
        this.logger = Logger.getLogger();
    }

    public void downloadSongs(String url) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("yt-dlp.exe",
                "--replace-in-metadata", "\"title\"", "\"[\\\"]\"", "\"\"",
                "-x",
                "--audio-format", "mp3",
                "-P \"%USERPROFILE%/Downloads\"",
                "-o \"%(title)s.%(ext)s\"",
                url);
        Process p = pb.start();
        Pattern pattern = Pattern.compile("Downloading item (\\d+) of (\\d+)");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String currentItem = matcher.group(1);
                String totalItems = matcher.group(2);
                this.logger.println("Song download progress: " + currentItem + " out of " + totalItems);
            }
        }
        p.waitFor();
        in.close();
    }
}
