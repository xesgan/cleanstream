package cat.dam.roig.cleanstream.ui;

import cat.dam.roig.cleanstream.ui.main.MainFrame;
import cat.dam.roig.cleanstream.services.prefs.UserPreferences;
import cat.dam.roig.cleanstream.services.prefs.PreferencesValidator;
import cat.dam.roig.cleanstream.config.PreferencesData;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Preferences UI panel for configuring and persisting application settings.
 *
 * <p>
 * This panel is responsible for:
 * <ul>
 * <li>Displaying the current configuration values stored in
 * {@link UserPreferences}</li>
 * <li>Allowing the user to edit paths and options (binaries, folders,
 * toggles)</li>
 * <li>Validating configuration before saving using
 * {@link PreferencesValidator}</li>
 * <li>Persisting configuration changes using {@link UserPreferences}</li>
 * <li>Tracking unsaved changes using a "dirty" state</li>
 * </ul>
 *
 * <p>
 * Separation of responsibilities:
 * <ul>
 * <li><b>PreferencesPanel</b>: UI logic + user interaction</li>
 * <li><b>UserPreferences</b>: persistence layer (java.util.prefs)</li>
 * <li><b>PreferencesValidator</b>: validation rules / error messages</li>
 * <li><b>PreferencesData</b>: DTO used to transfer settings between UI and
 * storage</li>
 * </ul>
 *
 * <p>
 * NetBeans Designer note: {@link #initComponents()} is generated code. Custom
 * behavior and listeners are configured in {@link #initCustoms()} to keep
 * Designer regeneration safe.
 *
 * <p>
 * Dirty state:
 * <ul>
 * <li>{@code loading}: prevents dirty tracking while values are being loaded
 * into UI</li>
 * <li>{@code dirty}: indicates there are unsaved changes (enables Save
 * button)</li>
 * </ul>
 *
 * @author metku
 */
public class PreferencesPanel extends javax.swing.JPanel {

    /**
     * Reference to the main window used for navigation (e.g., back to main
     * view).
     */
    private MainFrame mainFrame;

    /**
     * When true, the panel is populating UI fields and should not mark dirty.
     */
    private boolean loading = false;

    /**
     * Indicates whether there are unsaved changes. When true, Save button is
     * enabled.
     */
    private boolean dirty = false;

    /**
     * Creates a new PreferencesPanel.
     *
     * <p>
     * Initialization flow:
     * <ol>
     * <li>Stores {@link MainFrame} reference (used for navigation)</li>
     * <li>Builds UI via {@link #initComponents()} (NetBeans)</li>
     * <li>Installs listeners and custom behavior via
     * {@link #initCustoms()}</li>
     * </ol>
     *
     * @param mainFrame main window of the application
     */
    public PreferencesPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        // Initialize internal state (no pending changes at construction time)
        loading = false;
        dirty = false;

        initComponents();
        initCustoms();
    }

    // ---------------------------------------------------------------------
    // GETTERS (used by other parts of the UI / controller)
    // ---------------------------------------------------------------------
    public JTextField getTxtYtDlpPath() {
        return txtYtDlpPath;
    }

    public JTextField getTxtDownloadsDir() {
        return txtDownloadsDir;
    }

    public JTextField getTxtFfpmegDir() {
        return txtFfmegDir;
    }

    public JTextField getTxtFfprobeDir() {
        return txtFfprobeDir;
    }

    public JTextField getTxtTempDir() {
        return txtTempDir;
    }

    public JTextField getTxtScanDownloadsFolder() {
        return txtScanDownloadsFolder;
    }

    public String getSTxtYtDlpPath() {
        return txtYtDlpPath.getText();
    }

    public String getSTxtDownloadsDir() {
        return txtDownloadsDir.getText();
    }

    public String getSTxtFfpmegDir() {
        return txtFfmegDir.getText();
    }

    public String getSTxtFfprobeDir() {
        return txtFfprobeDir.getText();
    }

    public String getSTxtTempDir() {
        return txtTempDir.getText();
    }

    public String getSTxtScanDownloadsFolder() {
        return txtScanDownloadsFolder.getText();
    }

    public JButton getBtnSave() {
        return btnSave;
    }

    /**
     * Returns a human-readable label for the current speed slider selection.
     *
     * <p>
     * This is used only for display purposes (e.g., showing "512K", "1M",
     * "2M").
     *
     * @return label representing the selected speed
     */
    public String getSldLimitSpeed() {
        return switch (sldLimitSpeed.getValue()) {
            case 0 ->
                "512K";
            case 20 ->
                "1M";
            default ->
                "2M";
        };
    }

    /**
     * @return true if "Create .m3u" option is enabled
     */
    public boolean getChkCreateM3u() {
        return chkCreateM3u.isSelected();
    }

    // ---------------------------------------------------------------------
    // File/folder browser helpers
    // ---------------------------------------------------------------------
    /**
     * Opens a file chooser and writes the selected file path into a text field.
     *
     * <p>
     * Intended for selecting executable binaries such as:
     * <ul>
     * <li>yt-dlp</li>
     * <li>ffmpeg</li>
     * <li>ffprobe</li>
     * </ul>
     *
     * <p>
     * Behavior:
     * <ul>
     * <li>If a valid existing path exists in the field, the chooser opens
     * there</li>
     * <li>On Windows, it can optionally filter for .exe files</li>
     * <li>After selection, warns if the file is not executable</li>
     * </ul>
     *
     * @param target text field where the selected path will be written
     * @param title chooser dialog title
     * @param tryExeFilter true to apply executable filter on Windows
     */
    private void browseFileInto(JTextField target, String title, boolean tryExeFilter) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // If there is a previous value, open chooser near it
        File current = pathToExistingFileOrDir(target.getText());
        if (current != null) {
            fc.setCurrentDirectory(current.isDirectory() ? current : current.getParentFile());
            if (current.isFile()) {
                fc.setSelectedFile(current);
            }
        }

        // On Windows, filtering by .exe is helpful; on Linux/macOS it is not.
        if (tryExeFilter && isWindows()) {
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileFilter(new FileNameExtensionFilter("Executables (*.exe)", "exe"));
        }

        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            target.setText(f.getAbsolutePath());

            // Optional quick warning: file should be executable
            if (!f.canExecute()) {
                JOptionPane.showMessageDialog(this,
                        "Warning: the selected file can not be executed.\n" + f,
                        "Warning", JOptionPane.WARNING_MESSAGE);
            }

            markDirty();
        }
    }

    /**
     * Opens a directory chooser and writes the selected folder path into a text
     * field.
     *
     * <p>
     * Intended for selecting folders such as:
     * <ul>
     * <li>Downloads folder</li>
     * <li>Temp folder</li>
     * <li>Scan folder</li>
     * </ul>
     *
     * @param target text field where the folder path will be written
     * @param title chooser dialog title
     */
    private void browseDirectoryInto(JTextField target, String title) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);

        File current = pathToExistingFileOrDir(target.getText());
        if (current != null) {
            fc.setCurrentDirectory(current.isDirectory() ? current : current.getParentFile());
            if (current.isDirectory()) {
                fc.setSelectedFile(current);
            }
        }

        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile();
            target.setText(dir.getAbsolutePath());
            markDirty();
        }
    }

    /**
     * @return true if running on Windows
     */
    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    /**
     * Converts a text path into a File if it exists.
     *
     * @param path string path
     * @return File instance if it exists; null otherwise
     */
    private static File pathToExistingFileOrDir(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        File f = new File(path.trim());
        return f.exists() ? f : null;
    }

    // ---------------------------------------------------------------------
    // Panel lifecycle
    // ---------------------------------------------------------------------
    /**
     * Called when this panel becomes visible.
     *
     * <p>
     * Loads the stored preferences into the UI fields. This ensures the panel
     * always displays current values.
     */
    public void onShow() {
        loadUI();
    }

    // ---------------------------------------------------------------------
    // Save / Load logic
    // ---------------------------------------------------------------------
    /**
     * Reads values from the UI, validates them and persists them.
     *
     * <p>
     * Flow:
     * <ol>
     * <li>Read UI into {@link PreferencesData}</li>
     * <li>Validate using {@link PreferencesValidator}</li>
     * <li>If valid, save via {@link UserPreferences}</li>
     * <li>Reset dirty flag and show success feedback</li>
     * </ol>
     */
    private void savePreferences() {
        PreferencesData data = readFromUI();

        String error = PreferencesValidator.validate(data);
        if (error != null) {
            JOptionPane.showMessageDialog(
                    this,
                    error,
                    "Preferences",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        UserPreferences.save(data);
        setDirty(false);
        showStatusMessage("[ Preferences saved ✓ ]");
    }

    /**
     * Loads persisted preferences and updates the UI fields.
     *
     * <p>
     * Sets {@code loading=true} to avoid marking the panel dirty during the UI
     * update.
     */
    private void loadUI() {
        loading = true;

        PreferencesData d = UserPreferences.load();

        setBackground(AppTheme.BACKGROUND);
        setOpaque(true);

        txtDownloadsDir.setText(d.getDownloadDir());
        txtYtDlpPath.setText(d.getYtDlpPath());
        txtFfmegDir.setText(d.getFfmpegPath());
        txtScanDownloadsFolder.setText(d.getScanFolderPath());
        chkOpenWhenDone.setSelected(d.isOpenWhenDone());
        chkLimitSpeed.setSelected(d.isLimitSpeedEnabled());
        sldLimitSpeed.setValue(d.getSpeedKbps());
        sldLimitSpeed.setEnabled(d.isLimitSpeedEnabled());
        chkCreateM3u.setSelected(d.isCreateM3u());

        setDirty(false);
        loading = false;
    }

    /**
     * Reads current UI values into a {@link PreferencesData} DTO.
     *
     * @return populated PreferencesData with current form values
     */
    private PreferencesData readFromUI() {
        PreferencesData d = new PreferencesData();
        d.setDownloadDir(txtDownloadsDir.getText());
        d.setYtDlpPath(txtYtDlpPath.getText());
        d.setFfmpegPath(txtFfmegDir.getText());
        d.setScanFolderPath(txtScanDownloadsFolder.getText());
        d.setOpenWhenDone(chkOpenWhenDone.isSelected());
        d.setLimitSpeedEnabled(chkLimitSpeed.isSelected());
        d.setSpeedKbps(sldLimitSpeed.getValue());
        d.setCreateM3u(chkCreateM3u.isSelected());
        return d;
    }

    // ---------------------------------------------------------------------
    // Dirty tracking
    // ---------------------------------------------------------------------
    /**
     * Sets the dirty state.
     *
     * <p>
     * When dirty is true, Save button is enabled.
     *
     * @param dirty true if there are unsaved changes
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        btnSave.setEnabled(dirty);
    }

    /**
     * Marks the panel as dirty unless currently loading values.
     */
    private void markDirty() {
        if (loading) {
            return;
        }
        setDirty(true);
    }

    // ---------------------------------------------------------------------
    // Status feedback
    // ---------------------------------------------------------------------
    /**
     * Shows a short-lived status message (2 seconds) in the UI.
     *
     * <p>
     * Used after saving preferences to provide user feedback.
     *
     * @param message message to show
     */
    private void showStatusMessage(String message) {
        lblStatus.setText(message);
        lblStatus.setForeground(new java.awt.Color(0, 128, 0)); // green

        javax.swing.Timer timer = new javax.swing.Timer(2000, e -> lblStatus.setText(""));
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * NetBeans Designer generated method.
     *
     * <p>
     * Warning: Do NOT modify this method manually. It is regenerated by the
     * Form Editor and changes will be lost.
     * </p>
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgQuality = new javax.swing.ButtonGroup();
        lblBinaries = new javax.swing.JLabel();
        lblYtDlp = new javax.swing.JLabel();
        txtYtDlpPath = new javax.swing.JTextField();
        btnYtDplBrowse = new javax.swing.JButton();
        lblFfpmeg = new javax.swing.JLabel();
        txtFfmegDir = new javax.swing.JTextField();
        btnFfpmegBrowse = new javax.swing.JButton();
        lblFfprobe = new javax.swing.JLabel();
        txtFfprobeDir = new javax.swing.JTextField();
        btnFfprobeBrowse = new javax.swing.JButton();
        lblRoutes = new javax.swing.JLabel();
        lblTemp = new javax.swing.JLabel();
        txtTempDir = new javax.swing.JTextField();
        btnTempBrowse = new javax.swing.JButton();
        lblDownloads = new javax.swing.JLabel();
        txtDownloadsDir = new javax.swing.JTextField();
        btnDownloadsBrowse = new javax.swing.JButton();
        lblQuality = new javax.swing.JLabel();
        btnSave = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        lblScanDownloads = new javax.swing.JLabel();
        txtScanDownloadsFolder = new javax.swing.JTextField();
        btnScanDownloadsFolder = new javax.swing.JButton();
        chkOpenWhenDone = new javax.swing.JCheckBox();
        chkLimitSpeed = new javax.swing.JCheckBox();
        chkCreateM3u = new javax.swing.JCheckBox();
        sldLimitSpeed = new javax.swing.JSlider();
        lbl512K = new javax.swing.JLabel();
        lbl1M = new javax.swing.JLabel();
        lbl2M = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(590, 518));
        setLayout(null);

        lblBinaries.setFont(new java.awt.Font("sansserif", 0, 16)); // NOI18N
        lblBinaries.setText("Binaries:");
        add(lblBinaries);
        lblBinaries.setBounds(30, 30, 70, 23);

        lblYtDlp.setText("yt-dlp:");
        add(lblYtDlp);
        lblYtDlp.setBounds(40, 60, 70, 18);

        txtYtDlpPath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtYtDlpPathActionPerformed(evt);
            }
        });
        add(txtYtDlpPath);
        txtYtDlpPath.setBounds(110, 60, 310, 24);

        btnYtDplBrowse.setText("Browse...");
        btnYtDplBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnYtDplBrowseActionPerformed(evt);
            }
        });
        add(btnYtDplBrowse);
        btnYtDplBrowse.setBounds(430, 60, 90, 24);

        lblFfpmeg.setText("ffpmeg:");
        add(lblFfpmeg);
        lblFfpmeg.setBounds(40, 100, 70, 18);
        add(txtFfmegDir);
        txtFfmegDir.setBounds(110, 100, 310, 24);

        btnFfpmegBrowse.setText("Browse...");
        btnFfpmegBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFfpmegBrowseActionPerformed(evt);
            }
        });
        add(btnFfpmegBrowse);
        btnFfpmegBrowse.setBounds(430, 100, 90, 24);

        lblFfprobe.setText("ffprobe:");
        add(lblFfprobe);
        lblFfprobe.setBounds(40, 140, 70, 18);
        add(txtFfprobeDir);
        txtFfprobeDir.setBounds(110, 140, 310, 24);

        btnFfprobeBrowse.setText("Browse...");
        btnFfprobeBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFfprobeBrowseActionPerformed(evt);
            }
        });
        add(btnFfprobeBrowse);
        btnFfprobeBrowse.setBounds(430, 140, 90, 24);

        lblRoutes.setFont(new java.awt.Font("sansserif", 0, 16)); // NOI18N
        lblRoutes.setText("Routes:");
        add(lblRoutes);
        lblRoutes.setBounds(30, 180, 70, 23);

        lblTemp.setText("Temp:");
        add(lblTemp);
        lblTemp.setBounds(40, 220, 70, 18);
        add(txtTempDir);
        txtTempDir.setBounds(110, 220, 310, 24);

        btnTempBrowse.setText("Browse...");
        btnTempBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTempBrowseActionPerformed(evt);
            }
        });
        add(btnTempBrowse);
        btnTempBrowse.setBounds(430, 220, 90, 24);

        lblDownloads.setText("Downloads:");
        add(lblDownloads);
        lblDownloads.setBounds(40, 260, 70, 18);
        add(txtDownloadsDir);
        txtDownloadsDir.setBounds(120, 260, 300, 24);

        btnDownloadsBrowse.setText("Browse...");
        btnDownloadsBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownloadsBrowseActionPerformed(evt);
            }
        });
        add(btnDownloadsBrowse);
        btnDownloadsBrowse.setBounds(430, 260, 90, 24);

        lblQuality.setFont(new java.awt.Font("sansserif", 0, 16)); // NOI18N
        lblQuality.setText("Options:");
        add(lblQuality);
        lblQuality.setBounds(590, 30, 110, 23);

        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        add(btnSave);
        btnSave.setBounds(680, 290, 72, 24);

        btnCancel.setText("Back");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        add(btnCancel);
        btnCancel.setBounds(1080, 290, 72, 24);

        btnReset.setText("Reset");
        add(btnReset);
        btnReset.setBounds(760, 290, 72, 24);

        lblScanDownloads.setText("Scan Files:");
        add(lblScanDownloads);
        lblScanDownloads.setBounds(40, 300, 70, 18);
        add(txtScanDownloadsFolder);
        txtScanDownloadsFolder.setBounds(120, 300, 300, 24);

        btnScanDownloadsFolder.setText("Browse...");
        btnScanDownloadsFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnScanDownloadsFolderActionPerformed(evt);
            }
        });
        add(btnScanDownloadsFolder);
        btnScanDownloadsFolder.setBounds(430, 300, 90, 24);

        chkOpenWhenDone.setText("Open when done");
        chkOpenWhenDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkOpenWhenDoneActionPerformed(evt);
            }
        });
        add(chkOpenWhenDone);
        chkOpenWhenDone.setBounds(680, 70, 150, 22);

        chkLimitSpeed.setText("Limit Speed");
        chkLimitSpeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkLimitSpeedActionPerformed(evt);
            }
        });
        add(chkLimitSpeed);
        chkLimitSpeed.setBounds(680, 100, 120, 22);

        chkCreateM3u.setSelected(true);
        chkCreateM3u.setText("Create .m3u");
        chkCreateM3u.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkCreateM3uActionPerformed(evt);
            }
        });
        add(chkCreateM3u);
        chkCreateM3u.setBounds(680, 210, 100, 22);

        sldLimitSpeed.setMajorTickSpacing(20);
        sldLimitSpeed.setMaximum(40);
        sldLimitSpeed.setPaintTicks(true);
        sldLimitSpeed.setAutoscrolls(true);
        sldLimitSpeed.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldLimitSpeedStateChanged(evt);
            }
        });
        add(sldLimitSpeed);
        sldLimitSpeed.setBounds(680, 130, 470, 40);

        lbl512K.setText("512K");
        add(lbl512K);
        lbl512K.setBounds(680, 170, 29, 18);

        lbl1M.setText("1M");
        add(lbl1M);
        lbl1M.setBounds(910, 170, 30, 18);

        lbl2M.setText("2M");
        add(lbl2M);
        lbl2M.setBounds(1130, 170, 30, 18);
        add(lblStatus);
        lblStatus.setBounds(990, 250, 150, 20);
    }// </editor-fold>//GEN-END:initComponents

    // ---------------------------------------------------------------------
    // Event handlers (generated + small delegations)
    // ---------------------------------------------------------------------

    private void btnYtDplBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnYtDplBrowseActionPerformed
        // TODO add your handling code here:
        browseFileInto(txtYtDlpPath, "Select the exe of yt-dlp", true);
    }//GEN-LAST:event_btnYtDplBrowseActionPerformed

    private void btnFfpmegBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFfpmegBrowseActionPerformed
        // TODO add your handling code here:
        browseFileInto(txtFfmegDir, "Select the exe of ffmpeg", true);
    }//GEN-LAST:event_btnFfpmegBrowseActionPerformed

    private void btnFfprobeBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFfprobeBrowseActionPerformed
        // TODO add your handling code here:
        browseFileInto(txtFfprobeDir, "Select the exe of ffprobe", true);
    }//GEN-LAST:event_btnFfprobeBrowseActionPerformed

    private void btnTempBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTempBrowseActionPerformed
        // TODO add your handling code here:
        browseDirectoryInto(txtTempDir, "Select the folder of temp files");
    }//GEN-LAST:event_btnTempBrowseActionPerformed

    private void btnDownloadsBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadsBrowseActionPerformed
        // TODO add your handling code here:
        browseDirectoryInto(txtDownloadsDir, "Select the downloads folder");
    }//GEN-LAST:event_btnDownloadsBrowseActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        // TODO add your handling code here:
        mainFrame.showMain();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void txtYtDlpPathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtYtDlpPathActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtYtDlpPathActionPerformed

    private void btnScanDownloadsFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnScanDownloadsFolderActionPerformed
        // TODO add your handling code here:
        browseDirectoryInto(txtScanDownloadsFolder, "Select the downloads folder");
    }//GEN-LAST:event_btnScanDownloadsFolderActionPerformed

    private void chkOpenWhenDoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkOpenWhenDoneActionPerformed
        // TODO add your handling code here:
        if (!loading) {
            markDirty();
        }
    }//GEN-LAST:event_chkOpenWhenDoneActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
        savePreferences();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void chkLimitSpeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkLimitSpeedActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkLimitSpeedActionPerformed

    private void sldLimitSpeedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldLimitSpeedStateChanged
        // TODO add your handling code here:
        if (!sldLimitSpeed.getValueIsAdjusting()) {
            if (!loading) {
                markDirty();
            }
        }
    }//GEN-LAST:event_sldLimitSpeedStateChanged

    private void chkCreateM3uActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkCreateM3uActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_chkCreateM3uActionPerformed

    // ---------------------------------------------------------------------
    // Listener utilities
    // ---------------------------------------------------------------------
    /**
     * Installs a DocumentListener that marks the panel dirty whenever the user
     * edits a text field.
     *
     * @param tf text field to track
     */
    private void hookDirty(javax.swing.JTextField tf) {
        tf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                markDirty();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                markDirty();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                markDirty();
            }
        });
    }

    /**
     * Installs all custom listeners and non-designer behavior.
     *
     * <p>
     * This is intentionally separated from {@link #initComponents()} so the
     * NetBeans designer can regenerate UI code safely.
     */
    private void initCustoms() {
        hookDirty(txtDownloadsDir);
        hookDirty(txtYtDlpPath);
        hookDirty(txtFfmegDir);
        hookDirty(txtScanDownloadsFolder);
        chkOpenWhenDone.addActionListener(e -> markDirty());
        chkCreateM3u.addActionListener(e -> markDirty());
        chkLimitSpeed.addActionListener(e -> {
            sldLimitSpeed.setEnabled(chkLimitSpeed.isSelected());
            markDirty();
        });
        if (!sldLimitSpeed.getValueIsAdjusting()) {
            if (!loading) {
                markDirty();
            }
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgQuality;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnDownloadsBrowse;
    private javax.swing.JButton btnFfpmegBrowse;
    private javax.swing.JButton btnFfprobeBrowse;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnScanDownloadsFolder;
    private javax.swing.JButton btnTempBrowse;
    private javax.swing.JButton btnYtDplBrowse;
    private javax.swing.JCheckBox chkCreateM3u;
    public javax.swing.JCheckBox chkLimitSpeed;
    public javax.swing.JCheckBox chkOpenWhenDone;
    private javax.swing.JLabel lbl1M;
    private javax.swing.JLabel lbl2M;
    private javax.swing.JLabel lbl512K;
    private javax.swing.JLabel lblBinaries;
    private javax.swing.JLabel lblDownloads;
    private javax.swing.JLabel lblFfpmeg;
    private javax.swing.JLabel lblFfprobe;
    private javax.swing.JLabel lblQuality;
    private javax.swing.JLabel lblRoutes;
    private javax.swing.JLabel lblScanDownloads;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblTemp;
    private javax.swing.JLabel lblYtDlp;
    private javax.swing.JSlider sldLimitSpeed;
    private javax.swing.JTextField txtDownloadsDir;
    private javax.swing.JTextField txtFfmegDir;
    private javax.swing.JTextField txtFfprobeDir;
    private javax.swing.JTextField txtScanDownloadsFolder;
    private javax.swing.JTextField txtTempDir;
    private javax.swing.JTextField txtYtDlpPath;
    // End of variables declaration//GEN-END:variables

}
