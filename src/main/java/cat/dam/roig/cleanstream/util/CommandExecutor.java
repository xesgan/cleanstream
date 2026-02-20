package cat.dam.roig.cleanstream.util;

import cat.dam.roig.cleanstream.domain.VideoQuality;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JOptionPane;

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
     * @param onProcessStart
     * @return
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public static int runStreaming(List<String> command, Consumer<String> onLine)
            throws IOException, InterruptedException {
        // Redirige internamente a la versión completa, sin capturar el proceso
        return runStreaming(command, onLine, null);
    }

    public static int runStreaming(
            List<String> command,
            Consumer<String> onLine,
            Consumer<Process> onProcessStart)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Si el caller necesita guardar el proceso (para Stop), lo notificamos
        if (onProcessStart != null) {
            onProcessStart.accept(process);
        }

        // Lectura de la salida en tiempo real
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                onLine.accept(line);
            }
        }

        // Esperamos a que el proceso termine y devolvemos su código de salida
        return process.waitFor();
    }

    public static void appendQualityArgs(List<String> cmd, VideoQuality q) {
        String f;

        switch (q) {
            case P1080 ->
                // Mejor vídeo <=1080p + mejor audio
                f = "[height<=1080]";
            case P720 ->
                // Mejor vídeo <=720p + mejor audio
                f = "[height<=720]";
            case P480 ->
                // Mejor vídeo <=480p + mejor audio
                f = "[height<=480]";
            default ->
                // Mejor disponible
                f = "bv*+ba/b";
        }
        
        System.out.println(">>> USING FORMAT SELECTOR: " + f);

        cmd.add("-f");

        // Siempre dejamos el contenedor final en MP4
        cmd.add("bv" + f + "+bestaudio[ext=m4a]/mp4");
        cmd.add("--merge-output-format");
        cmd.add("mp4");
    }
}
