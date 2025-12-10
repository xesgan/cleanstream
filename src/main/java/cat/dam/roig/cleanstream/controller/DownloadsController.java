package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.models.MetadataTableModel;
import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import cat.dam.roig.cleanstream.models.ResourceState;
import cat.dam.roig.cleanstream.services.DownloadsScanner;
import cat.dam.roig.cleanstream.ui.renderers.ResourceDownloadedRenderer;
import cat.dam.roig.roigmediapollingcomponent.Media;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final List<Media> cloudMedia = new ArrayList<>();

    // estado que antes estaba en MainFrame
    private boolean hasScanned = false;
    private boolean isScanning = false;

    public DownloadsController(
            DefaultListModel<ResourceDownloaded> downloadsModel,
            List<ResourceDownloaded> allResources,
            JComboBox<String> cmbTipo,
            JCheckBox chkSemana,
            JList<ResourceDownloaded> downloadsList,
            MetadataTableModel metaModel,
            JButton btnDelete,
            RoigMediaPollingComponent mediaComponent
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

        initSelectionListener();
        downloadsList.setCellRenderer(new ResourceDownloadedRenderer(stateByFileName));
    }

    private void initSelectionListener() {
        btnDelete.setEnabled(false);

        downloadsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ResourceDownloaded sel = downloadsList.getSelectedValue();
                metaModel.setResource(sel);
                downloadsList.setEnabled(sel != null);
            }
        });
    }

    public void scanDownloads(Path downloadsDir, JButton btnScan) {

        btnScan.setEnabled(false);

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
                    btnScan.setEnabled(true);
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

        Path file = Paths.get(selected.getRoute());

        if (!confirmDeletion(parentForDialog, file)) {
            return;
        }

        try {
            boolean deleted = Files.deleteIfExists(file);

            if (deleted) {
                downloadsModel.remove(idx);

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
        if (isScanning) {
            return;
        }
        applyFilters();
    }

    /**
     * Rellena el modelo de la JList en función de los filtros activos.
     */
    private void applyFilters() {
        downloadsModel.clear();

        // 1) Recursos locales (LOCAL_ONLY o BOTH)
        for (ResourceDownloaded r : allResources) {
            if (matchTipo(r) && matchSemana(r)) {
                downloadsModel.addElement(r);
            }
        }

        // 2) Recursos que solo están en la nube (CLOUD_ONLY)
        for (Media m : cloudMedia) {
            String name = m.mediaFileName;
            if (name == null) {
                continue;
            }

            ResourceState state = stateByFileName.get(name);
            if (state == ResourceState.CLOUD_ONLY) {
                ResourceDownloaded virtualRes = toVirtualResource(m);

                if (matchTipo(virtualRes) && matchSemana(virtualRes)) {
                    downloadsModel.addElement(virtualRes);
                }
            }
        }
    }

    // ---- helpers de filtro ----
    private static String norm(String s) {
        return (s == null) ? "" : s.toLowerCase(java.util.Locale.ROOT).trim();
    }

    public void onScanStarted() {
        isScanning = true;
    }

    public void onScanFinished(List<ResourceDownloaded> lista) {
        allResources.clear();
        allResources.addAll(lista);
        hasScanned = true;
        isScanning = false;

        recomputeStates();
        applyFilters();   // aquí ya tenemos datos y filtros
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
        SwingWorker<List<Media>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Media> doInBackground() throws Exception {
                // El componente ya tiene el token después del login
                return mediaComponent.getAllMedia();
            }

            @Override
            protected void done() {
                try {
                    List<Media> remote = get();
                    cloudMedia.clear();
                    cloudMedia.addAll(remote);

                    // Merge Cloud + local
                    recomputeStates();
                    applyFiltersIfReady();

//                    System.out.println("Cloud media loaded: " + cloudMedia.size() + " items");
//                    cloudMedia.stream()
//                            .limit(5)
//                            .forEach(m -> System.out.println(" - " + m.mediaFileName));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            parentForDialog,
                            "Error al cargar los medios de la nube.",
                            "Cloud sync error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void recomputeStates() {
        stateByFileName.clear();

        // 1) Todos los locales empiezan como LOCAL_ONLY
        for (ResourceDownloaded local : allResources) {
            String name = local.getName();
            if (name != null) {
                stateByFileName.put(name, ResourceState.LOCAL_ONLY);
            }
        }

        // 2) Para cada cloud:
        for (Media m : cloudMedia) {
            String name = m.mediaFileName;
            if (name == null) {
                continue;
            }

            ResourceState current = stateByFileName.get(name);
            if (current == null) {
                // No existe en local → solo cloud
                stateByFileName.put(name, ResourceState.CLOUD_ONLY);
            } else if (current == ResourceState.LOCAL_ONLY) {
                // Existe en local → ahora es BOTH
                stateByFileName.put(name, ResourceState.BOTH);
            }
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

        return r;
    }

}
