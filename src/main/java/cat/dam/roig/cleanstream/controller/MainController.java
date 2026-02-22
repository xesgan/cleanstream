package cat.dam.roig.cleanstream.controller;

import java.nio.file.Path;

import cat.dam.roig.cleanstream.services.auth.AuthManager;
import cat.dam.roig.cleanstream.ui.main.MainFrame;
import cat.dam.roig.cleanstream.services.polling.MediaPolling;

/**
 *
 * @author metku
 */
public class MainController {

    private final MainFrame mainFrame;
    private final AuthManager authManager;
    private final MediaPolling mediaPolling;
    private boolean mediaListenerRegistered = false;

    public MainController(MainFrame mainFrame, AuthManager auth, MediaPolling mediaPolling) {
        this.mainFrame = mainFrame;
        this.authManager = auth;
        this.mediaPolling = mediaPolling;
    }

    public void start() {
        // Login manual
        authManager.setOnLoginSuccess(() -> {
            startSession();
        });

        // Auto-login
        if (authManager.tryAutoLogin()) {
            startSession();
        } else {
            mainFrame.updateSessionUI(false);
            mainFrame.showLogin();
        }
    }

    private void startSession() {

        mainFrame.updateSessionUI(true);
        mainFrame.showMainView();

        // Obtener ruta actual de preferencias
        Path dir = mainFrame.getScanDownloadsFolderPathFromUI();

        mainFrame.getDownloadsController().appStart(dir, mainFrame);

        initMediaPollingListener();
        mediaPolling.setRunning(true);
    }

    public void doLogout() {
        if (!mainFrame.confirmLogout()) {
            return; // usuario canceló
        }
        if (authManager.isRememberEnabled()) {
            authManager.logoutButKeepEmail();
        } else {
            authManager.clearRememberMe();
        }
        mediaPolling.setRunning(false);
        authManager.logout();
        mainFrame.updateSessionUI(false);
        mainFrame.showLogin();

    }

    private void initMediaPollingListener() {
        if (mediaListenerRegistered) {
            return;
        }

        mediaPolling.addMediaListener(event -> {
            System.out.println("[APP] New cloud media found: " + event.getNewMediaCount());
            mainFrame.getDownloadsController().loadCloudMedia(mainFrame);
        });

        mediaListenerRegistered = true;
    }
}
