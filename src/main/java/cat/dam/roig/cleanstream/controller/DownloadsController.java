package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.models.MetadataTableModel;
import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import cat.dam.roig.cleanstream.models.ResourceState;
import cat.dam.roig.cleanstream.services.DownloadsScanner;
import cat.dam.roig.cleanstream.services.UserPreferences;
import cat.dam.roig.cleanstream.services.UploaderResolver;
import cat.dam.roig.cleanstream.ui.renderers.ResourceDownloadedRenderer;
import cat.dam.roig.roigmediapollingcomponent.Media;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DownloadsController {

    private final RoigMediaPollingComponent mediaComponent;

    private final DefaultListModel<ResourceDownloaded> downloadsModel;
    private final List<ResourceDownloaded> allResources;
    private final JComboBox<String> cmbTipo;
    private final JCheckBox chkSemana;

    private final JList<ResourceDownloaded> downloadsList;
    private final Map<String, ResourceState> stateByFileName = new HashMap<>();
    private final MetadataTableModel metaModel;
    private final JButton btnDelete;
    private final JButton btnDownloadFromCloud;
    private final JButton btnUploadFromLocal;
    private JLabel lblStatusScan;
    private final JProgressBar pbDownload;

    private UploaderResolver uploaderResolver;

    private final List<Media> cloudMedia = new ArrayList<>();
    private Set<String> lastScanKeys = new HashSet<>();
    private boolean hasScannedOnce = false;

    private String pendingSelectKey = null;

    enum ViewMode {
        LOCAL, CLOUD, ALL
    }
    private ViewMode viewMode = ViewMode.ALL;

    private boolean hasScanned = false;
    private boolean isScanning = false;
    private boolean cloudLoading = false;

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
            RoigMediaPollingComponent mediaComponent,
            JLabel lblStatusScan,
            JProgressBar pbDownload
    ) {
        if (mediaComponent == null) {
            throw new IllegalArgumentException("mediaComponent no puede ser null");
        }

        this.mediaComponent = mediaComponent;
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
                userId -> this.mediaComponent.getNickName(userId)
        );
        initSelectionListener();
        initDoubleClickOpen();
        downloadsList.setCellRenderer(new ResourceDownloadedRenderer(stateByFileName));
    }

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
                 if (st == ResourceState.CLOUD_ONLY) return;
                openWithSystemPlayer(r.getRoute());
            }
        });
    }

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
     * Elimina el recurso seleccionado en la lista (si existe) tanto del disco
     * como del modelo de la JList.
     *
     * @param parentForDialog
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
                mediaComponent.download(media.id, dest);
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
                mediaComponent.uploadFileMultipart(file, fromUrl);
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
     * Muestra un diálogo de confirmación antes de eliminar el archivo.
     *
     * @param file Ruta del archivo a eliminar.
     * @return true si el usuario confirma, false en caso contrario.
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
     * Aplica los filtros de tipo y semana solo si ya se ha realizado al menos
     * un escaneo y no hay uno en curso.
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

    private void applyFiltersPreservingSelection() {
        SelectionSnapshot snap = captureSelection();
        applyFilters();              // tu método actual (ya modificado con ViewMode)
        restoreSelection(snap);
    }

    /**
     * Rellena el modelo de la JList en función de los filtros activos.
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
    private static String norm(String s) {
        return (s == null) ? "" : s.toLowerCase(java.util.Locale.ROOT).trim();
    }

    public void onScanStarted() {
        isScanning = true;
        if (lblStatusScan != null) {
            lblStatusScan.setText("Escaneando carpeta local…");
        }
    }

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

    public void refreshScanStatusLabel() {
        setScanStatus(lastScanMessage);
    }

    // --------- HELPERS PROGRESS BAR----------
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

    // --------- HELPERS FOCO LISTA ----------
    private String normalize(String name) {
        if (name == null) {
            return null;
        }
        String s = name.trim();
        return s.isEmpty() ? null : s.toLowerCase();
    }

    private String keyOf(ResourceDownloaded r) {
        if (r == null) {
            return null;
        }
        return normalize(r.getName());
    }

    private static class SelectionSnapshot {

        final String key;   // nombre de archivo (estable)
        final int index;    // índice actual (fallback)

        SelectionSnapshot(String key, int index) {
            this.key = key;
            this.index = index;
        }
    }

    private SelectionSnapshot captureSelection() {
        int idx = downloadsList.getSelectedIndex();
        ResourceDownloaded sel = downloadsList.getSelectedValue();
        return new SelectionSnapshot(keyOf(sel), idx);
    }

    private void restoreSelection(SelectionSnapshot snap) {
        if (snap == null || downloadsModel.isEmpty()) {
            downloadsList.clearSelection();
            return;
        }

        // 1) Buscar por key
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

        // 2) Fallback por índice
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

    public void onDownloadCompleted(String fileNameJustDownloaded) {
        recomputeStates();
        applyFilters(); // o applyFiltersPreservingSelection()
        selectByKey(fileNameJustDownloaded);
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

    // ========= CLOUD MEDIA METHODS ==========
    public void loadCloudMedia(java.awt.Component parentForDialog) {
        if (cloudLoading) {
            return;
        }
        cloudLoading = true;

        SwingWorker<List<Media>, Void> worker = new SwingWorker<>() {

            @Override
            protected List<Media> doInBackground() throws Exception {
                return mediaComponent.getAllMedia();
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
                    ex.printStackTrace();
                } finally {
                    cloudLoading = false;
                }
            }

        };

        worker.execute();
    }

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
