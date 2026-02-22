package cat.dam.roig.cleanstream.services.polling;

/**
 * Listener for media update events.
 */
@FunctionalInterface
public interface MediaUpdateListener {

    void onMediaUpdate(MediaUpdateEvent event);
}