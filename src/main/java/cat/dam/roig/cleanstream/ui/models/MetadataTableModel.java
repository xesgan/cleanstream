package cat.dam.roig.cleanstream.ui.models;

import cat.dam.roig.cleanstream.domain.ResourceDownloaded;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Table model used to display metadata for a selected
 * {@link ResourceDownloaded}.
 *
 * <p>
 * This model follows a simple "property/value" structure with 2 columns:
 * <ul>
 * <li><b>Propiedad</b>: the label of the metadata field (e.g., "Nombre",
 * "Ruta")</li>
 * <li><b>Valor</b>: the formatted value for the selected resource</li>
 * </ul>
 *
 * <p>
 * Usage:
 * <ul>
 * <li>The UI (MainFrame/DownloadsController) calls
 * {@link #setResource(ResourceDownloaded)} whenever the user selects a
 * different item in the downloads list.</li>
 * <li>The model rebuilds its internal rows and calls
 * {@link #fireTableDataChanged()} to refresh the JTable view.</li>
 * </ul>
 *
 * <p>
 * Design decisions:
 * <ul>
 * <li>Rows are stored as {@code String[]} to keep rendering simple and
 * consistent.</li>
 * <li>File size is formatted via {@link #humanReadable(long)} for
 * readability.</li>
 * <li>Null-safe formatting is applied to optional fields (mimeType, extension,
 * date).</li>
 * </ul>
 *
 * <p>
 * Threading: Swing table models should be updated on the EDT. Ensure calls to
 * {@link #setResource(ResourceDownloaded)} happen on the Swing thread.
 * </p>
 *
 * @author metku
 */
public class MetadataTableModel extends AbstractTableModel {

    /**
     * Internal table rows. Each row has exactly 2 columns: property and value.
     */
    private final List<String[]> rows = new ArrayList<>();

    /**
     * Column headers for the table.
     */
    private final String[] cols = {"Propiedad", "Valor"};

    /**
     * Rebuilds the model rows for the provided resource and refreshes the
     * table.
     *
     * <p>
     * If {@code r} is {@code null}, the table will be cleared.
     * </p>
     *
     * @param r selected resource (may be null)
     */
    public void setResource(ResourceDownloaded r) {
        rows.clear();

        if (r != null) {
            rows.add(new String[]{"Nombre", safe(r.getName())});
            rows.add(new String[]{"Ruta", safe(r.getRoute())});
            rows.add(new String[]{"Tamaño", humanReadable(r.getSize())});
            rows.add(new String[]{"MIME", safe(String.valueOf(r.getMimeType()))});
            rows.add(new String[]{"Extensión", safe(String.valueOf(r.getExtension()))});
            rows.add(new String[]{"Fecha descarga", r.getDownloadDate() != null ? r.getDownloadDate().toString() : ""});
        }

        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return cols.length;
    }

    @Override
    public String getColumnName(int c) {
        return cols[c];
    }

    @Override
    public Object getValueAt(int r, int c) {
        return rows.get(r)[c];
    }

    /**
     * Formats a byte count using binary units (1024 base).
     *
     * <p>
     * Example: 1536 -> "1.5 KB"
     * </p>
     *
     * @param bytes size in bytes
     * @return human-readable formatted size
     */
    private static String humanReadable(long bytes) {
        String[] u = {"B", "KB", "MB", "GB"};
        double v = bytes;
        int i = 0;

        while (v >= 1024 && i < u.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format("%.1f %s", v, u[i]);
    }

    /**
     * Simple null-safe helper to avoid showing "null" strings in the UI.
     *
     * @param s input string (may be null)
     * @return empty string if null; otherwise trimmed value
     */
    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
