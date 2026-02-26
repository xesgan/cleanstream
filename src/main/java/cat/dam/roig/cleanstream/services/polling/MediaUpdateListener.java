package cat.dam.roig.cleanstream.services.polling;

/**
 * Listener interface used to receive media update notifications from
 * {@link MediaPolling} implementations.
 *
 * <p>
 * This interface is part of the Observer pattern used in CleanStream. When the
 * polling component detects new media in the cloud, it notifies all registered
 * listeners through this interface.
 * </p>
 *
 * <p>
 * Typical usage:
 * <pre>
 * mediaPolling.addMediaListener(event -> {
 *     System.out.println("New media detected: " + event.getNewMediaCount());
 *     refreshUi();
 * });
 * </pre>
 *
 * <p>
 * Threading note: Implementations should be aware that this callback may be
 * invoked from a background thread depending on the polling implementation. If
 * UI updates are required, they must be executed on the Swing Event Dispatch
 * Thread (EDT) using {@code SwingUtilities.invokeLater()}.
 * </p>
 *
 * @author metku
 */
@FunctionalInterface
public interface MediaUpdateListener {

    /**
     * Called when new media is detected in the cloud.
     *
     * @param event event containing update information
     */
    void onMediaUpdate(MediaUpdateEvent event);
}
