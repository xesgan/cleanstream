package cat.dam.roig.cleanstream.utils;

import cat.dam.roig.cleanstream.models.VideoQuality;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author metku
 */
public class CommandExecutor {

    /**
     * Ejecuta un comando y va enviando cada línea al consumer. Devuelve exit
     * code.
     *
     * @param command
     * @param onLine
     * @return
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public static int runStreaming(List<String> command, Consumer<String> onLine)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                onLine.accept(line);
            }
        }
        return process.waitFor();
    }

    public static void appendQualityArgs(List<String> cmd, VideoQuality q) {
        // Devuelve el "-f" adecuado y, si aplica, el merge de salida.
        String format;
        String merge = null; // "mp4" cuando queramos forzar contenedor final

        switch (q) {
            case P1080 -> {
                format = "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/best[height<=1080]";
                merge = "mp4";
            }
            case P720 -> {
                format = "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[height<=720]";
                merge = "mp4";
            }
            case P480 -> {
                format = "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/best[height<=480]";
                merge = "mp4";
            }
            default -> {
                format = "bv*+ba/best"; // Best Available
            }
        }

        cmd.add("-f");
        cmd.add(format);
        if (merge != null) {
            cmd.add("--merge-output-format");
            cmd.add(merge);
        }
    }
}
