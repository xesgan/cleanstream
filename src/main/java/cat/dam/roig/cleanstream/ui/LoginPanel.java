package cat.dam.roig.cleanstream.ui;

import cat.dam.roig.cleanstream.services.AuthManager;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

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
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnExit;
    private JCheckBox chkRememberMe;

    public LoginPanel(AuthManager authManager) {
        this.authManager = authManager;
        this.authManager.setLoginPanel(this);

        initComponents();
        initEvents();
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
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                authManager.doLogin();
            }
        });

        // EXIT
        btnExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
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

        pnlBtnsLogin.add(btnLogin);
        pnlBtnsLogin.add(btnExit);
        pnlBtnsLogin.add(chkRememberMe);

        return pnlBtnsLogin;
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
