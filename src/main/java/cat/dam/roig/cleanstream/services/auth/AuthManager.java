package cat.dam.roig.cleanstream.services.auth;

import cat.dam.roig.cleanstream.services.polling.MediaPolling;
import cat.dam.roig.cleanstream.ui.LoginPanel;
import java.util.Arrays;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;

/**
 * Manages authentication, session token persistence and "Remember me" behavior.
 *
 * <p>
 * This service is responsible for:
 * <ul>
 *   <li>Performing login using {@link MediaPolling#login(String, String)}</li>
 *   <li>Storing and retrieving a JWT token and remembered email using {@link Preferences}</li>
 *   <li>Trying auto-login by validating the stored token against the backend</li>
 *   <li>Clearing auth state on logout, optionally keeping the remembered email</li>
 *   <li>Notifying the application when login succeeds via a callback</li>
 * </ul>
 *
 * <p>
 * Important notes:
 * <ul>
 *   <li>This class coordinates UI feedback (dialogs) because it reads credentials from {@link LoginPanel}.
 *       In a stricter architecture, UI feedback would be moved to a controller.</li>
 *   <li>The token validity depends on the backend. If validation fails, stored data is cleared.</li>
 *   <li>Passwords are handled as {@code char[]} and wiped after use to reduce exposure in memory.</li>
 * </ul>
 *
 * @author metku
 */
public class AuthManager {

    // ---- Preferences keys ----

    /**
     * Preferences node used to store authentication data for this application.
     */
    private final Preferences prefs;

    /**
     * Key that stores the current JWT token.
     */
    private static final String KEY_TOKEN = "auth.token";

    /**
     * Key that stores the remembered email (used to pre-fill the login form).
     */
    private static final String KEY_EMAIL = "auth.email";

    /**
     * Key that stores whether remember-me is enabled in this machine.
     */
    private static final String KEY_REMEMBER_ENABLED = "rememberEnabled";

    // ---- Dependencies ----

    /**
     * UI panel that provides user credentials and shows UI feedback.
     * It is injected after construction.
     */
    private LoginPanel loginPanel;

    /**
     * Service/component used to communicate with the backend API (login, validate token, etc.).
     */
    private final MediaPolling polling;

    /**
     * Callback executed when login is successful (e.g., to let MainController change the UI view).
     */
    private Runnable onLoginSuccess;

    /**
     * Creates an AuthManager that will authenticate through the provided {@link MediaPolling} component.
     *
     * @param comp MediaPolling instance used to call backend endpoints (must not be null)
     * @throws IllegalArgumentException if {@code comp} is null
     */
    public AuthManager(MediaPolling comp) {
        if (comp == null) {
            throw new IllegalArgumentException("MediaPolling cannot be null");
        }
        this.polling = comp;

        // Using a custom node ensures the values don't mix with other packages.
        this.prefs = Preferences.userRoot().node("cat/dam/roig/cleanstream/auth");
    }

    /**
     * Injects the LoginPanel dependency.
     *
     * <p>
     * AuthManager reads the credentials from this panel and updates UI fields when clearing state
     * or keeping the email.
     *
     * @param loginPanel login UI panel (can be null if auth is used without UI)
     */
    public void setLoginPanel(LoginPanel loginPanel) {
        this.loginPanel = loginPanel;
    }

    /**
     * Sets the callback that will be executed when login succeeds.
     *
     * @param onLoginSuccess callback to run after a successful login
     */
    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    // ----------------- Token / Remember-me -----------------

    /**
     * Stores a JWT token in preferences.
     *
     * @param token JWT token. If null or blank, nothing is stored.
     */
    public void saveToken(String token) {
        if (token != null && !token.isBlank()) {
            prefs.put(KEY_TOKEN, token);
        }
    }

    /**
     * Retrieves the stored JWT token.
     *
     * @return stored token, or null if not found
     */
    public String getStoredToken() {
        return prefs.get(KEY_TOKEN, null);
    }

    /**
     * Removes any stored token from preferences.
     */
    public void clearToken() {
        prefs.remove(KEY_TOKEN);
    }

    /**
     * Stores remember-me data: email and token.
     *
     * <p>
     * Email is used to pre-fill the login form. Token is used for auto-login.
     *
     * @param email user's email to remember
     * @param token token to store for auto-login
     */
    public void saveRememberMe(String email, String token) {
        prefs.put(KEY_EMAIL, email);
        saveToken(token);

        // Debug output
        System.out.println("SAVE remember: email=" + prefs.get(KEY_EMAIL, "<null>"));
    }

    /**
     * @return the remembered email, or empty string if not stored
     */
    public String getRememberedEmail() {
        return prefs.get(KEY_EMAIL, "");
    }

    /**
     * Indicates whether remember-me is enabled for this machine.
     *
     * @return true if remember-me is enabled; false otherwise
     */
    public boolean isRememberEnabled() {
        return prefs.getBoolean(KEY_REMEMBER_ENABLED, false);
    }

    /**
     * Updates remember-me flag in preferences.
     *
     * @param enabled true to enable remember-me; false to disable it
     */
    private void setRememberEnabled(boolean enabled) {
        prefs.putBoolean(KEY_REMEMBER_ENABLED, enabled);
    }

    /**
     * Clears all remember-me data: email and token.
     *
     * <p>
     * Also clears the token in {@link MediaPolling} and resets UI fields if {@link #loginPanel}
     * is available.
     */
    public void clearRememberMe() {
        prefs.remove(KEY_EMAIL);
        clearToken();
        polling.setToken(null);

        if (loginPanel != null) {
            loginPanel.setTxtEmail("");
            loginPanel.getTxtPassword().setText("");
        }
    }

    /**
     * Logs out but keeps the remembered email.
     *
     * <p>
     * This method:
     * <ul>
     *   <li>Clears token from preferences</li>
     *   <li>Clears token from {@link MediaPolling}</li>
     *   <li>Restores remembered email in the UI, clears password field</li>
     * </ul>
     */
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

    // ----------------- Auto login -----------------

    /**
     * Attempts to auto-login using the stored token (if present).
     *
     * <p>
     * Flow:
     * <ol>
     *   <li>Reads token from preferences</li>
     *   <li>Sets token into {@link MediaPolling}</li>
     *   <li>Validates token with a minimal backend call</li>
     *   <li>If invalid: clears stored auth state and returns false</li>
     * </ol>
     *
     * @return true if stored token exists and is valid; false otherwise
     */
    public boolean tryAutoLogin() {
        String token = getStoredToken();
        if (token == null || token.isBlank()) {
            return false;
        }

        polling.setToken(token);

        try {
            // Minimal backend call to confirm token validity
            polling.validateToken();

            // Token is valid
            if (loginPanel != null) {
                loginPanel.setTxtEmail(getRememberedEmail());
            }
            return true;

        } catch (Exception ex) {
            // Token invalid or request failed: clean up local state
            ex.printStackTrace();
            clearRememberMe();
            polling.setToken(null);
            return false;
        }
    }

    // ----------------- Manual login (UI-driven) -----------------

    /**
     * Performs a login using the credentials currently entered in {@link LoginPanel}.
     *
     * <p>
     * Behavior:
     * <ul>
     *   <li>Validates non-empty email and password</li>
     *   <li>Calls backend login to obtain a token</li>
     *   <li>Stores or clears remember-me data depending on checkbox selection</li>
     *   <li>Calls {@link #onLoginSuccess} if login succeeds</li>
     *   <li>Shows user feedback via {@link JOptionPane} on validation or login failure</li>
     * </ul>
     *
     * <p>
     * Security:
     * Password is read as {@code char[]} and wiped in {@code finally}.
     *
     * @throws IllegalStateException if {@link #loginPanel} has not been injected
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
            // Obtain the JWT token from backend
            polling.login(email, pass);

            // Persist remember-me state based on checkbox
            if (loginPanel.isRememberMeSelected()) {
                saveRememberMe(email, polling.getToken());
                setRememberEnabled(true);
            } else {
                clearRememberMe();
                setRememberEnabled(false);
            }

            // Notify application that login succeeded
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
            // Wipe password from memory as best-effort
            Arrays.fill(pwChars, '\0');
        }
    }

    /**
     * Logs out the current user, clearing the token from both preferences and {@link MediaPolling}.
     *
     * <p>
     * Also resets UI state (if {@link #loginPanel} is present).
     * Depending on remember-me behavior, callers may prefer using
     * {@link #logoutButKeepEmail()} or {@link #clearRememberMe()}.
     */
    public void logout() {
        clearToken();
        polling.setToken(null);

        if (loginPanel != null) {
            loginPanel.resetUiState();
            loginPanel.getTxtPassword().setText("");
        }
    }
}