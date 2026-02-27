package cat.dam.roig.cleanstream.util;

import cat.dam.roig.cleanstream.domain.VideoQuality;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class responsible for:
 * <ul>
 * <li>Executing external system commands (e.g. yt-dlp)</li>
 * <li>Streaming process output line-by-line in real time</li>
 * <li>Appending quality-related arguments to yt-dlp commands</li>
 * </ul>
 *
 * <p>
 * This class centralizes all low-level process execution logic so that
 * controllers (e.g. DownloadExecutionController) remain clean and focused on UI
 * behavior.
 * </p>
 *
 * <p>
 * <b>Important:</b> This class does NOT manage threads. It should be executed
 * from a background thread (e.g. SwingWorker) to avoid blocking the EDT.</p>
 *
 * @author metku
 */
public class CommandExecutor {

    /**
     * Executes a system command and streams its output line-by-line.
     *
     * <p>
     * This is a simplified overload that does not expose the underlying
     * {@link Process} instance.
     * </p>
     *
     * @param command full command including arguments
     * @param onLine consumer invoked for each output line
     * @return process exit code
     * @throws IOException if process cannot be started
     * @throws InterruptedException if the current thread is interrupted
     */
    public static int runStreaming(List<String> command, Consumer<String> onLine)
            throws IOException, InterruptedException {

        return runStreaming(command, onLine, null);
    }

    /**
     * Executes a system command and streams its combined output (stdout +
     * stderr) line-by-line.
     *
     * <p>
     * Features:
     * <ul>
     * <li>Merges stderr into stdout ({@code redirectErrorStream(true)})</li>
     * <li>Streams output in real time</li>
     * <li>Optionally exposes the {@link Process} instance (useful for Stop
     * button)</li>
     * </ul>
     * </p>
     *
     * @param command full command including arguments
     * @param onLine consumer invoked for each output line (must not be null)
     * @param onProcessStart optional consumer invoked once process starts
     * @return process exit code
     * @throws IOException if process cannot be started
     * @throws InterruptedException if the current thread is interrupted
     */
    public static int runStreaming(
            List<String> command,
            Consumer<String> onLine,
            Consumer<Process> onProcessStart)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);

        // Merge stderr into stdout so we only need one reader
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Notify caller that process has started (useful to store reference)
        if (onProcessStart != null) {
            onProcessStart.accept(process);
        }

        // Stream output in real time
        try (BufferedReader reader
                = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                onLine.accept(line);
            }
        }

        // Wait for process to finish and return exit code
        return process.waitFor();
    }

    /**
     * Appends yt-dlp quality selection arguments to the provided command list.
     *
     * <p>
     * This method modifies the given {@code cmd} list by adding:
     * <ul>
     * <li>{@code -f} format selector</li>
     * <li>merge format settings</li>
     * </ul>
     * </p>
     *
     * <p>
     * Behavior:
     * <ul>
     * <li>P1080 → best video ≤ 1080p + best audio</li>
     * <li>P720 → best video ≤ 720p + best audio</li>
     * <li>P480 → best video ≤ 480p + best audio</li>
     * <li>BEST_AVAILABLE → best available</li>
     * </ul>
     * </p>
     *
     * <p>
     * Final container is always forced to MP4 using
     * {@code --merge-output-format mp4}.
     * </p>
     *
     * @param cmd command list to modify
     * @param q selected video quality
     */
    public static void appendQualityArgs(List<String> cmd, VideoQuality q) {

        String formatSelector;

        switch (q) {
            case P1080 ->
                // Best video up to 1080p
                formatSelector = "[height<=1080]";
            case P720 ->
                // Best video up to 720p
                formatSelector = "[height<=720]";
            case P480 ->
                // Best video up to 480p
                formatSelector = "[height<=480]";
            default ->
                // Best available (fallback strategy)
                formatSelector = "bv*+ba/b";
        }

        System.out.println(">>> USING FORMAT SELECTOR: " + formatSelector);

        cmd.add("-f");

        // Compose final format string for yt-dlp
        cmd.add("bv" + formatSelector + "+bestaudio[ext=m4a]/mp4");

        // Force final merged container format
        cmd.add("--merge-output-format");
        cmd.add("mp4");
    }
}
