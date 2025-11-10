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
                f = "137+140/248+140/22/b[height<=720]/b[height<=480]/b";
            case P720 ->
                f = "b[format_id=22]/bv*+ba/b[height<=720]/b[height<=480]/b";
            case P480 ->
                f = "bv*[height<=480]+ba/b[height<=480]/b";
            default ->
                f = "22/137+140/248+140/b[height<=720]/b[height<=480]/b";
        }
        cmd.add("-f");
        cmd.add(f);

        // Opcional: si prefieres ficheros finales .mp4
        cmd.add("--merge-output-format");
        cmd.add("mp4");
    }
}
