package cat.dam.roig.cleanstream.ui.main;

import cat.dam.roig.cleanstream.controller.DownloadExecutionController;
import cat.dam.roig.cleanstream.controller.DownloadsController;
import cat.dam.roig.cleanstream.controller.MainController;
import cat.dam.roig.cleanstream.domain.ResourceDownloaded;
import cat.dam.roig.cleanstream.domain.VideoQuality;
import cat.dam.roig.cleanstream.services.auth.AuthManager;
import cat.dam.roig.cleanstream.services.polling.MediaPolling;
import cat.dam.roig.cleanstream.services.prefs.UserPreferences;
import cat.dam.roig.cleanstream.ui.AboutDialog;
import cat.dam.roig.cleanstream.ui.AppTheme;
import cat.dam.roig.cleanstream.ui.LoginPanel;
import cat.dam.roig.cleanstream.ui.PreferencesPanel;
import cat.dam.roig.cleanstream.ui.UiColors;
import cat.dam.roig.cleanstream.ui.models.MetadataTableModel;
import cat.dam.roig.cleanstream.util.DetectOS;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Main application window for CleanStream.
 *
 * <p>
 * This JFrame is the central UI container of the application. It hosts:
 * <ul>
 * <li>The main view (download actions + scan list + metadata table)</li>
 * <li>The login view ({@link LoginPanel})</li>
 * <li>The preferences view ({@link PreferencesPanel})</li>
 * </ul>
 *
 * <p>
 * Responsibilities (high-level):
 * <ul>
 * <li>Build the NetBeans-generated Swing UI ({@link #initComponents()})</li>
 * <li>Initialize controllers and connect them to UI widgets</li>
 * <li>Provide navigation between screens (login/main/preferences)</li>
 * <li>Expose small helper methods used by controllers (e.g., confirm dialogs,
 * selected quality)</li>
 * </ul>
 *
 * <p>
 * What this class should NOT do:
 * <ul>
 * <li>Implement business logic (download logic belongs to
 * controllers/services)</li>
 * <li>Handle persistence logic (goes to {@link UserPreferences})</li>
 * </ul>
 *
 * <p>
 * NetBeans Designer note: The method {@link #initComponents()} is generated and
 * should not be edited manually. Custom behavior is implemented in dedicated
 * init methods (e.g. {@link #initUx()},
 * {@link #initPreferencesPanel()}, {@link #initFilters()}).
 * </p>
 *
 * @author metku
 */
public class MainFrame extends javax.swing.JFrame {

    // ---------------------------------------------------------------------
    // Controllers and application dependencies
    // ---------------------------------------------------------------------
    /**
     * Controller that manages local scan list, filters, metadata selection and
     * cloud interactions (upload/fetch/delete).
     */
    private DownloadsController downloadsController;

    /**
     * Abstraction of the cloud media component. This is passed into
     * controllers/services that need network/cloud operations.
     */
    private final MediaPolling polling;

    /**
     * Manages authentication and "remember me" behavior.
     */
    private final AuthManager authManager;

    /**
     * Controller that orchestrates "start session / logout / autologin".
     */
    private final MainController mainController;

    /**
     * Controller that manages the download execution workflow (start/stop/open
     * last), and writes to the log and progress bar.
     */
    private final DownloadExecutionController downloadExecutionController;

    // ---------------------------------------------------------------------
    // UI modules / panels
    // ---------------------------------------------------------------------
    /**
     * Preferences screen panel.
     */
    private PreferencesPanel pnlPreferencesPanel;

    /**
     * Login screen panel.
     */
    private final LoginPanel loginPanel;

    // ---------------------------------------------------------------------
    // Data models used by UI widgets
    // ---------------------------------------------------------------------
    /**
     * Swing list model backing the scanned downloads list UI.
     */
    private final DefaultListModel<ResourceDownloaded> downloadsModel = new DefaultListModel<>();

    /**
     * Internal backing list used by {@link DownloadsController} to store all
     * scanned resources.
     */
    private final List<ResourceDownloaded> resourceDownloadeds = new ArrayList<>();

    /**
     * Table model for metadata view.
     */
    private MetadataTableModel metaModel;

    /**
     * Creates and initializes the MainFrame (UI + controllers).
     *
     * <p>
     * Initialization order matters:
     * <ol>
     * <li>Build UI widgets with NetBeans-generated
     * {@link #initComponents()}</li>
     * <li>Apply theme and UX improvements (styles, logo, table formatting)</li>
     * <li>Create and wire panels (login, preferences)</li>
     * <li>Create and wire controllers (downloads controller, main controller,
     * download execution)</li>
     * </ol>
     *
     * @param polling cloud media abstraction used by the application
     * @param authManager authentication manager (remember-me, login)
     */
    public MainFrame(MediaPolling polling, AuthManager authManager) {
        // 1) Build base UI (NetBeans Designer)
        initComponents();

        // Ensure status label always has stable size (prevents layout jumps)
        lblStatusScan.setText(" ");
        lblStatusScan.setPreferredSize(new Dimension(250, 18));
        lblStatusScan.setMinimumSize(new Dimension(50, 18));

        // 2) UX + theme
        initUx();
        loadLogo();

        // 3) Window settings
        initWindow();

        // 4) Dependencies
        this.polling = polling;
        this.authManager = authManager;

        // 5) Screens
        this.loginPanel = new LoginPanel(authManager);
        authManager.setLoginPanel(loginPanel); // ensure AuthManager can update login fields

        initPreferencesPanel();
        loadPreferencesToUi();

        // 6) Main view widgets setup
        initDownloadsList();
        initMetadataTable();
        initFilters();

        // 7) Controllers
        this.mainController = new MainController(this, authManager, polling);

        this.downloadExecutionController = new DownloadExecutionController(
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

    /**
     * @return preferences panel instance (useful for controllers/tests)
     */
    public PreferencesPanel getPnlPreferencesPanel() {
        return pnlPreferencesPanel;
    }

    // ---------------------------------------------------------------------
    // Initialization helpers
    // ---------------------------------------------------------------------
    /**
     * Configures basic JFrame properties (title, size, location).
     */
    private void initWindow() {
        setTitle("CleanStream");
        setMinimumSize(new java.awt.Dimension(1200, 700));
        setPreferredSize(new java.awt.Dimension(1200, 700));
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Creates the preferences panel and hooks its Save action.
     */
    private void initPreferencesPanel() {
        pnlPreferencesPanel = new PreferencesPanel(this);
        pnlPreferencesPanel.getBtnSave().addActionListener(e -> savePreferencesFromUi());
    }

    /**
     * Loads persisted preferences and writes them into the preferences UI
     * fields.
     *
     * <p>
     * Note: the source of truth is {@link UserPreferences}. The panel UI is
     * just a view.
     */
    private void loadPreferencesToUi() {
        String downloadDir = UserPreferences.getDownloadDir();
        if (downloadDir != null) {
            pnlPreferencesPanel.getTxtDownloadsDir().setText(downloadDir);
        }

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

    /**
     * Reads preferences values from the preferences UI and persists them.
     *
     * <p>
     * This is a "quick save" method (does not show validation here). Validation
     * and richer UX is implemented inside
     * {@link PreferencesPanel#savePreferences()}.
     */
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

    /**
     * Configures the downloads scanned list UI component.
     */
    private void initDownloadsList() {
        lstDownloadScanList.setModel(downloadsModel);
        lstDownloadScanList.setFixedCellHeight(78);

        scpScanListPane.getViewport().setBackground(new Color(0x121212));
        scpScanListPane.setBorder(BorderFactory.createEmptyBorder());

        lstDownloadScanList.setBackground(new Color(0x121212));
        lstDownloadScanList.setForeground(new Color(0xE6E6E6));
        lstDownloadScanList.setSelectionBackground(new Color(0x2A2A2A));
        lstDownloadScanList.setSelectionForeground(Color.WHITE);
    }

    /**
     * Initializes the metadata table model and applies styling (header, zebra
     * rows).
     */
    private void initMetadataTable() {
        metaModel = new MetadataTableModel();
        tblMetaData.setModel(metaModel);

        var col0 = tblMetaData.getColumnModel().getColumn(0);
        col0.setPreferredWidth(120);
        col0.setMinWidth(100);
        col0.setMaxWidth(180);

        styleMetadataTable(tblMetaData);
        installZebra(tblMetaData);
    }

    /**
     * Creates the {@link DownloadsController} and wires filter controls to it.
     */
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
                polling,
                lblStatusScan,
                pbDownload
        );

        cmbTipo.addActionListener(e -> downloadsController.applyFiltersIfReady());
        chkSemana.addActionListener(e -> downloadsController.applyFiltersIfReady());
        cmbTipo.setSelectedItem("Todo");
    }

    // ---------------------------------------------------------------------
    // Navigation between views
    // ---------------------------------------------------------------------
    /**
     * Shows the preferences panel inside the main content area.
     */
    public void showPreferences() {
        pnlPreferencesPanel.onShow();
        showInContentPanel(pnlPreferencesPanel);
    }

    /**
     * Shows the main view and refreshes scan status label.
     */
    public void showMain() {
        showMainView();
        downloadsController.refreshScanStatusLabel();
    }

    /**
     * Shows the login panel inside the main content area.
     */
    public void showLogin() {
        showInContentPanel(loginPanel);
    }

    /**
     * Replaces the current content panel view with the provided component.
     *
     * @param comp new component to display
     */
    private void showInContentPanel(java.awt.Component comp) {
        pnlContent.removeAll();
        pnlContent.setLayout(new java.awt.BorderLayout());
        pnlContent.add(comp, java.awt.BorderLayout.CENTER);
        pnlContent.revalidate();
        pnlContent.repaint();
    }

    /**
     * Shows the main app panel in the content area.
     */
    public void showMainView() {
        pnlMainPanel.setBackground(AppTheme.BACKGROUND);
        pnlMainPanel.setOpaque(true);
        showInContentPanel(pnlMainPanel);
    }

    /**
     * Updates menu visibility according to authentication state.
     *
     * @param loggedIn true if user is logged in, false otherwise
     */
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
        scpLogArea = new javax.swing.JScrollPane();
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
        btnFetchFromCloud = new javax.swing.JButton();
        btnUploadFromLocal = new javax.swing.JButton();
        pbDownload = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();
        lblStatusScan = new javax.swing.JLabel();
        roigMediaPollingComponent = new cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent();
        lblLogo = new javax.swing.JLabel();
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
        scpLogArea.setViewportView(txaLogArea);

        pnlMainPanel.add(scpLogArea);
        scpLogArea.setBounds(30, 380, 490, 170);

        scpScanListPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        lstDownloadScanList.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                lstDownloadScanListComponentResized(evt);
            }
        });
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

        roigMediaPollingComponent.setApiUrl("https://dimedianetapi9.azurewebsites.net");
        roigMediaPollingComponent.setPollingInterval(3);
        pnlMainPanel.add(roigMediaPollingComponent);
        roigMediaPollingComponent.setBounds(590, 590, 10, 10);

        lblLogo.setText(" ");
        pnlMainPanel.add(lblLogo);
        lblLogo.setBounds(330, -70, 520, 270);

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

    // ---------------------------------------------------------------------
    // UI event handlers (delegating to controllers where possible)
    // ---------------------------------------------------------------------

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
    }//GEN-LAST:event_btnUploadFromLocalActionPerformed

    private void btnFetchFromCloudActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFetchFromCloudActionPerformed
        // TODO add your handling code here:
        downloadsController.downloadFromCloud(this);
    }//GEN-LAST:event_btnFetchFromCloudActionPerformed

    /**
     * Reads a URL from the system clipboard and pastes it into the URL input.
     * Shows a warning message if clipboard does not contain text.
     */
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

    /**
     * Updates UI buttons when the user selects an item in the downloads list.
     *
     * <p>
     * Current behavior:
     * <ul>
     * <li>When selection is stable (not adjusting), enable upload button only
     * if it is allowed</li>
     * </ul>
     */
    private void lstDownloadScanListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstDownloadScanListValueChanged
        // TODO add your handling code here
        if (!evt.getValueIsAdjusting()) {
            ResourceDownloaded sel = lstDownloadScanList.getSelectedValue();
            btnUploadFromLocal.setEnabled(downloadsController.canUpload(sel));
        }
    }//GEN-LAST:event_lstDownloadScanListValueChanged

    private void lstDownloadScanListComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_lstDownloadScanListComponentResized
        // TODO add your handling code here:
        lstDownloadScanList.revalidate();
        lstDownloadScanList.repaint();
    }//GEN-LAST:event_lstDownloadScanListComponentResized

    // ---------------------------------------------------------------------
    // Public helpers used by controllers
    // ---------------------------------------------------------------------
    /**
     * Determines the selected video quality from the quality radio buttons.
     *
     * @return selected {@link VideoQuality} (defaults to BEST_AVAILABLE if none
     * matched)
     */
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

    /**
     * Exposes the embedded RoigMediaPollingComponent (NetBeans widget) if
     * needed.
     *
     * <p>
     * Note: In CleanStream, you should preferably depend on
     * {@link MediaPolling} instead of direct component usage.
     *
     * @return embedded component instance
     */
    public RoigMediaPollingComponent getRoigMediaPollingComponent1() {
        return roigMediaPollingComponent;
    }

    /**
     * @return app authentication manager
     */
    public AuthManager getAuthManager() {
        return authManager;
    }

    /**
     * @return downloads controller used by the main view
     */
    public DownloadsController getDownloadsController() {
        return downloadsController;
    }

    /**
     * Applies theme and UX tweaks: buttons styling, tooltips, log area style.
     */
    private void initUx() {
        getContentPane().setBackground(AppTheme.BACKGROUND);

        styleButtons();
        styleTopButtons();

        btnDownload.setPreferredSize(new Dimension(140, 36));

        btnUploadFromLocal.setBackground(UiColors.PRIMARY);
        btnUploadFromLocal.setForeground(Color.WHITE);
        btnFetchFromCloud.setBackground(UiColors.PRIMARY);
        btnFetchFromCloud.setForeground(Color.WHITE);

        btnScanDownloadFolder.setToolTipText("Scan local media library");
        btnDeleteDownloadFileFolder.setToolTipText("Delete selected local media");
        btnUploadFromLocal.setToolTipText("Upload selected item to cloud");
        btnFetchFromCloud.setToolTipText("Download selected item from cloud");

        styleLogArea(txaLogArea);
        scpLogArea.setBorder(BorderFactory.createEmptyBorder());
        scpLogArea.getViewport().setBackground(new Color(0x0F0F0F));
        txaLogArea.setCaretPosition(txaLogArea.getDocument().getLength());
    }

    /**
     * Loads and scales the application logo into {@code lblLogo}.
     */
    private void loadLogo() {
        ImageIcon icon = new ImageIcon(getClass().getResource("/images/logoCleanStream.png"));

        Image img = icon.getImage().getScaledInstance(
                lblLogo.getWidth(),
                lblLogo.getHeight(),
                Image.SCALE_SMOOTH
        );

        lblLogo.setIcon(new ImageIcon(img));
    }

    /**
     * Shows a logout confirmation dialog.
     *
     * @return true if user confirmed logout; false otherwise
     */
    public boolean confirmLogout() {
        int opt = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "Do you really want to log out?",
                "Confirm logout",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE
        );
        return opt == javax.swing.JOptionPane.YES_OPTION;
    }

    /**
     * Returns the scan downloads folder path currently shown in preferences UI.
     * This is used at startup to initialize the downloads scanner.
     *
     * @return scan folder path, or {@code null} if not set/empty
     */
    public Path getScanDownloadsFolderPathFromUI() {
        String ruta = pnlPreferencesPanel.getTxtScanDownloadsFolder().getText().trim();
        return ruta.isEmpty() ? null : Path.of(ruta);
    }

    // ---------------------------------------------------------------------
    // Styling helpers (FlatLaf + Swing table tweaks)
    // ---------------------------------------------------------------------
    private void styleButtons() {
        stylePrimary(btnDownload);
        styleSecondary(btnStop);
        styleNeutral(btnOpenLast);
    }

    private void styleTopButtons() {
        stylePrimary(btnScanDownloadFolder);
        styleSecondary(btnUploadFromLocal);
        styleSecondary(btnFetchFromCloud);
        styleDanger(btnDeleteDownloadFileFolder);
    }

    private void stylePrimary(JButton b) {
        if (b == null) {
            return;
        }
        b.putClientProperty("FlatLaf.style",
                "font: bold; background: #2B6CB0; foreground: #FFFFFF; arc: 10");
    }

    private void styleSecondary(JButton b) {
        if (b == null) {
            return;
        }
        b.putClientProperty("FlatLaf.style",
                "background: #2A2A2A; foreground: #E6E6E6; arc: 10");
    }

    private void styleNeutral(JButton b) {
        if (b == null) {
            return;
        }
        b.putClientProperty("FlatLaf.style",
                "font: bold; background: #2A2A2A; foreground: #E6E6E6; arc: 10");
    }

    private void styleDanger(JButton b) {
        if (b == null) {
            return;
        }
        b.putClientProperty("FlatLaf.style",
                "background: #4A1F1F; foreground: #FFFFFF; arc: 10");
    }

    private void styleMetadataTable(JTable table) {
        table.setRowHeight(28);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
    }

    private void installZebra(JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row, int column) {

                Component c = super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column
                );

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? t.getBackground() : new Color(0x161616));
                }
                return c;
            }
        });
    }

    private void styleLogArea(JTextArea ta) {
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);

        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setBackground(new Color(0x0F0F0F));
        ta.setForeground(new Color(0xD0D0D0));
        ta.setCaretColor(new Color(0xD0D0D0));
        ta.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
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
    private javax.swing.JLabel lblLogo;
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
    private javax.swing.JScrollPane scpLogArea;
    private javax.swing.JScrollPane scpMetaDataTable;
    private javax.swing.JScrollPane scpScanListPane;
    private javax.swing.JTable tblMetaData;
    private javax.swing.JTextArea txaLogArea;
    private javax.swing.JTextField txtUrl;
    // End of variables declaration//GEN-END:variables
}
