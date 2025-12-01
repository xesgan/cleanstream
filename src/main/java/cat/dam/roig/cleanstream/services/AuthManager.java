package cat.dam.roig.cleanstream.services;

import cat.dam.roig.cleanstream.models.Usuari;
import cat.dam.roig.cleanstream.ui.LoginPanel;
import java.io.IOException;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;

/**
 * Gestiona la autenticación y el Remember Me.
 *
 * @author metku
 */
public class AuthManager {

    // ---- Preferencias ----
    private final Preferences prefs;
    private static final String KEY_TOKEN  = "auth.token";
    private static final String KEY_EMAIL  = "auth.email";

    // ---- Dependencias ----
    private final ApiClient apiClient;
    private LoginPanel loginPanel;

    // Callback cuando el login tiene éxito (para que el MainFrame cambie de panel)
    private Runnable onLoginSuccess;

    public AuthManager(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.prefs = Preferences.userNodeForPackage(AuthManager.class);
    }

    // Permite inyectar el loginPanel desde fuera
    public void setLoginPanel(LoginPanel loginPanel) {
        this.loginPanel = loginPanel;
    }

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    // ----------------- Gestión de token / remember me -----------------

    public void saveToken(String token) {
        if (token != null && !token.isBlank()) {
            prefs.put(KEY_TOKEN, token);
        }
    }

    public String getStoredToken() {
        return prefs.get(KEY_TOKEN, null);
    }

    public void clearToken() {
        prefs.remove(KEY_TOKEN);
    }

    public void saveRememberMe(String email, String token) {
        // Guardamos el email y el token con las mismas claves que se usan en AuthManager
        prefs.put(KEY_EMAIL, email);
        saveToken(token);
    }

    public String getRememberedEmail() {
        return prefs.get(KEY_EMAIL, "");
    }

    public void clearRememberMe() {
        prefs.remove(KEY_EMAIL);
        clearToken();

        if (loginPanel != null) {
            loginPanel.setTxtEmail("");
            // Si quieres, también: loginPanel.setTxtPassword("");
        }
    }

    // ----------------- Auto login con token -----------------

    /**
     * Intenta usar el token guardado.
     *
     * @return true si el token es válido, false si no lo es (o no existe).
     */
    public boolean tryAutoLogin() {

        String token = getStoredToken();
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            // Endpoint ligero que requiere auth (por ejemplo /me)
            Usuari me = apiClient.getMe(token);
            System.out.println("CONFIRMAMOS QUE FUNCIONA: " + me.nickName);

            return true; // si no lanza excepción, asumimos OK
        } catch (IOException | InterruptedException ex) {
            // Aquí asumimos que no podemos auto loguear al usuario.
            clearToken();
            return false;
        } catch (Exception ex) {
            // Auth inválida, token caducado o similar
            System.getLogger(AuthManager.class.getName())
                    .log(System.Logger.Level.ERROR, (String) null, ex);
            clearToken();
            return false;
        }
    }

    // ----------------- Login normal desde el LoginPanel -----------------

    /**
     * Ejecuta el login leyendo usuario y password del LoginPanel.
     */
    public void doLogin() {

        if (loginPanel == null) {
            throw new IllegalStateException("LoginPanel no ha sido asignado a AuthManager");
        }

        String email = loginPanel.getTxtEmail().getText();
        char[] pwChars = loginPanel.getTxtPassword().getPassword();
        String pass = new String(pwChars);

        if (email == null || email.isBlank() || pass == null || pass.isBlank()) {
            JOptionPane.showMessageDialog(
                    loginPanel,
                    "Password or Email cannot be empty!",
                    "Check your data",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            // Obtener el jwt
            String token = apiClient.login(email, pass);

            // Guardamos o limpiamos el Remember Me segun el estado del checkbox
            if (loginPanel.isRememberMeSelected()) {
                saveRememberMe(email, token);
            } else {
                clearRememberMe();
            }

            // Avisamos de que el login ha sido un exito
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    loginPanel,
                    "Password or Email are not valid!",
                    "Check your data",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
