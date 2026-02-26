package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.services.auth.AuthManager;
import cat.dam.roig.cleanstream.services.polling.MediaPolling;
import cat.dam.roig.cleanstream.ui.main.MainFrame;
import java.nio.file.Path;

/**
 * MainController is the central orchestrator of the CleanStream application.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Coordinate application startup</li>
 * <li>Handle login and auto-login flow</li>
 * <li>Start and stop the user session</li>
 * <li>Initialize and control MediaPolling (cloud synchronization)</li>
 * <li>Coordinate UI transitions between Login and Main view</li>
 * </ul>
 *
 * <p>
 * This class does NOT implement business logic itself. It only coordinates
 * different services and the UI layer.
 *
 * <p>
 * Session lifecycle:
 * <pre>
 * start()
 *   ├── tryAutoLogin()
 *   │       ├── success → startSession()
 *   │       └── failure → showLogin()
 *   └── manual login → startSession()
 *
 * startSession()
 *   ├── Initialize UI
 *   ├── Initialize downloads
 *   └── Start polling service
 * </pre>
 *
 * @author metku
 */
public class MainController {

    private final MainFrame mainFrame;
    private final AuthManager authManager;
    private final MediaPolling mediaPolling;

    /**
     * Prevents registering multiple listeners in MediaPolling.
     */
    private boolean mediaListenerRegistered = false;

    /**
     * Creates the main controller of the application.
     *
     * @param mainFrame Main application window (UI layer)
     * @param authManager Handles authentication and remember-me logic
     * @param mediaPolling Handles communication with the cloud backend
     */
    public MainController(MainFrame mainFrame,
            AuthManager authManager,
            MediaPolling mediaPolling) {
        this.mainFrame = mainFrame;
        this.authManager = authManager;
        this.mediaPolling = mediaPolling;
    }

    /**
     * Starts the application.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Registers the login success callback</li>
     * <li>Attempts auto-login if remember-me is enabled</li>
     * <li>Shows the login screen if auto-login fails</li>
     * </ul>
     */
    public void start() {

        // Register callback for manual login
        authManager.setOnLoginSuccess(new Runnable() {
            @Override
            public void run() {
                startSession();
            }
        });

        // Attempt auto-login
        if (authManager.tryAutoLogin()) {
            startSession();
        } else {
            showLoginScreen();
        }
    }

    /**
     * Starts a full user session after successful authentication.
     *
     * <p>
     * This method initializes:
     * <ul>
     * <li>UI session state</li>
     * <li>Local downloads management</li>
     * <li>Cloud polling service</li>
     * </ul>
     */
    private void startSession() {
        startUiSession();
        startDownloads();
        startPolling();
    }

    /**
     * Updates the UI to reflect a logged-in state.
     *
     * <p>
     * Enables session-related menu items and shows the main view.
     */
    private void startUiSession() {
        mainFrame.updateSessionUI(true);
        mainFrame.showMainView();
    }

    /**
     * Initializes the downloads subsystem.
     *
     * <p>
     * Retrieves the configured downloads folder from the UI and starts the
     * DownloadsController.
     */
    private void startDownloads() {
        Path dir = mainFrame.getScanDownloadsFolderPathFromUI();
        mainFrame.getDownloadsController().appStart(dir, mainFrame);
    }

    /**
     * Starts the MediaPolling service.
     *
     * <p>
     * Registers the polling listener (if not already registered) and enables
     * periodic server checks.
     */
    private void startPolling() {
        initMediaPollingListener();
        mediaPolling.setRunning(true);
    }

    /**
     * Performs logout flow.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Asks for confirmation</li>
     * <li>Clears authentication state</li>
     * <li>Stops cloud polling</li>
     * <li>Returns to login screen</li>
     * </ul>
     */
    public void doLogout() {
        if (!mainFrame.confirmLogout()) {
            return;
        }

        clearAuthState();
        stopPolling();
        showLoginScreen();
    }

    /**
     * Clears authentication state depending on remember-me configuration.
     */
    private void clearAuthState() {
        if (authManager.isRememberEnabled()) {
            authManager.logoutButKeepEmail();
        } else {
            authManager.clearRememberMe();
        }
        authManager.logout();
    }

    /**
     * Stops the MediaPolling service.
     */
    private void stopPolling() {
        mediaPolling.setRunning(false);
    }

    /**
     * Displays the login screen and updates UI to logged-out state.
     */
    private void showLoginScreen() {
        mainFrame.updateSessionUI(false);
        mainFrame.showLogin();
    }

    /**
     * Registers a MediaPolling listener only once.
     *
     * <p>
     * When new cloud media is detected, the DownloadsController reloads cloud
     * content to keep local state synchronized.
     */
    private void initMediaPollingListener() {

        if (mediaListenerRegistered) {
            return;
        }

        mediaPolling.addMediaListener(event -> {
            System.out.println("[APP] New cloud media found: "
                    + event.getNewMediaCount());
            mainFrame.getDownloadsController().loadCloudMedia(mainFrame);
        });

        mediaListenerRegistered = true;
    }
}
