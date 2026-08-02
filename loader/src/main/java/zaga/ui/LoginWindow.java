package zaga.ui;

import zaga.api.ApiClient;
import zaga.utils.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.BasicStroke;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

public class LoginWindow extends JFrame {

    // ==================== THEMES ====================

    static class Theme {
        final String name;
        final Color bg, bg2, text, textDim, error, green, fieldBg, fieldBorder;
        final Color accent, buttonBg, buttonHover, buttonPressed;
        final Color pulseColor;

        Theme(String name, Color bg, Color bg2, Color text, Color textDim, Color error, Color green,
              Color fieldBg, Color fieldBorder, Color accent, Color buttonBg, Color buttonHover,
              Color buttonPressed, Color pulseColor) {
            this.name = name;
            this.bg = bg; this.bg2 = bg2; this.text = text; this.textDim = textDim;
            this.error = error; this.green = green; this.fieldBg = fieldBg;
            this.fieldBorder = fieldBorder; this.accent = accent; this.buttonBg = buttonBg;
            this.buttonHover = buttonHover; this.buttonPressed = buttonPressed;
            this.pulseColor = pulseColor;
        }
    }

    static final Theme THEME_DEFAULT = new Theme("Default",
        Color.BLACK, new Color(15, 15, 15), Color.WHITE, new Color(120, 120, 120),
        new Color(220, 50, 50), new Color(80, 255, 80), new Color(20, 20, 20),
        new Color(50, 50, 50), new Color(100, 100, 100), new Color(25, 25, 25),
        new Color(40, 40, 40), Color.WHITE, new Color(255, 255, 255));

    static final Theme THEME_CYBERPUNK = new Theme("Cyberpunk",
        new Color(5, 0, 15), new Color(15, 0, 30), new Color(0, 255, 255), new Color(100, 100, 120),
        new Color(255, 50, 100), new Color(0, 255, 100), new Color(10, 5, 25),
        new Color(30, 10, 50), new Color(255, 0, 128), new Color(15, 5, 35),
        new Color(25, 10, 50), new Color(0, 255, 255), new Color(0, 255, 255));

    static final Theme THEME_MIDNIGHT = new Theme("Midnight",
        new Color(0, 0, 20), new Color(10, 10, 40), new Color(180, 200, 255), new Color(80, 90, 130),
        new Color(255, 80, 80), new Color(100, 255, 150), new Color(8, 8, 30),
        new Color(20, 20, 60), new Color(70, 100, 200), new Color(12, 12, 45),
        new Color(20, 20, 60), new Color(180, 200, 255), new Color(100, 140, 255));

    static final Theme THEME_DRACULA = new Theme("Dracula",
        new Color(12, 12, 20), new Color(25, 25, 40), new Color(200, 200, 255), new Color(120, 120, 160),
        new Color(255, 85, 85), new Color(80, 255, 120), new Color(18, 18, 30),
        new Color(35, 35, 55), new Color(189, 147, 249), new Color(22, 22, 38),
        new Color(35, 35, 55), new Color(200, 200, 255), new Color(189, 147, 249));

    static final Theme THEME_NEON = new Theme("Neon",
        new Color(0, 0, 0), new Color(5, 5, 5), new Color(0, 255, 0), new Color(0, 130, 0),
        new Color(255, 0, 0), new Color(0, 255, 0), new Color(0, 10, 0),
        new Color(0, 30, 0), new Color(0, 255, 0), new Color(0, 15, 0),
        new Color(0, 30, 0), new Color(0, 255, 0), new Color(0, 255, 0));

    static final Theme[] ALL_THEMES = { THEME_DEFAULT, THEME_CYBERPUNK, THEME_MIDNIGHT, THEME_DRACULA, THEME_NEON };
    static Theme currentTheme = THEME_DEFAULT;

    private static final String VERSION = "1.21.4";
    private static final String FABRIC_VERSION = "0.16.14";

    private static final String MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private File mcDir;
    private File baseDir;
    private volatile boolean loading = false;
    private final Object loadingLock = new Object();

    private JPanel mainPanel;
    private JPanel loginPanel;
    private JPanel registerPanel;
    private JPanel loadingPanel;
    private CardLayout cardLayout;

    private JTextField loginField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel loginStatusLabel;
    private JProgressBar loginProgressBar;

    private JTextField regLoginField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmField;
    private JButton registerButton;
    private JLabel regStatusLabel;

    private JLabel loadingStatusLabel;
    private JProgressBar loadingProgressBar;

    private MouseTrail mouseTrail;
    private Particles particles;
    private float pulsePhase = 0f;
    private final javax.swing.Timer pulseTimer;
    private java.util.List<JTextField> allTextFields = new java.util.ArrayList<>();

    static void applyTheme(Theme theme) {
        currentTheme = theme;
    }

    private static Color bg() { return currentTheme.bg; }
    private static Color bg2() { return currentTheme.bg2; }
    private static Color text() { return currentTheme.text; }
    private static Color textDim() { return currentTheme.textDim; }
    private static Color error() { return currentTheme.error; }
    private static Color green() { return currentTheme.green; }
    private static Color fieldBg() { return currentTheme.fieldBg; }
    private static Color fieldBorder() { return currentTheme.fieldBorder; }

    public LoginWindow() {
        Config.load();
        if (Config.get().themeName != null) {
            for (Theme t : ALL_THEMES) {
                if (t.name.equals(Config.get().themeName)) { applyTheme(t); break; }
            }
        }

        pulseTimer = new javax.swing.Timer(30, e -> {
            pulsePhase += 0.08f;
            if (pulsePhase > (float)(Math.PI * 2)) pulsePhase -= (float)(Math.PI * 2);
            if (mainPanel != null) mainPanel.repaint();
        });
        pulseTimer.start();

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getenv("APPDATA");
        }
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getProperty("user.home");
        }
        mcDir = new File(localAppData, "zagaDLC");
        baseDir = new File(mcDir, "game");

        setTitle("zagaDLC");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                pulseTimer.stop();
            }
        });
        setResizable(false);
        setSize(480, 500);
        setLocationRelativeTo(null);
        getContentPane().setBackground(bg());

        cardLayout = new CardLayout();
        setLayout(cardLayout);

        mainPanel = createMainPanel();
        loginPanel = createLoginPanel();
        registerPanel = createRegisterPanel();
        loadingPanel = createLoadingPanel();

        add(mainPanel, "main");
        add(loginPanel, "login");
        add(registerPanel, "register");
        add(loadingPanel, "loading");

        cardLayout.show(getContentPane(), "main");
        setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.bg2);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(false);

        JLabel versionLabel = new JLabel("v1.0");
        versionLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        versionLabel.setForeground(textDim());
        versionLabel.setBounds(15, 8, 60, 20);
        panel.add(versionLabel);

        JLabel themeBtn = new JLabel(currentTheme.name, SwingConstants.CENTER);
        themeBtn.setFont(new Font("Consolas", Font.PLAIN, 10));
        themeBtn.setForeground(textDim());
        themeBtn.setBounds(400, 8, 65, 18);
        themeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = 0;
                for (int i = 0; i < ALL_THEMES.length; i++) {
                    if (ALL_THEMES[i] == currentTheme) { idx = i; break; }
                }
                applyTheme(ALL_THEMES[(idx + 1) % ALL_THEMES.length]);
                Config.get().themeName = currentTheme.name;
                Config.save();
                themeBtn.setText(currentTheme.name);
                themeBtn.setForeground(currentTheme.textDim);
                repaint();
                SwingUtilities.getWindowAncestor(themeBtn).repaint();
            }
            public void mouseEntered(MouseEvent e) { themeBtn.setForeground(currentTheme.text); }
            public void mouseExited(MouseEvent e) { themeBtn.setForeground(currentTheme.textDim); }
        });
        panel.add(themeBtn);

        TypewriterLabel titleText = new TypewriterLabel("zagaDLC");
        titleText.setBounds(0, 30, 480, 40);
        panel.add(titleText);

        mouseTrail = new MouseTrail();
        mouseTrail.setBounds(0, 0, 480, 500);
        panel.add(mouseTrail);

        particles = new Particles();
        particles.setBounds(0, 0, 480, 500);
        panel.add(particles);

        QuoteLabel quoteLabel = new QuoteLabel();
        quoteLabel.setBounds(0, 95, 480, 35);
        panel.add(quoteLabel);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(LoginWindow.this, e.getPoint(), mouseTrail);
                mouseTrail.addPoint(p);
                if (particles != null) particles.updateMouse(e.getPoint().x, e.getPoint().y);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(LoginWindow.this, e.getPoint(), mouseTrail);
                mouseTrail.addPoint(p);
                if (particles != null) particles.updateMouse(e.getPoint().x, e.getPoint().y);
            }
        });

        JButton loginBtn = createStyledButton("LOGIN", 280, 48);
        loginBtn.setBounds(100, 195, 280, 48);
        loginBtn.addActionListener(e -> cardLayout.show(getContentPane(), "login"));
        panel.add(loginBtn);

        JButton registerBtn = createStyledButton("REGISTER", 280, 48);
        registerBtn.setBounds(100, 257, 280, 48);
        registerBtn.addActionListener(e -> cardLayout.show(getContentPane(), "register"));
        panel.add(registerBtn);

        JLabel contactTitle = new JLabel("связь с владельцем", SwingConstants.CENTER);
        contactTitle.setFont(new Font("Consolas", Font.BOLD, 11));
        contactTitle.setForeground(textDim());
        contactTitle.setBounds(0, 380, 480, 18);
        panel.add(contactTitle);

        JLabel contacts = new JLabel("<html><center>"
                + "<span style='color:#888'>telegram</span> — <span style='color:#fff'>@sexsexov</span><br>"
                + "<span style='color:#888'>tiktok</span> — <span style='color:#fff'>@defzaga</span><br>"
                + "<span style='color:#888'>discord</span> — <span style='color:#fff'>@defzaga</span><br>"
                + "<span style='color:#888'>site</span> — <span style='color:#fff'>zagadlc.1c-umi.ru</span>"
                + "</center></html>", SwingConstants.CENTER);
        contacts.setFont(new Font("Consolas", Font.PLAIN, 11));
        contacts.setBounds(0, 400, 480, 75);
        panel.add(contacts);

        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.bg2);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(false);

        JLabel title = new JLabel("LOGIN", SwingConstants.CENTER);
        title.setBounds(0, 30, 480, 40);
        title.setFont(new Font("Consolas", Font.BOLD, 24));
        title.setForeground(text());
        panel.add(title);

        loginField = createStyledField("Username", 100);
        panel.add(loginField);

        passwordField = createStyledPasswordField("Password", 155);
        panel.add(passwordField);

        loginButton = createStyledButton("LOGIN");
        loginButton.setBounds(80, 230, 320, 44);
        loginButton.addActionListener(e -> doLogin());
        panel.add(loginButton);

        loginProgressBar = new JProgressBar(0, 100);
        loginProgressBar.setBounds(80, 290, 320, 4);
        loginProgressBar.setBackground(fieldBg());
        loginProgressBar.setForeground(text());
        loginProgressBar.setBorderPainted(false);
        loginProgressBar.setVisible(false);
        panel.add(loginProgressBar);

        loginStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        loginStatusLabel.setBounds(80, 305, 320, 20);
        loginStatusLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        loginStatusLabel.setForeground(textDim());
        panel.add(loginStatusLabel);

        JButton backBtn = new JButton("< BACK");
        backBtn.setBounds(80, 345, 100, 35);
        backBtn.setFont(new Font("Consolas", Font.PLAIN, 12));
        backBtn.setForeground(textDim());
        backBtn.setBackground(null);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            loginStatusLabel.setText(" ");
            cardLayout.show(getContentPane(), "main");
        });
        panel.add(backBtn);

        JButton closeBtn = new JButton("X");
        closeBtn.setBounds(430, 10, 30, 30);
        closeBtn.setFont(new Font("Consolas", Font.BOLD, 12));
        closeBtn.setForeground(textDim());
        closeBtn.setBackground(null);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> System.exit(0));
        panel.add(closeBtn);

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.bg2);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(false);

        JLabel title = new JLabel("REGISTER", SwingConstants.CENTER);
        title.setBounds(0, 20, 480, 40);
        title.setFont(new Font("Consolas", Font.BOLD, 24));
        title.setForeground(text());
        panel.add(title);

        regLoginField = createStyledField("Username", 80);
        panel.add(regLoginField);

        regPasswordField = createStyledPasswordField("Password", 130);
        panel.add(regPasswordField);

        regConfirmField = createStyledPasswordField("Confirm password", 180);
        panel.add(regConfirmField);

        registerButton = createStyledButton("REGISTER");
        registerButton.setBounds(80, 250, 320, 44);
        registerButton.addActionListener(e -> doRegister());
        panel.add(registerButton);

        regStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        regStatusLabel.setBounds(80, 310, 320, 20);
        regStatusLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        regStatusLabel.setForeground(textDim());
        panel.add(regStatusLabel);

        JButton backBtn = new JButton("< BACK");
        backBtn.setBounds(80, 345, 100, 35);
        backBtn.setFont(new Font("Consolas", Font.PLAIN, 12));
        backBtn.setForeground(textDim());
        backBtn.setBackground(null);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            regStatusLabel.setText(" ");
            cardLayout.show(getContentPane(), "main");
        });
        panel.add(backBtn);

        JButton closeBtn = new JButton("X");
        closeBtn.setBounds(430, 10, 30, 30);
        closeBtn.setFont(new Font("Consolas", Font.BOLD, 12));
        closeBtn.setForeground(textDim());
        closeBtn.setBackground(null);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> System.exit(0));
        panel.add(closeBtn);

        return panel;
    }

    private JPanel createLoadingPanel() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.bg2);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(false);

        JLabel title = new JLabel("zagaDLC", SwingConstants.CENTER);
        title.setBounds(0, 150, 480, 50);
        title.setFont(new Font("Consolas", Font.BOLD, 36));
        title.setForeground(text());
        panel.add(title);

        JLabel uidLabel = new JLabel("", SwingConstants.CENTER);
        uidLabel.setBounds(0, 200, 480, 25);
        uidLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        uidLabel.setForeground(currentTheme.accent);
        if (Config.get().uid != null) {
            uidLabel.setText("UID: " + Config.get().uid);
        }
        panel.add(uidLabel);

        loadingProgressBar = new JProgressBar(0, 100);
        loadingProgressBar.setBounds(80, 260, 320, 4);
        loadingProgressBar.setBackground(fieldBg());
        loadingProgressBar.setForeground(text());
        loadingProgressBar.setBorderPainted(false);
        loadingProgressBar.setVisible(false);
        panel.add(loadingProgressBar);

        loadingStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        loadingStatusLabel.setBounds(80, 280, 320, 20);
        loadingStatusLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        loadingStatusLabel.setForeground(textDim());
        panel.add(loadingStatusLabel);

        return panel;
    }

    private JTextField createStyledField(String placeholder, int y) {
        JTextField field = new JTextField() {
            private String ph = placeholder;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(textDim());
                    g2.setFont(getFont());
                    Insets insets = getInsets();
                    g2.drawString(ph, insets.left + 5, getHeight() / 2 + 5);
                }
            }
        };
        field.setBounds(80, y, 320, 40);
        field.setBackground(fieldBg());
        field.setForeground(text());
        field.setCaretColor(text());
        field.setFont(new Font("Consolas", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fieldBorder()),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        field.setOpaque(true);
        allTextFields.add(field);
        return field;
    }

    private JPasswordField createStyledPasswordField(String placeholder, int y) {
        JPasswordField field = new JPasswordField() {
            private String ph = placeholder;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(textDim());
                    g2.setFont(getFont());
                    Insets insets = getInsets();
                    g2.drawString(ph, insets.left + 5, getHeight() / 2 + 5);
                }
            }
        };
        field.setBounds(80, y, 320, 40);
        field.setBackground(fieldBg());
        field.setForeground(text());
        field.setCaretColor(text());
        field.setFont(new Font("Consolas", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fieldBorder()),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        field.setOpaque(true);
        allTextFields.add(field);
        return field;
    }

    private JButton createStyledButton(String text) {
        return createStyledButton(text, 320, 44);
    }

    private JButton createStyledButton(String text, int w, int h) {
        JButton btn = new JButton(text) {
            boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                float pulse = (float)(Math.sin(pulsePhase) * 0.5 + 0.5);
                Color accent = currentTheme.accent;

                if (getModel().isPressed()) {
                    g2.setColor(currentTheme.buttonPressed);
                } else if (hover) {
                    g2.setColor(currentTheme.buttonHover);
                } else {
                    g2.setColor(currentTheme.buttonBg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                int borderAlpha = (int)(80 + pulse * 100);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.min(borderAlpha, 255)));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                if (pulse > 0.3f) {
                    int glowAlpha = (int)((pulse - 0.3f) * 80);
                    if (glowAlpha > 0) {
                        for (int i = 1; i <= 3; i++) {
                            float spread = i * 1.5f;
                            int ga = glowAlpha / (i + 1);
                            if (ga > 0) {
                                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.min(ga, 255)));
                                g2.setStroke(new BasicStroke(spread));
                                g2.drawRoundRect(-i, -i, getWidth() + i * 2 - 1, getHeight() + i * 2 - 1, 8 + i * 2, 8 + i * 2);
                            }
                        }
                    }
                }

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(text());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                Rectangle2D r = fm.getStringBounds(getText(), g2);
                int tx = (getWidth() - (int) r.getWidth()) / 2;
                int ty = (getHeight() + (int) r.getHeight()) / 2 - fm.getAscent() + 2;
                g2.drawString(getText(), tx, ty);
            }
        };
        btn.setPreferredSize(new Dimension(w, h));
        btn.setMaximumSize(new Dimension(w, h));
        btn.setMinimumSize(new Dimension(w, h));
        btn.setFont(new Font("Consolas", Font.BOLD, 14));
        btn.setForeground(text());
        btn.setBackground(null);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ==================== TYPEWRITER ====================

    class TypewriterLabel extends JPanel {
        private final String fullText;
        private int charIndex = 0;
        private boolean typingDone = false;
        private boolean cursorVisible = true;
        private final Font font = new Font("Consolas", Font.BOLD, 30);
        private final javax.swing.Timer typeTimer;
        private final javax.swing.Timer cursorTimer;

        TypewriterLabel(String text) {
            this.fullText = text;
            setOpaque(false);
            typeTimer = new javax.swing.Timer(120, e -> {
                if (charIndex < fullText.length()) {
                    charIndex++;
                    repaint();
                } else {
                    typingDone = true;
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            });
            cursorTimer = new javax.swing.Timer(500, e -> {
                cursorVisible = !cursorVisible;
                repaint();
            });
            typeTimer.start();
            cursorTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(font);
            String typed = fullText.substring(0, charIndex);
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(fullText);
            int x = (getWidth() - textW) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - 4;

            float pulse = (float)(Math.sin(pulsePhase) * 0.5 + 0.5);
            Color pulseCol = currentTheme.pulseColor;

            if (typingDone) {
                int r = (int)(pulseCol.getRed() * (0.65f + pulse * 0.35f));
                int gr = (int)(pulseCol.getGreen() * (0.65f + pulse * 0.35f));
                int b = (int)(pulseCol.getBlue() * (0.65f + pulse * 0.35f));
                g2.setColor(new Color(Math.min(255, r), Math.min(255, gr), Math.min(255, b)));

                float glowSize = 4f + pulse * 6f;
                for (int layer = 3; layer >= 1; layer--) {
                    float spread = layer * glowSize * 0.4f;
                    int glowAlpha = (int)(pulse * 60 * (1.0f - (float) layer / 4));
                    if (glowAlpha > 0) {
                        Font glowFont = font.deriveFont(font.getSize() + spread);
                        g2.setFont(glowFont);
                        FontMetrics gFm = g2.getFontMetrics();
                        int scaledW = gFm.stringWidth(fullText);
                        int offsetX = x + (textW - scaledW) / 2;
                        g2.setColor(new Color(pulseCol.getRed(), pulseCol.getGreen(), pulseCol.getBlue(), Math.min(glowAlpha, 255)));
                        g2.drawString(fullText, offsetX, y);
                    }
                }

                g2.setFont(font);
                g2.setColor(new Color(pulseCol.getRed(), pulseCol.getGreen(), pulseCol.getBlue()));
                g2.drawString(fullText, x, y);
            } else {
                g2.setColor(text());
                g2.drawString(typed, x, y);
            }

            if (!typingDone && cursorVisible) {
                int cursorX = x + fm.stringWidth(typed);
                g2.fillRect(cursorX + 2, y - fm.getAscent() + 4, 2, fm.getAscent() - 2);
            }
        }
    }

    // ==================== MOUSE TRAIL ====================

    class MouseTrail extends JPanel implements ActionListener {
        private final java.util.List<int[]> points = new java.util.ArrayList<>();
        private static final int MAX_POINTS = 40;

        MouseTrail() {
            setOpaque(false);
            setDoubleBuffered(false);
            javax.swing.Timer timer = new javax.swing.Timer(16, this);
            timer.start();
        }

        void addPoint(Point p) {
            points.add(new int[]{p.x, p.y, 255});
            if (points.size() > MAX_POINTS) {
                points.remove(0);
            }
            repaint();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean changed = false;
            java.util.Iterator<int[]> it = points.iterator();
            while (it.hasNext()) {
                int[] p = it.next();
                p[2] -= 12;
                if (p[2] <= 0) {
                    it.remove();
                    changed = true;
                } else {
                    changed = true;
                }
            }
            if (changed) repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < points.size(); i++) {
                int[] p = points.get(i);
                int alpha = Math.max(0, Math.min(255, p[2]));
                if (alpha <= 0) continue;
                float progress = (float) i / points.size();
                float size = 2f + progress * 5f;
                g2.setColor(new Color(255, 255, 255, alpha));
                g2.fillOval(p[0] - (int) size / 2, p[1] - (int) size / 2, (int) size, (int) size);
            }
        }
    }

    // ==================== PARTICLES ====================

    class Particles extends JPanel {
        private static final int MAX_PARTICLES = 75;
        private final java.util.List<double[]> particles = new java.util.ArrayList<>();
        private int mouseX = -100, mouseY = -100;

        Particles() {
            setOpaque(false);
            setDoubleBuffered(false);
            for (int i = 0; i < MAX_PARTICLES; i++) {
                particles.add(createParticle(true));
            }
            javax.swing.Timer timer = new javax.swing.Timer(40, e -> {
                for (double[] p : particles) {
                    p[0] += p[2];
                    p[1] += p[3];
                    if (p[6] < 1.0) p[6] += 0.02;
                    p[4] -= 0.002;
                    if (p[4] <= 0 || p[1] < -10 || p[0] < -10 || p[0] > 490 || p[1] > 510) {
                        double[] np = createParticle(false);
                        p[0] = np[0]; p[1] = np[1]; p[2] = np[2]; p[3] = np[3];
                        p[4] = np[4]; p[5] = np[5]; p[6] = 0.0;
                    }
                    double dx = p[0] - mouseX;
                    double dy = p[1] - mouseY;
                    double distSq = dx * dx + dy * dy;
                    if (distSq < 6400 && distSq > 0) {
                        double dist = Math.sqrt(distSq);
                        double force = (80 - dist) / 80.0 * 0.25;
                        p[0] += (dx / dist) * force;
                        p[1] += (dy / dist) * force;
                    }
                }
                repaint();
            });
            timer.start();
        }

        private double[] createParticle(boolean randomY) {
            double x = Math.random() * 460 + 10;
            double y = Math.random() * 500;
            double vx = (Math.random() - 0.5) * 0.4;
            double vy = -(Math.random() * 0.3 + 0.1);
            double alpha = Math.random() * 0.5 + 0.2;
            double size = Math.random() * 2 + 2;
            double fadeIn = randomY ? 1.0 : 0.0;
            return new double[]{x, y, vx, vy, alpha, size, fadeIn};
        }

        void updateMouse(int x, int y) {
            mouseX = x;
            mouseY = y;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color accent = currentTheme.pulseColor;
            for (double[] p : particles) {
                double a = p[4] * p[6];
                int alpha = (int)(a * 255);
                if (alpha <= 0 || alpha > 255) continue;
                int sz = Math.max(2, (int) p[5]);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
                g2.fillOval((int) p[0] - sz / 2, (int) p[1] - sz / 2, sz, sz);
            }
        }
    }

    // ==================== QUOTE LABEL ====================

    class QuoteLabel extends JPanel {
        private final String[] quotes = {
            "твой любимый клиент",
            "не тормозит — это про нас",
            "элитры не нужны — мы и так летаем",
            "от читеров для читеров",
            "Bypa$$ в крови",
            "правила написаны не для нас",
            "ансофты плачут — мы смеёмся",
            "античит — наша игрушка",
            "банлист? Мы в него не вписываемся"
        };
        private String currentQuote = "";
        private float alpha = 0f;
        private boolean fadingIn = false;
        private boolean fadingOut = false;
        private final Font font = new Font("Consolas", Font.ITALIC, 17);

        QuoteLabel() {
            setOpaque(false);
            javax.swing.Timer timer = new javax.swing.Timer(50, e -> tick());
            timer.start();
            javax.swing.Timer showTimer = new javax.swing.Timer(4000 + new Random().nextInt(3000), e -> {
                if (!fadingIn && !fadingOut && alpha < 0.01f) {
                    currentQuote = quotes[new Random().nextInt(quotes.length)];
                    fadingIn = true;
                    fadingOut = false;
                }
            });
            showTimer.setInitialDelay(2000);
            showTimer.start();
        }

        private void tick() {
            if (fadingIn) {
                alpha += 0.03f;
                if (alpha >= 1f) {
                    alpha = 1f;
                    fadingIn = false;
                    javax.swing.Timer delay = new javax.swing.Timer(2500 + new Random().nextInt(1500), e -> {
                        fadingOut = true;
                        ((javax.swing.Timer) e.getSource()).stop();
                    });
                    delay.setRepeats(false);
                    delay.start();
                }
                repaint();
            } else if (fadingOut) {
                alpha -= 0.02f;
                if (alpha <= 0f) {
                    alpha = 0f;
                    fadingOut = false;
                }
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (alpha < 0.01f || currentQuote.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(currentQuote);
            int startX = (getWidth() - textW) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - 3;

            int curX = startX;
            for (int i = 0; i < currentQuote.length(); i++) {
                char ch = currentQuote.charAt(i);
                String chStr = String.valueOf(ch);
                int charW = fm.charWidth(ch);

                int glowAlpha = (int)(alpha * 80);
                if (glowAlpha > 0) {
                    for (int layer = 3; layer >= 1; layer--) {
                        float spread = layer * 2f;
                        int la = (int)(glowAlpha * (1.0f - (float) layer / 4));
                        if (la > 0) {
                            Font glowFont = font.deriveFont(font.getSize() + spread);
                            g2.setFont(glowFont);
                            FontMetrics gFm = g2.getFontMetrics();
                            int scaledW = gFm.charWidth(ch);
                            int offsetX = curX + (charW - scaledW) / 2;
                            g2.setColor(new Color(255, 255, 255, Math.min(la, 255)));
                            g2.drawString(chStr, offsetX, y);
                        }
                    }
                }

                g2.setFont(font);
                g2.setColor(new Color(255, 255, 255, (int)(alpha * 220)));
                g2.drawString(chStr, curX, y);

                curX += charW;
            }
        }
    }

    // ==================== AUTH ====================

    private void doLogin() {
        synchronized (loadingLock) {
            if (loading) return;
            loading = true;
        }
        String login = loginField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (login.isEmpty() || password.isEmpty()) {
            loginStatusLabel.setText("Fill in all fields");
            loginStatusLabel.setForeground(error());
            synchronized (loadingLock) { loading = false; }
            return;
        }
        loginButton.setEnabled(false);
        loginButton.setText("LOADING...");
        loginStatusLabel.setText("Connecting...");
        loginStatusLabel.setForeground(textDim());

        new Thread(() -> {
            try {
                var result = ApiClient.login(login, password);
                if (result.has("success") && result.get("success").getAsBoolean() && result.has("token")) {
                    String token = result.get("token").getAsString();
                    Config.get().token = token;
                    Config.get().login = login;
                    if (result.has("key") && !result.get("key").isJsonNull()) {
                        Config.get().jarKey = result.get("key").getAsString();
                    }
                    if (result.has("user") && result.getAsJsonObject("user").has("uid")) {
                        Config.get().uid = result.getAsJsonObject("user").get("uid").getAsString();
                    }
                    Config.save();
                    SwingUtilities.invokeLater(() -> {
                        loginStatusLabel.setText(" ");
                        cardLayout.show(getContentPane(), "loading");
                    });
                    installAndLaunch();
                } else {
                    String errorMsg = result.has("ERROR()") ? result.get("ERROR()").getAsString() : "Login failed";
                    setStatus(loginStatusLabel, errorMsg, error());
                }
            } catch (Exception e) {
                setStatus(loginStatusLabel, "Error: " + e.getMessage(), error());
            } finally {
                loading = false;
                SwingUtilities.invokeLater(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("LOGIN");
                });
            }
        }).start();
    }

    private void doRegister() {
        synchronized (loadingLock) {
            if (loading) return;
            loading = true;
        }
        String login = regLoginField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirm = new String(regConfirmField.getPassword());
        if (login.isEmpty() || password.isEmpty()) {
            regStatusLabel.setText("Fill in all fields");
            regStatusLabel.setForeground(error());
            synchronized (loadingLock) { loading = false; }
            return;
        }
        if (!password.equals(confirm)) {
            regStatusLabel.setText("Passwords don't match");
            regStatusLabel.setForeground(error());
            synchronized (loadingLock) { loading = false; }
            return;
        }
        registerButton.setEnabled(false);
        registerButton.setText("LOADING...");
        regStatusLabel.setText("Connecting...");
        regStatusLabel.setForeground(textDim());

        new Thread(() -> {
            try {
                var result = ApiClient.register(login, password);
                if (result.has("success") && result.get("success").getAsBoolean()) {
                    Config.get().login = login;
                    if (result.has("uid")) {
                        Config.get().uid = result.get("uid").getAsString();
                    }
                    Config.save();
                    setStatus(regStatusLabel, "Registered! Now login.", green());
                    SwingUtilities.invokeLater(() -> {
                        cardLayout.show(getContentPane(), "login");
                    });
                } else {
                    String errorMsg = result.has("ERROR()") ? result.get("ERROR()").getAsString() : "Registration failed";
                    setStatus(regStatusLabel, errorMsg, error());
                }
            } catch (Exception e) {
                setStatus(regStatusLabel, "Error: " + e.getMessage(), error());
            } finally {
                loading = false;
                SwingUtilities.invokeLater(() -> {
                    registerButton.setEnabled(true);
                    registerButton.setText("REGISTER");
                });
            }
        }).start();
    }

    // ==================== LAUNCH ====================

    private void installAndLaunch() {
        Thread launchThread = new Thread(() -> {
            try {
                if (zaga.utils.AntiDebug.isDebuggerDetected()) {
                    setStatus(loadingStatusLabel, zaga.utils.AntiDebug.getBlockedMessage(), error());
                    return;
                }

                if (!zaga.utils.JarIntegrity.verifyJarIntegrity()) {
                    setStatus(loadingStatusLabel, "JAR integrity check failed. Reinstall required.", error());
                    return;
                }

                File lockFile = new File(mcDir, "game.lock");
                if (lockFile.exists()) {
                    try {
                        long lockPid = Long.parseLong(Files.readString(lockFile.toPath()).trim());
                        ProcessHandle handle = ProcessHandle.of(lockPid).orElse(null);
                        if (handle != null && handle.isAlive()) {
                            setStatus(loadingStatusLabel, "Minecraft is already running!", error());
                            return;
                        }
                    } catch (Exception ignored) {}
                    lockFile.delete();
                }

                File javaDir = new File(mcDir, "java");
                String javaExe = ensureJava(javaDir);

                File versionsDir = new File(baseDir, "versions");
                File librariesDir = new File(baseDir, "libraries");
                File assetsDir = new File(baseDir, "assets");

                installMinecraft(versionsDir, librariesDir, assetsDir);
                installFabric(versionsDir, librariesDir);
                installMod(versionsDir);

                setStatus(loadingStatusLabel, "Setting language...", textDim());
                setLanguage(baseDir);

                setStatus(loadingStatusLabel, "Launching Minecraft...", text());
                launchGame(javaExe, versionsDir, librariesDir, assetsDir);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "zagaDLC loaded!\nGame is starting...",
                            "zagaDLC",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                });

            } catch (Exception e) {
                setStatus(loadingStatusLabel, "Error: " + e.getMessage(), error());
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                String fullTrace = sw.toString();
                System.err.println("[LOADER ERROR] " + fullTrace);
                try (java.io.FileWriter fw = new java.io.FileWriter(new File(System.getProperty("user.home"), "zagaDLC-error.txt"))) {
                    fw.write("LOCALAPPDATA=" + System.getenv("LOCALAPPDATA") + "\n");
                    fw.write("mcDir=" + mcDir.getAbsolutePath() + "\n");
                    fw.write("baseDir=" + baseDir.getAbsolutePath() + "\n");
                    fw.write("java.zip exists=" + new File(mcDir, "java.zip").exists() + "\n");
                    fw.write("java.zip size=" + new File(mcDir, "java.zip").length() + "\n\n");
                    fw.write(fullTrace);
                } catch (Exception ignored) {}
            }
        });
        launchThread.setName("zagaDLC-Launcher");
        launchThread.setDaemon(true);
        launchThread.start();
    }

    private String ensureJava(File javaDir) throws Exception {
        File javaExe = findJavaExe(javaDir);
        if (javaExe != null) {
            setProgress(100);
            return javaExe.getAbsolutePath();
        }

        mcDir.mkdirs();
        baseDir.mkdirs();

        setStatus(loadingStatusLabel, "Downloading Java...", textDim());
        setProgress(0);

        String url = "https://download.java.net/java/GA/jdk21.0.2/f2283984656d49d69e91c558476027ac/13/GPL/openjdk-21.0.2_windows-x64_bin.zip";

        File zipFile = new File(mcDir, "java.zip");
        if (!zipFile.exists() || zipFile.length() < 180_000_000) {
            download(url, zipFile, 0, 50);
        } else {
            setStatus(loadingStatusLabel, "Java zip exists, extracting...", textDim());
            setProgress(0);
        }

        setStatus(loadingStatusLabel, "Extracting Java...", textDim());
        setProgress(50);
        extractZip(zipFile, javaDir, 50, 95);
        zipFile.delete();

        setProgress(100);

        javaExe = findJavaExe(javaDir);
        if (javaExe == null) throw new Exception("Java extraction failed");
        return javaExe.getAbsolutePath();
    }

    private File findJavaExe(File dir) {
        File bin = new File(dir, "bin\\java.exe");
        if (bin.exists()) return bin;

        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    File found = findJavaExe(child);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private void installMinecraft(File versionsDir, File librariesDir, File assetsDir) throws Exception {
        File versionDir = new File(versionsDir, VERSION);
        File versionJar = new File(versionDir, VERSION + ".json");
        File versionClientJar = new File(versionDir, VERSION + ".jar");

        String versionJson;
        com.google.gson.JsonObject versionData;

        if (versionJar.exists() && versionClientJar.exists() && versionClientJar.length() > 0) {
            String existingJson = Files.readString(versionJar.toPath());
            versionData = com.google.gson.JsonParser.parseString(existingJson).getAsJsonObject();
        } else {
            setStatus(loadingStatusLabel, "Downloading Minecraft " + VERSION + "...", textDim());
            setProgress(0);

            String manifestJson = downloadString(MOJANG_MANIFEST);
            com.google.gson.JsonObject manifest = com.google.gson.JsonParser.parseString(manifestJson).getAsJsonObject();
            com.google.gson.JsonArray versions = manifest.getAsJsonArray("versions");

            String versionUrl = null;
            for (int i = 0; i < versions.size(); i++) {
                com.google.gson.JsonObject v = versions.get(i).getAsJsonObject();
                if (v.get("id").getAsString().equals(VERSION)) {
                    versionUrl = v.get("url").getAsString();
                    break;
                }
            }
            if (versionUrl == null) throw new Exception("Version " + VERSION + " not found in manifest");

            versionJson = downloadString(versionUrl);
            versionData = com.google.gson.JsonParser.parseString(versionJson).getAsJsonObject();

            versionDir.mkdirs();
            Files.writeString(versionJar.toPath(), versionJson);

            com.google.gson.JsonObject client = versionData.getAsJsonObject("downloads").getAsJsonObject("client");
            String jarUrl = client.get("url").getAsString();
            download(jarUrl, new File(versionDir, VERSION + ".jar"), 0, 40);

            setStatus(loadingStatusLabel, "Downloading libraries...", textDim());
            librariesDir.mkdirs();
            downloadLibraries(versionData.getAsJsonArray("libraries"), librariesDir, 40, 75);
        }

        setStatus(loadingStatusLabel, "Downloading assets...", textDim());
        assetsDir.mkdirs();
        downloadAssets(versionData, assetsDir, 75, 100);
    }

    private void downloadLibraries(com.google.gson.JsonArray libraries, File librariesDir, int startPct, int endPct) throws Exception {
        java.util.List<String[]> toDownload = new java.util.ArrayList<>();

        int total = libraries.size();
        for (int i = 0; i < total; i++) {
            com.google.gson.JsonObject lib = libraries.get(i).getAsJsonObject();
            if (!lib.has("downloads")) continue;

            com.google.gson.JsonObject downloads = lib.getAsJsonObject("downloads");
            if (!downloads.has("artifact")) continue;

            com.google.gson.JsonObject artifact = downloads.getAsJsonObject("artifact");
            String path = artifact.get("path").getAsString();
            String url = artifact.get("url").getAsString();

            File libFile = new File(librariesDir, path);
            if (!libFile.exists()) {
                toDownload.add(new String[]{url, libFile.getAbsolutePath()});
            }
        }

        if (toDownload.isEmpty()) return;

        int count = toDownload.size();
        int threads = 8;
        java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            int start = t * count / threads;
            int end = (t + 1) * count / threads;
            new Thread(() -> {
                for (int i = start; i < end; i++) {
                    String[] item = toDownload.get(i);
                    try {
                        File f = new File(item[1]);
                        f.getParentFile().mkdirs();
                        download(item[0], f, 0, 0);
                    } catch (Exception e) {
                        System.err.println("[WARN] Failed to download library: " + item[0] + " - " + e.getMessage());
                    }
                    int current = done.incrementAndGet();
                    int pct = startPct + (int)((double) current / count * (endPct - startPct));
                    setProgress(pct);
                }
                latch.countDown();
            }).start();
        }

        latch.await(30, java.util.concurrent.TimeUnit.MINUTES);
    }

    private void downloadAssets(com.google.gson.JsonObject versionData, File assetsDir, int startPct, int endPct) throws Exception {
        if (!versionData.has("assetIndex")) return;
        com.google.gson.JsonObject assetIndex = versionData.getAsJsonObject("assetIndex");
        String assetUrl = assetIndex.get("url").getAsString();
        String assetId = assetIndex.get("id").getAsString();

        File indexesDir = new File(assetsDir, "indexes");
        indexesDir.mkdirs();
        File indexFile = new File(indexesDir, assetId + ".json");
        if (!indexFile.exists()) {
            download(assetUrl, indexFile, startPct, startPct + 5);
        }

        String indexJson = Files.readString(indexFile.toPath());
        com.google.gson.JsonObject index = com.google.gson.JsonParser.parseString(indexJson).getAsJsonObject();
        com.google.gson.JsonObject objects = index.getAsJsonObject("objects");

        java.util.List<String[]> toDownload = new java.util.ArrayList<>();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : objects.entrySet()) {
            com.google.gson.JsonObject obj = entry.getValue().getAsJsonObject();
            String hashStr = obj.get("hash").getAsString();
            String first2 = hashStr.substring(0, 2);

            File objFile = new File(assetsDir, "objects" + File.separator + first2 + File.separator + hashStr);
            if (!objFile.exists()) {
                String objUrl = "https://resources.download.minecraft.net/" + first2 + "/" + hashStr;
                toDownload.add(new String[]{objUrl, objFile.getAbsolutePath()});
            }
        }

        if (toDownload.isEmpty()) {
            setProgress(endPct);
            return;
        }

        int total = toDownload.size();
        int threads = 16;
        java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threads);

        setStatus(loadingStatusLabel, "Downloading assets: 0/" + total + "...", textDim());

        for (int t = 0; t < threads; t++) {
            int start = t * total / threads;
            int end = (t + 1) * total / threads;
            new Thread(() -> {
                for (int i = start; i < end; i++) {
                    String[] item = toDownload.get(i);
                    try {
                        download(item[0], new File(item[1]), 0, 0);
                    } catch (Exception e) {
                        System.err.println("[WARN] Failed to download asset: " + item[0] + " - " + e.getMessage());
                    }
                    int current = done.incrementAndGet();
                    if (current % 50 == 0 || current == total) {
                        int pct = startPct + (int)((double) current / total * (endPct - startPct));
                        setProgress(pct);
                        setStatus(loadingStatusLabel, "Downloading assets: " + current + "/" + total + "...", textDim());
                    }
                }
                latch.countDown();
            }).start();
        }

        boolean finished = latch.await(30, java.util.concurrent.TimeUnit.MINUTES);
        if (!finished) {
            setStatus(loadingStatusLabel, "Download timed out, continuing...", error());
        }
        setProgress(endPct);
    }

    private void installFabric(File versionsDir, File librariesDir) throws Exception {
        String fabricProfileName = VERSION + "-fabric-" + FABRIC_VERSION;
        File fabricVersionDir = new File(versionsDir, fabricProfileName);
        File fabricJar = new File(fabricVersionDir, fabricProfileName + ".jar");

        setStatus(loadingStatusLabel, "Installing Fabric Loader...", textDim());
        setProgress(0);

        String loaderUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/" + FABRIC_VERSION + "/fabric-loader-" + FABRIC_VERSION + ".jar";

        File fabricLoaderJar = new File(librariesDir, "net/fabricmc/fabric-loader/" + FABRIC_VERSION + "/fabric-loader-" + FABRIC_VERSION + ".jar");
        fabricLoaderJar.getParentFile().mkdirs();
        if (!fabricLoaderJar.exists()) {
            download(loaderUrl, fabricLoaderJar, 10, 50);
        }

        setStatus(loadingStatusLabel, "Downloading Fabric libraries...", textDim());

        String installJsonUrl = "https://meta.fabricmc.net/v2/versions/loader/" + VERSION + "/" + FABRIC_VERSION + "/profile/json";
        String installJson = downloadString(installJsonUrl);
        com.google.gson.JsonObject installData = com.google.gson.JsonParser.parseString(installJson).getAsJsonObject();

        if (installData.has("libraries")) {
            com.google.gson.JsonArray fabricLibs = installData.getAsJsonArray("libraries");
            int total = fabricLibs.size();

            java.util.List<String[]> fabricToDownload = new java.util.ArrayList<>();
            for (int i = 0; i < total; i++) {
                com.google.gson.JsonObject lib = fabricLibs.get(i).getAsJsonObject();
                String name = lib.get("name").getAsString();
                String url = lib.has("url") ? lib.get("url").getAsString() : "https://maven.fabricmc.net/";

                String[] parts = name.split(":");
                if (parts.length >= 3) {
                    String groupPath = parts[0].replace('.', '/');
                    String artifact = parts[1];
                    String ver = parts[2];
                    String jarName = artifact + "-" + ver + ".jar";
                    String fullPath = groupPath + "/" + artifact + "/" + ver + "/" + jarName;

                    File libFile = new File(librariesDir, fullPath);
                    if (libFile.exists() && libFile.length() > 0) continue;
                    if (libFile.exists()) libFile.delete();
                    libFile.getParentFile().mkdirs();
                    String fullUrl = url.endsWith("/") ? url + fullPath : url + "/" + fullPath;
                    fabricToDownload.add(new String[]{fullUrl, libFile.getAbsolutePath()});
                }
            }

            if (!fabricToDownload.isEmpty()) {
                int fCount = fabricToDownload.size();
                int fThreads = 8;
                java.util.concurrent.atomic.AtomicInteger fDone = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.concurrent.CountDownLatch fLatch = new java.util.concurrent.CountDownLatch(fThreads);

                for (int t = 0; t < fThreads; t++) {
                    int start = t * fCount / fThreads;
                    int end = (t + 1) * fCount / fThreads;
                    new Thread(() -> {
                        for (int i = start; i < end; i++) {
                            String[] item = fabricToDownload.get(i);
                            try {
                                download(item[0], new File(item[1]), 0, 0);
                            } catch (Exception e) {
                                System.err.println("[WARN] Failed to download Fabric lib: " + item[0] + " - " + e.getMessage());
                            }
                            int current = fDone.incrementAndGet();
                            int pct = 50 + (int)((double) current / fCount * 40);
                            setProgress(pct);
                        }
                        fLatch.countDown();
                    }).start();
                }
                fLatch.await(10, java.util.concurrent.TimeUnit.MINUTES);
            }

            java.util.List<String> missingLibs = new java.util.ArrayList<>();
            for (int i = 0; i < fabricLibs.size(); i++) {
                com.google.gson.JsonObject lib = fabricLibs.get(i).getAsJsonObject();
                String name = lib.get("name").getAsString();
                String[] parts = name.split(":");
                if (parts.length >= 3) {
                    String groupPath = parts[0].replace('.', '/');
                    String artifact = parts[1];
                    String ver = parts[2];
                    String jarName = artifact + "-" + ver + ".jar";
                    String fullPath = groupPath + "/" + artifact + "/" + ver + "/" + jarName;
                    File libFile = new File(librariesDir, fullPath);
                    if (!libFile.exists() || libFile.length() == 0) {
                        missingLibs.add(name);
                    }
                }
            }
            if (!missingLibs.isEmpty()) {
                throw new Exception("Failed to download Fabric libraries: " + String.join(", ", missingLibs));
            }
        }

        if (!fabricJar.exists()) {
            fabricVersionDir.mkdirs();
            File originalJar = new File(versionsDir, VERSION + File.separator + VERSION + ".jar");
            if (originalJar.exists()) {
                Files.copy(originalJar.toPath(), fabricJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        installData.addProperty("id", fabricProfileName);
        String profileJson = installData.toString();
        Files.writeString(new File(fabricVersionDir, fabricProfileName + ".json").toPath(), profileJson);

        setProgress(100);
    }

    private void installMod(File versionsDir) throws Exception {
        File modsDir = new File(baseDir, "mods");
        if (!modsDir.mkdirs() && !modsDir.exists()) {
            throw new Exception("Failed to create mods directory: " + modsDir.getAbsolutePath());
        }

        installFabricApi(modsDir);
        installAccountSwitcher(modsDir);
        installBaritone(modsDir);
        installSodium(modsDir);
        installLithium(modsDir);
        installEntityCulling(modsDir);
        installFerriteCore(modsDir);
        installRockstarMod(modsDir);
    }

    private void installFabricApi(File modsDir) throws Exception {
        File fabricApiFile = new File(modsDir, "fabric-api-0.110.5+1.21.4.jar");
        if (fabricApiFile.exists()) return;

        setStatus(loadingStatusLabel, "Installing Fabric API...", textDim());
        setProgress(0);

        String fabricApiUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.110.5+1.21.4/fabric-api-0.110.5+1.21.4.jar";
        download(fabricApiUrl, fabricApiFile, 0, 50);

        setStatus(loadingStatusLabel, "Fabric API installed!", green());
        setProgress(50);
    }

    private void installAccountSwitcher(File modsDir) throws Exception {
        File iasFile = new File(modsDir, "IAS-9.0.7+1.21.4-fabric.jar");
        if (iasFile.exists()) return;

        setStatus(loadingStatusLabel, "Installing Account Switcher...", textDim());
        setProgress(0);

        String iasUrl = "https://cdn.modrinth.com/data/cudtvDnd/versions/Ks4d60aS/IAS-9.0.7%2B1.21.4-fabric.jar";
        download(iasUrl, iasFile, 0, 50);

        setStatus(loadingStatusLabel, "Account Switcher installed!", green());
        setProgress(50);
    }

    private void installBaritone(File modsDir) throws Exception {
        File baritoneFile = new File(modsDir, "baritone-api-fabric-1.13.1.jar");
        if (baritoneFile.exists()) return;

        setStatus(loadingStatusLabel, "Installing Baritone...", textDim());
        setProgress(0);

        try (InputStream baritoneStream = getClass().getResourceAsStream("/baritone-api-fabric-1.13.1.jar")) {
            if (baritoneStream == null) {
                setStatus(loadingStatusLabel, "Baritone not bundled.", textDim());
                return;
            }
            byte[] baritoneBytes = baritoneStream.readAllBytes();
            java.nio.file.Files.write(baritoneFile.toPath(), baritoneBytes);
        }

        setStatus(loadingStatusLabel, "Baritone installed!", green());
        setProgress(100);
    }

    private void installSodium(File modsDir) throws Exception {
        File sodiumFile = new File(modsDir, "sodium-fabric-0.6.13+mc1.21.4.jar");
        if (sodiumFile.exists()) return;

        setStatus(loadingStatusLabel, "Installing Sodium...", textDim());
        setProgress(0);

        String url = "https://cdn.modrinth.com/data/AANobbMI/versions/c3YkZvne/sodium-fabric-0.6.13+mc1.21.4.jar";
        download(url, sodiumFile, 0, 100);

        setStatus(loadingStatusLabel, "Sodium installed!", green());
        setProgress(100);
    }

    private void installLithium(File modsDir) throws Exception {
        File lithiumFile = new File(modsDir, "lithium-fabric-0.14.7+mc1.21.4.jar");
        if (lithiumFile.exists()) return;

        setStatus(loadingStatusLabel, "Installing Lithium...", textDim());
        setProgress(0);

        String url = "https://cdn.modrinth.com/data/gvQqBUqZ/versions/QCuodIia/lithium-fabric-0.14.7+mc1.21.4.jar";
        download(url, lithiumFile, 0, 100);

        setStatus(loadingStatusLabel, "Lithium installed!", green());
        setProgress(100);
    }

    private void installEntityCulling(File modsDir) throws Exception {
        File ecFile = new File(modsDir, "entityculling-fabric-1.9.3-mc1.21.4.jar");
        if (ecFile.exists()) return;

        setStatus(loadingStatusLabel, "Installing EntityCulling...", textDim());
        setProgress(0);

        String url = "https://cdn.modrinth.com/data/NNAgCjsB/versions/VvberZFK/entityculling-fabric-1.9.3-mc1.21.4.jar";
        download(url, ecFile, 0, 100);

        setStatus(loadingStatusLabel, "EntityCulling installed!", green());
        setProgress(100);
    }

    private void installFerriteCore(File modsDir) throws Exception {
        File fcFile = new File(modsDir, "ferritecore-7.1.3-fabric.jar");
        if (fcFile.exists()) return;

        setStatus(loadingStatusLabel, "Installing FerriteCore...", textDim());
        setProgress(0);

        String url = "https://cdn.modrinth.com/data/uXXizFIs/versions/7KqeXPRS/ferritecore-7.1.3-fabric.jar";
        download(url, fcFile, 0, 100);

        setStatus(loadingStatusLabel, "FerriteCore installed!", green());
        setProgress(100);
    }

    private void installRockstarMod(File modsDir) throws Exception {
        File modFile = new File(modsDir, "rockstar-1.0.0.jar");

        setStatus(loadingStatusLabel, "Decrypting zagaDLC client...", textDim());
        setProgress(0);

        byte[] encBytes;
        try (InputStream encStream = getClass().getResourceAsStream("/rockstar-1.0.0.jar.encrypted")) {
            if (encStream == null) {
                throw new Exception("Client JAR not bundled in loader. Rebuild loader.");
            }
            encBytes = encStream.readAllBytes();
        }

        if (encBytes.length < 28) {
            throw new Exception("Encrypted client file is corrupted (too small: " + encBytes.length + " bytes). Re-encrypt the mod JAR.");
        }

        String encKey = Config.get().jarKey;
        if (encKey == null || encKey.isEmpty()) {
            String token = Config.get().token;
            if (token != null) {
                try {
                    encKey = zaga.api.ApiClient.fetchKey(token);
                } catch (Exception e) {
                    System.err.println("[WARN] Failed to fetch key from API: " + e.getMessage());
                }
            }
        }
        if (encKey == null || encKey.isEmpty()) {
            throw new Exception("No decryption key. Please login again.");
        }

        try {
            zaga.crypto.JarDecryptor decryptor = new zaga.crypto.JarDecryptor(encKey);
            decryptor.decrypt(new ByteArrayInputStream(encBytes), modFile.toPath());
        } catch (Exception e) {
            if (modFile.exists()) modFile.delete();
            String msg = e.getMessage();
            if (msg != null && msg.contains("Tag mismatch")) {
                throw new Exception("Decryption failed: wrong key or corrupted file. The encryption key on the server may not match the bundled JAR. Re-encrypt the mod and rebuild the loader.");
            }
            throw new Exception("Decryption failed: " + msg);
        }

        if (!modFile.exists() || modFile.length() == 0) {
            throw new Exception("Decryption produced empty file. The encryption key does not match the bundled JAR.");
        }

        setProgress(100);
        setStatus(loadingStatusLabel, "Client installed!", green());
    }

    private void launchGame(String javaExe, File versionsDir, File librariesDir, File assetsDir) throws Exception {
        String fabricProfileName = VERSION + "-fabric-" + FABRIC_VERSION;
        File versionDir = new File(versionsDir, fabricProfileName);
        File versionJson = new File(versionDir, fabricProfileName + ".json");
        File versionJar = new File(versionDir, fabricProfileName + ".jar");

        if (!versionJson.exists()) throw new Exception("Fabric profile not found");
        if (!versionJar.exists()) throw new Exception("Game JAR not found");

        String jsonContent = Files.readString(versionJson.toPath());
        com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(jsonContent).getAsJsonObject();

        String mainClass = root.has("mainClass") ? root.get("mainClass").getAsString() : "net.fabricmc.loader.impl.launch.knot.KnotClient";

        List<String> classpath = new ArrayList<>();
        classpath.add(versionJar.getAbsolutePath());

        java.util.Map<String, String> libMap = new java.util.LinkedHashMap<>();

        addLibrariesFromJson(root, librariesDir, libMap);

        File baseVersionJson = new File(versionsDir, VERSION + File.separator + VERSION + ".json");
        if (baseVersionJson.exists()) {
            String baseJson = Files.readString(baseVersionJson.toPath());
            com.google.gson.JsonObject baseRoot = com.google.gson.JsonParser.parseString(baseJson).getAsJsonObject();
            addLibrariesFromJson(baseRoot, librariesDir, libMap);
        }

        classpath.addAll(libMap.values());

        String classpathStr = String.join(";", classpath);

        String assetIndexId = VERSION;
        File baseVersionJson2 = new File(versionsDir, VERSION + File.separator + VERSION + ".json");
        if (baseVersionJson2.exists()) {
            String baseJson2 = Files.readString(baseVersionJson2.toPath());
            com.google.gson.JsonObject baseRoot2 = com.google.gson.JsonParser.parseString(baseJson2).getAsJsonObject();
            if (baseRoot2.has("assetIndex")) {
                assetIndexId = baseRoot2.getAsJsonObject("assetIndex").get("id").getAsString();
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add("-Xmx2G");
        cmd.add("-XX:+UseG1GC");
        cmd.add("-cp");
        cmd.add(classpathStr);
        cmd.add("-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient");
        File nativesDir = extractNatives(librariesDir, versionsDir, root);
        if (nativesDir == null) nativesDir = new File(librariesDir, "natives");
        cmd.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        cmd.add("-Djna.tmpdir=" + nativesDir.getAbsolutePath());
        cmd.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + nativesDir.getAbsolutePath());
        cmd.add("-Dio.netty.native.workdir=" + nativesDir.getAbsolutePath());
        cmd.add(mainClass);
        cmd.add("--username");
        cmd.add(Config.get().login != null ? Config.get().login : "Player");
        cmd.add("--version");
        cmd.add(fabricProfileName);
        cmd.add("--gameDir");
        cmd.add(baseDir.getAbsolutePath());
        cmd.add("--assetsDir");
        cmd.add(assetsDir.getAbsolutePath());
        cmd.add("--assetIndex");
        cmd.add(assetIndexId);
        cmd.add("--accessToken");
        cmd.add(Config.get().token != null ? Config.get().token : "0");
        cmd.add("--uuid");
        cmd.add(UUID.randomUUID().toString().replace("-", ""));
        cmd.add("--userType");
        cmd.add("legacy");
        cmd.add("--versionType");
        cmd.add("zagaDLC");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(baseDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        File lockFile = new File(mcDir, "game.lock");
        Files.writeString(lockFile.toPath(), String.valueOf(process.pid()));

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(10, 10, 10));
        logArea.setForeground(new Color(180, 180, 180));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setCaretColor(new Color(180, 180, 180));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(800, 500));

        JFrame logFrame = new JFrame("zagaDLC - Game Log");
        logFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        logFrame.add(scrollPane);
        logFrame.pack();
        logFrame.setLocationRelativeTo(null);
        logFrame.setVisible(true);

        Thread logThread = new Thread(() -> {
            java.io.File logFile = new java.io.File(System.getProperty("user.home"), "zagaDLC-game.log");
            try (java.io.PrintWriter logWriter = new java.io.PrintWriter(new java.io.FileWriter(logFile), true)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[MC] " + line);
                        logWriter.println(line);
                        String logLine = line;
                        SwingUtilities.invokeLater(() -> {
                            logArea.append(logLine + "\n");
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        });
                    }
                } catch (IOException ignored) {}
            } catch (Exception ignored) {}
            lockFile.delete();
            SwingUtilities.invokeLater(() -> {
                logArea.append("\n--- Process ended ---");
            });
        });
        logThread.setDaemon(true);
        logThread.start();
    }

    private void setLanguage(File baseDir) throws Exception {
        File optionsFile = new File(baseDir, "options.txt");
        String newContent = "lang:ru_ru\n";
        if (optionsFile.exists()) {
            String content = Files.readString(optionsFile.toPath());
            if (content.contains("lang:")) {
                newContent = content.replaceAll("lang:\\S+", "lang:ru_ru");
            } else {
                newContent = "lang:ru_ru\n" + content;
            }
        }
        Files.writeString(optionsFile.toPath(), newContent);
    }

    private void addLibrariesFromJson(com.google.gson.JsonObject root, File librariesDir, java.util.Map<String, String> libMap) {
        if (!root.has("libraries")) return;
        com.google.gson.JsonArray libs = root.getAsJsonArray("libraries");
        for (int i = 0; i < libs.size(); i++) {
            com.google.gson.JsonObject lib = libs.get(i).getAsJsonObject();
            if (!lib.has("name")) continue;
            String name = lib.get("name").getAsString();
            String[] parts = name.split(":");
            if (parts.length >= 3) {
                String groupPath = parts[0].replace('.', '/');
                String artifact = parts[1];
                String ver = parts[2];
                String key = parts[0] + ":" + artifact;
                String jarPath = groupPath + "/" + artifact + "/" + ver + "/" + artifact + "-" + ver + ".jar";
                if (!libMap.containsKey(key)) {
                    File jarFile = new File(librariesDir, jarPath);
                    if (jarFile.exists()) {
                        libMap.put(key, jarFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private File extractNatives(File librariesDir, File versionsDir, com.google.gson.JsonObject fabricJson) throws Exception {
        File nativesDir = new File(librariesDir, "natives");
        nativesDir.mkdirs();

        boolean extractedAny = false;

        String osSuffix = System.getProperty("os.name").toLowerCase().contains("win") ? "natives-windows"
                     : System.getProperty("os.name").toLowerCase().contains("mac") ? "natives-macos"
                     : "natives-linux";

        java.util.List<com.google.gson.JsonObject> allLibs = new java.util.ArrayList<>();

        File baseJson = new File(versionsDir, VERSION + File.separator + VERSION + ".json");
        if (baseJson.exists()) {
            String baseStr = Files.readString(baseJson.toPath());
            com.google.gson.JsonObject baseRoot = com.google.gson.JsonParser.parseString(baseStr).getAsJsonObject();
            if (baseRoot.has("libraries")) {
                com.google.gson.JsonArray arr = baseRoot.getAsJsonArray("libraries");
                for (int i = 0; i < arr.size(); i++) allLibs.add(arr.get(i).getAsJsonObject());
            }
        }

        for (com.google.gson.JsonObject lib : allLibs) {
            if (!lib.has("name")) continue;
            String name = lib.get("name").getAsString();

            if (!name.endsWith(":" + osSuffix)) continue;

            if (lib.has("rules")) {
                com.google.gson.JsonArray rules = lib.getAsJsonArray("rules");
                boolean allowed = false;
                for (int r = 0; r < rules.size(); r++) {
                    com.google.gson.JsonObject rule = rules.get(r).getAsJsonObject();
                    String action = rule.has("action") ? rule.get("action").getAsString() : "allow";
                    if (action.equals("allow")) {
                        if (rule.has("os")) {
                            String osName = rule.getAsJsonObject("os").get("name").getAsString();
                            if (System.getProperty("os.name").toLowerCase().contains(osName)) {
                                allowed = true;
                            }
                        } else {
                            allowed = true;
                        }
                    }
                }
                if (!allowed) continue;
            }

            if (!lib.has("downloads")) continue;
            com.google.gson.JsonObject downloads = lib.getAsJsonObject("downloads");
            if (!downloads.has("artifact")) continue;

            com.google.gson.JsonObject artifact = downloads.getAsJsonObject("artifact");
            String path = artifact.get("path").getAsString();
            String url = artifact.get("url").getAsString();

            File nativeJar = new File(librariesDir, path);
            if (!nativeJar.exists()) {
                nativeJar.getParentFile().mkdirs();
                download(url, nativeJar, 0, 0);
            }

            if (nativeJar.exists()) {
                extractedAny = extractDllsFromJar(nativeJar, nativesDir) || extractedAny;
            }
        }

        return extractedAny ? nativesDir : null;
    }

    private boolean extractDllsFromJar(File jarFile, File nativesDir) throws Exception {
        boolean extracted = false;
        if (!nativesDir.exists()) nativesDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib")) {
                    File outFile = new File(nativesDir, new File(name).getName());
                    if (outFile.exists() && outFile.length() > 0) continue;
                    try {
                        File parent = outFile.getParentFile();
                        if (parent != null && !parent.exists()) parent.mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                        }
                        extracted = true;
                    } catch (Exception e) {
                        System.err.println("[WARN] Failed to extract " + name + ": " + e.getMessage());
                    }
                }
            }
        }
        return extracted;
    }

    private static final java.net.http.HttpClient sharedHttpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();

    private String downloadString(String urlStr) throws Exception {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(new URI(urlStr))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .GET()
                        .build();
                java.net.http.HttpResponse<String> resp = sharedHttpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    throw new Exception("HTTP " + resp.statusCode() + " for " + urlStr);
                }
                return resp.body();
            } catch (Exception e) {
                if (attempt == maxRetries) throw e;
                Thread.sleep(1000 * attempt);
            }
        }
        throw new Exception("Failed to download");
    }

    private void download(String urlStr, File outputFile, int startPct, int endPct) throws Exception {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            HttpURLConnection conn = null;
            try {
                URL url = new URI(urlStr).toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "zagaDLC-Loader/1.0");
                conn.setInstanceFollowRedirects(false);

                int status = conn.getResponseCode();
                int maxRedirects = 5;
                for (int r = 0; r < maxRedirects && (status == 301 || status == 302 || status == 307 || status == 308); r++) {
                    String newUrl = conn.getHeaderField("Location");
                    if (newUrl == null || newUrl.isEmpty()) break;
                    conn.disconnect();
                    conn = (HttpURLConnection) new URI(newUrl).toURL().openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "zagaDLC-Loader/1.0");
                    conn.setInstanceFollowRedirects(false);
                    status = conn.getResponseCode();
                }

                if (status < 200 || status >= 300) {
                    throw new Exception("HTTP " + status + " for " + urlStr);
                }

                int fileLen = conn.getContentLength();
                outputFile.getParentFile().mkdirs();
                File tempFile = new File(outputFile.getParent(), outputFile.getName() + ".tmp");
                int totalRead = 0;
                try (InputStream is = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[65536];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        totalRead += len;
                        if (fileLen > 0) {
                            int pct = startPct + (int)((double) totalRead / fileLen * (endPct - startPct));
                            setProgress(pct);
                        } else {
                            setStatus(loadingStatusLabel, "Downloading: " + (totalRead / 1024 / 1024) + " MB...", textDim());
                        }
                    }
                }
                if (fileLen > 0 && totalRead != fileLen) {
                    tempFile.delete();
                    throw new Exception("Download truncated: expected " + fileLen + " bytes, got " + totalRead + " for " + urlStr);
                }
                if (!tempFile.renameTo(outputFile)) {
                    java.nio.file.Files.move(tempFile.toPath(), outputFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                if (!outputFile.exists() || outputFile.length() == 0) {
                    tempFile.delete();
                    throw new Exception("Download failed: file missing or empty for " + urlStr);
                }
                return;
            } catch (Exception e) {
                if (attempt == maxRetries) throw e;
                Thread.sleep(1000 * attempt);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }

    private void extractZip(File zipFile, File destDir, int startPct, int endPct) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            long totalSize = zipFile.length();
            long extracted = 0;

            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName()).getCanonicalFile();
                if (!outFile.toPath().startsWith(destDir.getCanonicalPath())) {
                    throw new IOException("Zip entry outside target dir: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                            extracted += len;
                        }
                    }
                }
                zis.closeEntry();
                if (totalSize > 0) {
                    int pct = startPct + (int)((double) extracted / totalSize * (endPct - startPct));
                    setProgress(Math.min(pct, endPct));
                }
            }
        }
    }

    private void setProgress(int pct) {
        SwingUtilities.invokeLater(() -> {
            loadingProgressBar.setVisible(pct >= 0 && pct < 100);
            loadingProgressBar.setValue(Math.min(Math.max(pct, 0), 100));
        });
    }

    private void setStatus(JLabel label, String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            label.setText(text);
            label.setForeground(color);
        });
    }
}
