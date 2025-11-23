package cat.dam.roig.cleanstream.ui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 *
 * @author metku
 */
public final class LoginPanel extends JPanel {

    /**
     * Constructor
     *
     * Configura la ventana.
     *
     * Crea panel principal.
     *
     * Añade componentes.
     *
     * Configura listeners.
     *
     * Muestra la ventana.
     *
     * @param mainFrame
     */
    public LoginPanel(JFrame mainFrame) {

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
                BorderFactory.createEmptyBorder(250, 220, 50, 420)
        );

        pnlPrincipal.add(wrapper, BorderLayout.CENTER);

        setVisible(true);
    }

    public void showLogin() {
        setSize(1000, 700);
    }

    public JPanel buildLoginPanel() {
        pnlPrincipal = new JPanel();

        pnlPrincipal.setLayout(new java.awt.BorderLayout());
        this.add(pnlPrincipal, BorderLayout.CENTER);

//        pnlPrincipal.setVisible(true);
        return pnlPrincipal;
    }

    public JPanel buildFormulario() {
        pnlFormulario = new JPanel();
        pnlFormulario.setLayout(new BoxLayout(pnlFormulario, BoxLayout.Y_AXIS));

        lblUser = new JLabel("User name");
        lblUser.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        txtUsername = new javax.swing.JTextField();

        lblPassword = new JLabel("Password");
        lblPassword.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        txtPassword = new javax.swing.JPasswordField();

        // FILA 1
        JPanel rowUser = new JPanel();
        rowUser.setLayout(new BorderLayout());
        rowUser.add(lblUser, BorderLayout.WEST);
        rowUser.add(txtUsername, BorderLayout.EAST);
        txtUsername.setColumns(15);

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

//        pnlMain.add(pnlFormulario, BorderLayout.CENTER);
        return pnlFormulario;
    }

    public JPanel buildBtnLogin( ) {
        pnlBtnsLogin = new JPanel();

        btnLogin = new JButton("Login");
        btnExit = new JButton("Exit");
        chkRememberMe = new JCheckBox("Remember me");

        pnlBtnsLogin.add(btnLogin);
        pnlBtnsLogin.add(btnExit);
        pnlBtnsLogin.add(chkRememberMe);

        return pnlBtnsLogin;
    }

    private JPanel pnlPrincipal;
    private JPanel pnlFormulario;
    private JPanel pnlBtnsLogin;
    private JLabel lblUser;
    private JLabel lblPassword;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnExit;
    private JCheckBox chkRememberMe;
}
