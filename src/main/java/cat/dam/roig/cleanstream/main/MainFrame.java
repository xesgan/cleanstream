package cat.dam.roig.cleanstream.main;

import cat.dam.roig.cleanstream.models.MetadataTableModel;
import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import cat.dam.roig.cleanstream.models.VideoQuality;
import cat.dam.roig.cleanstream.services.ApiClient;
import cat.dam.roig.cleanstream.services.AuthManager;
import cat.dam.roig.cleanstream.services.DownloadsScanner;
import cat.dam.roig.cleanstream.ui.AboutDialog;
import cat.dam.roig.cleanstream.ui.LoginPanel;
import cat.dam.roig.cleanstream.ui.PreferencesPanel;
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

    // --- Constantes de configuración por defecto ---
    private static final String YT_DLP_PATH = "/bin/yt-dlp";
    private static final String FFMPEG_PATH = "/bin/ffmpeg";      // opcional
    private static final String baseUrl = "https://dimedianetapi9.azurewebsites.net";

    // --- Dependencias de UI ---
    private PreferencesPanel pnlPreferencesPanel;
    private final DefaultListModel<ResourceDownloaded> downloadsModel = new DefaultListModel<>();
    private final ResourceDownloadedRenderer RDR = new ResourceDownloadedRenderer();
    private MetadataTableModel metaModel; // para la tabla de metadata

    private final ApiClient apiClient = new ApiClient(baseUrl);
    private final AuthManager authManager = new AuthManager(apiClient);
    private final LoginPanel loginPanel = new LoginPanel(authManager);

    // --- Lógica de estado ---
    private String lastDownloadedFile = null;
    private final List<ResourceDownloaded> resourceDownloadeds = new ArrayList<>();
    private boolean hasScanned = false;
    private boolean isScanning = false;

    // --- Descarga en curso ---
    private volatile Process currentProcess;
    private SwingWorker<Integer, String> currentDownloadWorker;

    // --- Referencias de log ---
    private JTextArea log;

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        // 1. Construye UI base (paneles, botones, menús...)
        initComponents();

        // 2. Panel de preferencias
        initPreferencesPanel();

        // 3. Configura ventana
        initWindow();

        // 4. Configura qué hacer cuando el login tenga éxito
        authManager.setOnLoginSuccess(this::showMainView);

        // 5. Intentamos auto-login inteligente
        if (authManager.tryAutoLogin()) {
            // Token válido → vamos a la vista principal
            showMainView();
        } else {
            // No hay token o no funciona → mostramos login
            showLogin();
        }
    }

    // ------------------- INIT HELPERS -------------------
    private void initWindow() {
        setTitle("CleanStream");
        setMinimumSize(new java.awt.Dimension(1200, 700));
        setPreferredSize(new java.awt.Dimension(1200, 700));
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void initPreferencesPanel() {
        pnlPreferencesPanel = new PreferencesPanel(this);
    }

    private void initDownloadsList() {
        lstDownloadScanList.setModel(downloadsModel);
        lstDownloadScanList.setCellRenderer(RDR);
        lstDownloadScanList.setFixedCellHeight(56);
    }

    private void initMetadataTable() {
        metaModel = new MetadataTableModel();
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
                btnDeleteDownloadFileFolder.setEnabled(sel != null);
            }
        });
    }

    private void initFilters() {
        cmbTipo.addActionListener(e -> applyFiltersIfReady());
        chkSemana.addActionListener(e -> applyFiltersIfReady());
        cmbTipo.setSelectedItem("Todo");  // arranque con "Todo"
    }

    // ------------------- NAVIGATION -------------------
    public void showPreferences() {
        showInContentPanel(pnlPreferencesPanel);
    }

    public void showMain() {
        showMainView();
    }

    public void showLogin() {
        showInContentPanel(loginPanel);
    }

    private void showInContentPanel(java.awt.Component comp) {
        pnlContent.removeAll();
        pnlContent.setLayout(new java.awt.BorderLayout());
        pnlContent.add(comp, java.awt.BorderLayout.CENTER);
        pnlContent.revalidate();
        pnlContent.repaint();
    }

    public void showMainView() {
        initDownloadsList();
        initMetadataTable();
        initFilters();
        showInContentPanel(pnlMainPanel);
    }

    public JTextArea getTxaLogArea() {
        return txaLogArea;
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
        mniLogout = new javax.swing.JMenuItem();
        mniExit = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        mniPreferences = new javax.swing.JMenuItem();
        mnuHelp = new javax.swing.JMenu();
        mniAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(new java.awt.Dimension(1200, 650));
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

        mniLogout.setText("Logout");
        mniLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniLogoutActionPerformed(evt);
            }
        });
        mnuFile.add(mniLogout);

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

    private void btnDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadActionPerformed
        startDownload();
    }//GEN-LAST:event_btnDownloadActionPerformed

    private void startDownload() {
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

    private DownloadContext buildDownloadContext() {
        String ytDlpPath = pnlPreferencesPanel.getTxtYtDlpPath();
        String ffmpegPath = pnlPreferencesPanel.getTxtFfpmegDir();
        String downloadDir = DetectOS.resolveDownloadDir(
                pnlPreferencesPanel.getTxtDownloadsDir().trim()
        );
        String url = txtUrl.getText().trim();

        if (url.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Video URL is missing.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        if (ytDlpPath.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Yt-Dlp path is missing. Please configure it in Preferences.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        File execFile = new File(ytDlpPath);
        if (!execFile.exists() || !execFile.canExecute()) {
            JOptionPane.showMessageDialog(
                    this,
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
            VideoQuality q = getSelectedQuality();
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
        command.add(ctx.url.trim());

        // debbuging
        System.out.println(">>> YT-DLP PATH EN CONTEXTO: " + ctx.ytDlpPath);
        System.out.println("CMD: " + String.join(" ", command));

        return command;
    }

    private void prepareUiBeforeDownload(List<String> command) {
        log = getTxaLogArea();
        log.setText("");
        log.append("CMD: " + String.join(" ", command) + "\n\n");

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
                    log.append(line + "\n");

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
                        log.append("\n[STOP] Cancelled by user.\n");
                        return;
                    }

                    int exit = get();

                    String allLog = txaLogArea.getText();

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
                        log.append("\nLimit Speed Applied: "
                                + pnlPreferencesPanel.getSldLimitSpeed());
                    }

                    if (exit == 0
                            && pnlPreferencesPanel.getChkCreateM3u()
                            && !downloadedFiles.isEmpty()) {

                        writeM3u(downloadedFiles, downloadDir);
                        log.append("\n[m3u] playlist updated\n");
                    }

                    if (exit == 0
                            && pnlPreferencesPanel.chkOpenWhenDone.isSelected()
                            && lastDownloadedFile != null) {

                        File file = new File(lastDownloadedFile);
                        if (file.exists()) {
                            log.append("Playing: " + file.getName() + "\n");
                            Desktop.getDesktop().open(file);
                        } else {
                            log.append("Couldn't find the downloaded file.\n");
                        }
                    }

                } catch (CancellationException ce) {
                    log.append("\n[STOP] Cancelled by user.\n");
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    log.append("ERROR when finished: "
                            + (cause != null ? cause.toString() : ee.toString()) + "\n");
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.append("ERROR when finished: " + ie.toString() + "\n");
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
                    resourceDownloadeds.clear();
                    resourceDownloadeds.addAll(lista);
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
        deleteSelectedDownloadFile();
    }//GEN-LAST:event_btnDeleteDownloadFileFolderActionPerformed

    /**
     * Elimina el recurso seleccionado en la lista (si existe) tanto del disco
     * como del modelo de la JList.
     */
    private void deleteSelectedDownloadFile() {
        int idx = lstDownloadScanList.getSelectedIndex();
        if (idx == -1) {
            // Nada seleccionado, no hacemos nada
            return;
        }

        ResourceDownloaded selected = lstDownloadScanList.getModel().getElementAt(idx);
        if (selected == null || selected.getRoute() == null || selected.getRoute().isBlank()) {
            return;
        }

        Path file = Paths.get(selected.getRoute());

        if (!confirmDeletion(file)) {
            return;
        }

        try {
            boolean deleted = Files.deleteIfExists(file);

            if (deleted) {
                downloadsModel.remove(idx);

                JOptionPane.showMessageDialog(
                        this,
                        "Archivo eliminado.",
                        "Eliminar",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo eliminar (¿ya no existe?).",
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE
                );
            }

        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, "Error al eliminar archivo", ex);
            JOptionPane.showMessageDialog(
                    this,
                    "Error al eliminar:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar el archivo.
     *
     * @param file Ruta del archivo a eliminar.
     * @return true si el usuario confirma, false en caso contrario.
     */
    private boolean confirmDeletion(Path file) {
        int opt = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar el archivo?\n" + file,
                "Eliminar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opt == JOptionPane.YES_OPTION;
    }


    private void rbAudioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbAudioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbAudioActionPerformed

    private void btnOpenLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenLastActionPerformed
        openLastDownloadedFile();
    }//GEN-LAST:event_btnOpenLastActionPerformed

    /**
     * Intenta abrir el último archivo descargado. Si es una playlist (.m3u /
     * .m3u8), usa VLC. En caso contrario, usa Desktop.open().
     */
    private void openLastDownloadedFile() {
        String last = getLastDownloadedFile();

        if (last == null || last.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No previous download found.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
            btnOpenLast.setEnabled(false);
            return;
        }

        File file = new File(last);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The last downloaded file cannot be found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            btnOpenLast.setEnabled(false);
            return;
        }

        try {
            if (isPlaylistFile(file)) {
                // Abrimos con VLC si es una playlist
                new ProcessBuilder("vlc", file.getAbsolutePath()).start();
            } else {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not open the file:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            btnOpenLast.setEnabled(false);
            return;
        }

        // Si ha ido bien, actualizamos el último archivo abierto
        setLastDownloadedFile(last);
        btnOpenLast.setEnabled(true);
    }

    /**
     * Indica si el archivo es una playlist soportada (.m3u / .m3u8).
     */
    private boolean isPlaylistFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".m3u") || name.endsWith(".m3u8");
    }


    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        stopCurrentDownload();
    }//GEN-LAST:event_btnStopActionPerformed

    private void mniLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniLogoutActionPerformed
        doLogout();
    }//GEN-LAST:event_mniLogoutActionPerformed

    private void doLogout() {
        // Preguntar si esta seguro
        int opt = JOptionPane.showConfirmDialog(
                this,
                "Do you really want to log out?",
                "Confirm logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (opt != JOptionPane.YES_OPTION) {
            return; // usuario canceló
        }
        // Limpiar estado de sesion
        authManager.clearRememberMe();
        // Volver a la pantalla login
        showLogin();
    }

    /**
     * Solicita detener la descarga en curso. Cancela el SwingWorker y
     * deshabilita los botones correspondientes.
     */
    private void stopCurrentDownload() {

        if (currentDownloadWorker != null && !currentDownloadWorker.isDone()) {
            currentDownloadWorker.cancel(false);
        }

        getTxaLogArea().append("[STOP] Solicitud de cancelación enviada.\n");

        btnStop.setEnabled(false);
        btnDownload.setEnabled(true);
    }

    // ------ FILTROS DE DESCARGAS ------
    /**
     * Aplica los filtros de tipo y semana solo si ya se ha realizado al menos
     * un escaneo y no hay uno en curso.
     */
    private void applyFiltersIfReady() {
        if (!hasScanned || isScanning) {
            return;  // no hay datos o estoy escaneando
        }
        applyFilters();
    }

    /**
     * Rellena el modelo de la JList en función de los filtros activos.
     */
    private void applyFilters() {
        downloadsModel.clear();
        for (ResourceDownloaded r : resourceDownloadeds) {
            if (matchTipo(r) && matchSemana(r)) {
                downloadsModel.addElement(r);
            }
        }
    }

    // ---- helpers de filtro ----
    private static String norm(String s) {
        return (s == null) ? "" : s.toLowerCase(java.util.Locale.ROOT).trim();
    }

    // Extensiones conocidas de audio / vídeo para desambiguar cuando el mimeType no ayuda
    private static final java.util.Set<String> AUDIO_EXTENSIONS
            = java.util.Set.of("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus");

    private static final java.util.Set<String> VIDEO_EXTENSIONS
            = java.util.Set.of("mp4", "mkv", "avi", "mov", "webm", "flv");

    private static boolean esAudio(ResourceDownloaded r) {
        String mt = norm(r.getMimeType());
        String ex = norm(r.getExtension()).replace(".", "");

        if (mt.startsWith("audio/")) {
            return true;
        }
        if (mt.startsWith("video/")) {
            return false;
        }
        return AUDIO_EXTENSIONS.contains(ex);
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
        return VIDEO_EXTENSIONS.contains(ex);
    }

    private boolean matchTipo(ResourceDownloaded r) {
        String tipo = norm(String.valueOf(cmbTipo.getSelectedItem()));

        if (tipo.contains("video")) {
            return esVideo(r) && !esAudio(r);
        }
        if (tipo.contains("audio")) {
            return esAudio(r) && !esVideo(r);
        }
        return true; // "Todo" / "Todos"
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
    private javax.swing.JMenuItem mniLogout;
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
