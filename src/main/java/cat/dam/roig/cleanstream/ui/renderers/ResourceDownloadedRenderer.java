package cat.dam.roig.cleanstream.ui.renderers;

import cat.dam.roig.cleanstream.domain.ResourceDownloaded;
import cat.dam.roig.cleanstream.domain.ResourceState;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Custom Swing renderer for {@link ResourceDownloaded} items inside a
 * {@link JList}.
 *
 * <p>
 * This renderer draws each resource as a "card-like" row with:
 * <ul>
 * <li>An icon representing the media type (video/audio/file)</li>
 * <li>A colored badge showing the resource state (LOCAL / CLOUD /
 * LOCAL+CLOUD)</li>
 * <li>A wrapped title (max 2 lines) using a {@link JTextArea}</li>
 * <li>A one-line subtitle with extension, size, date and uploader</li>
 * </ul>
 *
 * <p>
 * Why a custom renderer? {@link JList} normally renders each row with a single
 * {@link JLabel}. This class uses a {@link JPanel} layout to achieve a richer
 * UI and better readability.
 * </p>
 *
 * <h3>State resolution</h3>
 * The renderer receives a map ({@code stateByFileName}) where the key is a
 * normalized filename (lowercase + trimmed) and the value is the
 * {@link ResourceState}. When the current item is not present in the map, it
 * falls back to {@link ResourceState#LOCAL_ONLY}.
 *
 * <h3>Selection / hover</h3>
 * Background is adjusted depending on:
 * <ul>
 * <li>Selected row ({@code isSelected})</li>
 * <li>Hover row provided by {@code ListHoverSupport}</li>
 * </ul>
 *
 * <h3>Performance notes</h3>
 * Renderers are reused by Swing. The same instance is configured repeatedly for
 * different rows, so this class must not keep row-specific state outside the UI
 * components that are updated in {@link #getListCellRendererComponent}.
 *
 * @author metku
 */
public class ResourceDownloadedRenderer extends JPanel implements ListCellRenderer<ResourceDownloaded> {

    // ---------------------------------------------------------------------
    // UI components (reused by Swing for every row)
    // ---------------------------------------------------------------------
    /**
     * Leading icon representing the resource (video/audio/file).
     */
    private final JLabel lblIcon = new JLabel();

    /**
     * Small colored badge representing the resource state (LOCAL/CLOUD/BOTH).
     */
    private final JLabel lblBadge = new JLabel();

    /**
     * Title text with wrapping enabled (max 2 lines). JTextArea is used because
     * JLabel does not provide reliable wrapping.
     */
    private final JTextArea txtTitle = new JTextArea();

    /**
     * Subtitle with compact metadata (1 line).
     */
    private final JLabel lblSub = new JLabel();

    /**
     * Horizontal row that contains badge + title.
     */
    private final JPanel titleRow = new JPanel();

    /**
     * Vertical panel that holds titleRow + subtitle.
     */
    private final JPanel textPanel = new JPanel();

    // ---------------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------------
    /**
     * Map used to resolve the state (LOCAL_ONLY / CLOUD_ONLY / BOTH) by
     * filename. Key must be normalized using {@link #normalize(String)}.
     */
    private final Map<String, ResourceState> stateByFileName;

    // ---------------------------------------------------------------------
    // Palette (dark UI)
    // ---------------------------------------------------------------------
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

    /**
     * Builds the renderer and prepares the row layout.
     *
     * @param stateByFileName map filename -> resource state (must not be null)
     */
    public ResourceDownloadedRenderer(Map<String, ResourceState> stateByFileName) {
        this.stateByFileName = stateByFileName;

        setLayout(new BorderLayout(10, 0));
        setOpaque(true);

        // Soft separator + padding for "card" look
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

        // Title (wrap enabled)
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

        // Text panel vertical: titleRow + subtitle
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

    /**
     * Configures and returns the component used to render one list row.
     *
     * <p>
     * Swing reuses the same renderer instance for many rows; this method must
     * fully update all labels and layout constraints based on the provided
     * item.
     * </p>
     */
    @Override
    public Component getListCellRendererComponent(
            JList<? extends ResourceDownloaded> list,
            ResourceDownloaded value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        String name = (value != null && value.getName() != null) ? value.getName() : "";

        // Resolve state by normalized file name (fallback to LOCAL_ONLY)
        String key = normalize(value != null ? value.getName() : null);
        ResourceState state = (key == null)
                ? ResourceState.LOCAL_ONLY
                : stateByFileName.getOrDefault(key, ResourceState.LOCAL_ONLY);

        applyBadge(state);

        // Icon (and cloud overlay for CLOUD_ONLY)
        lblIcon.setIcon(loadThumbOrFallback(value, state));

        // Subtitle with uploader
        String uploader = safe(value != null ? value.getUploaderNick() : null);
        if (uploader.isBlank()) {
            uploader = "…"; // unresolved / not loaded yet
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

        // Compute available width in the viewport to size the wrapped title
        int viewportW = getViewportWidth(list);

        int iconW = 36;
        Icon ic = lblIcon.getIcon();
        if (ic != null) {
            iconW = ic.getIconWidth();
        }

        // extra width: icon + gaps + padding + estimated badge width
        int extra = iconW + 10 + 12 + 12 + 90;
        int textW = Math.max(220, viewportW - extra);

        // Title: wrap + limit to 2 lines
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

        // Ensure a minimum cell height (improves readability on dark theme)
        boolean twoLines = pref.height > (insetsTB + lineH + 1);
        int minH = twoLines ? 86 : 72;

        Dimension cellPref = getPreferredSize();
        setPreferredSize(new Dimension(cellPref.width, Math.max(cellPref.height, minH)));

        // Background / selection / hover
        int hoverIndex = cat.dam.roig.cleanstream.ui.util.ListHoverSupport.getHoverIndex(list);
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

    /**
     * Applies badge label and color based on resource state.
     *
     * @param st resolved resource state
     */
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

    /**
     * Attempts to read the viewport width for proper title wrapping. Falls back
     * to list width, then a default value.
     */
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

    /**
     * Null-safe string helper.
     *
     * @param s input string (may be null)
     * @return empty string if null, otherwise original string
     */
    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /**
     * Formats bytes to a readable string using binary units (1024 base).
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
     * Loads a base icon depending on mime type (video/audio/file). If the
     * resource is {@link ResourceState#CLOUD_ONLY}, overlays a small cloud
     * icon.
     *
     * <p>
     * Note: This does not generate real thumbnails; it selects a representative
     * icon.
     * </p>
     *
     * @param r resource (may be null)
     * @param state resolved state (LOCAL_ONLY / CLOUD_ONLY / BOTH)
     * @return icon to display for this row
     */
    private Icon loadThumbOrFallback(ResourceDownloaded r, ResourceState state) {
        Icon base;
        String mime = (r != null) ? r.getMimeType() : null;

        if (mime != null && mime.startsWith("video/")) {
            base = cat.dam.roig.cleanstream.ui.util.Icons.icon("/icons/video.png", 25);
        } else if (mime != null && mime.startsWith("audio/")) {
            base = cat.dam.roig.cleanstream.ui.util.Icons.icon("/icons/audio.png", 25);
        } else {
            base = cat.dam.roig.cleanstream.ui.util.Icons.icon("/icons/file.png", 25);
        }

        // Optional cloud overlay when the file exists only in cloud
        if (state == ResourceState.CLOUD_ONLY) {
            Icon cloud = cat.dam.roig.cleanstream.ui.util.Icons.icon("/icons/cloud.png", 14);
            int x = base.getIconWidth() - cloud.getIconWidth();
            int y = base.getIconHeight() - cloud.getIconHeight();
            return cat.dam.roig.cleanstream.ui.util.Icons.overlay(base, cloud, x, y);
        }

        return base;
    }

    /**
     * Normalizes file keys to make map lookups stable.
     *
     * <p>
     * Rules:
     * <ul>
     * <li>null -> null</li>
     * <li>trim spaces</li>
     * <li>empty -> null</li>
     * <li>lowercase</li>
     * </ul>
     *
     * @param s original string
     * @return normalized key or null
     */
    private String normalize(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s.toLowerCase();
    }
}
