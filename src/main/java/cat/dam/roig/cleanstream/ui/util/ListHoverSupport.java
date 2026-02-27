package cat.dam.roig.cleanstream.ui.util;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Utility class that adds hover (mouse-over) support to a {@link JList}.
 *
 * <p>
 * Swing's {@link JList} does not provide built-in hover detection per cell.
 * This helper tracks the index currently under the mouse cursor and stores it
 * as a client property on the list.
 * </p>
 *
 * <h3>How it works</h3>
 * <ul>
 * <li>Installs a {@link MouseAdapter} on the list</li>
 * <li>On {@code mouseMoved}, calculates the hovered index</li>
 * <li>Saves the index in a client property ({@link #HOVER_INDEX_KEY})</li>
 * <li>Repaints only the affected cells (old and new hover index)</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *     ListHoverSupport.install(myJList);
 * </pre>
 *
 * Then inside your {@link ListCellRenderer}, you can read:
 * <pre>
 *     int hoverIndex = ListHoverSupport.getHoverIndex(list);
 * </pre>
 *
 * and adjust background styling accordingly.
 *
 * <p>
 * The hover index is stored using
 * {@link JComponent#putClientProperty(Object, Object)} to avoid subclassing
 * {@link JList}.
 * </p>
 *
 * @author metku
 */
public final class ListHoverSupport {

    /**
     * Private constructor to prevent instantiation.
     */
    private ListHoverSupport() {
    }

    /**
     * Client property key used to store the current hover index.
     */
    public static final String HOVER_INDEX_KEY = "hoverIndex";

    /**
     * Installs hover tracking behavior on the given {@link JList}.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Initializes hover index to -1</li>
     * <li>Adds mouse motion and mouse exit listeners</li>
     * </ul>
     *
     * @param list target list (must not be null)
     */
    public static void install(JList<?> list) {
        list.putClientProperty(HOVER_INDEX_KEY, -1);

        MouseAdapter ma = new MouseAdapter() {

            /**
             * Called when mouse moves over the list. Updates hover index and
             * repaints affected cells only.
             */
            @Override
            public void mouseMoved(MouseEvent e) {
                int old = getHoverIndex(list);
                int idx = list.locationToIndex(e.getPoint());

                // Validate that mouse is actually inside the cell bounds
                if (idx >= 0) {
                    var b = list.getCellBounds(idx, idx);
                    if (b == null || !b.contains(e.getPoint())) {
                        idx = -1;
                    }
                }

                if (old != idx) {
                    setHoverIndex(list, idx);

                    // Repaint only affected rows (optimization)
                    if (old >= 0) {
                        list.repaint(list.getCellBounds(old, old));
                    }
                    if (idx >= 0) {
                        list.repaint(list.getCellBounds(idx, idx));
                    }
                }
            }

            /**
             * Called when mouse exits the list area. Clears hover state and
             * repaints the previously hovered cell.
             */
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

    /**
     * Returns the current hover index stored in the list.
     *
     * @param list target list
     * @return hovered index, or -1 if none
     */
    public static int getHoverIndex(JList<?> list) {
        Object v = list.getClientProperty(HOVER_INDEX_KEY);
        return (v instanceof Integer) ? (Integer) v : -1;
    }

    /**
     * Updates the hover index stored as a client property.
     *
     * @param list target list
     * @param idx new hover index (-1 if none)
     */
    private static void setHoverIndex(JList<?> list, int idx) {
        list.putClientProperty(HOVER_INDEX_KEY, idx);
    }
}
