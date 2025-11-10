package cat.dam.roig.cleanstream;

import cat.dam.roig.cleanstream.models.MetadataTableModel;
import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import cat.dam.roig.cleanstream.models.VideoQuality;
import cat.dam.roig.cleanstream.services.DownloadsScanner;
import cat.dam.roig.cleanstream.utils.CommandExecutor;
import cat.dam.roig.cleanstream.utils.DetectOS;
import cat.dam.roig.cleanstream.view.ResourceDownloadedRenderer;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 *
 * @author metku
 */
public class MainFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());
    private PreferencesPanel pnlPreferencesPanel;
    private DetectOS scanOS;
    private static final String YT_DLP_PATH = "/bin/yt-dlp";
    private static final String FFMPEG_PATH = "/bin/ffmpeg";      // opcional
    private static final String COOKIES_TXT = System.getProperty("user.home") + "/Downloads/youtube_cookies.txt"; // fallback
    private String lastDownloadedFile = null;
    private final DefaultListModel<ResourceDownloaded> downloadsModel = new DefaultListModel<>();
    private final ResourceDownloadedRenderer RDR = new ResourceDownloadedRenderer();
    private final List<ResourceDownloaded> master = new ArrayList<>();
    private MetadataTableModel metaModel; // para la tabla de metadata
    private boolean hasScanned = false;
    private boolean isScanning = false;
    private volatile Process currentProcess;
    private volatile boolean stopRequest = false;
    private SwingWorker<Integer, String> currentDownloadWorker;
    private JTextArea log;

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();

        pnlPreferencesPanel = new PreferencesPanel(this);

        setResizable(false);
        setLocationRelativeTo(null);

        // Same size and position
        pnlMainPanel.setBounds(0, 0, getWidth(), getHeight());
        pnlPreferencesPanel.setBounds(0, 0, getWidth(), getHeight());

        getContentPane().add(pnlPreferencesPanel);
        pnlPreferencesPanel.setVisible(false);
        pnlContent.setVisible(true);

        // JList (renderer + model solo una vez)
        lstDownloadScanList.setModel(downloadsModel);
        lstDownloadScanList.setCellRenderer(new ResourceDownloadedRenderer());
        lstDownloadScanList.setFixedCellHeight(56);

        // Selección -> metadata
        MetadataTableModel metaModel = new MetadataTableModel();
        tblMetaData.setModel(metaModel);

        var col0 = tblMetaData.getColumnModel().getColumn(0);
        col0.setPreferredWidth(120);
        col0.setMinWidth(100);
        col0.setMaxWidth(180);

        btnDeleteDownloadFileFolder.setEnabled(false);
        lstDownloadScanList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ResourceDownloaded sel = lstDownloadScanList.getSelectedValue();
                metaModel.setResource(sel);
                btnDeleteDownloadFileFolder.setEnabled(true);
            }
        });

        cmbTipo.addActionListener(e -> applyFiltersIfReady());
        chkSemana.addActionListener(e -> applyFiltersIfReady());
        // arranque con "Todo"
        cmbTipo.setSelectedItem("Todo");
    }

    // Navigation Methods
    public void showPreferences() {
        pnlContent.setVisible(false);
        pnlPreferencesPanel.setVisible(true);
        // Ordering the layers
        getContentPane().setComponentZOrder(pnlPreferencesPanel, 0);
    }

    public void showMain() {
        pnlPreferencesPanel.setVisible(false);
        pnlContent.setVisible(true);
        getContentPane().setComponentZOrder(pnlContent, 0);
    }

    public JTextArea getTxaLogArea() {
        return txaLogArea;
    }

    // ------- HELPERS --------
    private static void setExtractorClient(java.util.List<String> cmd, String client) {
        int idx = cmd.indexOf("--extractor-args");
        if (idx >= 0 && idx + 1 < cmd.size()) {
            cmd.set(idx + 1, "youtube:player_client=" + client);
        } else {
            cmd.add("--extractor-args");
            cmd.add("youtube:player_client=" + client);
        }
    }

    /**
     * Elimina una opción y su valor inmediatamente siguiente. Solo la primera
     * ocurrencia.
     */
    private static void removeOptionWithValue(java.util.List<String> cmd, String option) {
        for (int i = 0; i < cmd.size(); i++) {
            if (option.equals(cmd.get(i))) {
                cmd.remove(i);                  // quita la opción
                if (i < cmd.size()) {
                    cmd.remove(i); // quita el valor que ahora ocupa ese índice
                }
                break;
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgFormat = new javax.swing.ButtonGroup();
        bgQuality = new javax.swing.ButtonGroup();
        pnlContent = new javax.swing.JPanel();
        pnlMainPanel = new javax.swing.JPanel();
        lblUrl = new javax.swing.JLabel();
        txtUrl = new javax.swing.JTextField();
        btnPaste = new java.awt.Button();
        btnClear = new java.awt.Button();
        lblFormat = new javax.swing.JLabel();
        rbVideo = new javax.swing.JRadioButton();
        rbAudio = new javax.swing.JRadioButton();
        lblOptions = new javax.swing.JLabel();
        lblControls = new javax.swing.JLabel();
        btnDownload = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        btnOpenLast = new javax.swing.JButton();
        lblOutput = new javax.swing.JLabel();
        scrLogArea = new javax.swing.JScrollPane();
        txaLogArea = new javax.swing.JTextArea();
        lblStatus = new javax.swing.JLabel();
        lblActualDir = new javax.swing.JLabel();
        scpScanListPane = new javax.swing.JScrollPane();
        lstDownloadScanList = new javax.swing.JList<>();
        btnScanDownloadFolder = new javax.swing.JButton();
        btnDeleteDownloadFileFolder = new javax.swing.JButton();
        scpMetaDataTable = new javax.swing.JScrollPane();
        tblMetaData = new javax.swing.JTable();
        cmbTipo = new javax.swing.JComboBox<>();
        chkSemana = new javax.swing.JCheckBox();
        jrbBestAvailable = new javax.swing.JRadioButton();
        jrb1080p = new javax.swing.JRadioButton();
        jrb720p = new javax.swing.JRadioButton();
        jrb480p = new javax.swing.JRadioButton();
        mnbBar = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        mniExit = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        mniPreferences = new javax.swing.JMenuItem();
        mnuHelp = new javax.swing.JMenu();
        mniAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1200, 700));
        setResizable(false);
        setSize(new java.awt.Dimension(1200, 700));
        getContentPane().setLayout(null);

        pnlContent.setLayout(new java.awt.CardLayout());

        pnlMainPanel.setLayout(null);

        lblUrl.setFont(new java.awt.Font("sansserif", 0, 15)); // NOI18N
        lblUrl.setText("URL:");
        pnlMainPanel.add(lblUrl);
        lblUrl.setBounds(40, 100, 46, 29);

        txtUrl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtUrlActionPerformed(evt);
            }
        });
        pnlMainPanel.add(txtUrl);
        txtUrl.setBounds(110, 100, 320, 24);

        btnPaste.setLabel("Paste");
        pnlMainPanel.add(btnPaste);
        btnPaste.setBounds(460, 100, 47, 25);

        btnClear.setLabel("Clear");
        pnlMainPanel.add(btnClear);
        btnClear.setBounds(520, 100, 46, 25);

        lblFormat.setText("Format:");
        pnlMainPanel.add(lblFormat);
        lblFormat.setBounds(40, 160, 47, 18);

        bgFormat.add(rbVideo);
        rbVideo.setSelected(true);
        rbVideo.setText("Video");
        rbVideo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbVideoActionPerformed(evt);
            }
        });
        pnlMainPanel.add(rbVideo);
        rbVideo.setBounds(110, 160, 57, 22);

        bgFormat.add(rbAudio);
        rbAudio.setText("Audio");
        rbAudio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbAudioActionPerformed(evt);
            }
        });
        pnlMainPanel.add(rbAudio);
        rbAudio.setBounds(200, 160, 58, 22);

        lblOptions.setText("Quality:");
        pnlMainPanel.add(lblOptions);
        lblOptions.setBounds(40, 200, 46, 18);

        lblControls.setText("Controls:");
        pnlMainPanel.add(lblControls);
        lblControls.setBounds(40, 280, 54, 18);

        btnDownload.setText("Download");
        btnDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownloadActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnDownload);
        btnDownload.setBounds(30, 310, 140, 24);

        btnStop.setText("Stop");
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnStop);
        btnStop.setBounds(230, 310, 140, 24);

        btnOpenLast.setText("Open last");
        btnOpenLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenLastActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnOpenLast);
        btnOpenLast.setBounds(430, 310, 140, 24);

        lblOutput.setText("Output:");
        pnlMainPanel.add(lblOutput);
        lblOutput.setBounds(40, 350, 47, 18);

        txaLogArea.setEditable(false);
        txaLogArea.setColumns(20);
        txaLogArea.setRows(5);
        scrLogArea.setViewportView(txaLogArea);

        pnlMainPanel.add(scrLogArea);
        scrLogArea.setBounds(30, 380, 550, 170);
        pnlMainPanel.add(lblStatus);
        lblStatus.setBounds(30, 560, 115, 27);
        pnlMainPanel.add(lblActualDir);
        lblActualDir.setBounds(400, 560, 163, 27);

        scpScanListPane.setViewportView(lstDownloadScanList);

        pnlMainPanel.add(scpScanListPane);
        scpScanListPane.setBounds(600, 140, 560, 230);

        btnScanDownloadFolder.setText("Scan");
        btnScanDownloadFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnScanDownloadFolderActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnScanDownloadFolder);
        btnScanDownloadFolder.setBounds(990, 100, 72, 24);

        btnDeleteDownloadFileFolder.setText("Delete");
        btnDeleteDownloadFileFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteDownloadFileFolderActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnDeleteDownloadFileFolder);
        btnDeleteDownloadFileFolder.setBounds(1070, 100, 72, 24);

        tblMetaData.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        scpMetaDataTable.setViewportView(tblMetaData);

        pnlMainPanel.add(scpMetaDataTable);
        scpMetaDataTable.setBounds(600, 380, 560, 170);

        cmbTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "Only Video", "Only Audio" }));
        cmbTipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbTipoActionPerformed(evt);
            }
        });
        pnlMainPanel.add(cmbTipo);
        cmbTipo.setBounds(760, 100, 110, 24);

        chkSemana.setText("This Week");
        pnlMainPanel.add(chkSemana);
        chkSemana.setBounds(890, 100, 100, 22);

        bgQuality.add(jrbBestAvailable);
        jrbBestAvailable.setSelected(true);
        jrbBestAvailable.setText("Best Available");
        pnlMainPanel.add(jrbBestAvailable);
        jrbBestAvailable.setBounds(50, 230, 105, 22);

        bgQuality.add(jrb1080p);
        jrb1080p.setText("1080p");
        pnlMainPanel.add(jrb1080p);
        jrb1080p.setBounds(170, 230, 60, 22);

        bgQuality.add(jrb720p);
        jrb720p.setText("720p");
        pnlMainPanel.add(jrb720p);
        jrb720p.setBounds(250, 230, 60, 22);

        bgQuality.add(jrb480p);
        jrb480p.setText("480p");
        pnlMainPanel.add(jrb480p);
        jrb480p.setBounds(320, 230, 60, 22);

        pnlContent.add(pnlMainPanel, "card3");

        getContentPane().add(pnlContent);
        pnlContent.setBounds(0, 0, 1200, 610);

        mnuFile.setText("File");

        mniExit.setText("Exit");
        mniExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniExitActionPerformed(evt);
            }
        });
        mnuFile.add(mniExit);

        mnbBar.add(mnuFile);

        mnuEdit.setText("Edit");

        mniPreferences.setText("Preferences");
        mniPreferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniPreferencesActionPerformed(evt);
            }
        });
        mnuEdit.add(mniPreferences);

        mnbBar.add(mnuEdit);

        mnuHelp.setText("Help");

        mniAbout.setText("About");
        mniAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniAboutActionPerformed(evt);
            }
        });
        mnuHelp.add(mniAbout);

        mnbBar.add(mnuHelp);

        setJMenuBar(mnbBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtUrlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtUrlActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtUrlActionPerformed

    private void rbVideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbVideoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbVideoActionPerformed

    private void mniExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_mniExitActionPerformed

    private void mniPreferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniPreferencesActionPerformed
        // TODO add your handling code here:
        showPreferences();
    }//GEN-LAST:event_mniPreferencesActionPerformed

    private void btnDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadActionPerformed
        String ytDlpPath = pnlPreferencesPanel.getTxtYtDlpPath();
        String ffmpegPath = pnlPreferencesPanel.getTxtFfpmegDir();

        final String downloadDir = DetectOS.resolveDownloadDir(pnlPreferencesPanel.getTxtDownloadsDir().trim());
        final List<String> downloadedFiles = new ArrayList<>();
        String url = txtUrl.getText().trim();

        if (url.isBlank()) {
            JOptionPane.showMessageDialog(this, "Video URL is missing.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Aqui hacemos la validacion de que el usuario haya ingresado bien la ruta de su yt-dlp antes de empezar.
        if (ytDlpPath.isBlank()) {
            JOptionPane.showMessageDialog(this, "Yt-Dlp path is missing. Please configure it in Preferences.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File execFile = new File(ytDlpPath);
        if (!execFile.exists() || !execFile.canExecute()) {
            JOptionPane.showMessageDialog(this, "yt-dlp executable not found or not accessible. \n Check your Preferences path.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean audio = rbAudio.isSelected();

        // Construcción del comando base
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(ytDlpPath);

        if (audio) {
            command.add("-x");

            // Forzar formato de salida
            String audioFormat = "mp3"; // o desde combo si lo tienes
            command.add("--audio-format");
            command.add(audioFormat);
        } else {

// Insertamos calidad seleccionada (mantén tu mapeo actual en appendQualityArgs)
            VideoQuality q = getSelectedQuality();
            CommandExecutor.appendQualityArgs(command, q);
        }

// Directorio de salida
        if (!downloadDir.isBlank()) {
            command.add("-P");
            command.add(downloadDir);
            command.add("-o");
            command.add("%(title)s.%(ext)s");
        }

// Ruta fija a ffmpeg
        if (ffmpegPath != null && !ffmpegPath.isBlank() && new File(ffmpegPath).exists()) {
            command.add("--ffmpeg-location");
            command.add(ffmpegPath);
        }

// Detecta si es YouTube
        boolean isYouTube = url.contains("youtube.com") || url.contains("youtu.be");

// Flags de estabilidad comunes
        command.add("--force-ipv4");
        command.add("--http-chunk-size");
        command.add("10M");
        command.add("--concurrent-fragments");
        command.add("1");
        command.add("--retries");
        command.add("infinite");
        command.add("--fragment-retries");
        command.add("infinite");

// >>> Diferenciación por sitio
        if (isYouTube) {
            // Para YouTube: NO cookies ni headers personalizados (activan SABR con más frecuencia)
            // Cliente alternativo “limpio”
            command.add("--extractor-args");
            command.add("youtube:player_client=web_safari");

            // Evita que cargue config global y caché del sistema
            command.add("--ignore-config");
            command.add("--no-cache-dir");

        } else {
            // Para otros sitios: mantén tus headers/cookies como antes
            command.add("--user-agent");
            command.add("Mozilla/5.0");
            command.add("--add-header");
            command.add("Referer:https://www.youtube.com/"); // si no es YouTube, elimina o cambia el referer adecuado
            command.add("--add-header");
            command.add("Accept-Language:es-ES,es;q=0.9");
            command.add("--cookies-from-browser");
            command.add("vivaldi:Default::" + System.getProperty("user.home") + "/.config/vivaldi");
        }

// Imprime en stdout la ruta final de cada item descargado
        command.add("--print");
        command.add("after_move:filepath");

// Limit rate (si procede)
        if (pnlPreferencesPanel.chkLimitSpeed.isSelected()) {
            String rate = pnlPreferencesPanel.getSldLimitSpeed();
            if (rate != null && !rate.isBlank()) {
                command.add("--limit-rate");
                command.add(rate);
            }
        }

// Imprime la calidad REAL seleccionada
        command.add("--print");
        command.add("QUALITY:%(format_id)s|%(resolution)s|%(fps)s|v:%(vcodec)s|a:%(acodec)s");

// URL al final
        command.add(url.trim());

        // Mostrar comando y versión
        java.util.List<String> verCmd = java.util.List.of(ytDlpPath, "--version");
        log = getTxaLogArea();
        log.setText("");
        log.append("CMD: " + String.join(" ", command) + "\n\n");

        btnDownload.setEnabled(false);
        btnStop.setEnabled(true);

        // SwingWorker
        currentDownloadWorker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                try {
                    // 0) versión
                    CommandExecutor.runStreaming(java.util.List.of(ytDlpPath, "--version"),
                            line -> publish("[yt-dlp --version] " + line));

                    if (isCancelled()) {
                        return -2;
                    }

                    // === 1) INTENTO WEB (con cookies) ===
                    java.util.List<String> cmdWeb = new java.util.ArrayList<>(command);
                    setExtractorClient(cmdWeb, "web");
                    publish("[try] web + cookies");
                    int exitWeb = CommandExecutor.runStreaming(
                            cmdWeb,
                            this::publish,
                            p -> currentProcess = p // <<-- CAPTURAMOS EL PROCESS
                    );

                    if (isCancelled()) {
                        destroyProcessQuiet();
                        return -2;
                    }

                    if (exitWeb == 0) {
                        return 0; // ✅ no reintentes si ya fue bien
                    }

                    // === 2) FALLBACK ANDROID (sin cookies) ===
                    java.util.List<String> cmdAndroid = new java.util.ArrayList<>(command);
                    setExtractorClient(cmdAndroid, "android");
                    // Android no soporta cookies → quitar opción y su valor
                    removeOptionWithValue(cmdAndroid, "--cookies-from-browser");
                    publish("[retry] android (without cookies)");
                    int exitAndroid = CommandExecutor.runStreaming(cmdAndroid, this::publish, p -> currentProcess = p);

                    if (isCancelled()) {
                        destroyProcessQuiet();
                        return -2;
                    }
                    return exitAndroid;

                } catch (Exception e) {
                    String msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
                    publish("ERROR: " + msg);
                    e.printStackTrace(); // para ver el origen real en consola
                    return -1;
                } finally {
                    // ya no habria proceso asociado
                    currentProcess = null;
                }
            }

            @Override
            protected void process(java.util.List<String> lines) {
                for (String line : lines) {
                    log.append(line + "\n");

                    // Captura directa del ouput de --print after_move:filepath
                    if (!line.isBlank() && new File(line).isAbsolute()) {
                        downloadedFiles.add(line);
                        lastDownloadedFile = line.trim();
                        continue;
                    }

                    // Detectar el archivo descargado (fallback)
                    if (line.contains("Destination:")) {
                        String path = line.substring(line.indexOf("Destination:") + "Destination:".length()).trim();
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

                    // Si el worker está cancelado, no intentes get(): sal limpio
                    if (isCancelled()) {
                        log.append("\n[STOP] Cancelled by user.\n");
                        return; // evita .m3u y abrir el player
                    }

                    int exit = get();

                    // Obtener el texto completo del log
                    String allLog = txaLogArea.getText();

                    // Buscar el patrón que indica SABR (360p)
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

                    log.append("\nProcess ended with code: " + exit + "\n");
                    log.append("OS Detected: " + DetectOS.detectOS());
                    log.append("\nDownload dir (final): " + downloadDir);

                    if (pnlPreferencesPanel.chkLimitSpeed.isSelected()) {
                        log.append("\nLimit Speed Applied: " + pnlPreferencesPanel.getSldLimitSpeed());
                    }

                    // Crear .m3u si todo OK
                    if (exit == 0 && pnlPreferencesPanel.getChkCreateM3u() && !downloadedFiles.isEmpty()) {
                        writeM3u(downloadedFiles, downloadDir); // el nombre ya no se usa
                        log.append("\n[m3u] playlist updated\n");
                    }

                    // Solo abrir si el checkbox está marcado y el proceso fue correcto
                    if (exit == 0 && pnlPreferencesPanel.chkOpenWhenDone.isSelected() && lastDownloadedFile != null) {
                        java.io.File file = new java.io.File(lastDownloadedFile);
                        if (file.exists()) {
                            log.append("Playing: " + file.getName() + "\n");
                            java.awt.Desktop.getDesktop().open(file);
                        } else {
                            log.append("Couldn't find the downloaded file.\n");
                        }
                    }

                } catch (CancellationException ce) {
                    log.append("\n[STOP] Cancelled by user.\n");
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    log.append("ERROR when finished: " + (cause != null ? cause.toString() : ee.toString()) + "\n");
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.append("ERROR when finished: " + ie.toString() + "\n");
                } catch (IOException ex) {
                    System.getLogger(MainFrame.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
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

        currentDownloadWorker.execute();
    }//GEN-LAST:event_btnDownloadActionPerformed

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
            JOptionPane.showMessageDialog(this,
                    "Playlist updated: " + m3u.toAbsolutePath(),
                    "Playlist", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Couldn't update playlist:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mniAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniAboutActionPerformed
        // TODO add your handling code here:
        AboutDialog dlg = new AboutDialog(this, true); // modal
        dlg.setVisible(true);
    }//GEN-LAST:event_mniAboutActionPerformed

    private void btnScanDownloadFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnScanDownloadFolderActionPerformed
        // 1) Leer ruta desde preferencias
        String input = pnlPreferencesPanel.getTxtScanDownloadsFolder().getText();
        String finalDirStr = DetectOS.resolveDownloadDir(input);
        Path downloads = java.nio.file.Paths.get(finalDirStr);

        btnScanDownloadFolder.setEnabled(false);

        SwingWorker<java.util.List<ResourceDownloaded>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.List<ResourceDownloaded> doInBackground() {
                try {
                    DownloadsScanner scanner = new DownloadsScanner();
                    return scanner.scan(downloads, false); // no recursivo
                } catch (java.io.IOException e) {
                    System.err.println("Scan error: " + e.getMessage());
                    return java.util.Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    List<ResourceDownloaded> lista = get();
                    master.clear();
                    master.addAll(lista);
                    hasScanned = true;
                    applyFilters(); // ahora sí aplico filtros con datos nuevos
                } catch (InterruptedException ie) {
                    // Vuelves a marcar el hilo como interrumpido
                    Thread.currentThread().interrupt();
                    System.err.println("Scan interrumpido.");
                } catch (ExecutionException ee) {
                    // El fallo real está en ee.getCause()
                    Throwable cause = ee.getCause();
                    System.err.println("Scan falló: " + (cause != null ? cause.getMessage() : ee.getMessage()));
                    cause.printStackTrace();
                } finally {
                    isScanning = false;          // <- si usas el flag
                    btnScanDownloadFolder.setEnabled(true);
                }
            }
        };
        worker.execute();
    }//GEN-LAST:event_btnScanDownloadFolderActionPerformed

    private void cmbTipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbTipoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbTipoActionPerformed

    private void btnDeleteDownloadFileFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteDownloadFileFolderActionPerformed
        int idx = lstDownloadScanList.getSelectedIndex();
        if (idx == -1) {
            return;
        }

        ResourceDownloaded rd = lstDownloadScanList.getModel().getElementAt(idx);
        String route = rd.getRoute();
        if (route == null || route.isBlank()) {
            return;
        }
        Path file = Paths.get(route);

        int opt = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar el archivo?\n" + file,
                "Eliminar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (opt != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean deleted = java.nio.file.Files.deleteIfExists(file);
            if (deleted) {
                // Quitar del modelo
                DefaultListModel<ResourceDownloaded> model = (DefaultListModel<ResourceDownloaded>) lstDownloadScanList.getModel();
                model.remove(idx);
                // (Opcional) si tienes JTable o contador, refresca aquí
                JOptionPane.showMessageDialog(this, "Archivo eliminado.");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar (¿ya no existe?).", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnDeleteDownloadFileFolderActionPerformed

    private void rbAudioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbAudioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbAudioActionPerformed

    private void btnOpenLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenLastActionPerformed
        String last = getLastDownloadedFile();

        if (last == null || last.isBlank()) {
            JOptionPane.showMessageDialog(this, "No previous download found.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        File f = new File(last);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "The last downloaded file cannot be found.", "Error", JOptionPane.ERROR_MESSAGE);
            btnOpenLast.setEnabled(false);
            return;
        }

        try {
            if (f.getName().endsWith(".m3u") || f.getName().endsWith(".m3u8")) {
                new ProcessBuilder("vlc", f.getAbsolutePath()).start();
            } else {
                Desktop.getDesktop().open(f);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not open the file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        setLastDownloadedFile(lastDownloadedFile);
        btnOpenLast.setEnabled(true);

    }//GEN-LAST:event_btnOpenLastActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        stopRequest = true;
        // Cancelamos el worker
        if (currentDownloadWorker != null && !currentDownloadWorker.isDone()) {
            currentDownloadWorker.cancel(false);
        }

        // intenta parar el proceso (si aún no está asignado no pasa nada)
        getTxaLogArea().append("[STOP] Solicitud de cancelación enviada.\n");
        btnStop.setEnabled(false);
        btnDownload.setEnabled(true);
    }//GEN-LAST:event_btnStopActionPerformed

    private void applyFiltersIfReady() {
        if (!hasScanned || isScanning) {
            return;  // no hay datos o estoy escaneando
        }
        applyFilters(); // tu antiguo reloadListFiltered()
    }

    private void applyFilters() {
        downloadsModel.clear();
        for (ResourceDownloaded r : master) {
            if (matchTipo(r) && matchSemana(r)) {
                downloadsModel.addElement(r);
            }
        }
    }

    // ---- filtros ----
    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(java.util.Locale.ROOT).trim();
    }

    private static boolean esAudio(ResourceDownloaded r) {
        String mt = norm(r.getMimeType());
        String ex = norm(r.getExtension()).replace(".", "");
        if (mt.startsWith("audio/")) {
            return true;
        }
        if (mt.startsWith("video/")) {
            return false;
        }
        return java.util.Set.of("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus").contains(ex);
    }

    private static boolean esVideo(ResourceDownloaded r) {
        String mt = norm(r.getMimeType());
        String ex = norm(r.getExtension()).replace(".", "");
        if (mt.startsWith("video/")) {
            return true;
        }
        if (mt.startsWith("audio/")) {
            return false;
        }
        return java.util.Set.of("mp4", "mkv", "avi", "mov", "webm", "flv").contains(ex);
    }

    private boolean matchTipo(ResourceDownloaded r) {
        String tipo = norm(String.valueOf(cmbTipo.getSelectedItem()));
        if (tipo.contains("video")) {
            return esVideo(r) && !esAudio(r);
        }
        if (tipo.contains("audio")) {
            return esAudio(r) && !esVideo(r);
        }
        return true; // Todo/Todos
    }

    private boolean matchSemana(ResourceDownloaded r) {
        if (!chkSemana.isSelected()) {
            return true;
        }
        if (r.getDownloadDate() == null) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        LocalDate ini = hoy.with(DayOfWeek.MONDAY);
        LocalDate fin = ini.plusDays(6);
        LocalDate f = r.getDownloadDate().toLocalDate();
        return !f.isBefore(ini) && !f.isAfter(fin);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // ----- GETTERS Y SETTERS ------
    public String getLastDownloadedFile() {
        return lastDownloadedFile;
    }

    public void setLastDownloadedFile(String lastDownloadedFile) {
        this.lastDownloadedFile = lastDownloadedFile;
    }

    public VideoQuality getSelectedQuality() {
        if (jrb1080p.isSelected()) {
            return VideoQuality.P1080;
        }
        if (jrb720p.isSelected()) {
            return VideoQuality.P720;
        }
        if (jrb480p.isSelected()) {
            return VideoQuality.P480;
        }
        return VideoQuality.BEST_AVAILABLE;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgFormat;
    private javax.swing.ButtonGroup bgQuality;
    private java.awt.Button btnClear;
    private javax.swing.JButton btnDeleteDownloadFileFolder;
    private javax.swing.JButton btnDownload;
    private javax.swing.JButton btnOpenLast;
    private java.awt.Button btnPaste;
    private javax.swing.JButton btnScanDownloadFolder;
    private javax.swing.JButton btnStop;
    private javax.swing.JCheckBox chkSemana;
    private javax.swing.JComboBox<String> cmbTipo;
    private javax.swing.JRadioButton jrb1080p;
    private javax.swing.JRadioButton jrb480p;
    private javax.swing.JRadioButton jrb720p;
    private javax.swing.JRadioButton jrbBestAvailable;
    private javax.swing.JLabel lblActualDir;
    private javax.swing.JLabel lblControls;
    private javax.swing.JLabel lblFormat;
    private javax.swing.JLabel lblOptions;
    private javax.swing.JLabel lblOutput;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblUrl;
    private javax.swing.JList<ResourceDownloaded> lstDownloadScanList;
    private javax.swing.JMenuBar mnbBar;
    private javax.swing.JMenuItem mniAbout;
    private javax.swing.JMenuItem mniExit;
    private javax.swing.JMenuItem mniPreferences;
    private javax.swing.JMenu mnuEdit;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenu mnuHelp;
    private javax.swing.JPanel pnlContent;
    private javax.swing.JPanel pnlMainPanel;
    private javax.swing.JRadioButton rbAudio;
    private javax.swing.JRadioButton rbVideo;
    private javax.swing.JScrollPane scpMetaDataTable;
    private javax.swing.JScrollPane scpScanListPane;
    private javax.swing.JScrollPane scrLogArea;
    private javax.swing.JTable tblMetaData;
    private javax.swing.JTextArea txaLogArea;
    private javax.swing.JTextField txtUrl;
    // End of variables declaration//GEN-END:variables
}
