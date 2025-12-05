package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.main.MainFrame;
import cat.dam.roig.cleanstream.services.AuthManager;
import javax.swing.JOptionPane;

/**
 *
 * @author metku
 */
public class MainController {

    private final MainFrame mainFrame;
    private final AuthManager authManager;

    public MainController(MainFrame mainFrame, AuthManager auth) {
        this.mainFrame = mainFrame;
        this.authManager = auth;
    }

    public void start() {
        // Configurar que hacer cuando el login tenga exito
        authManager.setOnLoginSuccess(mainFrame::showMainView);

        // Intentar auto-login inteligente
        if (authManager.tryAutoLogin()) {
            mainFrame.showMainView();
        } else {
            mainFrame.showLogin();
        }
    }

    public void doLogout() {
        // Preguntar si esta seguro
        int opt = JOptionPane.showConfirmDialog(
                mainFrame,
                "Do you really want to log out?",
                "Confirm logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (opt != JOptionPane.YES_OPTION) {
            return; // usuario canceló
        }
        // Limpiar estado de sesion
        authManager.clearRememberMe();
        // Volver a la pantalla login
        authManager.logout();
        mainFrame.showLogin();
    }
}
