package cat.dam.roig.cleanstream.services.cloud;

import javax.swing.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class UploaderResolver {

    public interface NickApi {
        String getNickName(int userId) throws Exception; // tu llamada real a API
    }

    private final NickApi api;

    private final Map<Integer, String> cache = new ConcurrentHashMap<>();
    private final Set<Integer> inFlight = ConcurrentHashMap.newKeySet();

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    public UploaderResolver(NickApi api) {
        this.api = api;
    }

    public String getCachedNick(Integer userId) {
        if (userId == null) return null;
        return cache.get(userId);
    }

    /**
     * Si no está en caché, lo pide en background.
     * onReady se ejecuta en EDT para que puedas refrescar UI.
     */
    public void fetchNickAsync(Integer userId, Runnable onReady) {
        if (userId == null) return;
        if (cache.containsKey(userId)) return;
        if (!inFlight.add(userId)) return; // ya se está pidiendo

        pool.submit(() -> {
            try {
                String nick = api.getNickName(userId);
                if (nick != null && !nick.isBlank()) {
                    cache.put(userId, nick);
                } else {
                    cache.put(userId, "—");
                }
            } catch (Exception ex) {
                cache.put(userId, "—");
            } finally {
                inFlight.remove(userId);
                SwingUtilities.invokeLater(onReady);
            }
        });
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
