package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.ui.models.MetadataTableModel;
import cat.dam.roig.cleanstream.domain.ResourceDownloaded;
import cat.dam.roig.cleanstream.domain.ResourceState;
import cat.dam.roig.cleanstream.services.scan.DownloadsScanner;
import cat.dam.roig.cleanstream.services.prefs.UserPreferences;
import cat.dam.roig.cleanstream.services.cloud.UploaderResolver;
import cat.dam.roig.cleanstream.services.polling.MediaPolling;
import cat.dam.roig.cleanstream.ui.renderers.ResourceDownloadedRenderer;
import cat.dam.roig.roigmediapollingcomponent.Media;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Controller that manages the "Downloads" screen: local scan, cloud listing,
 * filtering, selection UX and cloud sync actions.
 *
 * <h2>Main responsibilities</h2>
 * <ul>
 * <li><b>Local library:</b> scans the configured downloads folder and builds a
 * list of {@link ResourceDownloaded} items.</li>
 * <li><b>Cloud library:</b> fetches remote media using
 * {@link MediaPolling#getAllMedia()} and keeps an in-memory list of
 * {@link Media}.</li>
 * <li><b>State reconciliation:</b> computes a {@link ResourceState} per file
 * name: LOCAL_ONLY, CLOUD_ONLY or BOTH.</li>
 * <li><b>Filters:</b> applies UI filters (type + this week) and a view mode
 * (LOCAL / CLOUD / ALL).</li>
 * <li><b>UX:</b> keeps selection stable across refreshes, updates metadata
 * table, enables/disables action buttons correctly, supports double-click
 * open.</li>
 * <li><b>Actions:</b> delete local file, download from cloud, upload to
 * cloud.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <ul>
 * <li>Local scan and cloud loading run in background using
 * {@link SwingWorker}.</li>
 * <li>All Swing UI changes must occur on the EDT (most callbacks in SwingWorker
 * already are).</li>
 * </ul>
 *
 * <h2>Important data structures</h2>
 * <ul>
 * <li>{@code allResources}: master list of local resources (results of scanning
 * disk).</li>
 * <li>{@code cloudMedia}: master list of cloud resources (results of API
 * call).</li>
 * <li>{@code downloadsModel}: the visible list model after applying filters and
 * view mode.</li>
 * <li>{@code stateByFileName}: reconciliation map (normalized filename ->
 * state).</li>
 * </ul>
 *
 * <p>
 * <b>Key normalization:</b> file names are normalized to lowercase and trimmed
 * so that local and cloud entries can be matched reliably.
 */
public class DownloadsController {

    /**
     * API facade used to communicate with the remote cloud
     * (list/download/upload).
     */
    private final MediaPolling mediaPolling;

    /**
     * Visible list model bound to the JList.
     */
    private final DefaultListModel<ResourceDownloaded> downloadsModel;

    /**
     * Master list of local resources (unfiltered).
     */
    private final List<ResourceDownloaded> allResources;

    /**
     * Filter: type (Video / Audio / All).
     */
    private final JComboBox<String> cmbTipo;

    /**
     * Filter: restrict to downloads made within the current week (Mon-Sun).
     */
    private final JCheckBox chkSemana;

    /**
     * JList showing visible resources (after filtering).
     */
    private final JList<ResourceDownloaded> downloadsList;

    /**
     * Map containing the computed state for each resource based on its presence
     * in local list and/or cloud list.
     *
     * <p>
     * Key is the normalized file name.</p>
     */
    private final Map<String, ResourceState> stateByFileName = new HashMap<>();

    /**
     * Table model used to show metadata of the currently selected resource.
     */
    private final MetadataTableModel metaModel;

    /**
     * Action buttons controlled by selection and state.
     */
    private final JButton btnDelete;
    private final JButton btnDownloadFromCloud;
    private final JButton btnUploadFromLocal;

    /**
     * Status label used to display scan results and short messages.
     */
    private JLabel lblStatusScan;

    /**
     * Progress bar used to show busy state during cloud upload/download.
     */
    private final JProgressBar pbDownload;

    /**
     * Resolves uploader nicknames from uploader IDs with caching and async
     * retrieval. It relies on {@link MediaPolling#getNickName(int)} to fetch
     * nicknames.
     */
    private UploaderResolver uploaderResolver;

    /**
     * Master list of media currently available in the cloud.
     */
    private final List<Media> cloudMedia = new ArrayList<>();

    /**
     * Key set from the previous scan to compute delta messages (+added /
     * -removed).
     */
    private Set<String> lastScanKeys = new HashSet<>();

    /**
     * True after the first scan is completed, used to adapt scan status
     * messages.
     */
    private boolean hasScannedOnce = false;

    /**
     * Optional: key that should be selected after an async refresh completes.
     */
    private String pendingSelectKey = null;

    /**
     * View mode defines which sources are shown in the list. LOCAL = show only
     * local resources, CLOUD = show cloud resources, ALL = mixed view.
     */
    enum ViewMode {
        LOCAL, CLOUD, ALL
    }

    /**
     * Current view mode (default: ALL).
     */
    private ViewMode viewMode = ViewMode.ALL;

    /**
     * Local scan has completed at least once successfully.
     */
    private boolean hasScanned = false;

    /**
     * Local scan is currently running.
     */
    private boolean isScanning = false;

    /**
     * Cloud loading is currently running (prevents duplicate concurrent loads).
     */
    private boolean cloudLoading = false;

    /**
     * Creates a DownloadsController and wires it with the UI components it
     * controls.
     *
     * <p>
     * This constructor also installs:
     * <ul>
     * <li>a selection listener to update metadata and enable/disable
     * actions</li>
     * <li>a double-click handler to open local files with the system
     * player</li>
     * <li>a cell renderer that visually indicates {@link ResourceState}</li>
     * <li>hover support and progress bar styling</li>
     * </ul>
     *
     * @param downloadsModel list model bound to the JList
     * @param allResources master local resource list
     * @param cmbTipo filter combo box (type)
     * @param chkSemana filter checkbox (week)
     * @param downloadsList list UI component
     * @param metaModel table model showing metadata of selected resource
     * @param btnDelete delete action button
     * @param btnDownloadFromCloud cloud download action button
     * @param btnUploadFromLocal cloud upload action button
     * @param mediaPolling API facade used for cloud operations
     * @param lblStatusScan status label
     * @param pbDownload progress bar used to show busy state
     * @throws IllegalArgumentException if {@code mediaPolling} is null
     */
    public DownloadsController(
            DefaultListModel<ResourceDownloaded> downloadsModel,
            List<ResourceDownloaded> allResources,
            JComboBox<String> cmbTipo,
            JCheckBox chkSemana,
            JList<ResourceDownloaded> downloadsList,
            MetadataTableModel metaModel,
            JButton btnDelete,
            JButton btnDownloadFromCloud,
            JButton btnUploadFromLocal,
            MediaPolling mediaPolling,
            JLabel lblStatusScan,
            JProgressBar pbDownload
    ) {
        if (mediaPolling == null) {
            throw new IllegalArgumentException("mediaComponent no puede ser null");
        }

        this.mediaPolling = mediaPolling;
        this.downloadsModel = downloadsModel;
        this.allResources = allResources;
        this.cmbTipo = cmbTipo;
        this.chkSemana = chkSemana;
        this.downloadsList = downloadsList;
        this.metaModel = metaModel;
        this.btnDelete = btnDelete;
        this.btnDownloadFromCloud = btnDownloadFromCloud;
        this.btnUploadFromLocal = btnUploadFromLocal;
        this.lblStatusScan = lblStatusScan;
        this.pbDownload = pbDownload;

        uploaderResolver = new UploaderResolver(
                userId -> this.mediaPolling.getNickName(userId)
        );

        initSelectionListener();
        initDoubleClickOpen();

        downloadsList.setCellRenderer(new ResourceDownloadedRenderer(stateByFileName));
        cat.dam.roig.cleanstream.ui.util.ListHoverSupport.install(downloadsList);
        styleProgressBar();
    }

    /**
     * Initializes the downloads screen when the application starts.
     *
     * <p>
     * Flow:
     * <ol>
     * <li>Loads cloud media (if there is an active token).</li>
     * <li>If the local downloads directory is valid, scans it.</li>
     * <li>If local directory is invalid, clears local state and refreshes the
     * view.</li>
     * </ol>
     *
     * @param downloadsDir local directory to scan (may be null)
     * @param parentForDialog parent component used for modal dialogs
     */
    public void appStart(Path downloadsDir, Component parentForDialog) {

        // 1) Cloud siempre
        loadCloudMedia(parentForDialog);

        boolean localOk = (downloadsDir != null && Files.isDirectory(downloadsDir));

        // 2) Local solo si está configurado y es válido
        if (localOk) {
            scanDownloads(downloadsDir, null); // null => no hay botón
        } else {
            // limpia “estado local” para no arrastrar basura
            allResources.clear();
            stateByFileName.clear();

            // importante: reflejar la vista actual sin local
            // (si usas viewMode, ponlo aquí)
            viewMode = ViewMode.ALL; // o lo que hayas definido
            applyFiltersPreservingSelection();
            // opcional: status / disable acciones locales
            // ui.setLocalStatus("Local desactivado...");
        }
    }

    /**
     * Installs a list selection listener.
     *
     * <p>
     * When selection changes:
     * <ul>
     * <li>Updates the metadata table model</li>
     * <li>Enables/disables action buttons according to
     * {@link ResourceState}</li>
     * </ul>
     *
     * <p>
     * Rules:
     * <ul>
     * <li>Delete enabled only when a local route exists</li>
     * <li>Download-from-cloud enabled only for CLOUD_ONLY items</li>
     * <li>Upload enabled only for LOCAL_ONLY items that exist on disk</li>
     * </ul>
     */
    private void initSelectionListener() {
        btnDelete.setEnabled(false);
        btnDownloadFromCloud.setEnabled(false);
        btnUploadFromLocal.setEnabled(false);

        downloadsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ResourceDownloaded sel = downloadsList.getSelectedValue();
                metaModel.setResource(sel);

                if (sel == null) {
                    btnDelete.setEnabled(false);
                    btnDownloadFromCloud.setEnabled(false);
                    btnUploadFromLocal.setEnabled(false);
                    return;
                }

                String name = sel.getName();
                ResourceState state = stateByFileName.getOrDefault(name, ResourceState.LOCAL_ONLY);

                btnDelete.setEnabled(sel.getRoute() != null);               // solo si existe en disco
                btnDownloadFromCloud.setEnabled(state == ResourceState.CLOUD_ONLY); // solo si es cloud-only
                btnUploadFromLocal.setEnabled(state == ResourceState.LOCAL_ONLY && sel.getRoute() != null);
            }
        });
    }

    /**
     * Installs a double-click handler on the list.
     *
     * <p>
     * Double-click on a local item opens the file using the system default
     * player. Cloud-only items (without route) are ignored.</p>
     */
    private void initDoubleClickOpen() {
        downloadsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }

                int idx = downloadsList.locationToIndex(e.getPoint());
                if (idx < 0) {
                    return;
                }

                ResourceDownloaded r = downloadsModel.get(idx);
                if (r == null) {
                    return;
                }

                // Solo si existe en disco (local)
                if (r.getRoute() == null || r.getRoute().isBlank()) {
                    return;
                }

                // Opcional: si quieres bloquear también cloud-only aunque tenga route null
                ResourceState st = stateByFileName.getOrDefault(normalize(r.getName()), ResourceState.LOCAL_ONLY);
                if (st == ResourceState.CLOUD_ONLY) {
                    return;
                }
                openWithSystemPlayer(r.getRoute());
            }
        });
    }

    /**
     * Opens a local file using {@link java.awt.Desktop#open(File)}. Shows an
     * error dialog if the operation fails.
     *
     * @param path absolute path to the local file
     */
    private void openWithSystemPlayer(String path) {
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(path));
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(
                    downloadsList,
                    "No se ha podido abrir el reproductor.\n" + ex.getMessage(),
                    "Abrir archivo",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Scans the local downloads directory in background.
     *
     * <p>
     * Uses {@link DownloadsScanner} to build a list of
     * {@link ResourceDownloaded}. After finishing, the controller updates
     * internal state and refreshes the list view.</p>
     *
     * @param downloadsDir directory to scan
     * @param btnScan optional scan button to disable while scanning (may be
     * null)
     */
    public void scanDownloads(Path downloadsDir, JButton btnScan) {

        if (btnScan != null) {
            btnScan.setEnabled(false);
        }

        SwingWorker<List<ResourceDownloaded>, Void> worker = new SwingWorker<>() {

            @Override
            protected List<ResourceDownloaded> doInBackground() {
                onScanStarted();
                try {
                    DownloadsScanner scanner = new DownloadsScanner();
                    return scanner.scan(downloadsDir, false);
                } catch (IOException e) {
                    System.err.println("Scan error: " + e.getMessage());
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<ResourceDownloaded> lista = get();
                    onScanFinished(lista);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (btnScan != null) {
                        btnScan.setEnabled(true);
                    }
                }
            }
        };

        worker.execute();
    }

    /**
     * Deletes the currently selected local file from disk (if it exists).
     *
     * <p>
     * If the resource is local-only, it is removed from the visible model. If
     * the resource exists in cloud too (BOTH), it remains visible as
     * cloud-only.</p>
     *
     * <p>
     * After deletion, the controller triggers:
     * <ul>
     * <li>a rescan of the local folder (if configured)</li>
     * <li>a cloud reload to recompute states</li>
     * <li>a selection restore to keep UX stable</li>
     * </ul>
     *
     * @param parentForDialog parent component used for confirmation and info
     * dialogs
     */
    public void deleteSelectedDownloadFile(java.awt.Component parentForDialog) {
        int idx = downloadsList.getSelectedIndex();
        if (idx == -1) {
            // Nada seleccionado, no hacemos nada
            return;
        }

        ResourceDownloaded selected = downloadsList.getModel().getElementAt(idx);
        if (selected == null || selected.getRoute() == null || selected.getRoute().isBlank()) {
            return;
        }

        String key = keyOf(selected);
        int oldIdx = idx;

        Path file = Paths.get(selected.getRoute());

        if (!confirmDeletion(parentForDialog, file)) {
            return;
        }

        try {
            boolean deleted = Files.deleteIfExists(file);
            ResourceState state = stateByFileName.get(normalize(selected.getName()));
            boolean isInCloud = state == ResourceState.CLOUD_ONLY || state == ResourceState.BOTH;

            if (deleted) {
                if (!isInCloud) {
                    downloadsModel.remove(idx);
                }

                // Re-scan local
                String scanDir = UserPreferences.getScanFolderPath();
                if (scanDir != null && !scanDir.isBlank()) {
                    scanDownloads(Paths.get(scanDir), null);
                }

                loadCloudMedia(parentForDialog);

                SwingUtilities.invokeLater(() -> restoreSelectionAfterDelete(key, oldIdx));

                JOptionPane.showMessageDialog(
                        parentForDialog,
                        "Archivo eliminado.",
                        "Eliminar",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        parentForDialog,
                        "No se pudo eliminar (¿ya no existe?).",
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE
                );
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    parentForDialog,
                    "Error al eliminar:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Restores selection after a delete operation.
     *
     * <p>
     * Strategy:
     * <ol>
     * <li>If the same resource still exists (e.g., BOTH -> CLOUD_ONLY), keep it
     * selected.</li>
     * <li>Otherwise select the previous item (or nearest valid index).</li>
     * </ol>
     *
     * @param key normalized key of the previously selected item
     * @param oldIdx previous index before deletion
     */
    private void restoreSelectionAfterDelete(String key, int oldIdx) {
        ListModel<ResourceDownloaded> m = downloadsList.getModel();
        int size = m.getSize();
        if (size == 0) {
            downloadsList.clearSelection();
            return;
        }

        // 1) Mantener el mismo recurso si sigue existiendo (BOTH -> CLOUD)
        int same = -1;
        for (int i = 0; i < size; i++) {
            if (key.equals(keyOf(m.getElementAt(i)))) {
                same = i;
                break;
            }
        }
        if (same >= 0) {
            downloadsList.setSelectedIndex(same);
            downloadsList.ensureIndexIsVisible(same);
            downloadsList.requestFocusInWindow();
            return;
        }

        // 2) Si ya no existe, ir al anterior
        int target = oldIdx - 1;
        if (target < 0) {
            target = 0;
        }
        if (target >= size) {
            target = size - 1;
        }

        downloadsList.setSelectedIndex(target);
        downloadsList.ensureIndexIsVisible(target);
        downloadsList.requestFocusInWindow();
    }

    /**
     * Downloads a cloud-only media item to the local downloads directory.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates that selection exists and is CLOUD_ONLY</li>
     * <li>Downloads the file in background via
     * {@link MediaPolling#download(int, File)}</li>
     * <li>Refreshes local scan to make the resource appear as local/both</li>
     * </ul>
     *
     * @param parent parent component for dialogs
     */
    public void downloadFromCloud(Component parent) {

        ResourceDownloaded sel = downloadsList.getSelectedValue();
        if (sel == null) {
            return;
        }

        String key = normalize(sel.getName());
        ResourceState state = stateByFileName.get(key);
        if (state != ResourceState.CLOUD_ONLY) {
            return;
        }

        // Buscar el Media real (por nombre normalizado)
        Media media = cloudMedia.stream()
                .filter(m -> normalize(m.mediaFileName).equals(key))
                .findFirst()
                .orElse(null);

        if (media == null) {
            JOptionPane.showMessageDialog(parent, "Cloud media not found.");
            return;
        }

        // Carpeta destino (la de scan)
        String baseDir = UserPreferences.getDownloadDir();
        if (baseDir == null || baseDir.isBlank()) {
            JOptionPane.showMessageDialog(parent, "Scan folder not configured.");
            return;
        }

        File dest = new File(baseDir, media.mediaFileName);

        // ✅ UX: iniciar barra + deshabilitar botones
        startBusy("Descargando desde la nube…");
        if (btnDownloadFromCloud != null) {
            btnDownloadFromCloud.setEnabled(false);
        }

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                mediaPolling.download(media.id, dest);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // ✅ captura error real si falla
                    stopBusy("Descarga completada ✔");

                    // Re-scan local y refresco (esto ya aplica filtros)
                    scanDownloads(Paths.get(baseDir), null);

                    // (Opcional) dejar seleccionado el item descargado:
                    pendingSelectKey = key;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    stopBusy("Descarga fallida ✖");
                    JOptionPane.showMessageDialog(parent, "Download failed.", "Fetch", JOptionPane.ERROR_MESSAGE);

                } finally {
                    if (btnDownloadFromCloud != null) {
                        btnDownloadFromCloud.setEnabled(true);
                    }
                }
            }
        }.execute();
    }

    /**
     * Uploads a local-only item to the cloud.
     *
     * <p>
     * Valid only when the selected resource exists on disk and is not already
     * present in cloud.</p>
     *
     * @param parent parent component for dialogs
     */
    public void uploadToCloud(Component parent) {

        ResourceDownloaded sel = downloadsList.getSelectedValue();
        if (sel == null) {
            return;
        }

        String key = normalize(sel.getName());
        ResourceState state = stateByFileName.getOrDefault(key, ResourceState.LOCAL_ONLY);

        String fromUrl = (sel.getSourceURL() != null) ? sel.getSourceURL() : "";

        if (state != ResourceState.LOCAL_ONLY || sel.getRoute() == null || sel.getRoute().isBlank()) {
            return;
        }

        File file = new File(sel.getRoute());
        if (!file.exists() || !file.isFile()) {
            JOptionPane.showMessageDialog(parent, "Local file not found:\n" + file, "Upload", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ✅ barra “busy”
        startBusy("Subiendo a la nube…");
        btnUploadFromLocal.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                mediaPolling.uploadFileMultipart(file, fromUrl);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    stopBusy("Upload completado ✔");
                    loadCloudMedia(parent); // refresca nube/estados
                    btnUploadFromLocal.setEnabled(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    stopBusy("Upload fallido ✖");
                    JOptionPane.showMessageDialog(parent, "Upload failed.", "Upload", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Checks if a resource is eligible to be uploaded to cloud.
     *
     * <p>
     * A resource can be uploaded only if:
     * <ul>
     * <li>it has a valid local route</li>
     * <li>the file exists on disk</li>
     * <li>it is not already in cloud (not CLOUD_ONLY/BOTH)</li>
     * </ul>
     *
     * @param r resource to check
     * @return true if upload is allowed
     */
    public boolean canUpload(ResourceDownloaded r) {
        if (r == null) {
            return false;
        }

        String route = r.getRoute();
        if (route == null || route.isBlank()) {
            return false;
        }

        try {
            if (!java.nio.file.Files.exists(java.nio.file.Paths.get(route))) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }

        ResourceState state = stateByFileName.get(normalize(r.getName()));
        boolean inCloud = state == ResourceState.CLOUD_ONLY || state == ResourceState.BOTH;

        return !inCloud;
    }

    /**
     * Shows a confirmation dialog before deleting a file.
     *
     * @param parent parent component for the dialog
     * @param file file path to delete
     * @return true if user confirms deletion
     */
    private boolean confirmDeletion(java.awt.Component parent, Path file) {
        int opt = JOptionPane.showConfirmDialog(
                parent,
                "¿Eliminar el archivo?\n" + file,
                "Eliminar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opt == JOptionPane.YES_OPTION;
    }

    // ------ FILTROS DE DESCARGAS ------
    /**
     * Applies filters only when the data required for the current view mode is
     * ready.
     *
     * <p>
     * LOCAL view requires a completed local scan. CLOUD/ALL can refresh
     * anytime.</p>
     */
    public void applyFiltersIfReady() {
        // Si el usuario está viendo cloud o all, siempre podemos refrescar
        if (viewMode == ViewMode.CLOUD || viewMode == ViewMode.ALL) {
            applyFiltersPreservingSelection();
            return;
        }

        // Si está en LOCAL, solo refrescamos si tenemos scan local válido
        if (viewMode == ViewMode.LOCAL && hasScanned && !isScanning) {
            applyFiltersPreservingSelection();
        }
    }

    /**
     * Applies current filters while preserving the user selection.
     *
     * <p>
     * This method captures a stable selection key (normalized file name),
     * rebuilds the model, and restores selection if possible.</p>
     */
    private void applyFiltersPreservingSelection() {
        SelectionSnapshot snap = captureSelection();
        applyFilters();              // tu método actual (ya modificado con ViewMode)
        restoreSelection(snap);
    }

    /**
     * Rebuilds the visible {@link DefaultListModel} according to:
     * <ul>
     * <li>{@link ViewMode} (LOCAL/CLOUD/ALL)</li>
     * <li>type filter (audio/video)</li>
     * <li>week filter</li>
     * </ul>
     *
     * <p>
     * Cloud-only items are converted into "virtual" {@link ResourceDownloaded}
     * objects using {@link #toVirtualResource(Media)}.</p>
     */
    private void applyFilters() {
        downloadsModel.clear();

        // 1) Local (LOCAL o ALL)
        if (viewMode == ViewMode.LOCAL || viewMode == ViewMode.ALL) {
            for (ResourceDownloaded r : allResources) {
                if (matchTipo(r) && matchSemana(r)) {
                    downloadsModel.addElement(r);
                }
            }
        }

        // 2) Cloud (CLOUD o ALL)
        if (viewMode == ViewMode.CLOUD || viewMode == ViewMode.ALL) {
            for (Media m : cloudMedia) {
                String key = normalize(m.mediaFileName); // ✅ CLAVE NORMALIZADA
                if (key == null) {
                    continue;
                }

                ResourceState state = stateByFileName.get(key); // ✅ BUSQUEDA CORRECTA
                if (state == null) {
                    // por seguridad, si no está en el map, lo tratamos como cloud-only
                    state = ResourceState.CLOUD_ONLY;
                }

                if (viewMode == ViewMode.CLOUD) {
                    // En CLOUD mostramos cloud-only y BOTH (si quieres BOTH visibles aquí)
                    if (state == ResourceState.CLOUD_ONLY || state == ResourceState.BOTH) {
                        ResourceDownloaded vr = toVirtualResource(m);
                        if (matchTipo(vr) && matchSemana(vr)) {
                            downloadsModel.addElement(vr);
                        }
                    }
                } else {
                    // En ALL solo añadimos cloud-only para no duplicar los BOTH (que ya están en local)
                    if (state == ResourceState.CLOUD_ONLY) {
                        ResourceDownloaded vr = toVirtualResource(m);
                        if (matchTipo(vr) && matchSemana(vr)) {
                            downloadsModel.addElement(vr);
                        }
                    }
                }
            }
        }
        resolveUploadersForCurrentModel();
    }

    /**
     * Resolves uploader nicknames asynchronously for currently visible items.
     *
     * <p>
     * Uses {@link UploaderResolver} with caching:
     * <ul>
     * <li>If nickname is already present, no action is taken.</li>
     * <li>If cached, it is applied immediately.</li>
     * <li>If not cached, it fetches async and repaints the list when done.</li>
     * </ul>
     */
    private void resolveUploadersForCurrentModel() {
        for (int i = 0; i < downloadsModel.size(); i++) {
            ResourceDownloaded r = downloadsModel.get(i);

            Integer uid = r.getUploaderId();
            if (uid == null) {
                continue;
            }

            String current = r.getUploaderNick();
            if (current != null && !current.isBlank() && !current.equals("…")) {
                continue;
            }

            String cached = uploaderResolver.getCachedNick(uid);
            if (cached != null) {
                r.setUploaderNick(cached);
                continue;
            }

            uploaderResolver.fetchNickAsync(uid, () -> {
                String nick = uploaderResolver.getCachedNick(uid);
                if (nick != null) {
                    r.setUploaderNick(nick);
                    downloadsList.repaint();
                }
            });
        }
    }

    // ---- helpers de filtro ----
    /**
     * Normalizes a string for safe comparisons and filtering.
     *
     * <p>
     * This helper avoids {@link NullPointerException} and ensures consistent
     * matching by:
     * <ul>
     * <li>Converting {@code null} to an empty string</li>
     * <li>Trimming surrounding whitespace</li>
     * <li>Lowercasing using {@link java.util.Locale#ROOT} for
     * locale-independent behavior</li>
     * </ul>
     *
     * <p>
     * It is mainly used to normalize MIME types, extensions and filter values
     * coming from UI components or external sources.</p>
     *
     * @param s input string (may be null)
     * @return a normalized string (never null)
     */
    private static String norm(String s) {
        return (s == null) ? "" : s.toLowerCase(java.util.Locale.ROOT).trim();
    }

    /**
     * Callback invoked when local scan starts. Sets flags and updates the
     * status label.
     */
    public void onScanStarted() {
        isScanning = true;
        if (lblStatusScan != null) {
            lblStatusScan.setText("Escaneando carpeta local…");
        }
    }

    /**
     * Callback invoked when local scan finishes successfully.
     *
     * <p>
     * Updates master list, recomputes states, refreshes view and updates scan
     * status message. Also computes a delta against previous scan (+added /
     * -removed) to show a helpful message.</p>
     *
     * @param lista scanned local resources
     */
    public void onScanFinished(List<ResourceDownloaded> lista) {
        // snapshot actual
        Set<String> nowKeys = new HashSet<>();
        for (ResourceDownloaded r : lista) {
            String k = keyOf(r);
            if (k != null) {
                nowKeys.add(k);
            }
        }

        // diferencias
        int added = 0;
        for (String k : nowKeys) {
            if (!lastScanKeys.contains(k)) {
                added++;
            }
        }

        int removed = 0;
        for (String k : lastScanKeys) {
            if (!nowKeys.contains(k)) {
                removed++;
            }
        }

        // actualiza tus datos como ya haces
        allResources.clear();
        allResources.addAll(lista);
        hasScanned = true;
        isScanning = false;

        recomputeStates();

        if (viewMode == ViewMode.LOCAL || viewMode == ViewMode.ALL) {
            applyFiltersPreservingSelection();
        } else {
            downloadsList.repaint();
        }

        // mensaje no invasivo
        if (lblStatusScan != null) {
            if (!hasScannedOnce) {
                setScanStatus("Scan completado: " + nowKeys.size() + " archivos.");
            } else if (added == 0 && removed == 0) {
                setScanStatus("Scan completado: sin cambios.");
            } else {
                setScanStatus("Scan completado: +" + added + " nuevos, -" + removed + " eliminados.");
            }
        }

        hasScannedOnce = true;
        lastScanKeys = nowKeys;
    }

    private String lastScanMessage = "";

    private void setScanStatus(String msg) {
        lastScanMessage = msg;
        if (lblStatusScan == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            lblStatusScan.setText(lastScanMessage);
            Container p = lblStatusScan.getParent();
            if (p != null) {
                p.revalidate();
                p.repaint();
            }
        });
    }

    /**
     * Refreshes the scan status label with the last stored message. Useful if
     * the status label is recreated or UI is refreshed.
     */
    public void refreshScanStatusLabel() {
        setScanStatus(lastScanMessage);
    }

    // --------- HELPERS PROGRESS BAR----------
    /**
     * Sets busy mode for cloud transfers (download/upload).
     *
     * <p>
     * Updates the progress bar and the scan status label with a short
     * message.</p>
     *
     * @param msg message to show
     */
    private void startBusy(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (pbDownload != null) {
                pbDownload.setIndeterminate(true);
                pbDownload.setStringPainted(true);
                pbDownload.setString(msg);
            }
            if (lblStatusScan != null) {
                lblStatusScan.setText(msg);
            }
        });
    }

    /**
     * Ends busy mode for cloud transfers.
     *
     * <p>
     * Updates the progress bar to 100% and sets the status message.</p>
     *
     * @param msg message to show
     */
    private void stopBusy(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (pbDownload != null) {
                pbDownload.setIndeterminate(false);
                pbDownload.setStringPainted(true);
                pbDownload.setValue(100);
                pbDownload.setString("100%");
            }
            if (lblStatusScan != null) {
                lblStatusScan.setText(msg);
            }
        });
    }

    /**
     * Applies consistent styling to the progress bar to match the application
     * theme.
     */
    private void styleProgressBar() {
        if (pbDownload == null) {
            return;
        }

        pbDownload.setBorderPainted(false);
        pbDownload.setStringPainted(true);

        // verde sobrio (queda muy bien en dark)
        pbDownload.setForeground(new java.awt.Color(0x2F, 0x85, 0x5A));

        // opcional: un pelín más oscuro el fondo del track
        pbDownload.setBackground(new java.awt.Color(0x1A, 0x1A, 0x1A));
    }

    // --------- HELPERS FOCO LISTA ----------
    /**
     * Normalizes a file name to be used as a stable comparison key.
     *
     * <p>
     * The UI merges local resources and cloud media. File names may come with
     * different casing or surrounding spaces, so we normalize them to ensure
     * consistent matching.</p>
     *
     * <p>
     * Normalization rules:
     * <ul>
     * <li>{@code null} stays {@code null}</li>
     * <li>trim surrounding whitespace</li>
     * <li>convert to lowercase</li>
     * <li>empty string becomes {@code null}</li>
     * </ul>
     *
     * @param name original file name
     * @return normalized key or null if input is null/blank
     */
    private String normalize(String name) {
        if (name == null) {
            return null;
        }
        String s = name.trim();
        return s.isEmpty() ? null : s.toLowerCase();
    }

    /**
     * Returns the stable key used to identify a {@link ResourceDownloaded}
     * across list refreshes.
     *
     * <p>
     * We use the normalized resource name as the stable identifier because
     * indices change whenever filters are applied or the model is rebuilt.</p>
     *
     * @param r resource
     * @return normalized name key or null if resource/name is null
     */
    private String keyOf(ResourceDownloaded r) {
        if (r == null) {
            return null;
        }
        return normalize(r.getName());
    }

    /**
     * Snapshot representing the current selection in a stable way.
     *
     * <p>
     * Selection is stored both:
     * <ul>
     * <li>by {@code key}: normalized file name (preferred, stable across
     * refreshes)</li>
     * <li>by {@code index}: current index (fallback if key cannot be
     * found)</li>
     * </ul>
     *
     * <p>
     * This allows keeping selection consistent after:
     * <ul>
     * <li>applying filters</li>
     * <li>rebuilding the list model</li>
     * <li>syncing local/cloud state</li>
     * </ul>
     */
    private static class SelectionSnapshot {

        /**
         * Normalized file name key (preferred stable identifier).
         */
        final String key;
        /**
         * Previous selected index (fallback when key cannot be found).
         */
        final int index;

        /**
         * Creates a new selection snapshot.
         *
         * @param key normalized stable key (may be null)
         * @param index selected index at snapshot time
         */
        SelectionSnapshot(String key, int index) {
            this.key = key;
            this.index = index;
        }
    }

    /**
     * Captures the current list selection.
     *
     * <p>
     * This is typically invoked before rebuilding the list model, so selection
     * can be restored afterwards, improving UX (avoids losing focus after
     * filtering/refresh).</p>
     *
     * @return selection snapshot (never null)
     */
    private SelectionSnapshot captureSelection() {
        int idx = downloadsList.getSelectedIndex();
        ResourceDownloaded sel = downloadsList.getSelectedValue();
        return new SelectionSnapshot(keyOf(sel), idx);
    }

    /**
     * Restores selection after the list model has been rebuilt.
     *
     * <p>
     * Restore strategy:
     * <ol>
     * <li>Try to locate the same element by normalized key (stable name).</li>
     * <li>If not found, fall back to the previous index (clamped to current
     * model size).</li>
     * </ol>
     *
     * <p>
     * If the model is empty, selection is cleared.</p>
     *
     * @param snap selection snapshot captured before refreshing the model
     */
    private void restoreSelection(SelectionSnapshot snap) {
        if (snap == null || downloadsModel.isEmpty()) {
            downloadsList.clearSelection();
            return;
        }

        // 1) Find by stable key
        if (snap.key != null) {
            for (int i = 0; i < downloadsModel.size(); i++) {
                ResourceDownloaded r = downloadsModel.getElementAt(i);
                if (snap.key.equals(keyOf(r))) {
                    downloadsList.setSelectedIndex(i);
                    downloadsList.ensureIndexIsVisible(i);
                    return;
                }
            }
        }

        // 2) Fallback by previous index (clamped)
        int i = snap.index;
        if (i < 0) {
            i = 0;
        }
        if (i >= downloadsModel.size()) {
            i = downloadsModel.size() - 1;
        }

        downloadsList.setSelectedIndex(i);
        downloadsList.ensureIndexIsVisible(i);
    }

    /**
     * Selects the first list element whose normalized key matches the provided
     * file name.
     *
     * <p>
     * Useful after:
     * <ul>
     * <li>downloading from cloud</li>
     * <li>finishing a local scan</li>
     * <li>refreshing filters</li>
     * </ul>
     *
     * @param key file name (or key) to select
     */
    private void selectByKey(String key) {
        String k = normalize(key);
        if (k == null) {
            return;
        }

        for (int i = 0; i < downloadsModel.size(); i++) {
            if (k.equals(keyOf(downloadsModel.getElementAt(i)))) {
                downloadsList.setSelectedIndex(i);
                downloadsList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    /**
     * Callback invoked after a download finishes successfully (local or cloud).
     *
     * <p>
     * Recomputes local/cloud states, refreshes the model and tries to keep the
     * UX stable by selecting the item that has just been downloaded.</p>
     *
     * @param fileNameJustDownloaded file name (or path-derived name) to select
     */
    public void onDownloadCompleted(String fileNameJustDownloaded) {
        recomputeStates();
        applyFilters(); // o applyFiltersPreservingSelection()
        selectByKey(fileNameJustDownloaded);
    }

    /**
     * Known audio extensions used as fallback when MIME type is missing or
     * unreliable.
     */
    private static final java.util.Set<String> AUDIO_EXTENSIONS
            = java.util.Set.of("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus");

    /**
     * Known video extensions used as fallback when MIME type is missing or
     * unreliable.
     */
    private static final java.util.Set<String> VIDEO_EXTENSIONS
            = java.util.Set.of("mp4", "mkv", "avi", "mov", "webm", "flv");

    /**
     * Determines whether a resource should be treated as audio.
     *
     * <p>
     * Decision order:
     * <ol>
     * <li>If MIME type starts with {@code audio/}, return true.</li>
     * <li>If MIME type starts with {@code video/}, return false.</li>
     * <li>Fallback to extension list.</li>
     * </ol>
     *
     * @param r resource to check
     * @return true if resource is considered audio
     */
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

    /**
     * Determines whether a resource should be treated as video.
     *
     * <p>
     * Decision order:
     * <ol>
     * <li>If MIME type starts with {@code video/}, return true.</li>
     * <li>If MIME type starts with {@code audio/}, return false.</li>
     * <li>Fallback to extension list.</li>
     * </ol>
     *
     * @param r resource to check
     * @return true if resource is considered video
     */
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

    /**
     * Applies the "Type" filter (Audio/Video/All) to a given resource.
     *
     * <p>
     * The selected filter value is read from the UI combo box
     * {@code cmbTipo}.</p>
     *
     * @param r resource to test
     * @return true if the resource matches the selected type filter
     */
    private boolean matchTipo(ResourceDownloaded r) {
        String tipo = norm(String.valueOf(cmbTipo.getSelectedItem()));

        if (tipo.contains("video")) {
            return esVideo(r) && !esAudio(r);
        }
        if (tipo.contains("audio")) {
            return esAudio(r) && !esVideo(r);
        }
        return true;
    }

    /**
     * Applies the "This week" filter to a given resource.
     *
     * <p>
     * If {@code chkSemana} is not selected, all resources match. Otherwise the
     * resource must have a download date within the current week, where the
     * week is defined as Monday to Sunday.</p>
     *
     * @param r resource to test
     * @return true if resource matches the week filter
     */
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

    // ========= CLOUD MEDIA METHODS ==========
    /**
     * Loads cloud media list in background (requires a valid token).
     *
     * <p>
     * If there is no token, the method returns without doing anything. This
     * prevents unauthorized calls before login.</p>
     *
     * <p>
     * After loading:
     * <ul>
     * <li>{@code cloudMedia} list is replaced</li>
     * <li>{@link #recomputeStates()} is executed</li>
     * <li>filters are applied and list repainted</li>
     * </ul>
     *
     * @param parentForDialog parent component for error dialogs
     */
    public void loadCloudMedia(java.awt.Component parentForDialog) {
        if (cloudLoading) {
            return;
        }

        String token = mediaPolling.getToken(); // o mediaPolling.getToken()
        if (token == null || token.isBlank()) {
            System.out.println("[cloud] skip loadCloudMedia (no token)");
            return;
        }

        cloudLoading = true;

        SwingWorker<List<Media>, Void> worker = new SwingWorker<>() {

            @Override
            protected List<Media> doInBackground() throws Exception {
                return mediaPolling.getAllMedia();
            }

            @Override
            protected void done() {
                try {
                    List<Media> remote = get();

                    System.out.println("[cloud] remoteSize=" + remote.size()
                            + " viewMode=" + viewMode
                            + " hasScanned=" + hasScanned
                            + " modelBefore=" + downloadsModel.size()
                            + " cloudMediaBefore=" + cloudMedia.size());

                    cloudMedia.clear();
                    cloudMedia.addAll(remote);

                    recomputeStates();

                    applyFiltersPreservingSelection(); // o applyFiltersIfReady() si lo tienes bien

                    System.out.println("[cloud] modelAfter=" + downloadsModel.size()
                            + " cloudMediaAfter=" + cloudMedia.size());

                    downloadsList.repaint();

                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    System.err.println("[cloud] load failed: " + cause.getMessage());

                    // Si quieres: mostrar diálogo solo si hay sesión activa
                    JOptionPane.showMessageDialog(parentForDialog,
                            "Cloud session expired or unauthorized (401). Please log in again.",
                            "Cloud access",
                            JOptionPane.WARNING_MESSAGE);
                } finally {
                    cloudLoading = false;
                }
            }
        };

        worker.execute();
    }

    /**
     * Recomputes {@link ResourceState} for every known file name based on local
     * and cloud presence.
     *
     * <p>
     * Algorithm:
     * <ol>
     * <li>Mark all local resources as LOCAL_ONLY.</li>
     * <li>Iterate cloud media:
     * <ul>
     * <li>If not found locally -> CLOUD_ONLY</li>
     * <li>If found locally -> BOTH</li>
     * </ul>
     * </li>
     * </ol>
     */
    private void recomputeStates() {

        stateByFileName.clear();

        // 1️ Primero procesamos locales
        for (ResourceDownloaded local : allResources) {

            String name = normalize(local.getName());
            if (name == null) {
                continue;
            }

            stateByFileName.put(name, ResourceState.LOCAL_ONLY);
        }

        // 2️ Luego procesamos cloud
        for (Media m : cloudMedia) {

            String name = normalize(m.mediaFileName);
            if (name == null) {
                continue;
            }

            ResourceState current = stateByFileName.get(name);

            if (current == null) {
                // Solo está en cloud
                stateByFileName.put(name, ResourceState.CLOUD_ONLY);

            } else if (current == ResourceState.LOCAL_ONLY) {
                // Está en ambos
                stateByFileName.put(name, ResourceState.BOTH);
            }
            // Si ya estaba en BOTH no hacemos nada
        }
    }

    /**
     * Converts a cloud {@link Media} instance into a "virtual"
     * {@link ResourceDownloaded} so it can be rendered in the JList.
     *
     * <p>
     * Virtual resources have:
     * <ul>
     * <li>{@code route = null} (not present on disk yet)</li>
     * <li>{@code downloadDate = null}</li>
     * <li>mimeType and extension inferred from cloud metadata/file name</li>
     * </ul>
     *
     * @param m cloud media
     * @return a ResourceDownloaded representation usable by UI and filters
     */
    private ResourceDownloaded toVirtualResource(Media m) {
        ResourceDownloaded r = new ResourceDownloaded();
        r.setName(m.mediaFileName);
        r.setRoute(null); // no existe en disco todavía

        // Tamaño desconocido por ahora
        r.setSize(0L);

        // MIME y extensión desde el nombre
        r.setMimeType(m.mediaMimeType);
        String ext = null;
        if (m.mediaFileName != null) {
            int i = m.mediaFileName.lastIndexOf('.');
            if (i > 0 && i < m.mediaFileName.length() - 1) {
                ext = m.mediaFileName.substring(i + 1);
            }
        }
        r.setExtension(ext);

        // Fecha de descarga local: null (no está descargado)
        r.setDownloadDate(null);

        r.setUploaderId(m.userId); // o m.ownerId, el campo real

        return r;
    }

}
