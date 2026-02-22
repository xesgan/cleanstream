package cat.dam.roig.cleanstream.app;

import cat.dam.roig.cleanstream.ui.main.MainFrame;
import cat.dam.roig.cleanstream.controller.MainController;
import cat.dam.roig.cleanstream.services.auth.AuthManager;
import cat.dam.roig.cleanstream.services.polling.MediaPolling;
import cat.dam.roig.cleanstream.services.polling.RoigMediaPollingAdapter;
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
            
            // 1) Crear componente externo
            RoigMediaPollingComponent mediaComponent = new RoigMediaPollingComponent();
            
            // 2) Adaptarlo a nuestra interfaz
            MediaPolling polling = new RoigMediaPollingAdapter(mediaComponent);
            
            // 3) Servicios que dependen de polling
            AuthManager authManager = new AuthManager(polling);
            
            //4 Crear UI principal inyectando dependencias
            MainFrame frame = new MainFrame(polling, authManager);
            
            MainController controller = new MainController(frame, authManager, polling);
            controller.start();           // decide login/autologin
            
            polling.setRunning(true);
            frame.setVisible(true);
        });
    }
}
