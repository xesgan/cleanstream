package cat.dam.roig.cleanstream.ui.util;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class ListHoverSupport {

    private ListHoverSupport() {}

    public static final String HOVER_INDEX_KEY = "hoverIndex";

    public static void install(JList<?> list) {
        list.putClientProperty(HOVER_INDEX_KEY, -1);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int old = getHoverIndex(list);
                int idx = list.locationToIndex(e.getPoint());
                // Si el mouse está fuera del bounds real del item, no lo cuentes como hover
                if (idx >= 0) {
                    var b = list.getCellBounds(idx, idx);
                    if (b == null || !b.contains(e.getPoint())) idx = -1;
                }

                if (old != idx) {
                    setHoverIndex(list, idx);
                    if (old >= 0) list.repaint(list.getCellBounds(old, old));
                    if (idx >= 0) list.repaint(list.getCellBounds(idx, idx));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                int old = getHoverIndex(list);
                if (old != -1) {
                    setHoverIndex(list, -1);
                    list.repaint(list.getCellBounds(old, old));
                }
            }
        };

        list.addMouseMotionListener(ma);
        list.addMouseListener(ma);
    }

    public static int getHoverIndex(JList<?> list) {
        Object v = list.getClientProperty(HOVER_INDEX_KEY);
        return (v instanceof Integer) ? (Integer) v : -1;
    }

    private static void setHoverIndex(JList<?> list, int idx) {
        list.putClientProperty(HOVER_INDEX_KEY, idx);
    }
}
