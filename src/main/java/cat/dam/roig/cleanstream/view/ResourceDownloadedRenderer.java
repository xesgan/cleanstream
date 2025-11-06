package cat.dam.roig.cleanstream.view;

import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ResourceDownloadedRenderer extends JPanel implements ListCellRenderer<ResourceDownloaded> {

    private final JLabel lblIcon = new JLabel();
    private final JLabel lblTitle = new JLabel();
    private final JLabel lblMeta  = new JLabel();
    private final JPanel right    = new JPanel(new GridLayout(2,1));

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ResourceDownloadedRenderer() {
        setLayout(new BorderLayout(8, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Tipografías
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD));
        lblMeta.setFont(lblMeta.getFont().deriveFont(11f));

        // Composición
        right.add(lblTitle);
        right.add(lblMeta);

        add(lblIcon, BorderLayout.WEST);
        add(right,   BorderLayout.CENTER);

        setOpaque(true);
        right.setOpaque(false);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ResourceDownloaded> list,
            ResourceDownloaded value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        if (value == null) {
            lblTitle.setText("");
            lblMeta.setText("");
            lblIcon.setIcon(null);
            return this;
        }

        // 1) Título (nombre)
        lblTitle.setText(value.getName());

        // 2) Línea de metadatos: extensión, tamaño, mime, fecha
        String meta = String.format(".%s  —  %s  —  %s  —  %s",
                safe(value.getExtension()),
                humanSize(value.getSize()),
                safe(value.getMimeType()),
                value.getDownloadDate() != null ? dtf.format(value.getDownloadDate()) : "sin fecha");
        lblMeta.setText(meta);

        // 3) Icono simple según tipo (puedes mejorar con tus propios PNGs)
        lblIcon.setIcon(iconFor(value));

        // 4) Colores de selección/normal respetando L&F
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
            lblTitle.setForeground(list.getSelectionForeground());
            lblMeta.setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            lblTitle.setForeground(list.getForeground());
            // tono gris para meta
            lblMeta.setForeground(UIManager.getColor("Label.disabledForeground"));
            if (lblMeta.getForeground() == null) {
                lblMeta.setForeground(new Color(120,120,120));
            }
        }

        return this; // IMPORTANTÍSIMO: devolver SIEMPRE el mismo componente
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static Icon iconFor(ResourceDownloaded r) {
        String ext = (r.getExtension() == null) ? "" : r.getExtension().toLowerCase();
        String mime = (r.getMimeType() == null) ? "" : r.getMimeType().toLowerCase();

        // Puedes mapear mejor por MIME/EXT (aquí unos ejemplos simples):
        String key;
        if (mime.startsWith("video/") || ext.matches("mp4|mkv|webm|mov|avi")) {
            key = UIManager.getString("FileView.directoryIcon") != null ? "FileView.fileIcon" : null; // placeholder
            return UIManager.getIcon("FileView.hardDriveIcon"); // reutilizo iconos swing por rapidez
        } else if (mime.startsWith("audio/") || ext.matches("mp3|wav|flac|ogg|m4a")) {
            return UIManager.getIcon("FileView.floppyDriveIcon");
        } else if (mime.startsWith("image/") || ext.matches("png|jpg|jpeg|gif|webp|bmp")) {
            return UIManager.getIcon("FileView.computerIcon");
        } else if (ext.matches("pdf|docx?|xlsx?|pptx?")) {
            return UIManager.getIcon("FileView.fileIcon");
        } else {
            return UIManager.getIcon("FileView.fileIcon");
        }
    }
}

