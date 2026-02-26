package cat.dam.roig.cleanstream.domain;

/**
 * Represents the video quality options available for download.
 *
 * <p>
 * This enum is used to:
 * <ul>
 * <li>Populate UI components such as JComboBox</li>
 * <li>Determine which quality option to pass to yt-dlp</li>
 * <li>Provide a user-friendly label for display</li>
 * </ul>
 *
 * <p>
 * Each constant contains:
 * <ul>
 * <li>An internal identifier (enum constant)</li>
 * <li>A human-readable label for the UI</li>
 * </ul>
 *
 * <p>
 * Example usage in UI:
 * <pre>
 * cmbQuality.setModel(new DefaultComboBoxModel<>(VideoQuality.values()));
 * </pre>
 *
 * <p>
 * Example usage when building yt-dlp command:
 * <pre>
 * switch(selectedQuality) {
 *     case P1080 -> addFormatOption("bestvideo[height<=1080]");
 * }
 * </pre>
 *
 * Note: The enum itself does not contain yt-dlp logic. It only represents
 * selectable quality levels.
 *
 * @author metku
 */
public enum VideoQuality {

    /**
     * Automatically selects the best available quality.
     */
    BEST_AVAILABLE("Best Available"),
    /**
     * Full HD (1920x1080).
     */
    P1080("1080p"),
    /**
     * HD Ready (1280x720).
     */
    P720("720p"),
    /**
     * Standard Definition (640x480).
     */
    P480("480p");

    /**
     * Human-readable label shown in the UI.
     */
    private final String label;

    /**
     * Creates a VideoQuality option with a display label.
     *
     * @param label Text displayed to the user
     */
    VideoQuality(String label) {
        this.label = label;
    }

    /**
     * Returns the display label used in UI components.
     *
     * @return User-friendly label
     */
    public String getLabel() {
        return label;
    }
}
