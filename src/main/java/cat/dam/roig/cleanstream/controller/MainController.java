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
//        this.downloadsController = downloadsController;
    }

    // método privado para no duplicar lógica
    private void onLoginSuccess() {
        mainFrame.showMainView();
        mainFrame.getDownloadsController().loadCloudMedia(mainFrame);
    }

    public void start() {
        // Login manual
        authManager.setOnLoginSuccess(this::onLoginSuccess);

        // Auto-login
        if (authManager.tryAutoLogin()) {
            onLoginSuccess();
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
        if (authManager.isRememberEnabled()) {
            authManager.logoutButKeepEmail();
        } else {
            authManager.clearRememberMe();
        }
        authManager.logout();   // si este hace algo extra
        mainFrame.showLogin();
    }
}
