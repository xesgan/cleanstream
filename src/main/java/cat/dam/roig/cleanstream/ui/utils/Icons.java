package cat.dam.roig.cleanstream.ui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Icons {

    private Icons() {
    }

    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    public static Icon icon(String path, int size) {
        String key = path + "@" + size;
        return CACHE.computeIfAbsent(key, k -> loadAndScale(path, size));
    }

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

    public static Icon overlay(Icon base, Icon over, int x, int y) {
        int w = Math.max(base.getIconWidth(), x + over.getIconWidth());
        int h = Math.max(base.getIconHeight(), y + over.getIconHeight());

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        base.paintIcon(null, g, 0, 0);
        over.paintIcon(null, g, x, y);

        g.dispose();
        return new ImageIcon(bi);
    }
}
