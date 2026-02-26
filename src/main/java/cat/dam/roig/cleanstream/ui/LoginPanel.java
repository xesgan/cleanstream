package cat.dam.roig.cleanstream.ui;

import cat.dam.roig.cleanstream.services.auth.AuthManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Login screen panel for CleanStream.
 *
 * <p>
 * This panel is responsible for:
 * <ul>
 * <li>Rendering the login UI (dark card layout with margins)</li>
 * <li>Collecting user credentials (email + password)</li>
 * <li>Providing UX helpers (placeholders, show password, live validation)</li>
 * <li>Providing user feedback (error label, loading state, progress
 * indicator)</li>
 * <li>Delegating the authentication action to {@link AuthManager}</li>
 * </ul>
 *
 * <p>
 * Important architectural note: This class is a UI component (Swing). It does
 * not validate server-side credentials, it only validates basic input format
 * and delegates login to {@link AuthManager}.
 *
 * <p>
 * UX design goals:
 * <ul>
 * <li>Centered card with real padding (no "layouts raros")</li>
 * <li>Simple GridBag-based layout to control margins and alignment</li>
 * <li>Placeholders for empty fields</li>
 * <li>Keyboard-friendly: Enter triggers login (default button)</li>
 * </ul>
 *
 * <p>
 * Threading note: Any UI update must be performed on the Swing EDT. This panel
 * uses {@link SwingUtilities#invokeLater(Runnable)} where needed (focus and
 * default button).
 *
 * @author metku
 */
public final class LoginPanel extends JPanel {

    /**
     * Auth manager used to perform login and remember-me behavior. This panel
     * registers itself into the manager via
     * {@link AuthManager#setLoginPanel(LoginPanel)}.
     */
    private final AuthManager authManager;

    // ===== Root & Card =====
    private JPanel pnlRoot;
    private JPanel pnlCard;

    // ===== Header =====
    private JLabel lblTitle;
    private JLabel lblSubtitle;

    // ===== Form =====
    private JLabel lblEmail;
    private JLabel lblPassword;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JCheckBox chkShowPassword;

    // ===== Actions =====
    private JCheckBox chkRememberMe;
    private JButton btnLogin;

    // ===== Feedback =====
    private JLabel lblError;
    private JProgressBar pb;
    private JLabel lblStatus;

    // ===== State =====
    private String loginBtnOriginalText = "Login";

    // ===== Placeholders =====
    private static final String PH_EMAIL = "user@domain.com";
    private static final String PH_PASS = "••••••••";

    // ===== Colors (local palette for this panel) =====
    private static final Color BG = new Color(45, 45, 45);
    private static final Color CARD_BG = new Color(56, 56, 56);
    private static final Color TEXT = new Color(230, 230, 230);
    private static final Color MUTED = new Color(170, 170, 170);
    private static final Color ERROR = new Color(231, 76, 60);

    private static final Color BTN_BG = new Color(46, 134, 193);
    private static final Color BTN_BG_DISABLED = new Color(85, 105, 120);

    /**
     * Creates the LoginPanel and wires it to the {@link AuthManager}.
     *
     * <p>
     * The constructor:
     * <ul>
     * <li>Stores the provided AuthManager</li>
     * <li>Registers this panel into it (so AuthManager can update UI fields on
     * logout/remember)</li>
     * <li>Builds UI components and installs event handlers</li>
     * <li>Sets UX defaults (focus + default button)</li>
     * </ul>
     *
     * @param authManager authentication manager used to execute login
     */
    public LoginPanel(AuthManager authManager) {
        this.authManager = authManager;
        this.authManager.setLoginPanel(this);

        initComponents();
        initEvents();
        initUxDefaults();
        refreshLoginEnabled();
    }

    /**
     * Builds the full UI layout: root container, centered card and all content
     * blocks.
     *
     * <p>
     * Layout structure:
     * <ul>
     * <li>Root panel (GridBagLayout) to center the card</li>
     * <li>Card panel (GridBagLayout) containing header, form, feedback, actions
     * and footer</li>
     * </ul>
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(BG);

        pnlRoot = new JPanel(new GridBagLayout());
        pnlRoot.setOpaque(true);
        pnlRoot.setBackground(BG);

        pnlCard = new JPanel(new GridBagLayout());
        pnlCard.setOpaque(true);
        pnlCard.setBackground(CARD_BG);
        pnlCard.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(85, 85, 85)),
                new EmptyBorder(18, 18, 18, 18)
        ));

        // Build blocks
        JPanel header = buildHeader();
        JPanel form = buildForm();
        JPanel actions = buildActions();
        JPanel footer = buildFooter();

        // Vertical layout inside the card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        pnlCard.add(header, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 8, 0);
        pnlCard.add(form, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 10, 0);
        pnlCard.add(lblError, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 10, 0);
        pnlCard.add(actions, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        pnlCard.add(footer, gbc);

        // Center card in root
        GridBagConstraints center = new GridBagConstraints();
        center.gridx = 0;
        center.gridy = 0;
        center.anchor = GridBagConstraints.CENTER;
        pnlRoot.add(pnlCard, center);

        // Fixed size for consistent UI
        pnlCard.setPreferredSize(new Dimension(460, 340));

        add(pnlRoot, BorderLayout.CENTER);
    }

    /**
     * Builds the header block (title + subtitle).
     *
     * @return header panel
     */
    private JPanel buildHeader() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        lblTitle = new JLabel("CleanStream");
        lblTitle.setForeground(TEXT);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 20f));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSubtitle = new JLabel("Sign in to continue");
        lblSubtitle.setForeground(MUTED);
        lblSubtitle.setFont(lblSubtitle.getFont().deriveFont(Font.PLAIN, 12f));
        lblSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSubtitle);

        return p;
    }

    /**
     * Builds the form block (email, password, show-password checkbox and error
     * label).
     *
     * @return form panel
     */
    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        lblEmail = new JLabel("Email");
        lblEmail.setForeground(MUTED);

        lblPassword = new JLabel("Password");
        lblPassword.setForeground(MUTED);

        txtEmail = new JTextField();
        txtPassword = new JPasswordField();

        styleTextField(txtEmail);
        stylePasswordField(txtPassword);

        installPlaceholder(txtEmail, PH_EMAIL);
        installPasswordPlaceholder(txtPassword, PH_PASS);

        chkShowPassword = new JCheckBox("Show password");
        chkShowPassword.setOpaque(false);
        chkShowPassword.setForeground(MUTED);

        // Error label is created here but added later in initComponents (card layout)
        lblError = new JLabel(" ");
        lblError.setForeground(ERROR);
        lblError.setVisible(false);
        lblError.setBorder(new EmptyBorder(4, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        p.add(lblEmail, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        p.add(txtEmail, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 4, 0);
        p.add(lblPassword, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 8, 0);
        p.add(txtPassword, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        p.add(chkShowPassword, gbc);

        return p;
    }

    /**
     * Builds the actions block (remember-me checkbox + login button).
     *
     * <p>
     * Uses GridBagLayout to guarantee margins so the login button never
     * "sticks" to the right edge of the card.
     *
     * @return actions panel
     */
    private JPanel buildActions() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        chkRememberMe = new JCheckBox("Remember me");
        chkRememberMe.setOpaque(false);
        chkRememberMe.setForeground(MUTED);

        btnLogin = new JButton("Login");
        btnLogin.setFocusPainted(false);
        btnLogin.setFont(btnLogin.getFont().deriveFont(Font.BOLD, 13f));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setBackground(BTN_BG);
        btnLogin.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnLogin.setPreferredSize(new Dimension(140, 38));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Left: remember checkbox
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        p.add(chkRememberMe, gbc);

        // Right: login button with extra right margin
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 6);
        p.add(btnLogin, gbc);

        return p;
    }

    /**
     * Builds the footer block (progress bar + status label).
     *
     * @return footer panel
     */
    private JPanel buildFooter() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        pb = new JProgressBar();
        pb.setIndeterminate(true);
        pb.setVisible(false);

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(MUTED);
        lblStatus.setBorder(new EmptyBorder(6, 0, 0, 0));

        p.add(pb);
        p.add(lblStatus);
        return p;
    }

    /**
     * Registers all UI listeners: login click, live validation and
     * show-password toggling.
     */
    private void initEvents() {
        // Login click
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearError();

                if (!validateInputs()) {
                    refreshLoginEnabled();
                    return;
                }

                setLoading(true);
                authManager.doLogin();
            }
        });

        // Live validation: whenever the user types, update button enabled state
        txtEmail.getDocument().addDocumentListener(new SimpleDocListener() {
            @Override
            public void onChange() {
                clearError();
                refreshLoginEnabled();
            }
        });
        txtPassword.getDocument().addDocumentListener(new SimpleDocListener() {
            @Override
            public void onChange() {
                clearError();
                refreshLoginEnabled();
            }
        });

        // Show/hide password
        chkShowPassword.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean show = (e.getStateChange() == ItemEvent.SELECTED);
                if (isPasswordPlaceholderActive()) {
                    return; // Don't change echo char while placeholder is visible
                }
                txtPassword.setEchoChar(show ? (char) 0 : '\u2022');
            }
        });
    }

    /**
     * Sets UX defaults for the panel:
     * <ul>
     * <li>Tooltips</li>
     * <li>Initial focus on the email field</li>
     * <li>Enter key triggers login via default button</li>
     * </ul>
     */
    private void initUxDefaults() {
        txtEmail.setToolTipText("Enter your email (e.g. user@domain.com)");
        txtPassword.setToolTipText("Enter your password");
        chkRememberMe.setToolTipText("Remember this session on this machine");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                txtEmail.requestFocusInWindow();
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JRootPane rp = SwingUtilities.getRootPane(LoginPanel.this);
                if (rp != null) {
                    rp.setDefaultButton(btnLogin);
                }
            }
        });
    }

    // ===== Validation & Enabled =====
    /**
     * Enables/disables the login button depending on basic input validity.
     *
     * <p>
     * The button is enabled only if:
     * <ul>
     * <li>Email looks valid (basic check)</li>
     * <li>Password is not empty</li>
     * </ul>
     *
     * <p>
     * If the panel is in loading state ("Logging in..."), this method does not
     * override that state.
     */
    private void refreshLoginEnabled() {
        if (!btnLogin.isEnabled() && "Logging in...".equals(btnLogin.getText())) {
            return;
        }

        String email = readEmail();
        String pass = readPassword();

        boolean enabled = isValidEmailBasic(email) && pass.length() > 0;

        btnLogin.setEnabled(enabled);
        btnLogin.setBackground(enabled ? BTN_BG : BTN_BG_DISABLED);
    }

    /**
     * Validates inputs and shows a user-friendly error if invalid.
     *
     * @return true if email + password are valid; false otherwise
     */
    private boolean validateInputs() {
        String email = readEmail();
        String pass = readPassword();

        if (email.isEmpty() && pass.isEmpty()) {
            showError("Email and password are required.");
            return false;
        }
        if (email.isEmpty()) {
            showError("Email is required.");
            return false;
        }
        if (!isValidEmailBasic(email)) {
            showError("Please enter a valid email.");
            return false;
        }
        if (pass.isEmpty()) {
            showError("Password is required.");
            return false;
        }
        return true;
    }

    /**
     * Very lightweight email validation used only for UX (not security).
     *
     * @param email email string
     * @return true if it looks like a valid email
     */
    private boolean isValidEmailBasic(String email) {
        if (email == null) {
            return false;
        }
        String s = email.trim();
        int at = s.indexOf('@');
        int dot = s.lastIndexOf('.');
        return at > 0 && dot > at + 1 && dot < s.length() - 1;
    }

    /**
     * Reads email input, ignoring placeholder values.
     *
     * @return email or empty string if placeholder/empty
     */
    private String readEmail() {
        String t = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        if (PH_EMAIL.equals(t) && txtEmail.getForeground().equals(MUTED)) {
            return "";
        }
        return t;
    }

    /**
     * Reads password input, ignoring placeholder values.
     *
     * @return password string or empty string if placeholder/empty
     */
    private String readPassword() {
        if (isPasswordPlaceholderActive()) {
            return "";
        }
        char[] pwd = txtPassword.getPassword();
        return pwd != null ? new String(pwd).trim() : "";
    }

    /**
     * Indicates whether the password placeholder is currently displayed.
     *
     * @return true if placeholder is active
     */
    private boolean isPasswordPlaceholderActive() {
        String t = new String(txtPassword.getPassword());
        return PH_PASS.equals(t) && txtPassword.getForeground().equals(MUTED);
    }

    // ===== Feedback =====
    /**
     * Displays an error message on the panel and exits loading state if needed.
     *
     * @param message error message to display
     */
    public void showError(String message) {
        lblError.setText(message != null ? message : "Unknown error.");
        lblError.setVisible(true);
        lblStatus.setText(" ");
        setLoading(false);
        revalidate();
        repaint();
    }

    /**
     * Clears the visible error label.
     */
    public void clearError() {
        if (lblError != null) {
            lblError.setText(" ");
            lblError.setVisible(false);
        }
    }

    /**
     * Toggles the loading state of the login screen.
     *
     * <p>
     * While loading:
     * <ul>
     * <li>All inputs are disabled</li>
     * <li>Login button text changes to "Logging in..."</li>
     * <li>Progress bar becomes visible</li>
     * <li>Status label indicates current action</li>
     * </ul>
     *
     * @param loading true to enable loading mode, false to restore normal mode
     */
    public void setLoading(boolean loading) {
        txtEmail.setEnabled(!loading);
        txtPassword.setEnabled(!loading);
        chkRememberMe.setEnabled(!loading);
        chkShowPassword.setEnabled(!loading);

        if (loading) {
            loginBtnOriginalText = btnLogin.getText();
            btnLogin.setText("Logging in...");
            btnLogin.setEnabled(false);
            btnLogin.setBackground(BTN_BG_DISABLED);

            pb.setVisible(true);
            lblStatus.setText("Signing in...");
        } else {
            btnLogin.setText(loginBtnOriginalText != null ? loginBtnOriginalText : "Login");
            pb.setVisible(false);
            lblStatus.setText(" ");
            refreshLoginEnabled();
        }
    }

    /**
     * Resets the login UI to a clean state after logout or failed attempts.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Clears errors</li>
     * <li>Exits loading state</li>
     * <li>Clears password field and restores placeholder</li>
     * <li>Restores focus to email field</li>
     * </ul>
     */
    public void resetUiState() {
        clearError();
        setLoading(false);

        txtPassword.setText("");
        installPasswordPlaceholder(txtPassword, PH_PASS);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                txtEmail.requestFocusInWindow();
            }
        });

        refreshLoginEnabled();
    }

    // ===== Styling =====
    /**
     * Applies consistent styling to text fields.
     *
     * @param tf text field to style
     */
    private void styleTextField(JTextField tf) {
        tf.setBackground(new Color(66, 66, 66));
        tf.setForeground(TEXT);
        tf.setCaretColor(TEXT);
        tf.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 90)),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    /**
     * Applies consistent styling to password fields, including default echo
     * char.
     *
     * @param pf password field to style
     */
    private void stylePasswordField(JPasswordField pf) {
        pf.setBackground(new Color(66, 66, 66));
        pf.setForeground(TEXT);
        pf.setCaretColor(TEXT);
        pf.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 90)),
                new EmptyBorder(8, 10, 8, 10)
        ));
        pf.setEchoChar('\u2022');
    }

    /**
     * Installs a placeholder behavior on a text field using a focus listener.
     *
     * <p>
     * Placeholder is shown only when the field is empty. When the field gains
     * focus, the placeholder is removed.
     *
     * @param tf text field to configure
     * @param placeholder placeholder text
     */
    private void installPlaceholder(final JTextField tf, final String placeholder) {
        if (tf.getText() == null || tf.getText().trim().isEmpty()) {
            tf.setText(placeholder);
            tf.setForeground(MUTED);
        }

        tf.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (placeholder.equals(tf.getText()) && tf.getForeground().equals(MUTED)) {
                    tf.setText("");
                    tf.setForeground(TEXT);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                String t = tf.getText() != null ? tf.getText().trim() : "";
                if (t.isEmpty()) {
                    tf.setText(placeholder);
                    tf.setForeground(MUTED);
                }
            }
        });
    }

    /**
     * Installs a placeholder behavior on a password field.
     *
     * <p>
     * When the placeholder is active, echo char is disabled so the placeholder
     * is visible. When user focuses the field, placeholder is removed and echo
     * char is restored.
     *
     * @param pf password field to configure
     * @param placeholder placeholder text
     */
    private void installPasswordPlaceholder(final JPasswordField pf, final String placeholder) {
        String t = new String(pf.getPassword());
        if (t.trim().isEmpty()) {
            pf.setText(placeholder);
            pf.setForeground(MUTED);
            pf.setEchoChar((char) 0);
        }

        pf.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                String val = new String(pf.getPassword());
                if (placeholder.equals(val) && pf.getForeground().equals(MUTED)) {
                    pf.setText("");
                    pf.setForeground(TEXT);
                    pf.setEchoChar(chkShowPassword != null && chkShowPassword.isSelected()
                            ? (char) 0
                            : '\u2022');
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                String val = new String(pf.getPassword()).trim();
                if (val.isEmpty()) {
                    pf.setText(placeholder);
                    pf.setForeground(MUTED);
                    pf.setEchoChar((char) 0);
                }
            }
        });
    }

    // ===== Public API used by AuthManager / MainController =====
    /**
     * Indicates whether the remember-me checkbox is selected.
     *
     * @return true if remember-me is enabled
     */
    public boolean isRememberMeSelected() {
        return chkRememberMe.isSelected();
    }

    /**
     * @return email text field (used by AuthManager to read/write email)
     */
    public JTextField getTxtEmail() {
        return txtEmail;
    }

    /**
     * @return password field (used by AuthManager to read password / clear it)
     */
    public JPasswordField getTxtPassword() {
        return txtPassword;
    }

    /**
     * Sets the email value and updates placeholder state.
     *
     * @param email email value; if null/blank, placeholder is restored
     */
    public void setTxtEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            txtEmail.setForeground(TEXT);
            txtEmail.setText(email.trim());
        } else {
            txtEmail.setText("");
            installPlaceholder(txtEmail, PH_EMAIL);
        }
        refreshLoginEnabled();
    }

    // ===== Simple DocumentListener =====
    /**
     * Minimal DocumentListener helper to run a single {@link #onChange()}
     * callback for all document change types.
     */
    private static abstract class SimpleDocListener implements DocumentListener {

        /**
         * Called on insert/remove/change events.
         */
        public abstract void onChange();

        @Override
        public void insertUpdate(DocumentEvent e) {
            onChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onChange();
        }
    }
}
