package cat.dam.roig.cleanstream.ui.renderers;

import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import cat.dam.roig.cleanstream.models.ResourceState;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ResourceDownloadedRenderer extends JPanel implements ListCellRenderer<ResourceDownloaded> {

    private final JLabel lblIcon = new JLabel();

    // Título con wrap real
    private final JTextArea txtTitle = new JTextArea();

    // Subtítulo en una línea (puedes poner uploader aquí)
    private final JLabel lblSub = new JLabel();

    private final JPanel textPanel = new JPanel();

    private final Map<String, ResourceState> stateByFileName;

    private static final Color SUB_FG = new Color(120, 120, 120);

    public ResourceDownloadedRenderer(Map<String, ResourceState> stateByFileName) {
        this.stateByFileName = stateByFileName;

        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setOpaque(true);

        // Icon
        lblIcon.setPreferredSize(new Dimension(36, 36));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblIcon.setVerticalAlignment(SwingConstants.TOP);

        // Title (wrap)
        txtTitle.setLineWrap(true);
        txtTitle.setWrapStyleWord(true);
        txtTitle.setEditable(false);
        txtTitle.setFocusable(false);
        txtTitle.setOpaque(false);
        txtTitle.setBorder(null);
        txtTitle.setMargin(new Insets(0, 0, 0, 0));

        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("SansSerif", Font.PLAIN, 12);
        txtTitle.setFont(base.deriveFont(Font.BOLD));

        // Subtitle
        lblSub.setFont(base.deriveFont(Font.PLAIN, 11f));
        lblSub.setForeground(SUB_FG);

        // Text panel vertical
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        txtTitle.setAlignmentX(LEFT_ALIGNMENT);
        lblSub.setAlignmentX(LEFT_ALIGNMENT);
        textPanel.setAlignmentX(LEFT_ALIGNMENT);

        textPanel.add(txtTitle);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(lblSub);

        add(lblIcon, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ResourceDownloaded> list,
            ResourceDownloaded value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        String name = (value != null && value.getName() != null) ? value.getName() : "";

        // Estado / prefijo
        String key = normalize(value != null ? value.getName() : null);
        ResourceState state = (key == null)
                ? ResourceState.LOCAL_ONLY
                : stateByFileName.getOrDefault(key, ResourceState.LOCAL_ONLY);

        String prefix = switch (state) {
            case BOTH -> "[LOCAL + CLOUD] ";
            case CLOUD_ONLY -> "[CLOUD] ";
            case LOCAL_ONLY -> "[LOCAL] ";
        };

        // Icono
        lblIcon.setIcon(loadThumbOrFallback(value));

        // Subtítulo (aquí luego metes uploader)
        String sub = String.format(".%s   —   %s   —   %s",
                safe(value != null ? value.getExtension() : ""),
                humanReadable(value != null ? value.getSize() : 0),
                (value != null && value.getDownloadDate() != null)
                        ? value.getDownloadDate().toLocalDate().toString()
                        : ""
        );
        lblSub.setText(sub);

        // Ancho real del viewport
        int viewportW = getViewportWidth(list);

        int iconW = 36;
        Icon ic = lblIcon.getIcon();
        if (ic != null) iconW = ic.getIconWidth();

        int hgap = 8;          // BorderLayout(8,0)
        int paddingLR = 8 + 8; // EmptyBorder(6,8,6,8)
        int extra = iconW + hgap + paddingLR;

        int textW = Math.max(180, viewportW - extra);

        // ===== TÍTULO: wrap + limitar a 2 líneas =====
        txtTitle.setText(prefix + name);

        // Forzar cálculo
        txtTitle.setSize(new Dimension(textW, Integer.MAX_VALUE));
        Dimension pref = txtTitle.getPreferredSize();

        FontMetrics fm = txtTitle.getFontMetrics(txtTitle.getFont());
        int lineH = fm.getHeight();
        int insetsTB = txtTitle.getInsets().top + txtTitle.getInsets().bottom;

        int maxTitleH = insetsTB + (2 * lineH); // 2 líneas
        int finalTitleH = Math.min(pref.height, maxTitleH);

        txtTitle.setPreferredSize(new Dimension(textW, finalTitleH));
        txtTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, finalTitleH));

        // ===== ALTURA MÍNIMA DE CELDA (evita pisado sí o sí) =====
        // si el título ocupa 2 líneas, damos más alto
        boolean twoLines = pref.height > (insetsTB + lineH + 1);
        int minH = twoLines ? 70 : 56;

        Dimension cellPref = getPreferredSize();
        setPreferredSize(new Dimension(cellPref.width, Math.max(cellPref.height, minH)));

        // Selección
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            txtTitle.setForeground(list.getSelectionForeground());
            lblSub.setForeground(list.getSelectionForeground().darker());
        } else {
            setBackground(list.getBackground());
            txtTitle.setForeground(list.getForeground());
            lblSub.setForeground(SUB_FG);
        }

        return this;
    }

    private int getViewportWidth(JList<?> list) {
        Container p = list.getParent();
        if (p instanceof JViewport vp) {
            int w = vp.getWidth();
            if (w > 0) return w;
        }
        int w = list.getWidth();
        if (w > 0) return w;
        return 600;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
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
        if (r == null) return UIManager.getIcon("FileView.fileIcon");

        String mime = r.getMimeType();
        if (mime != null && mime.startsWith("video/")) {
            return UIManager.getIcon("FileView.fileIcon");
        }
        if (mime != null && mime.startsWith("audio/")) {
            return UIManager.getIcon("FileView.fileIcon");
        }
        return UIManager.getIcon("FileView.directoryIcon");
    }

    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s.toLowerCase();
    }
}
