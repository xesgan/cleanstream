package cat.dam.roig.cleanstream.services.polling;

/**
 * Event triggered when new media is detected in the cloud.
 */
public class MediaUpdateEvent {

    private final int newMediaCount;

    public MediaUpdateEvent(int newMediaCount) {
        this.newMediaCount = newMediaCount;
    }

    /**
     * @return number of new media items detected.
     */
    public int getNewMediaCount() {
        return newMediaCount;
    }
}