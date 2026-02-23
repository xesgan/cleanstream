package cat.dam.roig.cleanstream.services.auth;

import cat.dam.roig.cleanstream.services.polling.MediaPolling;
import cat.dam.roig.cleanstream.ui.LoginPanel;
import java.util.Arrays;
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
    private static final String KEY_TOKEN = "auth.token";
    private static final String KEY_EMAIL = "auth.email";
    private static final String KEY_REMEMBER_ENABLED = "rememberEnabled";

    // ---- Dependencias ----
    private LoginPanel loginPanel;
    private final MediaPolling polling;

    // Callback cuando el login tiene éxito (para que el MainFrame cambie de panel)
    private Runnable onLoginSuccess;

    public AuthManager(MediaPolling comp) {
        if (comp == null) {
            throw new IllegalArgumentException("MediaPolling cannot be null");
        }
        this.polling = comp;
        this.prefs = Preferences.userRoot().node("cat/dam/roig/cleanstream/auth");
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
        System.out.println("SAVE remember: email=" + prefs.get(KEY_EMAIL, "<null>"));
    }

    public String getRememberedEmail() {
        return prefs.get(KEY_EMAIL, "");
    }

    public boolean isRememberEnabled() {
        return prefs.getBoolean(KEY_REMEMBER_ENABLED, false);
    }

    private void setRememberEnabled(boolean enabled) {
        prefs.putBoolean(KEY_REMEMBER_ENABLED, enabled);
    }

    public void clearRememberMe() {
        prefs.remove(KEY_EMAIL);
        clearToken();
        polling.setToken(null);

        if (loginPanel != null) {
            loginPanel.setTxtEmail("");
            loginPanel.getTxtPassword().setText("");
        }
    }

    public void logoutButKeepEmail() {
        clearToken();
        polling.setToken(null);

        if (loginPanel != null) {
            String email = prefs.get(KEY_EMAIL, "");
            System.out.println("LOGOUT keep: email=" + prefs.get(KEY_EMAIL, "<null>"));
            loginPanel.setTxtEmail(email);
            loginPanel.getTxtPassword().setText("");
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

        polling.setToken(token);

        try {
            // Llamada mínima para validar token
            polling.validateToken();

            // Si llegamos aquí, el token funciona
            if (loginPanel != null) {
                loginPanel.setTxtEmail(getRememberedEmail());
            }
            return true;

        } catch (Exception ex) {
            // Token no válido: limpiar
            ex.printStackTrace();
            clearRememberMe();
            polling.setToken(null);
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
            polling.login(email, pass);

            // Guardamos o limpiamos el Remember Me segun el estado del checkbox
            if (loginPanel.isRememberMeSelected()) {
                saveRememberMe(email, polling.getToken());
                setRememberEnabled(true);
            } else {
                clearRememberMe();
                setRememberEnabled(false);
            }

            // Avisamos de que el login ha sido un exito
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    loginPanel,
                    "Password or Email are not valid!",
                    "Check your data",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            Arrays.fill(pwChars, '\0');
        }
    }

    public void logout() {
        clearToken();
        polling.setToken(null);

        if (loginPanel != null) {
            loginPanel.resetUiState();
            loginPanel.getTxtPassword().setText("");
        }
    }
}
