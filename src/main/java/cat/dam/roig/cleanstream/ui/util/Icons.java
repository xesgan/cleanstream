package cat.dam.roig.cleanstream.ui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for loading, scaling and composing icons used in the UI.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Load icons from classpath resources</li>
 * <li>Scale them to a fixed square size</li>
 * <li>Cache scaled icons to avoid reloading and rescaling</li>
 * <li>Overlay one icon on top of another (e.g. cloud badge)</li>
 * </ul>
 *
 * <p>
 * This class is intentionally {@code final} with a private constructor because
 * it only exposes static utility methods.
 * </p>
 *
 * <h3>Caching strategy</h3>
 * Icons are cached using a {@link ConcurrentHashMap} with a composite key:
 * <pre>
 *     path + "@" + size
 * </pre> This ensures that:
 * <ul>
 * <li>The same icon path with the same size is only loaded and scaled once</li>
 * <li>Different sizes of the same resource are cached independently</li>
 * </ul>
 *
 * <p>
 * Thread-safe by design thanks to {@link ConcurrentHashMap}.
 * </p>
 *
 * @author metku
 */
public final class Icons {

    /**
     * Private constructor to prevent instantiation.
     */
    private Icons() {
    }

    /**
     * In-memory cache for scaled icons. Key format: "path@size"
     */
    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    /**
     * Loads an icon from the classpath and scales it to the specified size.
     *
     * <p>
     * If the icon has already been requested before with the same path and
     * size, it will be returned from cache.
     * </p>
     *
     * @param path resource path (e.g. "/icons/video.png")
     * @param size desired width and height in pixels (square)
     * @return scaled {@link Icon}, or a default file icon if resource not found
     */
    public static Icon icon(String path, int size) {
        String key = path + "@" + size;
        return CACHE.computeIfAbsent(key, k -> loadAndScale(path, size));
    }

    /**
     * Loads the icon resource and scales it to the requested size.
     *
     * <p>
     * If the resource cannot be found in the classpath, a default
     * {@code FileView.fileIcon} from {@link UIManager} is returned instead.
     * </p>
     *
     * @param path resource path inside the classpath
     * @param size target size (square)
     * @return scaled icon (never null)
     */
    private static Icon loadAndScale(String path, int size) {
        URL url = Icons.class.getResource(path);

        if (url == null) {
            System.out.println("[Icons] NOT FOUND: " + path);
            return UIManager.getIcon("FileView.fileIcon");
        }

        System.out.println("[Icons] OK: " + path + " -> " + url);

        ImageIcon src = new ImageIcon(url);
        Image img = src.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);

        return new ImageIcon(img);
    }

    /**
     * Creates a new icon by drawing one icon on top of another.
     *
     * <p>
     * Typical usage:
     * <ul>
     * <li>Base icon: video/audio/file</li>
     * <li>Overlay icon: cloud badge</li>
     * </ul>
     * </p>
     *
     * @param base base icon (drawn at 0,0)
     * @param over overlay icon
     * @param x x position of overlay relative to base
     * @param y y position of overlay relative to base
     * @return composed icon as a new {@link ImageIcon}
     */
    public static Icon overlay(Icon base, Icon over, int x, int y) {
        int w = Math.max(base.getIconWidth(), x + over.getIconWidth());
        int h = Math.max(base.getIconHeight(), y + over.getIconHeight());

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();

        // Improve scaling/render quality
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        base.paintIcon(null, g, 0, 0);
        over.paintIcon(null, g, x, y);

        g.dispose();

        return new ImageIcon(bi);
    }
}
