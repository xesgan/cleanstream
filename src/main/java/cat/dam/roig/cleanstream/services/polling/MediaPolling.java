package cat.dam.roig.cleanstream.services.polling;

import cat.dam.roig.roigmediapollingcomponent.Media;
import java.io.File;
import java.util.List;

/**
 * Abstraction layer over the external Media Polling component.
 *
 * <p>
 * This interface defines the minimal contract required by CleanStream
 * to interact with the cloud backend (DI Media Network).
 * </p>
 *
 * <p>
 * Purpose:
 * <ul>
 *     <li>Decouple CleanStream from the concrete implementation of the polling component</li>
 *     <li>Allow easier testing and mocking</li>
 *     <li>Restrict access to only the operations needed by the application</li>
 * </ul>
 *
 * <p>
 * The implementation is responsible for:
 * <ul>
 *     <li>Authentication (JWT handling)</li>
 *     <li>Cloud media retrieval</li>
 *     <li>Upload / Download operations</li>
 *     <li>Background polling lifecycle</li>
 * </ul>
 *
 * <p>
 * Important:
 * This interface does not define how polling is implemented,
 * only how CleanStream interacts with it.
 * </p>
 */
public interface MediaPolling {

    // ---------------------------------------------------------------------
    // Runtime lifecycle
    // ---------------------------------------------------------------------

    /**
     * Starts or stops the background polling mechanism.
     *
     * @param running true to start polling, false to stop it
     */
    void setRunning(boolean running);

    /**
     * Indicates whether polling is currently active.
     *
     * @return true if polling is running
     */
    boolean isRunning();

    // ---------------------------------------------------------------------
    // Authentication / Token management
    // ---------------------------------------------------------------------

    /**
     * Performs login against the backend and retrieves a JWT token.
     *
     * @param email user email
     * @param password user password
     * @return JWT token if authentication succeeds
     * @throws Exception if authentication fails
     */
    String login(String email, String password) throws Exception;

    /**
     * Sets the current authentication token.
     *
     * @param token JWT token
     */
    void setToken(String token);

    /**
     * Returns the current authentication token.
     *
     * @return JWT token or null
     */
    String getToken();

    /**
     * Retrieves the nickname of a user by ID.
     *
     * @param userId backend user ID
     * @return nickname string
     * @throws Exception if the request fails
     */
    String getNickName(int userId) throws Exception;

    // ---------------------------------------------------------------------
    // Media operations
    // ---------------------------------------------------------------------

    /**
     * Retrieves all media available for the authenticated user.
     *
     * @return list of Media objects from backend
     * @throws Exception if request fails
     */
    List<Media> getAllMedia() throws Exception;

    /**
     * Downloads a cloud media file into a local destination.
     *
     * @param mediaId identifier of the media in backend
     * @param destFile local file destination
     * @throws Exception if download fails
     */
    void download(int mediaId, File destFile) throws Exception;

    /**
     * Uploads a local file to the cloud using multipart request.
     *
     * @param f local file to upload
     * @param fromUrl original source URL (metadata)
     * @return backend response (e.g., success message or ID)
     * @throws Exception if upload fails
     */
    String uploadFileMultipart(File f, String fromUrl) throws Exception;

    /**
     * Makes a lightweight authenticated call to confirm that
     * the current token is still valid.
     *
     * <p>
     * Implementations may internally use:
     * <ul>
     *     <li>{@link #getAllMedia()}</li>
     *     <li>or another small endpoint</li>
     * </ul>
     *
     * @throws Exception if token is invalid or request fails
     */
    void validateToken() throws Exception;

    // ---------------------------------------------------------------------
    // Event system
    // ---------------------------------------------------------------------

    /**
     * Registers a listener that will be notified
     * when new cloud media updates are detected.
     *
     * @param listener listener implementation
     */
    void addMediaListener(MediaUpdateListener listener);
}