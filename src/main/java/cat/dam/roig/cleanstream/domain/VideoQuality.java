package cat.dam.roig.cleanstream.domain;

/**
 *
 * @author metku
 */
public enum VideoQuality {
    BEST_AVAILABLE("Best Available"),
    P1080("1080p"),
    P720("720p"),
    P480("480p");

    private final String label;

    VideoQuality(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
