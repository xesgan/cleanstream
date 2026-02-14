package cat.dam.roig.cleanstream.main;

import cat.dam.roig.cleanstream.controller.DownloadsController;
import cat.dam.roig.cleanstream.models.MetadataTableModel;
import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import cat.dam.roig.cleanstream.models.VideoQuality;
import cat.dam.roig.cleanstream.services.AuthManager;
import cat.dam.roig.cleanstream.ui.AboutDialog;
import cat.dam.roig.cleanstream.ui.LoginPanel;
import cat.dam.roig.cleanstream.ui.UiColors;
import cat.dam.roig.cleanstream.ui.PreferencesPanel;
import cat.dam.roig.cleanstream.utils.DetectOS;
import cat.dam.roig.cleanstream.controller.DownloadExecutionController;
import cat.dam.roig.cleanstream.controller.MainController;
import cat.dam.roig.cleanstream.services.UserPreferences;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 *
 * @author metku
 */
public class MainFrame extends javax.swing.JFrame {

    // Dependencias Controller
    private DownloadsController downloadsController;

    // --- Dependencias de UI ---
    private PreferencesPanel pnlPreferencesPanel;
    private final DefaultListModel<ResourceDownloaded> downloadsModel = new DefaultListModel<>();
    private final List<ResourceDownloaded> resourceDownloadeds = new ArrayList<>();
    private MetadataTableModel metaModel; // para la tabla de metadata

    private final RoigMediaPollingComponent mediaComponent;
    private final AuthManager authManager;
    private final LoginPanel loginPanel;
    private final DownloadExecutionController downloadExecutionController;
    private final MainController mainController;

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        // 1. Construye UI base (paneles, botones, menús...)
        initComponents();

        lblStatusScan.setText(" ");                 // texto vacío pero no null
        lblStatusScan.setPreferredSize(new Dimension(250, 18));
        lblStatusScan.setMinimumSize(new Dimension(50, 18));

        initUx();

        // 3. Configura ventana
        initWindow();

        this.mediaComponent = roigMediaPollingComponent;
        this.authManager = new AuthManager(mediaComponent);
        this.loginPanel = new LoginPanel(authManager);
        authManager.setLoginPanel(loginPanel);

        initPreferencesPanel();

        loadPreferencesToUi();

        initDownloadsList();
        initMetadataTable();
        initFilters();

        this.mainController = new MainController(this, authManager, mediaComponent);

        downloadExecutionController = new DownloadExecutionController(
                this,
                pnlPreferencesPanel,
                txtUrl,
                txaLogArea,
                btnDownload,
                btnStop,
                rbAudio,
                pbDownload,
                downloadsController
        );
    }

    public PreferencesPanel getPnlPreferencesPanel() {
        return pnlPreferencesPanel;
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
        pnlPreferencesPanel.getBtnSave().addActionListener(e -> savePreferencesFromUi());
    }

    private void loadPreferencesToUi() {
        // Carpeta de descargas
        String downloadDir = UserPreferences.getDownloadDir();
        if (downloadDir != null) {
            pnlPreferencesPanel.getTxtDownloadsDir().setText(downloadDir);
        }

        // Rutas de yt-dlp y ffmpeg SOLO si tienes esos campos en el panel
        String ytDlpPath = UserPreferences.getYtDlpPath();
        if (ytDlpPath != null) {
            pnlPreferencesPanel.getTxtYtDlpPath().setText(ytDlpPath);
        }

        String ffmpegPath = UserPreferences.getFfmpegPath();
        if (ffmpegPath != null) {
            pnlPreferencesPanel.getTxtFfpmegDir().setText(ffmpegPath);
        }

        String scanPath = UserPreferences.getScanFolderPath();
        if (scanPath != null) {
            pnlPreferencesPanel.getTxtScanDownloadsFolder().setText(scanPath);
        }
    }

    private void savePreferencesFromUi() {
        String downloadDir = pnlPreferencesPanel.getTxtDownloadsDir().getText();
        String ytDlpPath = pnlPreferencesPanel.getTxtYtDlpPath().getText();
        String ffmpegPath = pnlPreferencesPanel.getTxtFfpmegDir().getText();
        String scanPath = pnlPreferencesPanel.getTxtScanDownloadsFolder().getText();

        UserPreferences.setDownloadDir(downloadDir);
        UserPreferences.setYtDlpPath(ytDlpPath);
        UserPreferences.setFfmpegPath(ffmpegPath);
        UserPreferences.setScanFolderPath(scanPath);
    }

    private void initDownloadsList() {
        lstDownloadScanList.setModel(downloadsModel);
        lstDownloadScanList.setFixedCellHeight(56);
    }

    private void initMetadataTable() {
        metaModel = new MetadataTableModel();
        tblMetaData.setModel(metaModel);

        var col0 = tblMetaData.getColumnModel().getColumn(0);
        col0.setPreferredWidth(120);
        col0.setMinWidth(100);
        col0.setMaxWidth(180);
    }

    private void initFilters() {
        downloadsController = new DownloadsController(
                downloadsModel,
                resourceDownloadeds,
                cmbTipo,
                chkSemana,
                lstDownloadScanList,
                metaModel,
                btnDeleteDownloadFileFolder,
                btnFetchFromCloud,
                btnUploadFromLocal,
                getRoigMediaPollingComponent1(),
                lblStatusScan,
                pbDownload
        );

        cmbTipo.addActionListener(e -> downloadsController.applyFiltersIfReady());
        chkSemana.addActionListener(e -> downloadsController.applyFiltersIfReady());
        cmbTipo.setSelectedItem("Todo");
    }

    // ------------------- NAVIGATION -------------------
    public void showPreferences() {
        pnlPreferencesPanel.onShow();
        showInContentPanel(pnlPreferencesPanel);
    }

    public void showMain() {
        showMainView();
        downloadsController.refreshScanStatusLabel();
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
        showInContentPanel(pnlMainPanel);
    }

    // ------------------- NAVIGATION -------------------
    public void updateSessionUI(boolean loggedIn) {
        mniPreferences.setVisible(loggedIn);
        mniPreferences.setEnabled(loggedIn);
        mnuEdit.setVisible(loggedIn);
        mniLogout.setVisible(loggedIn);

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
        roigMediaPollingComponent = new cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent();
        btnFetchFromCloud = new javax.swing.JButton();
        btnUploadFromLocal = new javax.swing.JButton();
        pbDownload = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();
        lblStatusScan = new javax.swing.JLabel();
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
        txtUrl.setBounds(80, 100, 350, 24);

        btnPaste.setLabel("Paste");
        btnPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPasteActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnPaste);
        btnPaste.setBounds(440, 100, 80, 25);

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
        rbVideo.setBounds(110, 160, 70, 22);

        bgFormat.add(rbAudio);
        rbAudio.setText("Audio");
        rbAudio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbAudioActionPerformed(evt);
            }
        });
        pnlMainPanel.add(rbAudio);
        rbAudio.setBounds(200, 160, 80, 22);

        lblOptions.setText("Quality:");
        pnlMainPanel.add(lblOptions);
        lblOptions.setBounds(40, 200, 60, 18);

        lblControls.setText("Controls:");
        pnlMainPanel.add(lblControls);
        lblControls.setBounds(40, 280, 80, 18);

        btnDownload.setText("Download");
        btnDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownloadActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnDownload);
        btnDownload.setBounds(60, 310, 140, 24);

        btnStop.setText("Stop");
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnStop);
        btnStop.setBounds(210, 310, 140, 24);

        btnOpenLast.setText("Open last");
        btnOpenLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenLastActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnOpenLast);
        btnOpenLast.setBounds(360, 310, 140, 24);

        lblOutput.setText("Output:");
        pnlMainPanel.add(lblOutput);
        lblOutput.setBounds(40, 350, 47, 18);

        txaLogArea.setEditable(false);
        txaLogArea.setColumns(20);
        txaLogArea.setRows(5);
        scrLogArea.setViewportView(txaLogArea);

        pnlMainPanel.add(scrLogArea);
        scrLogArea.setBounds(30, 380, 490, 170);

        lstDownloadScanList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstDownloadScanListValueChanged(evt);
            }
        });
        scpScanListPane.setViewportView(lstDownloadScanList);

        pnlMainPanel.add(scpScanListPane);
        scpScanListPane.setBounds(540, 140, 620, 230);

        btnScanDownloadFolder.setText("Scan");
        btnScanDownloadFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnScanDownloadFolderActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnScanDownloadFolder);
        btnScanDownloadFolder.setBounds(760, 100, 72, 24);

        btnDeleteDownloadFileFolder.setText("Delete");
        btnDeleteDownloadFileFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteDownloadFileFolderActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnDeleteDownloadFileFolder);
        btnDeleteDownloadFileFolder.setBounds(840, 100, 90, 24);

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
        scpMetaDataTable.setBounds(540, 380, 620, 170);

        cmbTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "Only Video", "Only Audio" }));
        cmbTipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbTipoActionPerformed(evt);
            }
        });
        pnlMainPanel.add(cmbTipo);
        cmbTipo.setBounds(540, 100, 110, 24);

        chkSemana.setText("This Week");
        pnlMainPanel.add(chkSemana);
        chkSemana.setBounds(660, 100, 100, 22);

        bgQuality.add(jrbBestAvailable);
        jrbBestAvailable.setText("Best Available");
        jrbBestAvailable.setToolTipText("Under Manteinance");
        jrbBestAvailable.setEnabled(false);
        pnlMainPanel.add(jrbBestAvailable);
        jrbBestAvailable.setBounds(50, 230, 120, 22);

        bgQuality.add(jrb1080p);
        jrb1080p.setSelected(true);
        jrb1080p.setText("1080p");
        pnlMainPanel.add(jrb1080p);
        jrb1080p.setBounds(170, 230, 80, 22);

        bgQuality.add(jrb720p);
        jrb720p.setText("720p");
        pnlMainPanel.add(jrb720p);
        jrb720p.setBounds(250, 230, 60, 22);

        bgQuality.add(jrb480p);
        jrb480p.setText("480p");
        pnlMainPanel.add(jrb480p);
        jrb480p.setBounds(320, 230, 60, 22);

        roigMediaPollingComponent.setApiUrl("https://dimedianetapi9.azurewebsites.net");
        roigMediaPollingComponent.setPollingInterval(3);
        pnlMainPanel.add(roigMediaPollingComponent);
        roigMediaPollingComponent.setBounds(1120, 0, 100, 70);

        btnFetchFromCloud.setText("Fetch");
        btnFetchFromCloud.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFetchFromCloudActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnFetchFromCloud);
        btnFetchFromCloud.setBounds(1070, 100, 70, 24);

        btnUploadFromLocal.setText("Upload");
        btnUploadFromLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUploadFromLocalActionPerformed(evt);
            }
        });
        pnlMainPanel.add(btnUploadFromLocal);
        btnUploadFromLocal.setBounds(970, 100, 90, 24);

        pbDownload.setForeground(new java.awt.Color(0, 0, 255));
        pbDownload.setFocusable(false);
        pbDownload.setString("50%");
        pbDownload.setStringPainted(true);
        pnlMainPanel.add(pbDownload);
        pbDownload.setBounds(30, 560, 1130, 20);

        jLabel1.setFont(new java.awt.Font("sansserif", 0, 18)); // NOI18N
        jLabel1.setText(" |");
        pnlMainPanel.add(jLabel1);
        jLabel1.setBounds(940, 100, 20, 20);
        pnlMainPanel.add(lblStatusScan);
        lblStatusScan.setBounds(890, 590, 270, 22);

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
        showPreferences();
//        pnlPreferencesPanel.onShow(); 
    }//GEN-LAST:event_mniPreferencesActionPerformed

    private void btnDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadActionPerformed
        downloadExecutionController.startDownload();
    }//GEN-LAST:event_btnDownloadActionPerformed

    private void mniAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniAboutActionPerformed
        // TODO add your handling code here:
        AboutDialog dlg = new AboutDialog(this, true); // modal
        dlg.setVisible(true);
    }//GEN-LAST:event_mniAboutActionPerformed

    private void btnScanDownloadFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnScanDownloadFolderActionPerformed
        String input = pnlPreferencesPanel.getTxtScanDownloadsFolder().getText();
        String finalDirStr = DetectOS.resolveDownloadDir(input);
        Path downloads = Paths.get(finalDirStr);

        downloadsController.scanDownloads(downloads, btnScanDownloadFolder);
    }//GEN-LAST:event_btnScanDownloadFolderActionPerformed

    private void cmbTipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbTipoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbTipoActionPerformed

    private void btnDeleteDownloadFileFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteDownloadFileFolderActionPerformed
        downloadsController.deleteSelectedDownloadFile(this);
    }//GEN-LAST:event_btnDeleteDownloadFileFolderActionPerformed

    private void rbAudioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbAudioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbAudioActionPerformed

    private void btnOpenLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenLastActionPerformed
        downloadExecutionController.openLastDownloadedFile(this, btnOpenLast);
    }//GEN-LAST:event_btnOpenLastActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        downloadExecutionController.stopDownload();
    }//GEN-LAST:event_btnStopActionPerformed

    private void mniLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniLogoutActionPerformed
        mainController.doLogout();
    }//GEN-LAST:event_mniLogoutActionPerformed

    private void btnUploadFromLocalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUploadFromLocalActionPerformed
        // TODO add your handling code here:
        downloadsController.uploadToCloud(this);

        ResourceDownloaded sel = lstDownloadScanList.getSelectedValue();
        System.err.println("UPLOAD clicked name=" + sel.getName()
                + " route=" + sel.getRoute()
                + " routeExists=" + (sel.getRoute() != null && !sel.getRoute().isBlank() && java.nio.file.Files.exists(java.nio.file.Paths.get(sel.getRoute()))));

    }//GEN-LAST:event_btnUploadFromLocalActionPerformed

    private void btnFetchFromCloudActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFetchFromCloudActionPerformed
        // TODO add your handling code here:
        downloadsController.downloadFromCloud(this);
    }//GEN-LAST:event_btnFetchFromCloudActionPerformed

    private void btnPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPasteActionPerformed
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = clipboard.getContents(null);

            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);

                txtUrl.setText("");        // limpiar
                txtUrl.setText(text.trim()); // pegar
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "No text found in clipboard.",
                    "Paste",
                    JOptionPane.WARNING_MESSAGE
            );
        }

    }//GEN-LAST:event_btnPasteActionPerformed

    private void lstDownloadScanListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstDownloadScanListValueChanged
        // TODO add your handling code here
        if (!evt.getValueIsAdjusting()) {
            ResourceDownloaded sel = lstDownloadScanList.getSelectedValue();
            btnUploadFromLocal.setEnabled(downloadsController.canUpload(sel));
        }
    }//GEN-LAST:event_lstDownloadScanListValueChanged

    // ----- GETTERS Y SETTERS ------
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

    public RoigMediaPollingComponent getRoigMediaPollingComponent1() {
        return roigMediaPollingComponent;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public DownloadsController getDownloadsController() {
        return downloadsController;
    }

    private void initUx() {
        // Jerarquía visual
        btnDownload.setBackground(UiColors.PRIMARY);
        btnDownload.setForeground(Color.WHITE);

        btnStop.setBackground(UiColors.CAREFULL);
        btnStop.setForeground(Color.WHITE);

        btnDeleteDownloadFileFolder.setBackground(UiColors.NEUTRAL);

        btnUploadFromLocal.setBackground(UiColors.PRIMARY);
        btnUploadFromLocal.setForeground(Color.WHITE);
        btnFetchFromCloud.setBackground(UiColors.PRIMARY);
        btnFetchFromCloud.setForeground(Color.WHITE);

        // Tooltips
        btnScanDownloadFolder.setToolTipText("Scan local media library");
        btnDeleteDownloadFileFolder.setToolTipText("Delete selected local media");
        btnUploadFromLocal.setToolTipText("Upload selected item to cloud");
        btnFetchFromCloud.setToolTipText("Download selected item from cloud");

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgFormat;
    private javax.swing.ButtonGroup bgQuality;
    private javax.swing.JButton btnDeleteDownloadFileFolder;
    private javax.swing.JButton btnDownload;
    private javax.swing.JButton btnFetchFromCloud;
    private javax.swing.JButton btnOpenLast;
    private java.awt.Button btnPaste;
    private javax.swing.JButton btnScanDownloadFolder;
    private javax.swing.JButton btnStop;
    private javax.swing.JButton btnUploadFromLocal;
    private javax.swing.JCheckBox chkSemana;
    private javax.swing.JComboBox<String> cmbTipo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JRadioButton jrb1080p;
    private javax.swing.JRadioButton jrb480p;
    private javax.swing.JRadioButton jrb720p;
    private javax.swing.JRadioButton jrbBestAvailable;
    private javax.swing.JLabel lblControls;
    private javax.swing.JLabel lblFormat;
    private javax.swing.JLabel lblOptions;
    private javax.swing.JLabel lblOutput;
    private javax.swing.JLabel lblStatusScan;
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
    private javax.swing.JProgressBar pbDownload;
    private javax.swing.JPanel pnlContent;
    private javax.swing.JPanel pnlMainPanel;
    private javax.swing.JRadioButton rbAudio;
    private javax.swing.JRadioButton rbVideo;
    private cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent roigMediaPollingComponent;
    private javax.swing.JScrollPane scpMetaDataTable;
    private javax.swing.JScrollPane scpScanListPane;
    private javax.swing.JScrollPane scrLogArea;
    private javax.swing.JTable tblMetaData;
    private javax.swing.JTextArea txaLogArea;
    private javax.swing.JTextField txtUrl;
    // End of variables declaration//GEN-END:variables
}
