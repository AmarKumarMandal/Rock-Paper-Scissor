import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;

/**
 * LocalPvPPanel — Local 2-Player mode on the same machine.
 *
 * Player 1 (left side) :  clicks the Rock / Paper / Scissors buttons  OR  keys Q / W / E
 * Player 2 (right side):  keys  1 / 2 / 3  (numrow)   or  Numpad 1 / 2 / 3
 *
 * Neither choice is revealed until BOTH players have locked in.
 * Then both are shown simultaneously and the winner is declared.
 */
public class LocalPvPPanel extends JPanel {

    // ─── Shared color palette ─────────────────────────────────────────────────
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

    private static final String[] CHOICES      = {"Rock", "Paper", "Scissors"};
    private static final String[] P1_EMOJIS    = {"🪨", "📄", "✂️"};
    private static final String[] CHOICE_EMOJI = {"🪨", "📄", "✂️"};

    // ─── Game state ──────────────────────────────────────────────────────────
    private String  p1Choice = null, p2Choice = null;
    private int     p1Score  = 0, p2Score = 0, tieScore = 0, round = 0;
    private boolean locked   = false;   // true while reveal animation is running

    // ─── UI ──────────────────────────────────────────────────────────────────
    private JLabel p1EmojiLabel, p2EmojiLabel;
    private JLabel p1StatusLabel, p2StatusLabel;
    private JLabel p1ScoreLabel,  p2ScoreLabel, tieScoreLabel;
    private JLabel resultLabel, roundLabel;
    private JPanel historyPanel;
    private final java.util.List<String> history = new java.util.ArrayList<>();

    // P1 buttons (mouse)
    private JButton p1Rock, p1Paper, p1Scissors;

    private final Runnable backAction;

    private ImageIcon vsIcon;

    public LocalPvPPanel(Runnable backAction) {
        this.backAction = backAction;
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        loadResources();
        buildUI();
        setupKeys();
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
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    // ─── Header ──────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = gradient(new Color(15,15,30), new Color(8,8,20));
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0, new Color(80,80,120)),
            new EmptyBorder(14, 24, 14, 24)));

        JButton back = textBtn("← Back");
        back.addActionListener(e -> { resetGame(); backAction.run(); });

        JLabel title = new JLabel("LOCAL  2 - PLAYER", SwingConstants.CENTER);
        title.setFont(font(22f, Font.BOLD)); title.setForeground(TEXT_PRIMARY);

        JLabel sub = new JLabel(
            "<html><center><font color='#8B5CF6'>P1: Q/W/E or Buttons</font>" +
            "  &nbsp;|&nbsp;  " +
            "<font color='#F472B6'>P2: 1 / 2 / 3</font></center></html>",
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

    // ─── Center ───────────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 24, 12, 24));
        center.add(buildScores(),  BorderLayout.NORTH);
        center.add(buildArena(),   BorderLayout.CENTER);
        center.add(buildP1Buttons(), BorderLayout.SOUTH);
        return center;
    }

    // ─── Scoreboard ──────────────────────────────────────────────────────────
    private JPanel buildScores() {
        JPanel board = new JPanel(new GridLayout(1,3,14,0));
        board.setOpaque(false); board.setPreferredSize(new Dimension(0,90));

        JPanel p1c = scoreCard("PLAYER 1", ACCENT_PURPLE);
        p1ScoreLabel = (JLabel) p1c.getClientProperty("val");
        JPanel tc  = scoreCard("TIES", TIE_YELLOW);
        tieScoreLabel = (JLabel) tc.getClientProperty("val");
        JPanel p2c = scoreCard("PLAYER 2", ACCENT_PINK);
        p2ScoreLabel = (JLabel) p2c.getClientProperty("val");

        board.add(p1c); board.add(tc); board.add(p2c);
        return board;
    }

    private JPanel scoreCard(String title, Color accent) {
        JPanel card = roundedCard(16, BG_CARD, accent);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(8,0,8,0));
        JLabel tl = cLbl(title, 10f, Font.BOLD,  accent);
        JLabel vl = cLbl("0",   36f, Font.BOLD,  TEXT_PRIMARY);
        card.add(tl); card.add(vl);
        card.putClientProperty("val", vl);
        return card;
    }

    // ─── Arena ────────────────────────────────────────────────────────────────
    private JPanel buildArena() {
        JPanel arena = roundedCard(20, BG_CARD2, new Color(139,92,246,80));
        arena.setLayout(new BorderLayout(0,0));

        // ── Choice display row ────────────────────────────────────────────────
        JPanel row = new JPanel(new GridLayout(1,3,0,0));
        row.setOpaque(false); row.setBorder(new EmptyBorder(10,0,0,0));

        // P1 side
        JPanel p1Side = new JPanel();
        p1Side.setLayout(new BoxLayout(p1Side, BoxLayout.Y_AXIS));
        p1Side.setOpaque(false); p1Side.setBorder(new EmptyBorder(10,20,10,20));
        JLabel p1Label = cLbl("PLAYER 1", 11f, Font.BOLD, ACCENT_PURPLE);
        p1EmojiLabel   = cLbl("❓", 52f, Font.PLAIN, TEXT_PRIMARY);
        p1EmojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        p1StatusLabel   = cLbl("Waiting…", 11f, Font.BOLD, TEXT_MUTED);
        p1Side.add(p1Label); p1Side.add(Box.createVerticalStrut(4));
        p1Side.add(p1EmojiLabel); p1Side.add(Box.createVerticalStrut(4));
        p1Side.add(p1StatusLabel);

        // VS
        JPanel vsPane = new JPanel(new GridBagLayout()); vsPane.setOpaque(false);
        JLabel vs = new JLabel();
        if (vsIcon != null) {
            vs.setIcon(vsIcon);
        } else {
            vs.setText("VS");
            vs.setFont(font(22f, Font.BOLD));
            vs.setForeground(new Color(80,80,120));
        }
        vsPane.add(vs);

        // P2 side
        JPanel p2Side = new JPanel();
        p2Side.setLayout(new BoxLayout(p2Side, BoxLayout.Y_AXIS));
        p2Side.setOpaque(false); p2Side.setBorder(new EmptyBorder(10,20,10,20));
        JLabel p2Label = cLbl("PLAYER 2", 11f, Font.BOLD, ACCENT_PINK);
        p2EmojiLabel   = cLbl("❓", 52f, Font.PLAIN, TEXT_PRIMARY);
        p2EmojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        p2StatusLabel   = cLbl("Press 1/2/3", 11f, Font.BOLD, TEXT_MUTED);
        p2Side.add(p2Label); p2Side.add(Box.createVerticalStrut(4));
        p2Side.add(p2EmojiLabel); p2Side.add(Box.createVerticalStrut(4));
        p2Side.add(p2StatusLabel);

        row.add(p1Side); row.add(vsPane); row.add(p2Side);

        // ── Result bar ────────────────────────────────────────────────────────
        JPanel resultBar = new JPanel(new GridBagLayout());
        resultBar.setOpaque(false); resultBar.setPreferredSize(new Dimension(0,55));
        resultLabel = new JLabel("Both players, choose your weapon!", SwingConstants.CENTER);
        resultLabel.setFont(font(14f, Font.BOLD)); resultLabel.setForeground(TEXT_SECONDARY);
        resultBar.add(resultLabel);

        arena.add(row,       BorderLayout.CENTER);
        arena.add(resultBar, BorderLayout.SOUTH);
        return arena;
    }

    // ─── P1 Buttons ───────────────────────────────────────────────────────────
    private JPanel buildP1Buttons() {
        JPanel row = new JPanel(new GridLayout(1,3,16,0));
        row.setOpaque(false); row.setPreferredSize(new Dimension(0, 90));

        p1Rock     = choiceBtn("🪨", "ROCK (Q)",     ACCENT_PURPLE, () -> p1Pick("Rock"));
        p1Paper    = choiceBtn("📄", "PAPER (W)",    ACCENT_CYAN,   () -> p1Pick("Paper"));
        p1Scissors = choiceBtn("✂️",  "SCISSORS (E)", ACCENT_PINK,   () -> p1Pick("Scissors"));

        row.add(p1Rock); row.add(p1Paper); row.add(p1Scissors);

        JPanel wrapper = new JPanel(new BorderLayout(0,6));
        wrapper.setOpaque(false);
        JLabel hint = new JLabel("▲  PLAYER 1 controls  |  Player 2 uses keyboard: 1=Rock  2=Paper  3=Scissors",
            SwingConstants.CENTER);
        hint.setFont(font(10f, Font.PLAIN)); hint.setForeground(TEXT_MUTED);
        wrapper.add(hint, BorderLayout.NORTH);
        wrapper.add(row,  BorderLayout.CENTER);
        return wrapper;
    }

    // ─── Footer ───────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false); footer.setBorder(new EmptyBorder(0,24,14,24));
        JLabel title = new JLabel("History"); title.setFont(font(11f, Font.BOLD));
        title.setForeground(TEXT_MUTED); title.setBorder(new EmptyBorder(0,0,6,0));
        historyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        historyPanel.setOpaque(false);
        footer.add(title,        BorderLayout.NORTH);
        footer.add(historyPanel, BorderLayout.CENTER);
        return footer;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  KEY BINDINGS
    // ════════════════════════════════════════════════════════════════════════

    private void setupKeys() {
        InputMap  im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // P1: Q W E
        bindKey(im, am, 'q', "p1R", () -> p1Pick("Rock"));
        bindKey(im, am, 'Q', "p1Ru",() -> p1Pick("Rock"));
        bindKey(im, am, 'w', "p1P", () -> p1Pick("Paper"));
        bindKey(im, am, 'W', "p1Pu",() -> p1Pick("Paper"));
        bindKey(im, am, 'e', "p1S", () -> p1Pick("Scissors"));
        bindKey(im, am, 'E', "p1Su",() -> p1Pick("Scissors"));

        // P2: 1 2 3 (number row)
        bindKey(im, am, '1', "p2R", () -> p2Pick("Rock"));
        bindKey(im, am, '2', "p2P", () -> p2Pick("Paper"));
        bindKey(im, am, '3', "p2S", () -> p2Pick("Scissors"));

        // P2: Numpad 1 2 3
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1, 0), "p2Rn");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, 0), "p2Pn");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD3, 0), "p2Sn");
        am.put("p2Rn", new AbstractAction() { public void actionPerformed(ActionEvent e) { p2Pick("Rock");     }});
        am.put("p2Pn", new AbstractAction() { public void actionPerformed(ActionEvent e) { p2Pick("Paper");    }});
        am.put("p2Sn", new AbstractAction() { public void actionPerformed(ActionEvent e) { p2Pick("Scissors"); }});
    }

    private void bindKey(InputMap im, ActionMap am, char key, String name, Runnable run) {
        im.put(KeyStroke.getKeyStroke(key), name);
        am.put(name, new AbstractAction() { public void actionPerformed(ActionEvent e) { run.run(); } });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GAME LOGIC
    // ════════════════════════════════════════════════════════════════════════

    private void p1Pick(String choice) {
        if (locked || p1Choice != null) return;
        p1Choice = choice;
        p1EmojiLabel.setText("✅");    // hidden until both pick
        p1StatusLabel.setText("Locked in!");
        p1StatusLabel.setForeground(WIN_GREEN);
        setP1Enabled(false);
        checkBothPicked();
    }

    private void p2Pick(String choice) {
        if (locked || p2Choice != null) return;
        p2Choice = choice;
        p2EmojiLabel.setText("✅");
        p2StatusLabel.setText("Locked in!");
        p2StatusLabel.setForeground(WIN_GREEN);
        checkBothPicked();
    }

    private void checkBothPicked() {
        if (p1Choice == null || p2Choice == null) return;
        locked = true;
        round++;
        roundLabel.setText("Round " + round);
        resultLabel.setText("Revealing…");
        resultLabel.setForeground(TEXT_SECONDARY);

        // Short delay for suspense, then reveal
        Timer t = new Timer(700, ev -> revealResult());
        t.setRepeats(false); t.start();
    }

    private void revealResult() {
        p1EmojiLabel.setText(emoji(p1Choice));
        p2EmojiLabel.setText(emoji(p2Choice));

        String outcome = evaluate(p1Choice, p2Choice);
        String msg; Color col;
        switch (outcome) {
            case "P1WIN" -> { p1Score++; msg = "🏆 Player 1 Wins!  (" + p1Choice + " beats " + p2Choice + ")"; col = ACCENT_PURPLE; }
            case "P2WIN" -> { p2Score++; msg = "🏆 Player 2 Wins!  (" + p2Choice + " beats " + p1Choice + ")"; col = ACCENT_PINK;   }
            default      -> { tieScore++; msg = "🤝 It's a Tie!  (" + p1Choice + ")";                           col = TIE_YELLOW;   }
        }
        resultLabel.setText(msg); resultLabel.setForeground(col);
        p1ScoreLabel.setText(String.valueOf(p1Score));
        p2ScoreLabel.setText(String.valueOf(p2Score));
        tieScoreLabel.setText(String.valueOf(tieScore));

        addBadge(outcome);
        flashResult();
        p1StatusLabel.setText(""); p2StatusLabel.setText("");

        Timer reset = new Timer(1800, ev -> nextRound());
        reset.setRepeats(false); reset.start();
    }

    private void nextRound() {
        p1Choice = null; p2Choice = null; locked = false;
        p1EmojiLabel.setText("❓"); p2EmojiLabel.setText("❓");
        p1StatusLabel.setText("Waiting…"); p1StatusLabel.setForeground(TEXT_MUTED);
        p2StatusLabel.setText("Press 1/2/3"); p2StatusLabel.setForeground(TEXT_MUTED);
        resultLabel.setText("Both players, choose your weapon!"); resultLabel.setForeground(TEXT_SECONDARY);
        setP1Enabled(true);
    }

    private void resetGame() {
        p1Score = 0; p2Score = 0; tieScore = 0; round = 0;
        p1Choice = null; p2Choice = null; locked = false;
        history.clear(); historyPanel.removeAll(); historyPanel.repaint();
        p1ScoreLabel.setText("0"); p2ScoreLabel.setText("0"); tieScoreLabel.setText("0");
        roundLabel.setText("Round 0");
        nextRound();
    }

    private static String evaluate(String p1, String p2) {
        if (p1.equals(p2)) return "TIE";
        if ((p1.equals("Rock")     && p2.equals("Scissors")) ||
            (p1.equals("Paper")    && p2.equals("Rock"))     ||
            (p1.equals("Scissors") && p2.equals("Paper")))   return "P1WIN";
        return "P2WIN";
    }

    private void flashResult() {
        final int[] c = {0};
        Timer t = new Timer(90, null);
        t.addActionListener(e -> {
            c[0]++; resultLabel.setVisible(c[0] % 2 == 0);
            if (c[0] >= 6) { t.stop(); resultLabel.setVisible(true); }
        });
        t.start();
    }

    private void addBadge(String outcome) {
        String sym = outcome.equals("P1WIN") ? "P1" : outcome.equals("P2WIN") ? "P2" : "TIE";
        Color  col = outcome.equals("P1WIN") ? ACCENT_PURPLE : outcome.equals("P2WIN") ? ACCENT_PINK : TIE_YELLOW;
        if (history.size() >= 12) { history.remove(0); if (historyPanel.getComponentCount() > 0) historyPanel.remove(0); }
        history.add(outcome);
        JLabel b = new JLabel(sym, SwingConstants.CENTER);
        b.setOpaque(true); b.setFont(font(10f, Font.BOLD)); b.setForeground(col);
        b.setBackground(new Color(col.getRed(), col.getGreen(), col.getBlue(), 35));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(col,1), new EmptyBorder(2,7,2,7)));
        historyPanel.add(b); historyPanel.revalidate(); historyPanel.repaint();
    }

    private void setP1Enabled(boolean en) {
        p1Rock.setEnabled(en); p1Paper.setEnabled(en); p1Scissors.setEnabled(en);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private static String emoji(String c) {
        return switch (c) { case "Rock" -> "🪨"; case "Paper" -> "📄"; case "Scissors" -> "✂️"; default -> "❓"; };
    }

    private static Font font(float size, int style) { return new Font("Segoe UI", style, 1).deriveFont(style, size); }

    private static JLabel cLbl(String text, float size, int style, Color col) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(style == Font.PLAIN && size > 20 ? new Font("Segoe UI Emoji", Font.PLAIN, (int)size) : font(size, style));
        l.setForeground(col); l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private static JPanel gradient(Color c1, Color c2) {
        return new JPanel() {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
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

    private static JButton textBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(font(12f, Font.BOLD)); b.setForeground(new Color(100,100,140));
        b.setBackground(new Color(0,0,0,0)); b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(new Color(139,92,246)); }
            public void mouseExited (MouseEvent e) { b.setForeground(new Color(100,100,140)); }
        });
        return b;
    }

    private static JButton choiceBtn(String ico, String label, Color accent, Runnable onClick) {
        JButton b = new JButton("<html><center><span style='font-size:22px'>" + ico + "</span><br>"
            + "<b style='font-size:10px;color:rgb(" + accent.getRed() + "," + accent.getGreen() + "," + accent.getBlue() + ")'>"
            + label + "</b></center></html>");
        b.setBackground(new Color(18,18,35));
        b.setForeground(TEXT_PRIMARY);
        b.setBorder(BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100), 2));
        b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBorder(BorderFactory.createLineBorder(accent, 2)); }
            public void mouseExited (MouseEvent e) { b.setBorder(BorderFactory.createLineBorder(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),100),2)); }
        });
        b.addActionListener(e -> onClick.run());
        return b;
    }

    // Custom painting for background
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setPaint(new GradientPaint(0,0,BG_DARK,0,getHeight(),new Color(5,5,15)));
        g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
        super.paintComponent(g);
    }
}
