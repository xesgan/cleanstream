package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.models.MetadataTableModel;
import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import cat.dam.roig.cleanstream.services.DownloadsScanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DownloadsController {

    private final DefaultListModel<ResourceDownloaded> downloadsModel;
    private final List<ResourceDownloaded> allResources;
    private final JComboBox<String> cmbTipo;
    private final JCheckBox chkSemana;

    private final JList<ResourceDownloaded> downloadsList;
    private final MetadataTableModel metaModel;
    private final JButton btnDelete;

    // estado que antes estaba en MainFrame
    private boolean hasScanned = false;
    private boolean isScanning = false;

    public DownloadsController(DefaultListModel<ResourceDownloaded> downloadsModel,
            List<ResourceDownloaded> allResources,
            JComboBox<String> cmbTipo,
            JCheckBox chkSemana,
            JList<ResourceDownloaded> downloadsList,
            MetadataTableModel metaModel,
            JButton btnDelete) {
        this.downloadsModel = downloadsModel;
        this.allResources = allResources;
        this.cmbTipo = cmbTipo;
        this.chkSemana = chkSemana;
        this.downloadsList = downloadsList;
        this.metaModel = metaModel;
        this.btnDelete = btnDelete;

        initSelectionListener();
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
        for (ResourceDownloaded r : allResources) {
            if (matchTipo(r) && matchSemana(r)) {
                downloadsModel.addElement(r);
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

}
