package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.models.ResourceDownloaded;
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

    // estado que antes estaba en MainFrame
    private boolean hasScanned = false;
    private boolean isScanning = false;

    public DownloadsController(DefaultListModel<ResourceDownloaded> downloadsModel,
            List<ResourceDownloaded> allResources,
            JComboBox<String> cmbTipo,
            JCheckBox chkSemana) {
        this.downloadsModel = downloadsModel;
        this.allResources = allResources;
        this.cmbTipo = cmbTipo;
        this.chkSemana = chkSemana;
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
