package cat.dam.roig.cleanstream.ui;

import java.awt.Color;

/**
 * Common UI colors used across CleanStream components.
 *
 * <p>
 * This class provides a small shared palette for semantic colors,
 * intended to be reused in dialogs, labels, status messages and buttons.
 * </p>
 *
 * <p>
 * Difference vs {@link AppTheme}:
 * <ul>
 *   <li>{@link AppTheme} defines the global application theme (background, card, text, primary...)</li>
 *   <li>{@code UiColors} defines semantic feedback colors (success, error, warning, neutral)</li>
 * </ul>
 *
 * <p>
 * Usage examples:
 * <ul>
 *   <li>{@code label.setForeground(UiColors.ERROR);} for error messages</li>
 *   <li>{@code label.setForeground(UiColors.SUCCESS);} for success confirmations</li>
 * </ul>
 *
 * <p>
 * Note:
 * This class currently has a public constructor (implicit). If you want it to behave
 * like a pure utility holder (recommended), make it {@code final} and add a private
 * constructor. (Not changing it here to keep your file minimal.)
 *
 * @author metku
 */
public class UiColors {

    /**
     * Primary accent color (blue). Should match the primary action color of the theme.
     */
    public static final Color PRIMARY = new Color(46, 134, 193);

    /**
     * Error color (red). Used for validation errors and destructive actions.
     */
    public static final Color ERROR = new Color(192, 57, 43);

    /**
     * Warning/caution color (orange). Used for non-fatal warnings or attention states.
     */
    public static final Color CAREFULL = new Color(243, 156, 18);

    /**
     * Success color (green). Used for successful operations and confirmations.
     */
    public static final Color SUCCESS = new Color(39, 174, 96);

    /**
     * Neutral color (light gray). Used for secondary UI states or neutral messages.
     */
    public static final Color NEUTRAL = new Color(224, 224, 224);
}