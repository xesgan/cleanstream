package cat.dam.roig.cleanstream.services.polling;

import cat.dam.roig.roigmediapollingcomponent.Media;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;
import java.io.File;
import java.util.List;

/**
 * Adapter that wraps {@link RoigMediaPollingComponent} into the
 * {@link MediaPolling} abstraction used by CleanStream.
 */
public class RoigMediaPollingAdapter implements MediaPolling {

    private final RoigMediaPollingComponent delegate;

    public RoigMediaPollingAdapter(RoigMediaPollingComponent delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.delegate = delegate;
    }

    @Override
    public void setRunning(boolean running) {
        delegate.setRunning(running);
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public String login(String email, String password) throws Exception {
        return delegate.login(email, password);
    }

    @Override
    public void setToken(String token) {
        delegate.setToken(token);
    }

    @Override
    public String getToken() {
        return delegate.getToken();
    }

    @Override
    public void validateToken() throws Exception {
        delegate.getAllMedia();
    }

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

    @Override
    public List<Media> getAllMedia() throws Exception {
        return delegate.getAllMedia();
    }

    @Override
    public String getNickName(int userId) throws Exception {
        return delegate.getNickName(userId);
    }

    @Override
    public void download(int mediaId, File destFile) throws Exception {
        delegate.download(mediaId, destFile);
    }

    @Override
    public String uploadFileMultipart(File f, String fromUrl) throws Exception {
        return delegate.uploadFileMultipart(f, fromUrl);
    }
}
