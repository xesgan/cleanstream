package cat.dam.roig.cleanstream.ui.renderers;

import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import javax.swing.*;
import java.awt.*;

public class ResourceDownloadedRenderer extends JPanel implements ListCellRenderer<ResourceDownloaded> {

    private final JLabel lblIcon = new JLabel();
    private final JLabel lblTitle = new JLabel();
    private final JLabel lblSub = new JLabel();

    public ResourceDownloadedRenderer() {
        setLayout(new BorderLayout(8, 0));
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD));
        lblSub.setFont(lblSub.getFont().deriveFont(Font.PLAIN, 11f));
        lblSub.setForeground(new Color(120, 120, 120));

        var textPanel = new JPanel(new GridLayout(0, 1));
        textPanel.setOpaque(false);
        textPanel.add(lblTitle);
        textPanel.add(lblSub);

        add(lblIcon, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);

        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ResourceDownloaded> list,
            ResourceDownloaded value, int index,
            boolean isSelected, boolean cellHasFocus) {

        // Título
        lblTitle.setText(value.getName());

        // Subtítulo (elige lo que más te sirva)
        String sub = String.format(".%s   —   %s   —   %s",
                value.getExtension(),
                humanReadable(value.getSize()),
                value.getDownloadDate() != null ? value.getDownloadDate().toLocalDate().toString() : "");
        lblSub.setText(sub);

        // Miniatura (opcional): intenta cargarla; si no, icono por MIME
        lblIcon.setIcon(loadThumbOrFallback(value));

        // Selección
        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        lblTitle.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        lblSub.setForeground(isSelected ? list.getSelectionForeground().darker() : new Color(120, 120, 120));
        return this;
    }

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

    private Icon loadThumbOrFallback(ResourceDownloaded r) {
        // Si en el futuro generas thumbnails (con ffmpeg), pon aquí la ruta:
        // Path thumb = Paths.get(r.getRoute() + ".jpg"); // ejemplo
        // if (Files.exists(thumb)) return scaledIcon(new ImageIcon(thumb.toString()), 36, 36);

        // Fallback por MIME:
        if (r.getMimeType() != null && r.getMimeType().startsWith("video/")) {
            return UIManager.getIcon("FileView.hardDriveIcon"); // cámbialo por tu icono
        }
        if (r.getMimeType() != null && r.getMimeType().startsWith("audio/")) {
            return UIManager.getIcon("FileView.fileIcon");
        }
        return UIManager.getIcon("FileView.directoryIcon");
    }

    private Icon scaledIcon(ImageIcon src, int w, int h) {
        Image img = src.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
}
