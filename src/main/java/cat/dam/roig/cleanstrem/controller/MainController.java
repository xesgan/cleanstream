package cat.dam.roig.cleanstrem.controller;

import cat.dam.roig.cleanstream.main.MainFrame;
import cat.dam.roig.cleanstream.services.AuthManager;

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

        mainFrame.showLogin(); // de momento, simple
    }
}
