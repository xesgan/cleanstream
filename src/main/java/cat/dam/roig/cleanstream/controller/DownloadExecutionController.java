package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.main.MainFrame;
import cat.dam.roig.cleanstream.models.VideoQuality;
import cat.dam.roig.cleanstream.ui.PreferencesPanel;
import cat.dam.roig.cleanstream.utils.CommandExecutor;
import cat.dam.roig.cleanstream.utils.DetectOS;
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
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 *
 * @author metku
 */
public class DownloadExecutionController {

    private final MainFrame mainFrame;
    private final PreferencesPanel preferencesPanel;
    private final JTextField txtUrl;
    private final JTextArea logArea;
    private final JButton btnDownload;
    private final JButton btnStop;
    private final JRadioButton rbAudio;

    // Estado que antes estaba en MainFrame
    private volatile Process currentProcess;
    private SwingWorker<Integer, String> currentDownloadWorker;
    private String lastDownloadedFile;

    public DownloadExecutionController(MainFrame mainFrame,
            PreferencesPanel preferencesPanel,
            JTextField txtUrl,
            JTextArea logArea,
            JButton btnDownload,
            JButton btnStop,
            JRadioButton rbAudio) {
        this.mainFrame = mainFrame;
        this.preferencesPanel = preferencesPanel;
        this.txtUrl = txtUrl;
        this.logArea = logArea;
        this.btnDownload = btnDownload;
        this.btnStop = btnStop;
        this.rbAudio = rbAudio;
    }

    public void startDownload() {
        // Lista donde iremos guardando los archivos detectados
        final List<String> downloadedFiles = new ArrayList<>();

        // 1) Validar y construir contexto
        DownloadContext ctx = buildDownloadContext();
        if (ctx == null) {
            return; // algo ha fallado en la validación, ya se ha mostrado mensaje
        }

        // 2) Construir el comando completo de yt-dlp
        List<String> command = buildYtDlpCommand(ctx);

        // 3) Preparar UI (log y botones)
        prepareUiBeforeDownload(command);

        // 4) Crear y lanzar el SwingWorker
        currentDownloadWorker = createDownloadWorker(ctx, command, downloadedFiles);
        currentDownloadWorker.execute();
    }

    /**
     * Solicita detener la descarga en curso. Cancela el SwingWorker y
     * deshabilita los botones correspondientes.
     */
    public void stopDownload() {
        if (currentDownloadWorker != null && !currentDownloadWorker.isDone()) {
            currentDownloadWorker.cancel(true);

            logArea.append("[STOP] Solicitud de cancelación enviada.\n");

            btnStop.setEnabled(false);
            btnDownload.setEnabled(true);
        }
    }

    /**
     * Intenta abrir el último archivo descargado. Si es una playlist (.m3u /
     * .m3u8), usa VLC. En caso contrario, usa Desktop.open().
     * @param parent
     * @param btnOpenLast
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
     * Indica si el archivo es una playlist soportada (.m3u / .m3u8).
     */
    private boolean isPlaylistFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".m3u") || name.endsWith(".m3u8");
    }

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

        return new DownloadContext(ytDlpPath, ffmpegPath, downloadDir, url, audio, isYouTube);
    }

    /**
     * Construye el comando completo de yt-dlp a partir del contexto.
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

        // URL al final
        command.add(ctx.url.trim());

        // debbuging
        System.out.println(">>> YT-DLP PATH EN CONTEXTO: " + ctx.ytDlpPath);
        System.out.println("CMD: " + String.join(" ", command));

        return command;
    }

    private void prepareUiBeforeDownload(List<String> command) {
        logArea.setText("");
        logArea.append("CMD: " + String.join(" ", command) + "\n\n");

        btnDownload.setEnabled(false);
        btnStop.setEnabled(true);
    }

    /**
     * Crea el SwingWorker que ejecuta yt-dlp y procesa la salida.
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
                try {
                    if (isCancelled()) {
                        logArea.append("\n[STOP] Cancelled by user.\n");
                        return;
                    }

                    int exit = get();

                    String allLog = logArea.getText();

                    if (allLog.contains("QUALITY:18|640x360")) {
                        JOptionPane.showMessageDialog(
                                null,
                                "YouTube ha limitado este vídeo a 360p (SABR).\n"
                                + "Tu selección de calidad no se pudo aplicar.\n\n"
                                + "Si necesitas 1080p+, añade un PO token en Preferencias.",
                                "Aviso de calidad YouTube",
                                JOptionPane.INFORMATION_MESSAGE
                        );
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

                } catch (CancellationException ce) {
                    logArea.append("\n[STOP] Cancelled by user.\n");
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    logArea.append("ERROR when finished: "
                            + (cause != null ? cause.toString() : ee.toString()) + "\n");
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logArea.append("ERROR when finished: " + ie.toString() + "\n");
                } catch (IOException ex) {
                    System.getLogger(MainFrame.class.getName())
                            .log(System.Logger.Level.ERROR, (String) null, ex);
                } finally {
                    btnDownload.setEnabled(true);
                    btnStop.setEnabled(false);
                    currentProcess = null;
                    currentDownloadWorker = null;
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

    // Helper de btnDownload
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

    // Verificar si lo acabo usando
    public String getLastDownloadedFile() {
        return lastDownloadedFile;
    }

    // Datos necesarios para lanzar una descarga
    private static class DownloadContext {

        final String ytDlpPath;
        final String ffmpegPath;
        final String downloadDir;
        final String url;
        final boolean audio;
        final boolean isYouTube;

        DownloadContext(String ytDlpPath,
                String ffmpegPath,
                String downloadDir,
                String url,
                boolean audio,
                boolean isYouTube) {
            this.ytDlpPath = ytDlpPath;
            this.ffmpegPath = ffmpegPath;
            this.downloadDir = downloadDir;
            this.url = url;
            this.audio = audio;
            this.isYouTube = isYouTube;
        }
    }
}
