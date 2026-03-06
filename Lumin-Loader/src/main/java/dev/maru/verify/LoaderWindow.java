package dev.maru.verify;

import dev.maru.verify.util.AuthUtil;
import dev.maru.verify.util.HwidUtil;
import niurendeobf.ZKMIndy;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@ZKMIndy
public final class LoaderWindow {
    private static final int WIN_W = 900;
    private static final int WIN_H = 520;
    private static final int DESIGN_W = 820;
    private static final int DESIGN_H = 460;
    private static final byte[] CRED_MAGIC = new byte[]{'S', 'K', 'R', '1'};
    private static final int GCM_TAG_BITS = 128;

    private enum Mode {
        Login,
        Register
    }

    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile boolean success = false;
    private volatile boolean verifying = false;
    private volatile String statusLine = "";

    private JFrame frame;
    private SurfacePanel surfacePanel;
    private CardPanel cardPanel;
    private BrandingPanel brandingPanel;
    private JPanel formPanel;
    private JPanel fieldsPanel;
    private JPanel topBar;

    private ModeTabButton loginTab;
    private ModeTabButton registerTab;
    private HintTextField usernameField;
    private HintPasswordField passwordField;
    private HintTextField licenseField;
    private JCheckBox rememberCheck;
    private GradientButton primaryButton;
    private JButton secondaryButton;
    private JButton closeButton;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel statusLabel;

    private Mode mode = Mode.Login;
    private Timer repaintTimer;
    private float glowTick = 0f;
    private float successTransition = 0f;
    private float targetScale = 1f;

    private Font fontRegular;
    private Font fontMedium;
    private Font fontBold;
    private Font fontCjk;

    public static void verifyOrExitBlocking() {
        LoaderWindow w = new LoaderWindow();
        boolean ok = w.runBlocking();
        if (!ok) {
            System.exit(0);
        }
    }

    private boolean runBlocking() {
        try {
            SwingUtilities.invokeAndWait(this::initWindow);
            closeLatch.await();
            return success;
        } catch (Exception e) {
            return false;
        } finally {
            cleanup();
        }
    }

    private void initWindow() {
        applyPlatformHints();
        loadFonts();
        frame = new JFrame("欢迎来到 Sakura | Welcome to Sakura");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 460));
        frame.setSize(WIN_W, WIN_H);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeWindow(false);
            }
        });

        setWindowIcons();
        buildUi();
        bindKeys();
        loadSavedCredentials();
        applyMode(false);

        frame.setVisible(true);
        usernameField.requestFocusInWindow();

        repaintTimer = new Timer(16, e -> {
            glowTick = approach(glowTick, glowTick + 0.016f, 3f);
            if (success) {
                successTransition = approach(successTransition, 1f, 4.2f);
                if (successTransition > 0.995f) {
                    closeWindow(true);
                    return;
                }
            }
            animateInputs();
            surfacePanel.repaint();
        });
        repaintTimer.start();
    }

    private void applyPlatformHints() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void buildUi() {
        surfacePanel = new SurfacePanel();
        surfacePanel.setLayout(new GridBagLayout());
        frame.setContentPane(surfacePanel);

        cardPanel = new CardPanel();
        cardPanel.setLayout(new BorderLayout());
        cardPanel.setOpaque(false);
        cardPanel.setBorder(new EmptyBorder(18, 18, 18, 18));

        brandingPanel = new BrandingPanel();
        brandingPanel.setPreferredSize(new Dimension(330, 0));

        buildFormPanel();
        cardPanel.add(brandingPanel, BorderLayout.WEST);
        cardPanel.add(formPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(24, 24, 24, 24);
        surfacePanel.add(cardPanel, gbc);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyScale();
            }
        });
        applyScale();
    }

    private void buildFormPanel() {
        formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(12, 24, 12, 12));
        formPanel.setLayout(new BorderLayout());

        topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel tabsWrap = new JPanel();
        tabsWrap.setOpaque(false);
        tabsWrap.setLayout(new BoxLayout(tabsWrap, BoxLayout.X_AXIS));

        loginTab = new ModeTabButton("登录");
        registerTab = new ModeTabButton("注册");
        ButtonGroup group = new ButtonGroup();
        group.add(loginTab);
        group.add(registerTab);
        loginTab.setSelected(true);

        loginTab.addActionListener(e -> setMode(Mode.Login));
        registerTab.addActionListener(e -> setMode(Mode.Register));

        tabsWrap.add(loginTab);
        tabsWrap.add(Box.createHorizontalStrut(8));
        tabsWrap.add(registerTab);

        closeButton = new JButton("×");
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        closeButton.setContentAreaFilled(false);
        closeButton.setForeground(new Color(255, 255, 255, 210));
        closeButton.setOpaque(false);
        closeButton.setFont(resolveFont(Font.BOLD, 20f));
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(new Color(255, 120, 150));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(new Color(255, 255, 255, 210));
            }
        });
        closeButton.addActionListener(e -> closeWindow(false));

        topBar.add(tabsWrap, BorderLayout.WEST);
        topBar.add(closeButton, BorderLayout.EAST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BorderLayout());

        titleLabel = new JLabel("欢迎回来");
        titleLabel.setForeground(new Color(248, 250, 255));
        titleLabel.setFont(resolveFont(Font.BOLD, 28f));

        subtitleLabel = new JLabel("请验证你的账号以继续");
        subtitleLabel.setForeground(new Color(210, 214, 230, 210));
        subtitleLabel.setFont(resolveFont(Font.PLAIN, 14f));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setBorder(new EmptyBorder(0, 2, 12, 0));
        titles.add(titleLabel);
        titles.add(Box.createVerticalStrut(4));
        titles.add(subtitleLabel);

        fieldsPanel = new JPanel();
        fieldsPanel.setOpaque(false);
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));

        usernameField = new HintTextField("用户名");
        passwordField = new HintPasswordField("密码");
        licenseField = new HintTextField("卡密");

        fieldsPanel.add(usernameField);
        fieldsPanel.add(Box.createVerticalStrut(10));
        fieldsPanel.add(passwordField);
        fieldsPanel.add(Box.createVerticalStrut(10));
        fieldsPanel.add(licenseField);

        rememberCheck = new JCheckBox("记住密码");
        rememberCheck.setOpaque(false);
        rememberCheck.setFocusPainted(false);
        rememberCheck.setForeground(new Color(220, 226, 240, 225));
        rememberCheck.setFont(resolveFont(Font.PLAIN, 13f));

        JPanel rememberWrap = new JPanel(new BorderLayout());
        rememberWrap.setOpaque(false);
        rememberWrap.setBorder(new EmptyBorder(12, 2, 10, 2));
        rememberWrap.add(rememberCheck, BorderLayout.WEST);

        primaryButton = new GradientButton("登录");
        primaryButton.addActionListener(e -> trySubmit());

        secondaryButton = new JButton("没有账号？去注册");
        secondaryButton.setFocusPainted(false);
        secondaryButton.setContentAreaFilled(false);
        secondaryButton.setBorder(new EmptyBorder(4, 2, 4, 2));
        secondaryButton.setForeground(new Color(200, 208, 232, 225));
        secondaryButton.setHorizontalAlignment(SwingConstants.LEFT);
        secondaryButton.setFont(resolveFont(Font.PLAIN, 13f));
        secondaryButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                secondaryButton.setForeground(new Color(236, 72, 153, 232));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                secondaryButton.setForeground(new Color(200, 208, 232, 225));
            }
        });
        secondaryButton.addActionListener(e -> {
            if (mode == Mode.Login) {
                setMode(Mode.Register);
            } else {
                setMode(Mode.Login);
            }
        });

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(206, 211, 230, 210));
        statusLabel.setFont(resolveFont(Font.PLAIN, 13f));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBorder(new EmptyBorder(4, 0, 0, 0));
        buttons.add(primaryButton);
        buttons.add(Box.createVerticalStrut(8));
        buttons.add(secondaryButton);
        buttons.add(Box.createVerticalStrut(10));
        buttons.add(statusLabel);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(titles);
        content.add(fieldsPanel);
        content.add(rememberWrap);
        content.add(buttons);

        center.add(content, BorderLayout.NORTH);
        formPanel.add(topBar, BorderLayout.NORTH);
        formPanel.add(center, BorderLayout.CENTER);
    }

    private void bindKeys() {
        JComponent root = (JComponent) frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "close");
        root.getActionMap().put("close", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                closeWindow(false);
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "submit");
        root.getActionMap().put("submit", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                trySubmit();
            }
        });
    }

    private void setMode(Mode m) {
        if (mode == m || verifying) return;
        mode = m;
        applyMode(true);
    }

    private void applyMode(boolean focus) {
        boolean isLogin = mode == Mode.Login;
        loginTab.setSelected(isLogin);
        registerTab.setSelected(!isLogin);
        licenseField.setVisible(!isLogin);
        titleLabel.setText(isLogin ? "欢迎回来" : "创建账号");
        subtitleLabel.setText(isLogin ? "请验证你的账号以继续" : "填写信息并完成注册");
        primaryButton.setText(isLogin ? "登录" : "注册");
        secondaryButton.setText(isLogin ? "没有账号？去注册" : "已有账号？去登录");
        statusLine = "";
        setStatus("", new Color(206, 211, 230, 210));
        fieldsPanel.revalidate();
        fieldsPanel.repaint();
        if (focus) {
            if (isLogin) {
                usernameField.requestFocusInWindow();
            } else {
                licenseField.requestFocusInWindow();
            }
        }
    }

    private void trySubmit() {
        if (verifying || success) return;
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordText();
        String license = licenseField.getText() == null ? "" : licenseField.getText().trim();

        boolean valid = true;
        if (username.isEmpty()) {
            usernameField.pulseError();
            valid = false;
        }
        if (password.isEmpty()) {
            passwordField.pulseError();
            valid = false;
        }
        if (mode == Mode.Register && license.isEmpty()) {
            licenseField.pulseError();
            valid = false;
        }
        if (!valid) {
            setStatus("请先完整填写必填项", new Color(255, 119, 156, 235));
            return;
        }

        setVerifying(true);
        setStatus(mode == Mode.Login ? "正在登录..." : "正在注册...", new Color(184, 194, 234, 235));

        AuthUtil.AuthCallback callback = result -> SwingUtilities.invokeLater(() -> onAuthResult(result, username, password));
        if (mode == Mode.Login) {
            AuthUtil.login(username, password, callback);
        } else {
            AuthUtil.register(username, password, license, callback);
        }
    }

    private void onAuthResult(AuthUtil.AuthResult result, String username, String password) {
        setVerifying(false);
        if (result != null && result.isSuccess()) {
            if (rememberCheck.isSelected()) {
                saveCredentials(username, password);
            } else {
                clearSavedCredentials();
            }
            success = true;
            setStatus(result.getMessage() == null || result.getMessage().isBlank() ? "验证成功" : result.getMessage(), new Color(143, 238, 182, 235));
            primaryButton.setEnabled(false);
            secondaryButton.setEnabled(false);
            usernameField.setEditable(false);
            passwordField.setEditable(false);
            licenseField.setEditable(false);
            loginTab.setEnabled(false);
            registerTab.setEnabled(false);
            closeButton.setEnabled(false);
        } else {
            String message = result == null || result.getMessage() == null || result.getMessage().isBlank() ? "验证失败，请重试" : result.getMessage();
            setStatus(message, new Color(255, 119, 156, 235));
            if (mode == Mode.Login) {
                passwordField.pulseError();
            } else {
                passwordField.pulseError();
                licenseField.pulseError();
            }
        }
    }

    private void setVerifying(boolean value) {
        verifying = value;
        boolean interactive = !value && !success;
        primaryButton.setEnabled(interactive);
        secondaryButton.setEnabled(interactive);
        loginTab.setEnabled(interactive);
        registerTab.setEnabled(interactive);
        usernameField.setEditable(interactive);
        passwordField.setEditable(interactive);
        licenseField.setEditable(interactive);
        rememberCheck.setEnabled(interactive);
    }

    private void setStatus(String text, Color color) {
        statusLine = text == null ? "" : text;
        statusLabel.setText(statusLine.isEmpty() ? " " : statusLine);
        statusLabel.setForeground(color);
    }

    private String passwordText() {
        char[] chars = passwordField.getPassword();
        try {
            return new String(chars).trim();
        } finally {
            java.util.Arrays.fill(chars, '\0');
        }
    }

    private void closeWindow(boolean ok) {
        if (!closed.compareAndSet(false, true)) return;
        success = ok && success;
        if (repaintTimer != null) {
            repaintTimer.stop();
        }
        if (frame != null) {
            frame.dispose();
        }
        closeLatch.countDown();
    }

    private void cleanup() {
        if (frame != null && frame.isDisplayable()) {
            SwingUtilities.invokeLater(() -> {
                if (frame.isDisplayable()) {
                    frame.dispose();
                }
            });
        }
    }

    private void animateInputs() {
        List<AnimInput> inputs = new ArrayList<>();
        inputs.add(usernameField);
        inputs.add(passwordField);
        inputs.add(licenseField);
        for (AnimInput input : inputs) {
            input.tick();
        }
        primaryButton.tick();
        loginTab.tick();
        registerTab.tick();
    }

    private void applyScale() {
        int fw = Math.max(frame.getWidth(), 1);
        int fh = Math.max(frame.getHeight(), 1);
        float sx = fw / (float) WIN_W;
        float sy = fh / (float) WIN_H;
        float scale = Math.max(0.82f, Math.min(1.18f, Math.min(sx, sy)));
        targetScale = scale;

        int cardW = Math.max((int) (DESIGN_W * scale), 620);
        int cardH = Math.max((int) (DESIGN_H * scale), 360);
        cardPanel.setPreferredSize(new Dimension(cardW, cardH));
        cardPanel.revalidate();

        float titleSize = 28f * scale;
        float subSize = 14f * scale;
        float inputSize = 14f * scale;
        float smallSize = 13f * scale;

        titleLabel.setFont(resolveFont(Font.BOLD, titleSize));
        subtitleLabel.setFont(resolveFont(Font.PLAIN, subSize));
        usernameField.setFont(resolveFont(Font.PLAIN, inputSize));
        passwordField.setFont(resolveFont(Font.PLAIN, inputSize));
        licenseField.setFont(resolveFont(Font.PLAIN, inputSize));
        rememberCheck.setFont(resolveFont(Font.PLAIN, smallSize));
        secondaryButton.setFont(resolveFont(Font.PLAIN, smallSize));
        statusLabel.setFont(resolveFont(Font.PLAIN, smallSize));
        primaryButton.setFont(resolveFont(Font.BOLD, inputSize));
        loginTab.setFont(resolveFont(Font.BOLD, 13f * scale));
        registerTab.setFont(resolveFont(Font.BOLD, 13f * scale));
        closeButton.setFont(resolveFont(Font.BOLD, 20f * scale));

        int inputH = Math.max(34, Math.round(40 * scale));
        usernameField.setPreferredSize(new Dimension(260, inputH));
        passwordField.setPreferredSize(new Dimension(260, inputH));
        licenseField.setPreferredSize(new Dimension(260, inputH));
        primaryButton.setPreferredSize(new Dimension(260, inputH));

        topBar.revalidate();
        formPanel.revalidate();
        surfacePanel.repaint();
    }

    private void setWindowIcons() {
        List<java.awt.Image> icons = new ArrayList<>();
        BufferedImage i16 = loadIcon("/assets/sakura/icons/icon_16x16.png");
        BufferedImage i32 = loadIcon("/assets/sakura/icons/icon_32x32.png");
        if (i16 != null) icons.add(i16);
        if (i32 != null) icons.add(i32);
        if (!icons.isEmpty()) {
            frame.setIconImages(icons);
        } else if (Toolkit.getDefaultToolkit() != null) {
            frame.setIconImage(Toolkit.getDefaultToolkit().getImage(""));
        }
    }

    private static BufferedImage loadIcon(String path) {
        try (InputStream is = LoaderWindow.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (IOException e) {
            return null;
        }
    }

    private void loadFonts() {
        fontRegular = createFont("/assets/sakura/fonts/regular.otf");
        fontMedium = createFont("/assets/sakura/fonts/regular_medium.otf");
        fontBold = createFont("/assets/sakura/fonts/regular_bold.otf");
        fontCjk = createFont("/assets/sakura/fonts/kuriyama.ttf");
        if (fontRegular == null) {
            fontRegular = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        }
        if (fontMedium == null) {
            fontMedium = fontRegular.deriveFont(Font.PLAIN, 14f);
        }
        if (fontBold == null) {
            fontBold = fontRegular.deriveFont(Font.BOLD, 14f);
        }
        if (fontCjk == null) {
            fontCjk = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
        }
    }

    private static Font createFont(String path) {
        try (InputStream is = LoaderWindow.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (IOException | FontFormatException e) {
            return null;
        }
    }

    private Font resolveFont(int style, float size) {
        Font base = style == Font.BOLD ? fontBold : (style == Font.PLAIN ? fontRegular : fontMedium);
        if (base == null) {
            base = new Font(Font.SANS_SERIF, style, Math.max(12, Math.round(size)));
        }
        Font f = base.deriveFont(style, Math.max(11f, size));
        if (fontCjk != null) {
            return f.deriveFont(style, Math.max(11f, size));
        }
        return f;
    }

    private void loadSavedCredentials() {
        try {
            Path p = credentialPath();
            if (!Files.isRegularFile(p)) return;
            byte[] bytes = Files.readAllBytes(p);
            String plain = decryptCredentials(bytes);
            if (plain == null || plain.isBlank()) {
                clearSavedCredentials();
                return;
            }
            int sep = plain.indexOf('\0');
            if (sep < 0) {
                clearSavedCredentials();
                return;
            }
            String u = plain.substring(0, sep);
            String pw = plain.substring(sep + 1);
            if (!u.isBlank()) usernameField.setText(u);
            if (!pw.isBlank()) passwordField.setText(pw);
            rememberCheck.setSelected(true);
        } catch (Exception ignored) {
            clearSavedCredentials();
        }
    }

    private static void clearSavedCredentials() {
        try {
            Files.deleteIfExists(credentialPath());
        } catch (Exception ignored) {
        }
    }

    private static void saveCredentials(String u, String pw) {
        if (u == null || u.isBlank() || pw == null || pw.isBlank()) {
            clearSavedCredentials();
            return;
        }
        try {
            Path p = credentialPath();
            Files.createDirectories(p.getParent());
            byte[] data = encryptCredentials(u + "\0" + pw);
            Files.write(p, data);
        } catch (Exception ignored) {
        }
    }

    private static Path credentialPath() {
        return Paths.get(System.getProperty("user.home"), "Sakura", "credentials.bin");
    }

    private static byte[] encryptCredentials(String plain) throws Exception {
        byte[] key = credentialKey();
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        cipher.updateAAD("SakuraCredential-v1".getBytes(StandardCharsets.UTF_8));
        byte[] ct = cipher.doFinal((plain == null ? "" : plain).getBytes(StandardCharsets.UTF_8));

        ByteBuffer out = ByteBuffer.allocate(4 + 1 + iv.length + 4 + ct.length);
        out.put(CRED_MAGIC);
        out.put((byte) iv.length);
        out.put(iv);
        out.putInt(ct.length);
        out.put(ct);
        return out.array();
    }

    private static String decryptCredentials(byte[] data) {
        try {
            if (data == null || data.length < 4 + 1 + 12 + 4) return null;
            ByteBuffer in = ByteBuffer.wrap(data);
            for (int i = 0; i < CRED_MAGIC.length; i++) {
                if (in.get() != CRED_MAGIC[i]) return null;
            }
            int ivLen = Byte.toUnsignedInt(in.get());
            if (ivLen < 12 || ivLen > 32) return null;
            if (in.remaining() < ivLen + 4) return null;
            byte[] iv = new byte[ivLen];
            in.get(iv);
            int ctLen = in.getInt();
            if (ctLen <= 0 || ctLen > in.remaining()) return null;
            byte[] ct = new byte[ctLen];
            in.get(ct);

            byte[] key = credentialKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD("SakuraCredential-v1".getBytes(StandardCharsets.UTF_8));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] credentialKey() throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String seed = "Sakura|Cred|v1|" + HwidUtil.getHWID() + "|" + System.getProperty("user.name");
        byte[] digest = sha256.digest(seed.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(digest, 0, key, 0, 16);
        return key;
    }

    private static float approach(float current, float target, float speed) {
        float d = target - current;
        if (Math.abs(d) < 0.0001f) return target;
        float step = d * Math.min(1f, 0.18f * speed);
        return current + step;
    }

    private interface AnimInput {
        void tick();

        void pulseError();
    }

    private abstract static class BaseHintField extends javax.swing.JTextField implements AnimInput {
        private final String placeholder;
        private boolean hover = false;
        private float hoverV = 0f;
        private float focusV = 0f;
        private float errorV = 0f;
        private long errorUntil = 0L;

        protected BaseHintField(String placeholder) {
            this.placeholder = placeholder == null ? "" : placeholder;
            setOpaque(false);
            setForeground(new Color(248, 249, 255));
            setCaretColor(new Color(248, 249, 255));
            setSelectionColor(new Color(126, 153, 255, 170));
            setSelectedTextColor(new Color(255, 255, 255));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            float arc = Math.max(12f, 14f * (h / 40f));

            float baseA = 105f + 28f * hoverV + 34f * focusV;
            g2.setColor(new Color(22, 24, 33, clamp255(baseA)));
            g2.fillRoundRect(0, 0, w, h, Math.round(arc), Math.round(arc));

            Color border = mix(new Color(112, 130, 206, 165), new Color(173, 190, 255, 230), focusV);
            if (errorV > 0.01f) {
                border = mix(border, new Color(255, 97, 142, 240), errorV);
            }
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, Math.round(arc), Math.round(arc));

            super.paintComponent(g2);

            if (getText().isEmpty() && !isFocusOwner()) {
                g2.setFont(getFont());
                g2.setColor(new Color(215, 220, 235, 150));
                Insets in = getInsets();
                int y = (h + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
                g2.drawString(placeholder, in.left, y);
            }
            g2.dispose();
        }

        @Override
        public void tick() {
            hoverV = approach(hoverV, hover ? 1f : 0f, 1.8f);
            focusV = approach(focusV, isFocusOwner() ? 1f : 0f, 2.8f);
            float target = System.currentTimeMillis() < errorUntil ? 1f : 0f;
            errorV = approach(errorV, target, 4.8f);
        }

        @Override
        public void pulseError() {
            errorUntil = System.currentTimeMillis() + 640L;
        }

        protected static Color mix(Color a, Color b, float p) {
            float t = Math.max(0f, Math.min(1f, p));
            int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            int al = Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
            return new Color(r, g, bl, al);
        }

        protected static int clamp255(float a) {
            return Math.max(0, Math.min(255, Math.round(a)));
        }
    }

    private static final class HintTextField extends BaseHintField {
        private HintTextField(String placeholder) {
            super(placeholder);
        }
    }

    private static final class HintPasswordField extends JPasswordField implements AnimInput {
        private final String placeholder;
        private boolean hover = false;
        private float hoverV = 0f;
        private float focusV = 0f;
        private float errorV = 0f;
        private long errorUntil = 0L;

        private HintPasswordField(String placeholder) {
            this.placeholder = placeholder == null ? "" : placeholder;
            setOpaque(false);
            setForeground(new Color(248, 249, 255));
            setCaretColor(new Color(248, 249, 255));
            setSelectionColor(new Color(126, 153, 255, 170));
            setSelectedTextColor(new Color(255, 255, 255));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setEchoChar('•');
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            float arc = Math.max(12f, 14f * (h / 40f));

            float baseA = 105f + 28f * hoverV + 34f * focusV;
            g2.setColor(new Color(22, 24, 33, BaseHintField.clamp255(baseA)));
            g2.fillRoundRect(0, 0, w, h, Math.round(arc), Math.round(arc));

            Color border = BaseHintField.mix(new Color(112, 130, 206, 165), new Color(173, 190, 255, 230), focusV);
            if (errorV > 0.01f) {
                border = BaseHintField.mix(border, new Color(255, 97, 142, 240), errorV);
            }
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, Math.round(arc), Math.round(arc));

            super.paintComponent(g2);

            if (getPassword().length == 0 && !isFocusOwner()) {
                g2.setFont(getFont());
                g2.setColor(new Color(215, 220, 235, 150));
                Insets in = getInsets();
                int y = (h + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
                g2.drawString(placeholder, in.left, y);
            }
            g2.dispose();
        }

        @Override
        public void tick() {
            hoverV = approach(hoverV, hover ? 1f : 0f, 1.8f);
            focusV = approach(focusV, isFocusOwner() ? 1f : 0f, 2.8f);
            float target = System.currentTimeMillis() < errorUntil ? 1f : 0f;
            errorV = approach(errorV, target, 4.8f);
        }

        @Override
        public void pulseError() {
            errorUntil = System.currentTimeMillis() + 640L;
        }
    }

    private static final class ModeTabButton extends JToggleButton {
        private boolean hover = false;
        private float hoverV = 0f;
        private float selectV = 0f;

        private ModeTabButton(String text) {
            super(text);
            setFocusPainted(false);
            setBorder(new EmptyBorder(7, 16, 7, 16));
            setContentAreaFilled(false);
            setForeground(new Color(214, 221, 240, 220));
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                }
            });
        }

        private void tick() {
            hoverV = approach(hoverV, hover ? 1f : 0f, 2f);
            selectV = approach(selectV, isSelected() ? 1f : 0f, 3f);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            float arc = 14f;

            float alpha = 40f + 55f * hoverV + 130f * selectV;
            Paint fill = new LinearGradientPaint(0, 0, w, h, new float[]{0f, 1f},
                    new Color[]{
                            new Color(90, 120, 248, clamp255(alpha)),
                            new Color(236, 72, 153, clamp255(alpha * 0.75f))
                    });
            g2.setPaint(fill);
            g2.fillRoundRect(0, 0, w, h, Math.round(arc), Math.round(arc));

            Color border = BaseHintField.mix(new Color(116, 136, 211, 130), new Color(174, 194, 255, 235), selectV);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, Math.round(arc), Math.round(arc));

            Color fg = BaseHintField.mix(new Color(201, 210, 232, 210), new Color(245, 248, 255, 255), Math.max(selectV, hoverV * 0.45f));
            setForeground(fg);
            g2.dispose();
            super.paintComponent(g);
        }

        private static int clamp255(float a) {
            return Math.max(0, Math.min(255, Math.round(a)));
        }
    }

    private static final class GradientButton extends JButton {
        private boolean hover = false;
        private float hoverV = 0f;
        private float pressV = 0f;

        private GradientButton(String text) {
            super(text);
            setFocusPainted(false);
            setBorder(new EmptyBorder(9, 14, 9, 14));
            setContentAreaFilled(false);
            setForeground(new Color(252, 252, 255));
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                }
            });
        }

        private void tick() {
            ButtonModel model = getModel();
            hoverV = approach(hoverV, hover ? 1f : 0f, 2f);
            pressV = approach(pressV, model.isPressed() ? 1f : 0f, 3.2f);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            float arc = Math.max(13f, h * 0.4f);

            int yOffset = Math.round(pressV * 2f);
            float intensity = isEnabled() ? (0.95f + 0.12f * hoverV - 0.07f * pressV) : 0.45f;
            Color c1 = tint(new Color(92, 124, 255), intensity);
            Color c2 = tint(new Color(236, 72, 153), intensity);
            Paint fill = new GradientPaint(0, yOffset, c1, w, h + yOffset, c2);
            g2.setPaint(fill);
            g2.fillRoundRect(0, yOffset, w, h - yOffset, Math.round(arc), Math.round(arc));

            g2.setColor(new Color(255, 255, 255, isEnabled() ? 112 : 70));
            g2.drawRoundRect(0, yOffset, w - 1, h - yOffset - 1, Math.round(arc), Math.round(arc));

            g2.dispose();
            super.paintComponent(g);
        }

        private static Color tint(Color c, float p) {
            float t = Math.max(0f, Math.min(1.2f, p));
            int r = Math.max(0, Math.min(255, Math.round(c.getRed() * t)));
            int g = Math.max(0, Math.min(255, Math.round(c.getGreen() * t)));
            int b = Math.max(0, Math.min(255, Math.round(c.getBlue() * t)));
            return new Color(r, g, b, c.getAlpha());
        }
    }

    private final class SurfacePanel extends JPanel {
        private SurfacePanel() {
            setOpaque(true);
            setBackground(new Color(10, 10, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            Paint bg = new LinearGradientPaint(0, 0, w, h, new float[]{0f, 1f}, new Color[]{
                    new Color(10, 10, 14),
                    new Color(18, 18, 24)
            });
            g2.setPaint(bg);
            g2.fillRect(0, 0, w, h);

            float p1x = (float) (w * (0.22 + Math.sin(glowTick * 0.38f) * 0.03));
            float p1y = (float) (h * (0.26 + Math.cos(glowTick * 0.28f) * 0.04));
            float p2x = (float) (w * (0.8 + Math.cos(glowTick * 0.24f) * 0.03));
            float p2y = (float) (h * (0.7 + Math.sin(glowTick * 0.34f) * 0.04));

            g2.setPaint(new RadialGradientPaint(p1x, p1y, Math.max(220f, w * 0.34f), new float[]{0f, 1f}, new Color[]{
                    new Color(92, 124, 255, 110),
                    new Color(92, 124, 255, 0)
            }));
            g2.fillRect(0, 0, w, h);

            g2.setPaint(new RadialGradientPaint(p2x, p2y, Math.max(240f, w * 0.38f), new float[]{0f, 1f}, new Color[]{
                    new Color(236, 72, 153, 90),
                    new Color(236, 72, 153, 0)
            }));
            g2.fillRect(0, 0, w, h);

            if (successTransition > 0.01f) {
                int alpha = Math.max(0, Math.min(170, Math.round(successTransition * 170)));
                g2.setColor(new Color(144, 238, 182, alpha));
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();
        }
    }

    private final class CardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            float arc = Math.max(20f, 26f * targetScale);
            ShapeRound shadowShape = new ShapeRound(0, 0, w, h, arc);

            g2.setColor(new Color(0, 0, 0, 90));
            g2.translate(0, 8);
            g2.fill(shadowShape.shape);
            g2.translate(0, -8);

            Paint glass = new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 1f}, new Color[]{
                    new Color(20, 22, 31, 228),
                    new Color(18, 20, 28, 236)
            });
            g2.setPaint(glass);
            g2.fill(shadowShape.shape);

            Paint border = new LinearGradientPaint(0, 0, w, h, new float[]{0f, 1f}, new Color[]{
                    new Color(255, 255, 255, 48),
                    new Color(255, 255, 255, 15)
            });
            g2.setPaint(border);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(shadowShape.shape);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class ShapeRound {
        private final RoundRectangle2D.Float shape;

        private ShapeRound(float x, float y, float w, float h, float arc) {
            shape = new RoundRectangle2D.Float(x, y, w, h, arc, arc);
        }
    }

    private final class BrandingPanel extends JPanel {
        private BrandingPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(20, 24, 20, 16));
            setLayout(new BorderLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            float arc = Math.max(18f, 24f * targetScale);

            Paint panel = new LinearGradientPaint(0, 0, w, h, new float[]{0f, 1f}, new Color[]{
                    new Color(92, 124, 255, 96),
                    new Color(236, 72, 153, 70)
            });
            g2.setPaint(panel);
            g2.fillRoundRect(0, 0, w, h, Math.round(arc), Math.round(arc));

            g2.setPaint(new RadialGradientPaint(w * 0.3f, h * 0.2f, Math.max(120f, w * 0.45f), new float[]{0f, 1f}, new Color[]{
                    new Color(255, 255, 255, 62),
                    new Color(255, 255, 255, 0)
            }));
            g2.fillRoundRect(0, 0, w, h, Math.round(arc), Math.round(arc));

            g2.setColor(new Color(255, 255, 255, 168));
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, Math.round(arc), Math.round(arc));

            Font logo = resolveFont(Font.BOLD, 42f * targetScale);
            g2.setFont(logo);
            g2.setColor(new Color(250, 252, 255, 250));
            g2.drawString("Sakura", Math.round(24 * targetScale), Math.round(h * 0.42f));

            g2.setFont(resolveFont(Font.PLAIN, 14f * targetScale));
            g2.setColor(new Color(238, 241, 255, 210));
            g2.drawString("安全验证中心", Math.round(26 * targetScale), Math.round(h * 0.42f + 30 * targetScale));

            g2.setFont(resolveFont(Font.PLAIN, 12f * targetScale));
            g2.setColor(new Color(238, 241, 255, 170));
            g2.drawString("Fast • Secure • Elegant", Math.round(26 * targetScale), Math.round(h * 0.42f + 52 * targetScale));

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
