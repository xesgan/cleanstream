package cat.dam.roig.cleanstream.utils;

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

    /** Ejecuta un comando y va enviando cada línea al consumer. Devuelve exit code.
     * @param command
     * @param onLine
     * @return
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException  */
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
}
