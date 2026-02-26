package cat.dam.roig.cleanstream.ui;

import java.awt.Color;

/**
 * Centralized color palette for CleanStream UI.
 *
 * <p>
 * This class defines the visual theme of the application by grouping commonly
 * used colors in one place. It ensures:
 * <ul>
 * <li>Consistent styling across all UI components</li>
 * <li>Easier theme adjustments in the future</li>
 * <li>Avoidance of hard-coded color values scattered in the codebase</li>
 * </ul>
 *
 * <p>
 * Design approach:
 * <ul>
 * <li>Dark theme base</li>
 * <li>Card-style surfaces</li>
 * <li>Primary accent color for actions (buttons, highlights)</li>
 * <li>Muted color for secondary text</li>
 * </ul>
 *
 * <p>
 * This class is a pure utility holder:
 * <ul>
 * <li>All fields are {@code public static final}</li>
 * <li>Constructor is private to prevent instantiation</li>
 * </ul>
 */
public final class AppTheme {

    /**
     * Private constructor to prevent instantiation.
     */
    private AppTheme() {
    }

    /**
     * Main application background color.
     */
    public static final Color BACKGROUND = new Color(45, 45, 45);

    /**
     * Background color used for card-like panels.
     */
    public static final Color CARD = new Color(56, 56, 56);

    /**
     * Primary text color.
     */
    public static final Color TEXT = new Color(230, 230, 230);

    /**
     * Secondary/muted text color (labels, hints, less important info).
     */
    public static final Color MUTED = new Color(170, 170, 170);

    /**
     * Primary accent color used for buttons and active UI elements.
     */
    public static final Color PRIMARY = new Color(46, 134, 193);

    /**
     * Disabled version of the primary accent color. Used for inactive buttons
     * or disabled states.
     */
    public static final Color PRIMARY_DISABLED = new Color(85, 105, 120);
}
