package cat.dam.roig.cleanstream.services.polling;

/**
 * Event object emitted by {@link MediaPolling} implementations when new media
 * is detected in the cloud.
 *
 * <p>
 * This event is part of the observer pattern used by CleanStream to react to
 * backend updates.
 * </p>
 *
 * <p>
 * Typical flow:
 * <pre>
 * MediaPolling (background thread)
 *      └── detects new cloud media
 *              └── creates MediaUpdateEvent
 *                      └── notifies registered MediaUpdateListener
 *                              └── MainController / DownloadsController refresh UI
 * </pre>
 *
 * <p>
 * The event currently contains only the number of newly detected items, but it
 * can be extended in the future to include:
 * <ul>
 * <li>List of new media</li>
 * <li>Timestamps</li>
 * <li>Type of update</li>
 * </ul>
 *
 * This class is immutable.
 */
public class MediaUpdateEvent {

    /**
     * Number of new media items detected since last poll.
     */
    private final int newMediaCount;

    /**
     * Creates a new MediaUpdateEvent.
     *
     * @param newMediaCount number of newly detected cloud media items
     */
    public MediaUpdateEvent(int newMediaCount) {
        this.newMediaCount = newMediaCount;
    }

    /**
     * Returns how many new media items were detected.
     *
     * @return number of new cloud media items
     */
    public int getNewMediaCount() {
        return newMediaCount;
    }
}
