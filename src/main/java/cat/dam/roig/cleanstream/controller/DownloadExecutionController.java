package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.domain.VideoQuality;
import cat.dam.roig.cleanstream.ui.PreferencesPanel;
import cat.dam.roig.cleanstream.ui.main.MainFrame;
import cat.dam.roig.cleanstream.util.CommandExecutor;
import cat.dam.roig.cleanstream.util.DetectOS;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * Controller responsible for executing downloads using <b>yt-dlp</b> and
 * updating the Swing UI.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 * <li>Reads user input (URL and download options) and validates required
 * configuration.</li>
 * <li>Builds the correct yt-dlp command based on preferences and selected
 * quality.</li>
 * <li>Executes yt-dlp in the background using a {@link SwingWorker} to keep the
 * UI responsive.</li>
 * <li>Parses yt-dlp output lines to:
 * <ul>
 * <li>Append logs to the UI</li>
 * <li>Update the progress bar</li>
 * <li>Detect the final downloaded file(s)</li>
 * </ul>
 * </li>
 * <li>Supports canceling an ongoing download (stop button).</li>
 * <li>Optionally generates/updates an M3U playlist file.</li>
 * <li>Optionally opens the last downloaded file when the process ends
 * successfully.</li>
 * <li>Triggers a local library refresh scan after successful downloads.</li>
 * </ul>
 *
 * <h2>Threading model</h2>
 * <ul>
 * <li>{@link SwingWorker#doInBackground()} runs in a background thread (safe
 * for blocking calls).</li>
 * <li>{@link SwingWorker#process(java.util.List)} and
 * {@link SwingWorker#done()} run on the EDT and can safely update Swing
 * components.</li>
 * </ul>
 *
 * <p>
 * <b>Note:</b> This controller keeps references to UI components because it
 * coordinates UI state and background execution. It is intentionally not a
 * "pure" service.
 *
 * @author metku
 */
public class DownloadExecutionController {

    /**
     * Main application window (used for dialogs and to access other
     * controllers).
     */
    private final MainFrame mainFrame;

    /**
     * Preferences panel containing user-configurable settings used during
     * download.
     */
    private final PreferencesPanel preferencesPanel;

    /**
     * URL input field.
     */
    private final JTextField txtUrl;

    /**
     * Text area where yt-dlp output lines are appended.
     */
    private final JTextArea logArea;

    /**
     * Button that starts a download (disabled while downloading).
     */
    private final JButton btnDownload;

    /**
     * Button that cancels an ongoing download.
     */
    private final JButton btnStop;

    /**
     * Radio button that switches to "audio-only" mode.
     */
    private final JRadioButton rbAudio;

    /**
     * Progress bar shown while downloading, updated by parsing yt-dlp output.
     */
    private final JProgressBar pbDownload;

    /**
     * Controller responsible for managing the local downloads library. Used to
     * refresh the local scan after a successful download.
     */
    private final DownloadsController downloadsController;

    /**
     * Current running process (yt-dlp). Marked as volatile because it can be
     * accessed from different threads.
     */
    private volatile Process currentProcess;

    /**
     * SwingWorker used to execute the current download in background. Used to
     * cancel the job when the user clicks Stop.
     */
    private SwingWorker<Integer, String> currentDownloadWorker;

    /**
     * Absolute path to the last detected downloaded file. Used to implement the
     * "Open last download" functionality.
     */
    private String lastDownloadedFile;

    /**
     * Builds a controller that wires together UI components and download
     * execution logic.
     *
     * @param mainFrame main window used as owner for dialogs and to access
     * global UI state
     * @param preferencesPanel preferences panel providing paths and download
     * options
     * @param txtUrl URL input field
     * @param logArea log output UI
     * @param btnDownload download button
     * @param btnStop stop button
     * @param rbAudio audio mode radio button
     * @param pbDownload progress bar
     * @param downloadsController local library controller (refresh after
     * successful download)
     */
    public DownloadExecutionController(MainFrame mainFrame,
            PreferencesPanel preferencesPanel,
            JTextField txtUrl,
            JTextArea logArea,
            JButton btnDownload,
            JButton btnStop,
            JRadioButton rbAudio,
            JProgressBar pbDownload,
            DownloadsController downloadsController) {
        this.mainFrame = mainFrame;
        this.preferencesPanel = preferencesPanel;
        this.txtUrl = txtUrl;
        this.logArea = logArea;
        this.btnDownload = btnDownload;
        this.btnStop = btnStop;
        this.rbAudio = rbAudio;
        this.pbDownload = pbDownload;
        this.downloadsController = downloadsController;
    }

    /**
     * Starts a new download based on the current UI state.
     *
     * <p>
     * Flow:
     * <ol>
     * <li>Build and validate a {@link DownloadContext}. If validation fails,
     * the method returns.</li>
     * <li>Build yt-dlp command using
     * {@link #buildYtDlpCommand(DownloadContext)}.</li>
     * <li>Prepare the UI (clear log, set progress bar and buttons state).</li>
     * <li>Create and execute a {@link SwingWorker} that runs yt-dlp in the
     * background.</li>
     * </ol>
     */
    public void startDownload() {
        final List<String> downloadedFiles = new ArrayList<>();

        DownloadContext ctx = buildDownloadContext();
        if (ctx == null) {
            return;
        }

        List<String> command = buildYtDlpCommand(ctx);
        prepareUiBeforeDownload(command);

        currentDownloadWorker = createDownloadWorker(ctx, command, downloadedFiles);
        currentDownloadWorker.execute();
    }

    /**
     * Requests cancellation of the current download.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Cancels the current {@link SwingWorker}.</li>
     * <li>Updates button states to prevent repeated cancellation attempts.</li>
     * </ul>
     *
     * <p>
     * The actual process termination is handled inside the worker (see
     * {@code destroyProcessQuiet()}).
     */
    public void stopDownload() {
        if (currentDownloadWorker != null && !currentDownloadWorker.isDone()) {
            currentDownloadWorker.cancel(true);
            destroyCurrentProcessNow();

            logArea.append("[STOP] Solicitud de cancelación enviada.\n");

            btnStop.setEnabled(false);
            btnDownload.setEnabled(true);
        }
    }

    /**
     * Attempts to terminate the currently running yt-dlp process, if any.
     *
     * <p>
     * This method is usually invoked when the user clicks the Stop button. It
     * first tries a graceful stop with {@link Process#destroy()}, waits
     * briefly, and if the process is still alive it forces termination using
     * {@link Process#destroyForcibly()}.
     *
     * <p>
     * It is safe to call this method even when no process is running.
     */
    private void destroyCurrentProcessNow() {
        Process p = currentProcess;
        if (p == null) {
            return;
        }

        try {
            logArea.append("[STOP] Killing yt-dlp process...\n");

            p.destroy(); // intento suave
            p.waitFor(800, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (p.isAlive()) {
                logArea.append("[STOP] Forcing kill...\n");
                p.destroyForcibly();
            }
        } catch (Exception ignore) {
            // Best effort: do not crash the UI if we fail to kill the process.
        } finally {
            currentProcess = null;
        }
    }

    /**
     * Opens the last downloaded file, if available.
     *
     * <p>
     * If the file is an M3U playlist file ({@code .m3u} or {@code .m3u8}), the
     * method tries to open it using VLC. Otherwise it uses
     * {@link Desktop#open(File)}.
     *
     * <p>
     * If the file does not exist or cannot be opened, an error dialog is shown
     * and the open button is disabled.
     *
     * @param parent parent component used to show dialog messages
     * @param btnOpenLast button that triggers this action (may be
     * enabled/disabled by this method)
     */
    public void openLastDownloadedFile(Component parent, JButton btnOpenLast) {
        String last = lastDownloadedFile;

        if (last == null || last.isBlank()) {
            JOptionPane.showMessageDialog(parent,
                    "No previous download found.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            btnOpenLast.setEnabled(false);
            return;
        }

        File file = new File(last);

        if (!file.exists()) {
            JOptionPane.showMessageDialog(parent,
                    "The last downloaded file cannot be found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            btnOpenLast.setEnabled(false);
            return;
        }

        try {
            if (isPlaylistFile(file)) {
                new ProcessBuilder("vlc", file.getAbsolutePath()).start();
            } else {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                    "Could not open the file:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            btnOpenLast.setEnabled(false);
            return;
        }

        btnOpenLast.setEnabled(true);
    }

    /**
     * Checks whether the given file is a supported playlist file.
     *
     * @param file candidate file
     * @return true if the file extension is .m3u or .m3u8
     */
    private boolean isPlaylistFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".m3u") || name.endsWith(".m3u8");
    }

    /**
     * Builds and validates the parameters required to launch yt-dlp.
     *
     * <p>
     * Validations include:
     * <ul>
     * <li>URL not empty</li>
     * <li>yt-dlp path not empty and executable exists</li>
     * </ul>
     *
     * <p>
     * If validation fails, a dialog is shown and {@code null} is returned.
     *
     * @return a valid {@link DownloadContext} or null if validation fails
     */
    private DownloadContext buildDownloadContext() {
        String ytDlpPath = preferencesPanel.getSTxtYtDlpPath();
        String ffmpegPath = preferencesPanel.getSTxtFfpmegDir();
        String downloadDir = DetectOS.resolveDownloadDir(
                preferencesPanel.getSTxtDownloadsDir().trim()
        );
        String url = txtUrl.getText().trim();

        if (url.isBlank()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Video URL is missing.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        if (ytDlpPath.isBlank()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Yt-Dlp path is missing. Please configure it in Preferences.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        File execFile = new File(ytDlpPath);
        if (!execFile.exists() || !execFile.canExecute()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "yt-dlp executable not found or not accessible. \nCheck your Preferences path.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return null;
        }

        boolean audio = rbAudio.isSelected();
        boolean isYouTube = url.contains("youtube.com") || url.contains("youtu.be");

        return new DownloadContext(ytDlpPath, ffmpegPath, downloadDir, url, audio);
    }

    /**
     * Builds the full yt-dlp command line based on the provided context and UI
     * state.
     *
     * <p>
     * Command parts:
     * <ul>
     * <li>Audio mode: extract audio and convert to MP3.</li>
     * <li>Video mode: append quality arguments depending on
     * {@link VideoQuality} selection.</li>
     * <li>Output directory and file name template.</li>
     * <li>Optional ffmpeg location.</li>
     * <li>Progress output options for parsing.</li>
     * <li>Optional speed limit options.</li>
     * </ul>
     *
     * @param ctx validated download context
     * @return a mutable list of arguments representing the yt-dlp command
     */
    private List<String> buildYtDlpCommand(DownloadContext ctx) {
        List<String> command = new ArrayList<>();

        // Ejecutable
        command.add(ctx.ytDlpPath);

        // --- Audio / Vídeo ---
        if (ctx.audio) {
            command.add("-x");
            String audioFormat = "mp3"; // TODO: sacar de combo en el futuro
            command.add("--audio-format");
            command.add(audioFormat);
        } else {
            VideoQuality q = mainFrame.getSelectedQuality();
            System.out.println(">>> VIDEO QUALITY EN COMBO: " + q);
            CommandExecutor.appendQualityArgs(command, q);
        }

        // --- Directorio de salida ---
        if (!ctx.downloadDir.isBlank()) {
            command.add("-P");
            command.add(ctx.downloadDir);
            command.add("-o");
            command.add("%(title)s.%(ext)s");
        }

        // --- Ruta a ffmpeg ---
        if (ctx.ffmpegPath != null
                && !ctx.ffmpegPath.isBlank()
                && new File(ctx.ffmpegPath).exists()) {

            command.add("--ffmpeg-location");
            command.add(ctx.ffmpegPath);
        }

        // --- Diferenciación por sitio (versión simple) ---
        command.add("--ignore-config");   // <- SIEMPRE
        command.add("--no-cache-dir");    // opcional, pero limpio

        // Imprime path final de cada ítem descargado
        command.add("--print");
        command.add("after_move:filepath");

        // Limit rate (si procede)
        if (preferencesPanel.chkLimitSpeed.isSelected()) {
            String rate = preferencesPanel.getSldLimitSpeed();
            if (rate != null && !rate.isBlank()) {
                command.add("--limit-rate");
                command.add(rate);
            }
        }

        // Imprime la calidad REAL seleccionada
        command.add("--print");
        command.add("QUALITY:%(format_id)s|%(resolution)s|%(fps)s|v:%(vcodec)s|a:%(acodec)s");

        // ProgressBar
        command.add("--progress");
        command.add("--newline");

        // URL al final
        command.add(ctx.url.trim());

        // debbuging
        System.out.println(">>> YT-DLP PATH EN CONTEXTO: " + ctx.ytDlpPath);
        System.out.println("CMD: " + String.join(" ", command));

        return command;
    }

    /**
     * Updates the UI before starting the background download.
     *
     * <p>
     * Actions:
     * <ul>
     * <li>Clears the log area and prints the full command.</li>
     * <li>Sets the progress bar to indeterminate mode.</li>
     * <li>Disables the Download button and enables the Stop button.</li>
     * </ul>
     *
     * @param command command that will be executed (shown for debugging
     * purposes)
     */
    private void prepareUiBeforeDownload(List<String> command) {
        logArea.setText("");
        logArea.append("CMD: " + String.join(" ", command) + "\n\n");

        pbDownload.setIndeterminate(true);
        pbDownload.setString("Downloading...");
        pbDownload.setValue(0);

        btnDownload.setEnabled(false);
        btnStop.setEnabled(true);
    }

    /**
     * Creates a SwingWorker that executes yt-dlp and streams its output to the
     * UI.
     *
     * <p>
     * The worker:
     * <ul>
     * <li>Runs yt-dlp in {@link SwingWorker#doInBackground()}.</li>
     * <li>Publishes output lines (progress, messages, printed file paths).</li>
     * <li>Parses progress percent and updates the progress bar.</li>
     * <li>Detects final downloaded file path(s) using
     * {@code --print after_move:filepath} and fallback patterns such as
     * "Destination:" and "Merging formats into".</li>
     * <li>On success, optionally writes/updates an M3U playlist and optionally
     * opens the file.</li>
     * <li>On success, triggers a rescan of the local downloads folder.</li>
     * </ul>
     *
     * @param ctx download context
     * @param command full yt-dlp command
     * @param downloadedFiles list where detected output files will be stored
     * @return a ready-to-execute {@link SwingWorker} instance
     */
    private SwingWorker<Integer, String> createDownloadWorker(DownloadContext ctx,
            List<String> command,
            List<String> downloadedFiles) {

        final String downloadDir = ctx.downloadDir;
        final String ytDlpPath = ctx.ytDlpPath;

        return new SwingWorker<>() {

            @Override
            protected Integer doInBackground() {
                try {
                    // 0) versión
                    CommandExecutor.runStreaming(
                            List.of(ytDlpPath, "--version"),
                            line -> publish("[yt-dlp --version] " + line)
                    );

                    if (isCancelled()) {
                        return -2;
                    }

                    // === ÚNICO INTENTO: comando tal cual ===
                    List<String> cmd = new ArrayList<>(command);
                    publish("[try] default client");

                    int exit = CommandExecutor.runStreaming(
                            cmd,
                            this::publish,
                            p -> currentProcess = p
                    );

                    if (isCancelled()) {
                        destroyProcessQuiet();
                        return -2;
                    }

                    return exit;

                } catch (Exception e) {
                    String msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
                    publish("ERROR: " + msg);
                    e.printStackTrace();
                    return -1;
                } finally {
                    currentProcess = null;
                }
            }

            @Override
            protected void process(List<String> lines) {
                for (String line : lines) {
                    logArea.append(line + "\n");

                    Integer p = parseProgressPercent(line);
                    if (p != null) {
                        pbDownload.setIndeterminate(false);
                        pbDownload.setValue(p);
                        pbDownload.setString(p + "%");
                    }

                    // Captura directa del output de --print after_move:filepath
                    if (!line.isBlank() && new File(line).isAbsolute()) {
                        downloadedFiles.add(line);
                        lastDownloadedFile = line.trim();
                        continue;
                    }

                    // Detectar el archivo descargado (fallback)
                    if (line.contains("Destination:")) {
                        String path = line.substring(
                                line.indexOf("Destination:") + "Destination:".length()
                        ).trim();
                        downloadedFiles.add(path);
                        lastDownloadedFile = path;
                        continue;
                    }

                    if (line.contains("Merging formats into")) {
                        int q = line.indexOf('"');
                        int qq = line.lastIndexOf('"');
                        if (q >= 0 && qq > q) {
                            String path = line.substring(q + 1, qq).trim();
                            downloadedFiles.add(path);
                            lastDownloadedFile = path;
                        }
                    }
                }
            }

            @Override
            protected void done() {
                int exit = -1;
                boolean cancelled = isCancelled();

                try {
                    if (!cancelled) {
                        exit = get();
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    System.getLogger(DownloadExecutionController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
                try {
                    if (isCancelled()) {
                        logArea.append("\n[STOP] Cancelled by user.\n");
                        return;
                    }

                    logArea.append("\nProcess ended with code: " + exit + "\n");
                    logArea.append("OS Detected: " + DetectOS.detectOS());
                    logArea.append("\nDownload dir (final): " + downloadDir);

                    if (preferencesPanel.chkLimitSpeed.isSelected()) {
                        logArea.append("\nLimit Speed Applied: "
                                + preferencesPanel.getSldLimitSpeed());
                    }

                    if (exit == 0
                            && preferencesPanel.getChkCreateM3u()
                            && !downloadedFiles.isEmpty()) {

                        writeM3u(downloadedFiles, downloadDir);
                        logArea.append("\n[m3u] playlist updated\n");
                    }

                    if (exit == 0
                            && preferencesPanel.chkOpenWhenDone.isSelected()
                            && lastDownloadedFile != null) {

                        File file = new File(lastDownloadedFile);
                        if (file.exists()) {
                            logArea.append("Playing: " + file.getName() + "\n");
                            Desktop.getDesktop().open(file);
                        } else {
                            logArea.append("Couldn't find the downloaded file.\n");
                        }
                    }

                    if (exit == 0) {
                        // ✅ REFRESCAR LISTA LOCAL tras descarga OK
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            // 1) Lee el path que tengas en el preferences panel
                            String input = preferencesPanel.getTxtScanDownloadsFolder().getText();
                            String finalDirStr = DetectOS.resolveDownloadDir(input);
                            java.nio.file.Path downloads = java.nio.file.Paths.get(finalDirStr);

                            // 2) Re-escanea usando tu controlador (sin btnScan)
                            downloadsController.scanDownloads(downloads, null);
                        });
                    }

                } catch (CancellationException ce) {
                    logArea.append("\n[STOP] Cancelled by user.\n");
                } catch (IOException ex) {
                    System.getLogger(MainFrame.class.getName())
                            .log(System.Logger.Level.ERROR, (String) null, ex);
                } finally {
                    btnDownload.setEnabled(true);
                    btnStop.setEnabled(false);
                    currentProcess = null;
                    currentDownloadWorker = null;
                    pbDownload.setIndeterminate(false);
                    if (!isCancelled() && exit == 0) {
                        pbDownload.setValue(100);
                        pbDownload.setString("Completed");
                    } else {
                        pbDownload.setValue(0);
                        pbDownload.setString("Idle");
                    }
                }
            }

            // Helper ProgressBar
            private static Integer parseProgressPercent(String line) {
                if (line == null) {
                    return null;
                }

                // Ejemplo típico:
                // [download]  12.3% of 10.00MiB at 1.23MiB/s ETA 00:10
                int idx = line.indexOf('%');
                if (idx == -1) {
                    return null;
                }

                // Busca hacia atrás el número antes del %
                int start = idx - 1;
                while (start >= 0) {
                    char c = line.charAt(start);
                    if ((c >= '0' && c <= '9') || c == '.' || c == ' ') {
                        start--;
                    } else {
                        break;
                    }
                }
                String num = line.substring(start + 1, idx).trim();
                if (num.isEmpty()) {
                    return null;
                }

                try {
                    double d = Double.parseDouble(num);
                    int p = (int) Math.round(d);
                    if (p < 0) {
                        p = 0;
                    }
                    if (p > 100) {
                        p = 100;
                    }
                    return p;
                } catch (NumberFormatException ex) {
                    return null;
                }
            }

            private void destroyProcessQuiet() {
                try {
                    Process p = currentProcess;
                    if (p != null) {
                        publish("[STOP] Killing yt-dlp process...");
                        p.destroy();
                        p.waitFor(800, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (p.isAlive()) {
                            publish("[STOP] Forcing kill...");
                            p.destroyForcibly();
                        }
                    }
                } catch (Exception ignore) {
                }
            }
        };
    }

    /**
     * Writes or updates an M3U playlist file in the output directory.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Creates or updates {@code CleanStreamPlayList.m3u} in the download
     * directory.</li>
     * <li>Tries to store relative paths when possible (better
     * portability).</li>
     * <li>Prevents duplicate entries by comparing file names.</li>
     * </ul>
     *
     * <p>
     * On success or failure, it shows a message dialog to the user.
     *
     * @param files list of downloaded absolute file paths
     * @param outputDir output directory where the playlist is stored
     */
    private void writeM3u(List<String> files, String outputDir) {
        if (files == null || files.isEmpty() || outputDir == null || outputDir.isBlank()) {
            return;
        }
        Path outDir = Paths.get(outputDir);
        Path m3u = outDir.resolve("CleanStreamPlayList.m3u");

        try {
            // Leer las líneas existentes (si ya hay playlist)
            List<String> existing = Files.exists(m3u)
                    ? Files.readAllLines(m3u, StandardCharsets.UTF_8)
                    : new ArrayList<>();

            // Convertir las rutas nuevas a formato de texto
            for (String abs : files) {
                if (abs == null || abs.isBlank()) {
                    continue;
                }
                String pathToAdd;

                try {
                    Path rel = outDir.relativize(Paths.get(abs));
                    pathToAdd = rel.toString();
                } catch (Exception ex) {
                    pathToAdd = abs;
                }

                // Evitar duplicados (compara por nombre del archivo)
                boolean alreadyInList = existing.stream()
                        .anyMatch(line -> line.trim().endsWith(Paths.get(abs).getFileName().toString()));

                if (!alreadyInList) {
                    existing.add(pathToAdd);
                }
            }

            // Guardar el archivo actualizado (sobrescribe el antiguo)
            Files.write(m3u, existing, StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(mainFrame,
                    "Playlist updated: " + m3u.toAbsolutePath(),
                    "Playlist", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Couldn't update playlist:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Returns the last downloaded file absolute path, if detected.
     *
     * <p>
     * This can be used by other parts of the UI to implement actions such as
     * "Open last downloaded file".
     *
     * @return last downloaded file path or null if not available
     */
    public String getLastDownloadedFile() {
        return lastDownloadedFile;
    }

    /**
     * Small immutable object holding all required data to start a download.
     *
     * <p>
     * This context is created after validation and passed to worker and command
     * builders. It reduces parameter noise and keeps the code easier to read.
     */
    private static class DownloadContext {

        /**
         * Path to yt-dlp executable.
         */
        final String ytDlpPath;

        /**
         * Path to ffmpeg executable (optional).
         */
        final String ffmpegPath;

        /**
         * Output directory for downloaded media.
         */
        final String downloadDir;

        /**
         * URL to download.
         */
        final String url;

        /**
         * True if audio-only mode is selected.
         */
        final boolean audio;

        /**
         * Creates a new download context.
         *
         * @param ytDlpPath yt-dlp path (executable)
         * @param ffmpegPath ffmpeg path (optional)
         * @param downloadDir output directory
         * @param url target URL
         * @param audio audio-only flag
         * @param isYouTube whether the URL is YouTube (reserved for future
         * logic)
         */
        DownloadContext(String ytDlpPath,
                String ffmpegPath,
                String downloadDir,
                String url,
                boolean audio) {
            this.ytDlpPath = ytDlpPath;
            this.ffmpegPath = ffmpegPath;
            this.downloadDir = downloadDir;
            this.url = url;
            this.audio = audio;
        }
    }
}
