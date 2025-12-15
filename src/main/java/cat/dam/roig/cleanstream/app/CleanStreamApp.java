package cat.dam.roig.cleanstream.app;

import cat.dam.roig.cleanstream.main.MainFrame;
import cat.dam.roig.cleanstream.controller.MainController;

/**
 *
 * @author metku
 */
public class CleanStreamApp {
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            MainController controller = new MainController(frame, frame.getAuthManager(), frame.getRoigMediaPollingComponent1());
            controller.start();           // decide login/autologin
            frame.setVisible(true);
        });
    }
}
