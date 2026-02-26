package cat.dam.roig.cleanstream.app;

import cat.dam.roig.cleanstream.config.AppConfig;
import cat.dam.roig.cleanstream.ui.main.MainFrame;
import cat.dam.roig.cleanstream.controller.MainController;
import cat.dam.roig.cleanstream.services.auth.AuthManager;
import cat.dam.roig.cleanstream.services.polling.MediaPolling;
import cat.dam.roig.cleanstream.services.polling.RoigMediaPollingAdapter;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;
import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Entry point (main class) of the CleanStream desktop application.
 *
 * <p>
 * This class is responsible for bootstrapping the app:
 * <ul>
 * <li>Initializes the global Look & Feel (FlatLaf dark theme).</li>
 * <li>Creates and configures the media polling component that communicates with
 * the REST API.</li>
 * <li>Wraps the component with an application-level adapter
 * ({@link RoigMediaPollingAdapter}) so the rest of the app depends on the
 * {@link MediaPolling} interface instead of the concrete component.</li>
 * <li>Creates the core services (e.g., {@link AuthManager}).</li>
 * <li>Builds the main UI ({@link MainFrame}) and its controller
 * ({@link MainController}).</li>
 * <li>Starts the polling loop and shows the main window.</li>
 * </ul>
 *
 * <p>
 * <b>Threading note:</b> All Swing initialization is executed on the Event
 * Dispatch Thread (EDT) using {@code EventQueue.invokeLater}, as required by
 * Swing.
 *
 * @author metku
 */
public class CleanStreamApp {

    /**
     * Application entry point.
     *
     * <p>
     * Initializes the UI theme and then creates all application objects on the
     * Swing EDT: polling component, adapter, authentication manager, main frame
     * and controller.
     *
     * <p>
     * <b>Important:</b> This method throws an {@link IllegalStateException} if
     * the configured API base URL is invalid, to prevent the app from running
     * in an inconsistent state.
     *
     * @param args command-line arguments (currently not used)
     */
    public static void main(String[] args) {

        // Initialize global UI theme (Dark Look & Feel).
        // If it fails, the app continues with the default Swing look & feel.
        try {
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // Always create Swing UI on the EDT to avoid random UI bugs.
        java.awt.EventQueue.invokeLater(() -> {

            // 1) Create the polling component (low-level API bridge).
            RoigMediaPollingComponent mediaComponent = new RoigMediaPollingComponent();

            // 2) Validate and set the API base URL used by the component.
            String apiUrl = AppConfig.API_BASE_URL;
            if (apiUrl == null || apiUrl.isBlank()
                    || (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://"))) {
                throw new IllegalStateException("Invalid API_BASE_URL: " + apiUrl);
            }
            mediaComponent.setApiUrl(apiUrl);

            // 3) Wrap the component using an adapter so the rest of the app depends on an interface.
            // This decouples the UI/services from the component implementation.
            MediaPolling polling = new RoigMediaPollingAdapter(mediaComponent);

            // 4) Create core services.
            // AuthManager will use polling to login and manage token/session state.
            AuthManager authManager = new AuthManager(polling);

            // 5) Build UI and controller (MVC-ish structure).
            MainFrame frame = new MainFrame(polling, authManager);

            MainController controller = new MainController(frame, authManager, polling);
            controller.start();

            // 6) Start polling and show the app window.
            polling.setRunning(true);
            frame.setVisible(true);
        });
    }
}
