package cat.dam.roig.cleanstream.app;

import cat.dam.roig.cleanstream.config.AppConfig;
import cat.dam.roig.cleanstream.ui.main.MainFrame;
import cat.dam.roig.cleanstream.controller.MainController;
import cat.dam.roig.cleanstream.services.auth.AuthManager;
import cat.dam.roig.cleanstream.services.polling.MediaPolling;
import cat.dam.roig.cleanstream.services.polling.RoigMediaPollingAdapter;
import cat.dam.roig.cleanstream.services.prefs.UserPreferences;
import cat.dam.roig.cleanstream.util.UrlUtils;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;
import com.formdev.flatlaf.FlatDarkLaf;

/**
 *
 * @author metku
 */
public class CleanStreamApp {

    public static void main(String[] args) {

        try {
            FlatDarkLaf.setup();   // 👈 GLOBAL DARK THEME
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }
        java.awt.EventQueue.invokeLater(() -> {

            RoigMediaPollingComponent mediaComponent = new RoigMediaPollingComponent();

            String apiUrl = AppConfig.API_BASE_URL;
            if (apiUrl == null || apiUrl.isBlank()
                    || (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://"))) {
                throw new IllegalStateException("Invalid API_BASE_URL: " + apiUrl);
            }
            mediaComponent.setApiUrl(apiUrl);

            mediaComponent.setApiUrl(AppConfig.API_BASE_URL);

            // 4️ Adaptarlo
            MediaPolling polling = new RoigMediaPollingAdapter(mediaComponent);

            // 5️ Servicios
            AuthManager authManager = new AuthManager(polling);

            // 6️ UI
            MainFrame frame = new MainFrame(polling, authManager);

            MainController controller = new MainController(frame, authManager, polling);
            controller.start();

            polling.setRunning(true);
            frame.setVisible(true);
        });
    }
}
