package cat.dam.roig.cleanstream.services.cloud;

import javax.swing.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Resolves uploader nicknames from the backend in an asynchronous and
 * thread-safe way.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Fetch uploader nicknames from a remote API</li>
 * <li>Cache resolved nicknames to avoid duplicate API calls</li>
 * <li>Prevent concurrent duplicate requests for the same user</li>
 * <li>Execute UI updates safely on the Swing EDT</li>
 * </ul>
 *
 * <p>
 * This class is designed to be used from UI components that display cloud media
 * where uploader nicknames may not yet be available.
 *
 * <p>
 * Concurrency strategy:
 * <ul>
 * <li>ConcurrentHashMap for thread-safe cache</li>
 * <li>Concurrent Set (inFlight) to track ongoing requests</li>
 * <li>ExecutorService with a fixed thread pool for background execution</li>
 * </ul>
 *
 * <p>
 * UI Safety: The provided {@code onReady} callback is always executed on the
 * Swing Event Dispatch Thread (EDT).
 *
 * Example usage:
 * <pre>
 * resolver.fetchNickAsync(userId, () -> {
 *     myList.repaint();
 * });
 * </pre>
 */
public class UploaderResolver {

    /**
     * Functional interface representing the external API call to retrieve a
     * nickname from a user ID.
     */
    public interface NickApi {

        /**
         * Calls the backend to retrieve the nickname of a user.
         *
         * @param userId uploader ID
         * @return nickname string
         * @throws Exception if request fails
         */
        String getNickName(int userId) throws Exception;
    }

    /**
     * Backend API implementation used to fetch nicknames.
     */
    private final NickApi api;

    /**
     * Thread-safe cache storing userId -> nickname.
     */
    private final Map<Integer, String> cache = new ConcurrentHashMap<>();

    /**
     * Tracks userIds currently being requested to avoid duplicate calls.
     */
    private final Set<Integer> inFlight = ConcurrentHashMap.newKeySet();

    /**
     * Background thread pool used for nickname resolution.
     */
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    /**
     * Creates a new UploaderResolver.
     *
     * @param api implementation used to retrieve nicknames from backend
     */
    public UploaderResolver(NickApi api) {
        this.api = api;
    }

    /**
     * Returns the cached nickname if already resolved.
     *
     * @param userId uploader ID
     * @return cached nickname or null if not yet resolved
     */
    public String getCachedNick(Integer userId) {
        if (userId == null) {
            return null;
        }
        return cache.get(userId);
    }

    /**
     * Asynchronously fetches a nickname if not already cached.
     *
     * <p>
     * Behavior:
     * <ul>
     * <li>If userId is null → does nothing</li>
     * <li>If nickname is already cached → does nothing</li>
     * <li>If request is already in progress → does nothing</li>
     * <li>Otherwise → performs API call in background</li>
     * </ul>
     *
     * <p>
     * Once completed:
     * <ul>
     * <li>Nickname is stored in cache</li>
     * <li>Fallback value "—" is stored on error</li>
     * <li>{@code onReady} callback is executed on EDT</li>
     * </ul>
     *
     * @param userId uploader ID
     * @param onReady callback executed on Swing EDT when data is ready
     */
    public void fetchNickAsync(Integer userId, Runnable onReady) {

        if (userId == null) {
            return;
        }
        if (cache.containsKey(userId)) {
            return;
        }
        if (!inFlight.add(userId)) {
            return; // Already being requested
        }
        pool.submit(() -> {
            try {
                String nick = api.getNickName(userId);

                if (nick != null && !nick.isBlank()) {
                    cache.put(userId, nick);
                } else {
                    cache.put(userId, "—");
                }

            } catch (Exception ex) {
                // On error, store fallback value to avoid repeated failing calls
                cache.put(userId, "—");
            } finally {
                inFlight.remove(userId);

                // Ensure UI update runs on Swing thread
                SwingUtilities.invokeLater(onReady);
            }
        });
    }

    /**
     * Immediately stops all background tasks and shuts down the thread pool.
     *
     * <p>
     * Should be called when the application is closing to avoid thread leaks.
     */
    public void shutdown() {
        pool.shutdownNow();
    }
}
