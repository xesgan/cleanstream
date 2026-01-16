package cat.dam.roig.cleanstream.controller;

import cat.dam.roig.cleanstream.main.MainFrame;
import cat.dam.roig.cleanstream.services.AuthManager;
import cat.dam.roig.roigmediapollingcomponent.RoigMediaPollingComponent;
import javax.swing.JOptionPane;

/**
 *
 * @author metku
 */
public class MainController {

    private final MainFrame mainFrame;
    private final AuthManager authManager;
    private final RoigMediaPollingComponent mediaComponent;
    private boolean mediaListenerRegistered = false;

    public MainController(MainFrame mainFrame, AuthManager auth, RoigMediaPollingComponent mediaComponent) {
        this.mainFrame = mainFrame;
        this.authManager = auth;
        this.mediaComponent = mediaComponent;
    }

    public void start() {
        // Login manual
        authManager.setOnLoginSuccess(() -> {
            mainFrame.showMainView();
            initMediaPollingListener(); 
            mediaComponent.setRunning(true);
        });

        // Auto-login
        if (authManager.tryAutoLogin()) {
            mainFrame.showMainView();
            initMediaPollingListener(); 
            mainFrame.getDownloadsController().loadCloudMedia(mainFrame);
            mediaComponent.setRunning(true);
        } else {
            mainFrame.showLogin();
        }
    }

    public void doLogout() {
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
        mediaComponent.setRunning(false);
        authManager.logout();
        mainFrame.showLogin();
        
    }

    private void initMediaPollingListener() {
        if (mediaListenerRegistered) {
            return;
        }

        mediaComponent.addMediaListener(evt -> {
            System.out.println("[APP] New cloud media found: " + evt.getNewMedia().size());

            javax.swing.SwingUtilities.invokeLater(() -> {
                mainFrame.getDownloadsController().loadCloudMedia(mainFrame);
            });
        });

        mediaListenerRegistered = true;
    }
}
