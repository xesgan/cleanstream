package cat.dam.roig.cleanstream.ui;

import cat.dam.roig.cleanstream.services.AuthManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Panel de Login.
 *
 * Solo UI, la lógica de login la gestiona AuthManager.
 *
 * @author metku
 */
public final class LoginPanel extends JPanel {

    private final AuthManager authManager;

    private JPanel pnlPrincipal;
    private JPanel pnlFormulario;
    private JPanel pnlBtnsLogin;

    private JLabel lblUser;
    private JLabel lblPassword;
    private JLabel lblError;

    private JTextField txtEmail;
    private JPasswordField txtPassword;

    private JButton btnLogin;
    private JButton btnExit;
    private JCheckBox chkRememberMe;

    // Estado UI
    private String loginBtnOriginalText = "Login";

    public LoginPanel(AuthManager authManager) {
        this.authManager = authManager;
        this.authManager.setLoginPanel(this);

        initComponents();
        initEvents();
        initUxDefaults();
    }

    private void initComponents() {
        // Layout base del propio panel
        setLayout(new BorderLayout());
        setSize(1000, 700);

        // Panel principal con GridBagLayout para centrar el contenido
        pnlPrincipal = new JPanel(new GridBagLayout());

        // --- Formulario + botones envueltos en "wrapper" ---
        pnlFormulario = buildFormulario();
        pnlBtnsLogin = buildBtnLogin();

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(pnlFormulario);

        // Error label (feedback)
        lblError = new JLabel(" "); // espacio para mantener altura
        lblError.setForeground(new Color(192, 57, 43));
        lblError.setVisible(false);
        lblError.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        wrapper.add(lblError);
        wrapper.add(Box.createVerticalStrut(15));
        wrapper.add(pnlBtnsLogin);

        // Un pequeño margen alrededor, sin romper el centrado
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Centramos el wrapper dentro de pnlPrincipal
        GridBagConstraints gbcCenter = new GridBagConstraints();
        gbcCenter.gridx = 0;
        gbcCenter.gridy = 0;
        gbcCenter.anchor = GridBagConstraints.CENTER;
        pnlPrincipal.add(wrapper, gbcCenter);

        // Añadimos pnlPrincipal al centro del LoginPanel
        add(pnlPrincipal, BorderLayout.CENTER);

        setVisible(true);
    }

    private void initEvents() {
        // LOGIN
        btnLogin.addActionListener(e -> {
            clearError();

            if (!validateInputs()) {
                return;
            }

            // UX: estado cargando + evitar doble click
            setLoading(true);

            // La lógica vive en AuthManager
            authManager.doLogin();
        });

        // EXIT
        btnExit.addActionListener(e -> System.exit(0));
    }

    /**
     * Ajustes UX sin tocar lógica: - Enter = Login - Tooltips - Foco inicial
     */
    private void initUxDefaults() {
        // Tooltips
        txtEmail.setToolTipText("Enter your email (e.g. user@domain.com)");
        txtPassword.setToolTipText("Enter your password");
        chkRememberMe.setToolTipText("Remember this session on this machine");

        // “Exit” menos peligroso: lo hacemos menos protagonista
        btnExit.setToolTipText("Close the application");
        btnExit.setFocusable(false);

        // Foco inicial
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                txtEmail.requestFocusInWindow();
            }
        });

        // Enter = Login (default button)
        // Se hace aquí para no depender del JFrame.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (SwingUtilities.getRootPane(LoginPanel.this) != null) {
                    SwingUtilities.getRootPane(LoginPanel.this).setDefaultButton(btnLogin);
                }
            }
        });
    }

    // ================ BUILDINGS ================
    private JPanel buildFormulario() {
        pnlFormulario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        lblUser = new JLabel("Email: ");
        lblPassword = new JLabel("Password: ");
        txtEmail = new JTextField(15);
        txtPassword = new JPasswordField(15);

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.EAST;   // labels alineados a la derecha

        // Fila 1: Email
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnlFormulario.add(lblUser, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        pnlFormulario.add(txtEmail, gbc);

        // Fila 2: Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        pnlFormulario.add(lblPassword, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        pnlFormulario.add(txtPassword, gbc);

        return pnlFormulario;
    }

    private JPanel buildBtnLogin() {
        pnlBtnsLogin = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        btnLogin = new JButton("Login");
        btnExit = new JButton("Exit");
        chkRememberMe = new JCheckBox("Remember me");

        // Jerarquía visual simple: Login más importante
        btnLogin.setFont(btnLogin.getFont().deriveFont(java.awt.Font.BOLD));
        btnLogin.setBackground(new Color(46, 134, 193));
        btnLogin.setForeground(Color.WHITE);

        pnlBtnsLogin.add(btnLogin);
        pnlBtnsLogin.add(btnExit);
        pnlBtnsLogin.add(chkRememberMe);

        return pnlBtnsLogin;
    }

    // ================ UX HELPERS ================
    private boolean validateInputs() {
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String pass = new String(txtPassword.getPassword()).trim();

        if (email.isEmpty() && pass.isEmpty()) {
            showError("Email and password are required.");
            return false;
        }
        if (email.isEmpty()) {
            showError("Email is required.");
            return false;
        }
        if (pass.isEmpty()) {
            showError("Password is required.");
            return false;
        }
        return true;
    }

    /**
     * Muestra error en rojo (feedback).
     */
    public void showError(String message) {
        lblError.setText(message != null ? message : "Unknown error.");
        lblError.setVisible(true);
        // Re-layout por si cambia altura
        revalidate();
        repaint();
        // UX: devolvemos control al usuario
        setLoading(false);
    }

    /**
     * Oculta error.
     */
    public void clearError() {
        if (lblError != null) {
            lblError.setText(" ");
            lblError.setVisible(false);
        }
    }

    /**
     * Activa/desactiva estado "cargando". Llamar con false cuando AuthManager
     * termine (ok o error).
     */
    public void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnExit.setEnabled(!loading);
        txtEmail.setEnabled(!loading);
        txtPassword.setEnabled(!loading);
        chkRememberMe.setEnabled(!loading);

        if (loading) {
            loginBtnOriginalText = btnLogin.getText();
            btnLogin.setText("Logging in...");
        } else {
            btnLogin.setText(loginBtnOriginalText != null ? loginBtnOriginalText : "Login");
        }
    }

    /**
     * Resetea la Ui para que no quede la pantalla completamente deshabilitada
     * por el modo Loading agregado.
     */
    public void resetUiState() {
        clearError();
        setLoading(false);
        txtPassword.setText("");
        // opcional:
        // txtEmail.setText("");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                txtEmail.requestFocusInWindow();
            }
        });
    }

    // ================ GETTERS / SETTERS ================
    public boolean isRememberMeSelected() {
        return chkRememberMe.isSelected();
    }

    public JTextField getTxtEmail() {
        return txtEmail;
    }

    public JPasswordField getTxtPassword() {
        return txtPassword;
    }

    public void setTxtEmail(String txtEmail) {
        this.txtEmail.setText(txtEmail);
    }
}
