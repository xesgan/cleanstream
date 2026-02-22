package cat.dam.roig.cleanstream.services.polling;

/**
 * Minimal abstraction for the cloud media component used by CleanStream.
 *
 * <p>
 * Exposes only the operations required by the application (auth + token
 * handling + a lightweight call to validate the token).
 * </p>
 */
public interface MediaPolling {

    // --- Runtime ---
    void setRunning(boolean running);

    boolean isRunning();

    // --- Auth / token ---
    String login(String email, String password) throws Exception;

    void setToken(String token);

    String getToken();

    /**
     * Makes a lightweight authenticated call to confirm the current token is
     * valid. Implementations may use getAllMedia() or another small endpoint.
     */
    void validateToken() throws Exception;

    // --- Updates ---
    void addMediaListener(MediaUpdateListener listener);
}
