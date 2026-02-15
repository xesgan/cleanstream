package cat.dam.roig.cleanstream.ui.renderers;

import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import cat.dam.roig.cleanstream.models.ResourceState;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ResourceDownloadedRenderer extends JPanel implements ListCellRenderer<ResourceDownloaded> {

    private final JLabel lblIcon = new JLabel();

    // Badge estado (LOCAL/CLOUD/BOTH)
    private final JLabel lblBadge = new JLabel();

    // Título con wrap real (2 líneas máx)
    private final JTextArea txtTitle = new JTextArea();

    // Subtítulo (1 línea)
    private final JLabel lblSub = new JLabel();

    private final JPanel titleRow = new JPanel();
    private final JPanel textPanel = new JPanel();

    private final Map<String, ResourceState> stateByFileName;

    // ===== Dark palette =====
    private static final Color BG = new Color(0x121212);
    private static final Color BG_SEL = new Color(0x2A2A2A);
    private static final Color BG_HOVER = new Color(0x1A1A2A); 
    private static final Color FG = new Color(0xE6E6E6);
    private static final Color SUB_FG = new Color(0xA0A0A0);
    private static final Color DIV = new Color(0x2C2C2C);

    // Badge colors
    private static final Color BADGE_LOCAL = new Color(0x2F855A);
    private static final Color BADGE_CLOUD = new Color(0x805AD5);
    private static final Color BADGE_BOTH = new Color(0x2B6CB0);

    public ResourceDownloadedRenderer(Map<String, ResourceState> stateByFileName) {
        this.stateByFileName = stateByFileName;

        setLayout(new BorderLayout(10, 0));
        setOpaque(true);

        // Separador suave + padding
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIV),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        // Icon
        lblIcon.setPreferredSize(new Dimension(36, 36));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblIcon.setVerticalAlignment(SwingConstants.TOP);

        // Badge
        lblBadge.setOpaque(true);
        lblBadge.setForeground(Color.WHITE);
        lblBadge.setFont(lblBadge.getFont().deriveFont(Font.BOLD, 11f));
        lblBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // Title (wrap)
        txtTitle.setLineWrap(true);
        txtTitle.setWrapStyleWord(true);
        txtTitle.setEditable(false);
        txtTitle.setFocusable(false);
        txtTitle.setOpaque(false);
        txtTitle.setBorder(null);
        txtTitle.setMargin(new Insets(0, 0, 0, 0));

        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new Font("SansSerif", Font.PLAIN, 12);
        }
        txtTitle.setFont(base.deriveFont(Font.BOLD));
        txtTitle.setForeground(FG);

        // Subtitle
        lblSub.setFont(base.deriveFont(Font.PLAIN, 11f));
        lblSub.setForeground(SUB_FG);

        // Title row: badge + title
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);

        titleRow.add(lblBadge);
        titleRow.add(Box.createHorizontalStrut(8));
        titleRow.add(txtTitle);

        // Text panel vertical
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setAlignmentX(LEFT_ALIGNMENT);

        lblSub.setAlignmentX(LEFT_ALIGNMENT);

        textPanel.add(titleRow);
        textPanel.add(Box.createVerticalStrut(4));
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

        // Estado
        String key = normalize(value != null ? value.getName() : null);
        ResourceState state = (key == null)
                ? ResourceState.LOCAL_ONLY
                : stateByFileName.getOrDefault(key, ResourceState.LOCAL_ONLY);

        applyBadge(state);

        // Icono
        lblIcon.setIcon(loadThumbOrFallback(value));

        // Subtítulo (con uploader)
        String uploader = safe(value != null ? value.getUploaderNick() : null);
        if (uploader.isBlank()) {
            uploader = "…";
        }

        String ext = safe(value != null ? value.getExtension() : null);
        String sizeTxt = (value != null && value.getSize() > 0) ? humanReadable(value.getSize()) : "—";
        String dateTxt = (value != null && value.getDownloadDate() != null)
                ? value.getDownloadDate().toLocalDate().toString()
                : "—";

        String sub = String.format(".%s   —   %s   —   %s   —   Subido por: %s",
                ext, sizeTxt, dateTxt, uploader
        );
        lblSub.setText(sub);

        // Ancho real del viewport
        int viewportW = getViewportWidth(list);

        int iconW = 36;
        Icon ic = lblIcon.getIcon();
        if (ic != null) {
            iconW = ic.getIconWidth();
        }

        // extra: icon + gap + padding + (badge aprox)
        int extra = iconW + 10 + 12 + 12 + 90; // icon + hgap + left+right padding + badge area approx
        int textW = Math.max(220, viewportW - extra);

        // ===== TÍTULO: wrap + limitar a 2 líneas =====
        txtTitle.setText(name);

        txtTitle.setSize(new Dimension(textW, Integer.MAX_VALUE));
        Dimension pref = txtTitle.getPreferredSize();

        FontMetrics fm = txtTitle.getFontMetrics(txtTitle.getFont());
        int lineH = fm.getHeight();
        int insetsTB = txtTitle.getInsets().top + txtTitle.getInsets().bottom;

        int maxTitleH = insetsTB + (2 * lineH);
        int finalTitleH = Math.min(pref.height, maxTitleH);

        txtTitle.setPreferredSize(new Dimension(textW, finalTitleH));
        txtTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, finalTitleH));

        // ===== ALTURA MÍNIMA (por seguridad) =====
        boolean twoLines = pref.height > (insetsTB + lineH + 1);
        int minH = twoLines ? 86 : 72; // más “air” en dark

        Dimension cellPref = getPreferredSize();
        setPreferredSize(new Dimension(cellPref.width, Math.max(cellPref.height, minH)));

        // Fondo / selección
        int hoverIndex = cat.dam.roig.cleanstream.ui.utils.ListHoverSupport.getHoverIndex(list);
        boolean isHover = (index == hoverIndex);

        if (isSelected) {
            setBackground(BG_SEL);
        } else if (isHover) {
            setBackground(BG_HOVER);
        } else {
            setBackground(BG);
        }

        return this;
    }

    private void applyBadge(ResourceState st) {
        switch (st) {
            case LOCAL_ONLY -> {
                lblBadge.setText("LOCAL");
                lblBadge.setBackground(BADGE_LOCAL);
            }
            case CLOUD_ONLY -> {
                lblBadge.setText("CLOUD");
                lblBadge.setBackground(BADGE_CLOUD);
            }
            case BOTH -> {
                lblBadge.setText("LOCAL+CLOUD");
                lblBadge.setBackground(BADGE_BOTH);
            }
        }
    }

    private int getViewportWidth(JList<?> list) {
        Container p = list.getParent();
        if (p instanceof JViewport vp) {
            int w = vp.getWidth();
            if (w > 0) {
                return w;
            }
        }
        int w = list.getWidth();
        if (w > 0) {
            return w;
        }
        return 700;
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
        if (r == null) {
            return UIManager.getIcon("FileView.fileIcon");
        }

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
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s.toLowerCase();
    }
}
