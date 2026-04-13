import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.Timer;

/**
 * OnlinePanel — Online PvP via TCP sockets.
 *
 * Lobby flow:
 *   Host → clicks "🖥 Host Game" → GameServer starts → shows IP:Port → connects own GameClient to localhost
 *   Guest → clicks "🔗 Join Game" → enters IP:Port → GameClient connects
 *
 *   Once both are connected, both see the game UI.
 *   Each picks a choice → GameServer reveals result to both simultaneously.
 */
public class OnlinePanel extends JPanel {

    // ─── Colors ────────────────────────────────────────────────────────────────
    private static final Color BG_DARK        = new Color(10, 10, 20);
    private static final Color BG_CARD        = new Color(18, 18, 35);
    private static final Color BG_CARD2       = new Color(25, 25, 48);
    private static final Color ACCENT_PURPLE  = new Color(139, 92, 246);
    private static final Color ACCENT_CYAN    = new Color(34, 211, 238);
    private static final Color ACCENT_PINK    = new Color(244, 114, 182);
    private static final Color WIN_GREEN      = new Color(52, 211, 153);
    private static final Color LOSE_RED       = new Color(248, 113, 113);
    private static final Color TIE_YELLOW     = new Color(251, 191, 36);
    private static final Color TEXT_PRIMARY   = new Color(240, 240, 255);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 200);
    private static final Color TEXT_MUTED     = new Color(100, 100, 140);

    // ─── Network state ─────────────────────────────────────────────────────────
    private GameServer server;
    private GameClient client;
    private int myRole = 0;          // 1 = host/P1, 2 = guest/P2
    private volatile boolean opponentConnected = false;
    private volatile boolean choiceSent        = false;

    // ─── Game state ────────────────────────────────────────────────────────────
    private int myScore = 0, opponentScore = 0, tieScore = 0, round = 0;
    private final java.util.List<String> history = new java.util.ArrayList<>();

    // ─── UI ────────────────────────────────────────────────────────────────────
    private final CardLayout innerCards = new CardLayout();
    private final JPanel     innerPane  = new JPanel(innerCards);

    // Lobby refs
    private JLabel lobbyStatusLabel;
    private JTextField ipField;
    private JButton hostBtn, joinBtn, connectBtn;

    // Game refs
    private JLabel myEmojiLabel, opEmojiLabel;
    private JLabel myScoreLabel, opScoreLabel, tieScoreLabel;
    private JLabel resultLabel, roundLabel, myRoleLabel;
    private JButton gameRock, gamePaper, gameScissors;
    private JPanel  historyPanel;

    private final Runnable backAction;
    private ImageIcon vsIcon;
    
    public OnlinePanel(Runnable backAction) {
        this.backAction = backAction;
        setOpaque(false);
        setLayout(new BorderLayout(0,0));
        loadResources();
        buildUI();
    }

    private void loadResources() {
        try {
            java.net.URL imgUrl = getClass().getResource("/verses.jpg");
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                vsIcon = new ImageIcon(img);
            }
        } catch (Exception e) {
            System.err.println("Could not load verses image: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UI BUILD
    // ════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        innerPane.setOpaque(false);
        innerPane.add(buildLobby(), "lobby");
        innerPane.add(buildGame(),  "game");
        add(buildHeader(), BorderLayout.NORTH);
        add(innerPane,     BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        showLobby();
    }

    // ─── Header ───────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = gradient(new Color(15,15,30), new Color(8,8,20));
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0, new Color(80,80,120)),
            new EmptyBorder(14,24,14,24)));

        JButton back = textBtn("← Back");
        back.addActionListener(e -> { cleanup(); backAction.run(); });

        JLabel title = new JLabel("ONLINE  PvP", SwingConstants.CENTER);
        title.setFont(font(22f, Font.BOLD)); title.setForeground(TEXT_PRIMARY);

        JLabel sub = new JLabel(
            "<html><center><font color='#8B5CF6'>Host</font> " +
            "<font color='#A0A0C8'>a game or</font> " +
            "<font color='#F472B6'>Join</font> " +
            "<font color='#A0A0C8'>a friend on the same Wi-Fi</font></center></html>",
            SwingConstants.CENTER);
        sub.setFont(font(11f, Font.PLAIN));

        JPanel titleBox = new JPanel(new GridLayout(2,1,0,2)); titleBox.setOpaque(false);
        titleBox.add(title); titleBox.add(sub);

        roundLabel = new JLabel("Round 0", SwingConstants.RIGHT);
        roundLabel.setFont(font(12f, Font.BOLD)); roundLabel.setForeground(TEXT_MUTED);

        header.add(back,       BorderLayout.WEST);
        header.add(titleBox,   BorderLayout.CENTER);
        header.add(roundLabel, BorderLayout.EAST);
        return header;
    }

    // ─── Lobby ────────────────────────────────────────────────────────────────
    private JPanel buildLobby() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);

        JPanel card = roundedCard(20, BG_CARD, new Color(139,92,246,60));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(32, 48, 32, 48));
        card.setPreferredSize(new Dimension(480, 340));

        // Title
        JLabel title = cLbl("Choose how to play", 18f, Font.BOLD, TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0,0,24,0));
        card.add(title);

        // ── HOST section ──────────────────────────────────────────────────────
        hostBtn = bigBtn("🖥  Host a Game", ACCENT_PURPLE);
        hostBtn.addActionListener(e -> startHosting());
        card.add(hostBtn); card.add(Box.createVerticalStrut(8));

        lobbyStatusLabel = cLbl("", 12f, Font.BOLD, ACCENT_CYAN);
        lobbyStatusLabel.setBorder(new EmptyBorder(0,0,16,0));
        card.add(lobbyStatusLabel);

        // Divider
        JSeparator sep = new JSeparator(); sep.setForeground(new Color(60,60,100));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); card.add(sep);
        card.add(Box.createVerticalStrut(16));

        // ── JOIN section ──────────────────────────────────────────────────────
        JLabel joinHint = cLbl("— or join a friend —", 11f, Font.PLAIN, TEXT_MUTED);
        card.add(joinHint); card.add(Box.createVerticalStrut(10));

        // IP + Port field
        JPanel ipRow = new JPanel(new BorderLayout(8,0)); ipRow.setOpaque(false);
        ipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel ipHint = new JLabel("IP:Port"); ipHint.setFont(font(11f, Font.BOLD)); ipHint.setForeground(TEXT_MUTED);
        ipField = new JTextField("192.168.1.x:9876");
        ipField.setFont(font(12f, Font.PLAIN)); ipField.setForeground(TEXT_PRIMARY);
        ipField.setBackground(BG_CARD2); ipField.setCaretColor(TEXT_PRIMARY);
        ipField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60,60,100),1), new EmptyBorder(4,8,4,8)));
        ipRow.add(ipHint, BorderLayout.WEST); ipRow.add(ipField, BorderLayout.CENTER);
        card.add(ipRow); card.add(Box.createVerticalStrut(8));

        connectBtn = bigBtn("🔗  Join Game", ACCENT_PINK);
        connectBtn.addActionListener(e -> joinGame());
        card.add(connectBtn);

        outer.add(card);
        return outer;
    }

    // ─── Game screen ──────────────────────────────────────────────────────────
    private JPanel buildGame() {
        JPanel panel = new JPanel(new BorderLayout(0,14));
        panel.setOpaque(false); panel.setBorder(new EmptyBorder(16,24,10,24));

        // Scores
        JPanel scores = new JPanel(new GridLayout(1,3,14,0));
        scores.setOpaque(false); scores.setPreferredSize(new Dimension(0,90));

        JPanel myCard  = scoreCard(null, ACCENT_PURPLE);  // label set after role known
        myScoreLabel   = (JLabel) myCard.getClientProperty("val");
        myRoleLabel    = (JLabel) myCard.getClientProperty("title");

        JPanel tieCard = scoreCard("TIES", TIE_YELLOW);
        tieScoreLabel  = (JLabel) tieCard.getClientProperty("val");

        JPanel opCard  = scoreCard("OPPONENT", ACCENT_PINK);
        opScoreLabel   = (JLabel) opCard.getClientProperty("val");

        scores.add(myCard); scores.add(tieCard); scores.add(opCard);

        // Arena
        JPanel arena = roundedCard(20, BG_CARD2, new Color(139,92,246,80));
        arena.setLayout(new BorderLayout(0,0));

        JPanel row = new JPanel(new GridLayout(1,3,0,0));
        row.setOpaque(false); row.setBorder(new EmptyBorder(10,0,0,0));

        // My side
        JPanel mySide = sidePanel("YOU", ACCENT_PURPLE);
        myEmojiLabel  = (JLabel) mySide.getClientProperty("emoji");
        // Opponent side
        JPanel opSide = sidePanel("OPPONENT", ACCENT_PINK);
        opEmojiLabel  = (JLabel) opSide.getClientProperty("emoji");
        // VS
        JPanel vsPane = new JPanel(new GridBagLayout()); vsPane.setOpaque(false);
        JLabel vs = new JLabel();
        if (vsIcon != null) {
            vs.setIcon(vsIcon);
        } else {
            vs.setText("VS");
            vs.setFont(font(22f,Font.BOLD));
            vs.setForeground(new Color(80,80,120));
        }
        vsPane.add(vs);

        row.add(mySide); row.add(vsPane); row.add(opSide);

        JPanel resultBar = new JPanel(new GridBagLayout());
        resultBar.setOpaque(false); resultBar.setPreferredSize(new Dimension(0,55));
        resultLabel = new JLabel("Waiting for opponent…", SwingConstants.CENTER);
        resultLabel.setFont(font(14f, Font.BOLD)); resultLabel.setForeground(TEXT_SECONDARY);
        resultBar.add(resultLabel);

        arena.add(row,       BorderLayout.CENTER);
        arena.add(resultBar, BorderLayout.SOUTH);

        // Buttons
        JPanel btnRow = new JPanel(new GridLayout(1,3,16,0));
        btnRow.setOpaque(false); btnRow.setPreferredSize(new Dimension(0,90));
        gameRock     = choiceBtn("🪨","ROCK",     ACCENT_PURPLE, () -> submitChoice("Rock"));
        gamePaper    = choiceBtn("📄","PAPER",    ACCENT_CYAN,   () -> submitChoice("Paper"));
        gameScissors = choiceBtn("✂️", "SCISSORS", ACCENT_PINK,   () -> submitChoice("Scissors"));
        btnRow.add(gameRock); btnRow.add(gamePaper); btnRow.add(gameScissors);
        setGameBtnsEnabled(false);

        historyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        historyPanel.setOpaque(false);

        panel.add(scores,  BorderLayout.NORTH);
        panel.add(arena,   BorderLayout.CENTER);
        panel.add(btnRow,  BorderLayout.SOUTH);
        return panel;
    }

    // ─── Footer ───────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false); footer.setBorder(new EmptyBorder(0,24,14,24));
        JLabel tl = new JLabel("History"); tl.setFont(font(11f,Font.BOLD)); tl.setForeground(TEXT_MUTED);
        tl.setBorder(new EmptyBorder(0,0,6,0));
        historyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        historyPanel.setOpaque(false);
        footer.add(tl,           BorderLayout.NORTH);
        footer.add(historyPanel, BorderLayout.CENTER);
        return footer;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NETWORKING
    // ════════════════════════════════════════════════════════════════════════

    // ── HOST ─────────────────────────────────────────────────────────────────
    private void startHosting() {
        hostBtn.setEnabled(false);
        connectBtn.setEnabled(false);
        lobbyStatusLabel.setText("Starting server…");
        lobbyStatusLabel.setForeground(ACCENT_CYAN);

        new Thread(() -> {
            try {
                server = new GameServer(GameServer.DEFAULT_PORT);
                server.setListener(new GameServer.ServerListener() {
                    public void onPlayerConnected(int n) {
                        SwingUtilities.invokeLater(() ->
                            lobbyStatusLabel.setText("Waiting for opponent to join…"));
                    }
                    public void onBothReady() {
                        SwingUtilities.invokeLater(() -> {
                            opponentConnected = true;
                            lobbyStatusLabel.setText("Opponent connected! Starting…");
                        });
                    }
                    public void onPlayerDisconnected(int n) {
                        SwingUtilities.invokeLater(() -> handleOpponentQuit());
                    }
                });
                server.start();

                String ip   = GameServer.getLocalIP();
                int    port = GameServer.DEFAULT_PORT;
                SwingUtilities.invokeLater(() -> {
                    lobbyStatusLabel.setText("Your code:  " + ip + ":" + port + "  (share with friend)");
                    lobbyStatusLabel.setForeground(WIN_GREEN);
                });

                // Host connects to own server
                Thread.sleep(300);
                client = new GameClient();
                client.setListener(new GameClient.MessageListener() {
                    @Override
                    public void onMessage(String msg) {
                        SwingUtilities.invokeLater(() -> handleServerMessage(msg));
                    }
                    @Override
                    public void onDisconnected() {
                        SwingUtilities.invokeLater(() -> handleOpponentQuit());
                    }
                    @Override
                    public void onError(String err) {
                        SwingUtilities.invokeLater(() -> {
                            lobbyStatusLabel.setText("Error: " + err);
                            lobbyStatusLabel.setForeground(LOSE_RED);
                        });
                    }
                });
                client.connect("127.0.0.1", port);

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lobbyStatusLabel.setText("Error: " + ex.getMessage());
                    lobbyStatusLabel.setForeground(LOSE_RED);
                    hostBtn.setEnabled(true); connectBtn.setEnabled(true);
                });
            }
        }, "HostSetup").start();
    }

    // ── JOIN ──────────────────────────────────────────────────────────────────
    private void joinGame() {
        String raw = ipField.getText().trim();
        if (raw.isEmpty()) { shake(ipField); return; }

        String host; int port;
        try {
            if (raw.contains(":")) {
                host = raw.substring(0, raw.lastIndexOf(':'));
                port = Integer.parseInt(raw.substring(raw.lastIndexOf(':') + 1));
            } else {
                host = raw; port = GameServer.DEFAULT_PORT;
            }
        } catch (NumberFormatException e) { shake(ipField); return; }

        connectBtn.setEnabled(false); hostBtn.setEnabled(false);
        lobbyStatusLabel.setText("Connecting to " + host + ":" + port + "…");
        lobbyStatusLabel.setForeground(ACCENT_CYAN);

        final String h = host; final int p = port;
        new Thread(() -> {
            try {
                client = new GameClient();
                client.setListener(new GameClient.MessageListener() {
                    @Override
                    public void onMessage(String msg) {
                        SwingUtilities.invokeLater(() -> handleServerMessage(msg));
                    }
                    @Override
                    public void onDisconnected() {
                        SwingUtilities.invokeLater(() -> handleOpponentQuit());
                    }
                    @Override
                    public void onError(String err) {
                        SwingUtilities.invokeLater(() -> {
                            lobbyStatusLabel.setText("Connect Error: " + err);
                            lobbyStatusLabel.setForeground(LOSE_RED);
                            connectBtn.setEnabled(true); hostBtn.setEnabled(true);
                        });
                    }
                });
                client.connect(h, p);
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    lobbyStatusLabel.setText("Could not connect: " + ex.getMessage());
                    lobbyStatusLabel.setForeground(LOSE_RED);
                    connectBtn.setEnabled(true); hostBtn.setEnabled(true);
                });
            }
        }, "JoinThread").start();
    }

    // ── Message handler (runs on EDT) ─────────────────────────────────────────
    private void handleServerMessage(String msg) {
        if (msg.startsWith("ROLE:")) {
            myRole = Integer.parseInt(msg.substring(5));
            if (myRoleLabel != null) myRoleLabel.setText(myRole == 1 ? "YOU (P1)" : "YOU (P2)");

        } else if (msg.equals("OPPONENT_JOINED")) {
            opponentConnected = true;
            showGame();
            setStatus("Opponent connected! Choose your weapon! ⚡");

        } else if (msg.startsWith("RESULT:")) {
            // RESULT:WIN|Rock|Paper
            String[] parts = msg.substring(7).split("\\|");
            if (parts.length == 3) showResult(parts[0], parts[1], parts[2]);

        } else if (msg.equals("OPPONENT_REMATCH")) {
            setStatus("Opponent wants a rematch — choose again!");
            setGameBtnsEnabled(true); choiceSent = false;
            myEmojiLabel.setText("❓"); opEmojiLabel.setText("❓");

        } else if (msg.equals("OPPONENT_QUIT")) {
            handleOpponentQuit();
        }
    }

    // ─── Submit choice ────────────────────────────────────────────────────────
    private void submitChoice(String choice) {
        if (choiceSent || client == null || !client.isConnected()) return;
        choiceSent = true;
        setGameBtnsEnabled(false);
        myEmojiLabel.setText("✅");
        setStatus("Waiting for opponent…");
        client.sendChoice(choice);
    }

    // ─── Show result ──────────────────────────────────────────────────────────
    private void showResult(String outcome, String myChoice, String theirChoice) {
        round++;
        roundLabel.setText("Round " + round);
        myEmojiLabel.setText(emoji(myChoice));
        opEmojiLabel.setText(emoji(theirChoice));

        String msg; Color col;
        switch (outcome) {
            case "WIN"  -> { myScore++;       msg = "🏆 You Win!  (" + myChoice + " beats " + theirChoice + ")"; col = WIN_GREEN;  }
            case "LOSE" -> { opponentScore++; msg = "💀 You Lose!  (" + theirChoice + " beats " + myChoice + ")"; col = LOSE_RED;   }
            default     -> { tieScore++;      msg = "🤝 It's a Tie!  (" + myChoice + ")";                          col = TIE_YELLOW; }
        }
        myScoreLabel.setText(String.valueOf(myScore));
        opScoreLabel.setText(String.valueOf(opponentScore));
        tieScoreLabel.setText(String.valueOf(tieScore));
        resultLabel.setText(msg); resultLabel.setForeground(col);
        addBadge(outcome);

        // Auto-reset for next round after 2s
        Timer t = new Timer(2000, e -> {
            myEmojiLabel.setText("❓"); opEmojiLabel.setText("❓");
            setStatus("Choose your weapon! ⚡");
            setGameBtnsEnabled(true); choiceSent = false;
            if (client != null) client.sendRematch();
        });
        t.setRepeats(false); t.start();
    }

    private void handleOpponentQuit() {
        setStatus("⚠️  Opponent disconnected. Go back and host/join a new game.");
        setGameBtnsEnabled(false);
    }

    // ─── Navigation helpers ───────────────────────────────────────────────────
    private void showLobby() { innerCards.show(innerPane, "lobby"); }
    private void showGame()  {
        innerCards.show(innerPane, "game");
        setStatus("Connected! Choose your weapon! ⚡");
        setGameBtnsEnabled(true);
    }

    private void cleanup() {
        if (client != null) { client.sendQuit(); client.disconnect(); client = null; }
        if (server != null) { server.stop(); server = null; }
        opponentConnected = false; choiceSent = false; myRole = 0;
        myScore = 0; opponentScore = 0; tieScore = 0; round = 0;
        history.clear(); if (historyPanel != null) { historyPanel.removeAll(); historyPanel.repaint(); }
        showLobby();
        if (hostBtn != null)   hostBtn.setEnabled(true);
        if (connectBtn != null) connectBtn.setEnabled(true);
        if (lobbyStatusLabel != null) lobbyStatusLabel.setText("");
    }

    private void setStatus(String msg) {
        if (resultLabel != null) { resultLabel.setText(msg); resultLabel.setForeground(TEXT_SECONDARY); }
    }

    // ─── History badge ────────────────────────────────────────────────────────
    private void addBadge(String outcome) {
        String sym = outcome.equals("WIN") ? "W" : outcome.equals("LOSE") ? "L" : "T";
        Color  col = outcome.equals("WIN") ? WIN_GREEN : outcome.equals("LOSE") ? LOSE_RED : TIE_YELLOW;
        if (history.size() >= 12) { history.remove(0); if (historyPanel.getComponentCount()>0) historyPanel.remove(0); }
        history.add(outcome);
        JLabel b = new JLabel(sym, SwingConstants.CENTER);
        b.setOpaque(true); b.setFont(font(11f,Font.BOLD)); b.setForeground(col);
        b.setBackground(new Color(col.getRed(),col.getGreen(),col.getBlue(),35));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(col,1),new EmptyBorder(2,8,2,8)));
        historyPanel.add(b); historyPanel.revalidate(); historyPanel.repaint();
    }

    private void setGameBtnsEnabled(boolean en) {
        if (gameRock!=null)     gameRock.setEnabled(en);
        if (gamePaper!=null)    gamePaper.setEnabled(en);
        if (gameScissors!=null) gameScissors.setEnabled(en);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private static String emoji(String c) {
        return switch (c) { case "Rock"->"🪨"; case "Paper"->"📄"; case "Scissors"->"✂️"; default->"❓"; };
    }

    private static Font font(float size, int style) { return new Font("Segoe UI",style,1).deriveFont(style,size); }

    private static JLabel cLbl(String text, float size, int style, Color col) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font(size,style)); l.setForeground(col); l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private static JPanel gradient(Color c1, Color c2) {
        return new JPanel() {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setPaint(new GradientPaint(0,0,c1,0,getHeight(),c2));
                g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
            }
        };
    }

    private static JPanel roundedCard(int arc, Color bg, Color border) {
        return new JPanel() {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);     g2.fillRoundRect(1,1,getWidth()-2,getHeight()-2,arc,arc);
                g2.setColor(border); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,arc,arc);
                g2.dispose(); super.paintComponent(g);
            }
        };
    }

    private JPanel scoreCard(String titleText, Color accent) {
        JPanel card = roundedCard(16, BG_CARD, accent);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(8,0,8,0));
        JLabel tl = cLbl(titleText != null ? titleText : "YOU", 10f, Font.BOLD, accent);
        JLabel vl = cLbl("0", 36f, Font.BOLD, TEXT_PRIMARY);
        card.add(tl); card.add(vl);
        card.putClientProperty("title", tl);
        card.putClientProperty("val",   vl);
        return card;
    }

    private static JPanel sidePanel(String owner, Color accent) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false); p.setBorder(new EmptyBorder(10,20,10,20));
        JLabel ol = cLbl(owner, 11f, Font.BOLD, accent);
        JLabel el = new JLabel("❓", SwingConstants.CENTER);
        el.setFont(new Font("Segoe UI Emoji",Font.PLAIN,52));
        el.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(ol); p.add(Box.createVerticalStrut(4)); p.add(el);
        p.putClientProperty("emoji", el);
        return p;
    }

    private static JButton textBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(font(12f,Font.BOLD)); b.setForeground(new Color(100,100,140));
        b.setBackground(new Color(0,0,0,0)); b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(new Color(139,92,246)); }
            public void mouseExited(MouseEvent e)  { b.setForeground(new Color(100,100,140)); }
        });
        return b;
    }

    private static JButton bigBtn(String text, Color accent) {
        JButton b = new JButton(text);
        b.setFont(font(14f, Font.BOLD)); b.setForeground(Color.WHITE);
        b.setBackground(new Color(accent.getRed()/2, accent.getGreen()/2, accent.getBlue()/2));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent,2), new EmptyBorder(10,20,10,20)));
        b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(accent.getRed()/2+20, accent.getGreen()/2+10, accent.getBlue()/2+20)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(new Color(accent.getRed()/2, accent.getGreen()/2, accent.getBlue()/2)); }
        });
        return b;
    }

    private static JButton choiceBtn(String ico, String lbl, Color accent, Runnable onClick) {
        JButton b = new JButton("<html><center><span style='font-size:22px'>" + ico + "</span><br>"
            + "<b style='font-size:10px;color:rgb(" + accent.getRed() + "," + accent.getGreen() + "," + accent.getBlue() + ")'>"
            + lbl + "</b></center></html>");
        b.setBackground(BG_CARD);
        b.setBorder(BorderFactory.createLineBorder(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),100),2));
        b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if(b.isEnabled()) b.setBorder(BorderFactory.createLineBorder(accent,2)); }
            public void mouseExited(MouseEvent e)  { b.setBorder(BorderFactory.createLineBorder(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),100),2)); }
        });
        b.addActionListener(e -> onClick.run());
        return b;
    }

    private static void shake(JComponent c) {
        final int[] step = {0};
        Timer t = new Timer(30, null);
        t.addActionListener(e -> {
            step[0]++;
            int dx = (step[0] % 2 == 0) ? 4 : -4;
            c.setLocation(c.getX() + dx, c.getY());
            if (step[0] >= 8) { t.stop(); c.setLocation(c.getX() - (step[0]%2==0?0:0), c.getY()); }
        });
        t.start();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2=(Graphics2D)g.create();
        g2.setPaint(new GradientPaint(0,0,BG_DARK,0,getHeight(),new Color(5,5,15)));
        g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
        super.paintComponent(g);
    }
}
