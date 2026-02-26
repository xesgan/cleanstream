package cat.dam.roig.cleanstream.services.polling;

import cat.dam.roig.roigmediapollingcomponent.Media;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;
import java.io.File;
import java.util.List;

/**
 * Adapter implementation of {@link MediaPolling} that wraps
 * {@link RoigMediaPollingComponent}.
 *
 * <p>
 * This class applies the Adapter Pattern in order to:
 * <ul>
 * <li>Decouple CleanStream from the concrete external component</li>
 * <li>Expose only the methods required by the application</li>
 * <li>Translate external events into internal application events</li>
 * </ul>
 *
 * <p>
 * CleanStream depends only on the {@link MediaPolling} abstraction, not
 * directly on {@link RoigMediaPollingComponent}. This allows:
 * <ul>
 * <li>Easier testing (mocking MediaPolling)</li>
 * <li>Future replacement of the polling component</li>
 * <li>Cleaner architecture</li>
 * </ul>
 *
 * <p>
 * Event translation: The external component emits its own media update events.
 * This adapter converts them into {@link MediaUpdateEvent} before notifying
 * {@link MediaUpdateListener}.
 */
public class RoigMediaPollingAdapter implements MediaPolling {

    /**
     * External component instance used internally.
     */
    private final RoigMediaPollingComponent delegate;

    /**
     * Creates a new adapter wrapping the given polling component.
     *
     * @param delegate concrete polling component
     * @throws IllegalArgumentException if delegate is null
     */
    public RoigMediaPollingAdapter(RoigMediaPollingComponent delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.delegate = delegate;
    }

    // ---------------------------------------------------------------------
    // Runtime lifecycle
    // ---------------------------------------------------------------------
    /**
     * Starts or stops background polling.
     *
     * @param running true to start polling, false to stop
     */
    @Override
    public void setRunning(boolean running) {
        delegate.setRunning(running);
    }

    /**
     * Indicates whether polling is currently active.
     *
     * @return true if polling is running
     */
    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    // ---------------------------------------------------------------------
    // Authentication
    // ---------------------------------------------------------------------
    /**
     * Delegates login to the external component.
     *
     * @param email user email
     * @param password user password
     * @return JWT token
     * @throws Exception if authentication fails
     */
    @Override
    public String login(String email, String password) throws Exception {
        return delegate.login(email, password);
    }

    /**
     * Sets the JWT token in the external component.
     *
     * @param token JWT token
     */
    @Override
    public void setToken(String token) {
        delegate.setToken(token);
    }

    /**
     * Retrieves the current JWT token.
     *
     * @return token string
     */
    @Override
    public String getToken() {
        return delegate.getToken();
    }

    /**
     * Validates the current token by performing a lightweight backend call.
     *
     * <p>
     * Currently implemented by calling {@link #getAllMedia()}. If the call
     * fails, the token is considered invalid.
     *
     * @throws Exception if validation fails
     */
    @Override
    public void validateToken() throws Exception {
        delegate.getAllMedia();
    }

    // ---------------------------------------------------------------------
    // Event system
    // ---------------------------------------------------------------------
    /**
     * Registers a listener for media update events.
     *
     * <p>
     * This method converts the external event into an internal
     * {@link MediaUpdateEvent}, exposing only the number of new items.
     *
     * @param listener listener to notify
     */
    @Override
    public void addMediaListener(MediaUpdateListener listener) {
        if (listener == null) {
            return;
        }

        delegate.addMediaListener(evt -> {
            int count = evt.getNewMedia().size();
            MediaUpdateEvent event = new MediaUpdateEvent(count);
            listener.onMediaUpdate(event);
        });
    }

    // ---------------------------------------------------------------------
    // Media operations
    // ---------------------------------------------------------------------
    /**
     * Retrieves all media from backend.
     *
     * @return list of Media objects
     * @throws Exception if request fails
     */
    @Override
    public List<Media> getAllMedia() throws Exception {
        return delegate.getAllMedia();
    }

    /**
     * Retrieves uploader nickname.
     *
     * @param userId backend user ID
     * @return nickname string
     * @throws Exception if request fails
     */
    @Override
    public String getNickName(int userId) throws Exception {
        return delegate.getNickName(userId);
    }

    /**
     * Downloads a media file from backend.
     *
     * @param mediaId media identifier
     * @param destFile local destination file
     * @throws Exception if download fails
     */
    @Override
    public void download(int mediaId, File destFile) throws Exception {
        delegate.download(mediaId, destFile);
    }

    /**
     * Uploads a file to the backend using multipart request.
     *
     * @param f local file
     * @param fromUrl original source URL metadata
     * @return backend response
     * @throws Exception if upload fails
     */
    @Override
    public String uploadFileMultipart(File f, String fromUrl) throws Exception {
        return delegate.uploadFileMultipart(f, fromUrl);
    }
}
