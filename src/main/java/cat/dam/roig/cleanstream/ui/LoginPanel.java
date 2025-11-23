package cat.dam.roig.cleanstream.ui;

import cat.dam.roig.cleanstream.services.ApiClient;
import cat.dam.roig.cleanstream.models.Usuari;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 *
 * @author metku
 */
public final class LoginPanel extends JPanel {

    private final ApiClient apiClient;
    private String token;
    private static final long TOKEN_MAX_AGE_MILLIS = 3L * 24 * 60 * 60 * 1000; // 3 días
    private Runnable onLoginSucces;

    /**
     * @param apiClient
     * @param mainFrame
     */
    public LoginPanel(ApiClient apiClient) {
        this.apiClient = apiClient;
        initComponents();
        initEvents();

    }

    private void initComponents() {
        showLogin();

        pnlPrincipal = buildLoginPanel();
        pnlFormulario = buildFormulario();
        pnlBtnsLogin = buildBtnLogin();

        // WRAPPER: junta formulario + botones
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        wrapper.add(pnlFormulario);
        wrapper.add(Box.createVerticalStrut(20));     // separación entre formulario y botones
        wrapper.add(pnlBtnsLogin);

        // el border se aplica ahora al bloque entero
        wrapper.setBorder(
                BorderFactory.createEmptyBorder(250, 410, 50, 420)
        );

        pnlPrincipal.add(wrapper, BorderLayout.CENTER);

        // Llamamos al metodo para verificar que el chkbox este seleccionado o el token no haya expirado
        tryAutoFillRememberMe();

        setVisible(true);
    }

    private void initEvents() {
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin(); // ===== LOGIN EVENT =====
            }
        });
    }

    private void doLogin() {

        String email = txtEmail.getText();
        char[] pwChars = txtPassword.getPassword();
        String pass = new String(pwChars);
        if (email == null || email.isBlank() || pass == null || pass.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Password or Email cannot be empty!",
                    "Check your data",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            // Obtener el jwt
            token = apiClient.login(email, pass);

            // Obtener los datos del usuario
            Usuari me = apiClient.getMe(token);
            
            // Guardamos o limpiamos el Remember Me segun el estado del checkbox
            if (chkRememberMe.isSelected()) {
                saveRememberMe(email, token, System.currentTimeMillis());
            } else {
                clearRememberMe();
            }

            // Podemos avisar de que el login ha sido un exito
            if (onLoginSucces != null) {
                onLoginSucces.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Password or Email are not valid!",
                    "Check your data",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ================ DoLogin HELPERS ================ 
    private void saveRememberMe(String email, String token, long issuedAtMillis) {
        java.util.prefs.Preferences prefs = Preferences.userNodeForPackage(LoginPanel.class);
        prefs.put("remember.email", email);
        prefs.put("remember.token", token);
        prefs.putLong("remember.issuedAt", issuedAtMillis);
    }

    public void clearRememberMe() {
        Preferences prefs = Preferences.userNodeForPackage(LoginPanel.class);
        prefs.remove("remember.email");
        prefs.remove("remember.token");
        prefs.remove("remember.issuedAt");
        
        txtPassword.setText("");
    }

    private void tryAutoFillRememberMe() {
        Preferences prefs = Preferences.userNodeForPackage(LoginPanel.class);

        String savedEmail = prefs.get("remember.email", null);
        String savedToken = prefs.get("remember.token", null);
        long issuedAt = prefs.getLong("remember.issuedAt", -1L);

        if (savedEmail == null || savedToken == null || issuedAt <= 0) {
            chkRememberMe.setSelected(false);
            return;
        }

        long now = System.currentTimeMillis();
        long age = now - issuedAt;

        if (age > TOKEN_MAX_AGE_MILLIS) {
            clearRememberMe();
            chkRememberMe.setSelected(false);
            return;
        }

        txtEmail.setText(savedEmail);
        chkRememberMe.setSelected(true);
    }

    // ================ BUILDINGS ================ 
    private void showLogin() {
        setSize(1000, 700);
    }

    private JPanel buildLoginPanel() {
        pnlPrincipal = new JPanel();

        pnlPrincipal.setLayout(new java.awt.BorderLayout());
        this.add(pnlPrincipal, BorderLayout.CENTER);

        return pnlPrincipal;
    }

    private JPanel buildFormulario() {
        pnlFormulario = new JPanel();
        pnlFormulario.setLayout(new BoxLayout(pnlFormulario, BoxLayout.Y_AXIS));

        lblUser = new JLabel("Email: ");
        lblUser.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        txtEmail = new javax.swing.JTextField();

        lblPassword = new JLabel("Password: ");
        lblPassword.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        txtPassword = new javax.swing.JPasswordField();

        // FILA 1
        JPanel rowUser = new JPanel();
        rowUser.setLayout(new BorderLayout());
        rowUser.add(lblUser, BorderLayout.WEST);
        rowUser.add(txtEmail, BorderLayout.EAST);
        txtEmail.setColumns(15);

        rowUser.setMaximumSize(
                new java.awt.Dimension(Integer.MAX_VALUE, rowUser.getPreferredSize().height)
        );

        // FILA 2
        JPanel rowPass = new JPanel();
        rowPass.setLayout(new BorderLayout());
        rowPass.add(lblPassword, BorderLayout.WEST);
        rowPass.add(txtPassword, BorderLayout.EAST);
        txtPassword.setColumns(15);

        rowPass.setMaximumSize(
                new java.awt.Dimension(Integer.MAX_VALUE, rowPass.getPreferredSize().height)
        );

        pnlFormulario.add(rowUser);
        pnlFormulario.add(javax.swing.Box.createVerticalStrut(10));
        pnlFormulario.add(rowPass);

        return pnlFormulario;
    }

    private JPanel buildBtnLogin() {
        pnlBtnsLogin = new JPanel();

        btnLogin = new JButton("Login");
        btnExit = new JButton("Exit");
        chkRememberMe = new JCheckBox("Remember me");

        pnlBtnsLogin.add(btnLogin);
        pnlBtnsLogin.add(btnExit);
        pnlBtnsLogin.add(chkRememberMe);

        return pnlBtnsLogin;
    }

    // ================ GETTERS ================ 
    public boolean isRememberMeSelected() {
        return chkRememberMe.isSelected();
    }

    public JTextField getTxtUsername() {
        return txtEmail;
    }

    public JPasswordField getTxtPassword() {
        return txtPassword;
    }

    public void setOnLoginSucces(Runnable callBack) {
        this.onLoginSucces = callBack;
    }

    private JPanel pnlPrincipal;
    private JPanel pnlFormulario;
    private JPanel pnlBtnsLogin;
    private JLabel lblUser;
    private JLabel lblPassword;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnExit;
    private JCheckBox chkRememberMe;
}
