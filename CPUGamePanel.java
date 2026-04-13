import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.Timer;

/**
 * CPUGamePanel — The original vs-CPU game logic extracted into a reusable panel.
 */
public class CPUGamePanel extends JPanel {

    // ─── Constants (matching the original design) ─────────────────────────────
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
    private static final Color BORDER_GLOW    = new Color(139, 92, 246, 80);

    // ─── Game State ──────────────────────────────────────────────────────────
    private int    playerScore = 0, cpuScore = 0, tieScore = 0, roundsPlayed = 0;
    private String playerChoice = "", cpuChoice = "";
    private final  String[] CHOICES = {"Rock", "Paper", "Scissors"};
    private final  Random   random  = new Random();
    private boolean busy     = false;

    private int currentStreak = 0;
    private int bestWinStreak = 0;
    private int gameMode      = 0;
    private int matchPlayerWins = 0, matchCpuWins = 0;
    private boolean matchOver  = false;
    private boolean hardMode   = false;
    private final Deque<String> moveHistory = new ArrayDeque<>();
    private final Map<String, Integer> playerMoveCounts = new HashMap<>();
    private final Map<String, Integer> cpuMoveCounts    = new HashMap<>();
    private boolean muted = false;
    private final java.util.List<String> history = new ArrayList<>();

    // ─── UI Refs ─────────────────────────────────────────────────────────────
    private JLabel       resultLabel, roundLabel;
    private JLabel       playerChoiceLabel, cpuChoiceLabel;
    private JLabel       playerScoreLabel, cpuScoreLabel, tieScoreLabel;
    private JLabel       playerRateLabel, cpuRateLabel, streakLabel;
    private JPanel       pipsPanel, historyPanel;
    private JButton      muteBtn, diffBtn;
    private JButton[]    modeBtns = new JButton[4];
    private GlowButton   rockBtn, paperBtn, scissorsBtn;
    private JLayeredPane arenaLayered;
    private ConfettiPanel confettiPanel;

    private final Runnable backAction;
    private ImageIcon cpuAvatar;

    public CPUGamePanel(Runnable backAction) {
        this.backAction = backAction;
        setOpaque(false);
        setLayout(new BorderLayout());

        for (String c : CHOICES) { playerMoveCounts.put(c, 0); cpuMoveCounts.put(c, 0); }

        loadResources();
        buildUI();
        setupKeyBindings();
        startPulseAnimation();
    }

    private void loadResources() {
        try {
            java.net.URL imgUrl = getClass().getResource("/CPU image.png");
            if (imgUrl != null) {
                ImageIcon icon = new ImageIcon(imgUrl);
                Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                cpuAvatar = new ImageIcon(img);
            }
        } catch (Exception e) {
            System.err.println("Could not load CPU image: " + e.getMessage());
        }
    }

    private void buildUI() {
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        GradientPanel header = new GradientPanel(new Color(15, 15, 30), new Color(8, 8, 20));
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(80, 80, 120)),
            new EmptyBorder(14, 24, 14, 24)
        ));

        JButton back = createTextButton("← Back");
        back.addActionListener(e -> { resetGame(); backAction.run(); });

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("PLAYER vs CPU", SwingConstants.CENTER);
        title.setFont(loadFont(22f, Font.BOLD)); title.setForeground(TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Pattern-Reading AI ready", SwingConstants.CENTER);
        subtitle.setFont(loadFont(10f, Font.PLAIN)); subtitle.setForeground(TEXT_SECONDARY);
        titleBox.add(title); titleBox.add(subtitle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        streakLabel = new JLabel("");
        streakLabel.setFont(loadFont(12f, Font.BOLD));
        muteBtn = createIconButton("🔊");
        muteBtn.addActionListener(e -> toggleMute());
        roundLabel = new JLabel("Round 0");
        roundLabel.setFont(loadFont(12f, Font.BOLD)); roundLabel.setForeground(TEXT_MUTED);
        right.add(streakLabel); right.add(muteBtn); right.add(roundLabel);

        header.add(back,     BorderLayout.WEST);
        header.add(titleBox, BorderLayout.CENTER);
        header.add(right,    BorderLayout.EAST);

        JPanel sub = new JPanel(new BorderLayout());
        sub.setBackground(new Color(12, 12, 26));
        sub.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(40, 40, 80)),
            new EmptyBorder(7, 24, 7, 24)
        ));

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        modeRow.setOpaque(false);
        String[] mNames = {"∞ Free", "Best of 3", "Best of 5", "Best of 7"};
        int[]    mVals  = {0, 3, 5, 7};
        for (int i = 0; i < 4; i++) {
            final int v = mVals[i], idx = i;
            modeBtns[i] = createToggleBtn(mNames[i], i == 0);
            modeBtns[i].addActionListener(e -> setMode(v, idx));
            modeRow.add(modeBtns[i]);
        }

        JPanel diffRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        diffRow.setOpaque(false);
        diffBtn = createToggleBtn("🎲 Easy", false);
        diffBtn.addActionListener(e -> toggleDifficulty());
        diffRow.add(diffBtn);

        sub.add(modeRow, BorderLayout.WEST);
        sub.add(diffRow, BorderLayout.EAST);

        outer.add(header, BorderLayout.NORTH);
        outer.add(sub,    BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 24, 12, 24));
        center.add(buildScoreboard(), BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0, 8)); mid.setOpaque(false);
        pipsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2)); pipsPanel.setOpaque(false);
        pipsPanel.setPreferredSize(new Dimension(0, 28));
        mid.add(pipsPanel, BorderLayout.NORTH);
        mid.add(buildArena(), BorderLayout.CENTER);
        center.add(mid, BorderLayout.CENTER);
        center.add(buildChoiceButtons(), BorderLayout.SOUTH);
        return center;
    }

    private JPanel buildScoreboard() {
        JPanel board = new JPanel(new GridLayout(1, 3, 14, 0));
        board.setOpaque(false); board.setPreferredSize(new Dimension(0, 100));
        JPanel youCard = scoreCard("YOU", ACCENT_PURPLE);
        playerScoreLabel = (JLabel)youCard.getClientProperty("val");
        playerRateLabel  = (JLabel)youCard.getClientProperty("rate");
        JPanel tieCard = scoreCard("TIES", TIE_YELLOW);
        tieScoreLabel = (JLabel)tieCard.getClientProperty("val");
        JPanel cpuCard = scoreCard("CPU", ACCENT_PINK);
        cpuScoreLabel = (JLabel)cpuCard.getClientProperty("val");
        cpuRateLabel  = (JLabel)cpuCard.getClientProperty("rate");
        board.add(youCard); board.add(tieCard); board.add(cpuCard);
        return board;
    }

    private JPanel scoreCard(String title, Color accent) {
        RoundedPanel card = new RoundedPanel(16, BG_CARD, accent);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(8, 0, 8, 0));
        card.add(cLabel(title, 11f, Font.BOLD, accent));
        JLabel val = cLabel("0", 38f, Font.BOLD, TEXT_PRIMARY);
        card.add(val); card.putClientProperty("val", val);
        if (!title.equals("TIES")) {
            JLabel rate = cLabel("", 10f, Font.PLAIN, TEXT_MUTED);
            card.add(rate); card.putClientProperty("rate", rate);
        }
        return card;
    }

    private JLayeredPane buildArena() {
        arenaLayered = new JLayeredPane();
        RoundedPanel arena = new RoundedPanel(20, BG_CARD2, BORDER_GLOW);
        arena.setLayout(new BorderLayout(0, 0));
        JPanel choiceRow = new JPanel(new GridLayout(1, 3, 0, 0)); choiceRow.setOpaque(false);
        choiceRow.setBorder(new EmptyBorder(10, 0, 0, 0));
        playerChoiceLabel = buildChoiceLabel("❓", ACCENT_PURPLE, "YOU");
        choiceRow.add(playerChoiceLabel.getParent());
        JPanel vsPanel = new JPanel(new GridBagLayout()); vsPanel.setOpaque(false);
        JLabel vs = new JLabel("VS"); vs.setFont(loadFont(22f, Font.BOLD)); vs.setForeground(new Color(80, 80, 120));
        vsPanel.add(vs); choiceRow.add(vsPanel);
        cpuChoiceLabel = buildChoiceLabel("❓", ACCENT_PINK, "CPU");
        choiceRow.add(cpuChoiceLabel.getParent());
        JPanel resPanel = new JPanel(new GridBagLayout()); resPanel.setOpaque(false);
        resPanel.setPreferredSize(new Dimension(0, 58));
        resultLabel = new JLabel("Choose your weapon!", SwingConstants.CENTER);
        resultLabel.setFont(loadFont(16f, Font.BOLD)); resultLabel.setForeground(TEXT_SECONDARY);
        resPanel.add(resultLabel);
        arena.add(choiceRow, BorderLayout.CENTER); arena.add(resPanel, BorderLayout.SOUTH);
        confettiPanel = new ConfettiPanel(); confettiPanel.setOpaque(false);
        arenaLayered.add(arena, JLayeredPane.DEFAULT_LAYER);
        arenaLayered.add(confettiPanel, JLayeredPane.PALETTE_LAYER);
        arenaLayered.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int w = arenaLayered.getWidth(), h = arenaLayered.getHeight();
                arena.setBounds(0, 0, w, h); confettiPanel.setBounds(0, 0, w, h);
            }
        });
        return arenaLayered;
    }

    private JLabel buildChoiceLabel(String text, Color accent, String owner) {
        JPanel wrap = new JPanel(); wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false); wrap.setBorder(new EmptyBorder(10, 20, 10, 20));
        JLabel ownerLbl = new JLabel(owner, SwingConstants.CENTER);
        ownerLbl.setFont(loadFont(11f, Font.BOLD)); ownerLbl.setForeground(accent);
        ownerLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emojiLbl = new JLabel(text, SwingConstants.CENTER);
        if (owner.equals("CPU") && cpuAvatar != null) {
            emojiLbl.setText("");
            emojiLbl.setIcon(cpuAvatar);
        } else {
            emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        }
        emojiLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrap.add(ownerLbl); wrap.add(Box.createVerticalStrut(4)); wrap.add(emojiLbl);
        return emojiLbl;
    }

    private JPanel buildChoiceButtons() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0)); row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 100));
        rockBtn = new GlowButton("🪨", "ROCK", ACCENT_PURPLE, "[ R ]");
        paperBtn = new GlowButton("📄", "PAPER", ACCENT_CYAN, "[ P ]");
        scissorsBtn = new GlowButton("✂️", "SCISSORS", ACCENT_PINK, "[ S ]");
        rockBtn.addActionListener(e -> play("Rock"));
        paperBtn.addActionListener(e -> play("Paper"));
        scissorsBtn.addActionListener(e -> play("Scissors"));
        row.add(rockBtn); row.add(paperBtn); row.add(scissorsBtn);
        return row;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 24, 14, 24));
        JLabel histTitle = new JLabel("Recent History"); histTitle.setFont(loadFont(11f, Font.BOLD));
        histTitle.setForeground(TEXT_MUTED); histTitle.setBorder(new EmptyBorder(0, 0, 6, 0));
        historyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); historyPanel.setOpaque(false);
        footer.add(histTitle, BorderLayout.NORTH); footer.add(historyPanel, BorderLayout.CENTER);
        return footer;
    }

    private void setupKeyBindings() {
        // Handled via the main frame generally, but for single-panel focus:
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        bindKey(im, am, 'r', "rock", () -> play("Rock"));
        bindKey(im, am, 'p', "paper", () -> play("Paper"));
        bindKey(im, am, 's', "scissors", () -> play("Scissors"));
    }

    private void bindKey(InputMap im, ActionMap am, char key, String name, Runnable action) {
        im.put(KeyStroke.getKeyStroke(key), name);
        am.put(name, new AbstractAction() { public void actionPerformed(ActionEvent e) { action.run(); } });
    }

    private void play(String choice) {
        if (busy || matchOver) return;
        busy = true; setButtonsEnabled(false);
        playerChoice = choice; cpuChoice = hardMode ? getCpuHard() : CHOICES[random.nextInt(3)];
        roundsPlayed++;
        playerMoveCounts.merge(choice, 1, Integer::sum); cpuMoveCounts.merge(cpuChoice, 1, Integer::sum);
        moveHistory.addLast(choice); if (moveHistory.size() > 10) moveHistory.removeFirst();
        String result = determineWinner(playerChoice, cpuChoice);
        updateScores(result); animateResult(result);
    }

    private String getCpuHard() {
        if (moveHistory.size() < 2) return CHOICES[random.nextInt(3)];
        Map<String, Integer> freq = new HashMap<>(Map.of("Rock", 0, "Paper", 0, "Scissors", 0));
        String[] arr = moveHistory.toArray(new String[0]);
        int start = Math.max(0, arr.length - 5);
        for (int i = start; i < arr.length; i++) freq.merge(arr[i], 1, Integer::sum);
        String best = freq.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
        return switch (best) { case "Rock" -> "Paper"; case "Paper" -> "Scissors"; case "Scissors" -> "Rock"; default -> CHOICES[random.nextInt(3)]; };
    }

    private String determineWinner(String p, String c) {
        if (p.equals(c)) return "TIE";
        if ((p.equals("Rock") && c.equals("Scissors")) || (p.equals("Paper") && c.equals("Rock")) || (p.equals("Scissors") && c.equals("Paper"))) return "WIN";
        return "LOSE";
    }

    private void updateScores(String result) {
        switch (result) { case "WIN"->{playerScore++; matchPlayerWins++;} case "LOSE"->{cpuScore++; matchCpuWins++;} case "TIE"->tieScore++; }
        if (result.equals("WIN")) currentStreak = Math.max(1, currentStreak + 1); else if (result.equals("LOSE")) currentStreak = Math.min(-1, currentStreak - 1); else currentStreak = 0;
        if (currentStreak > bestWinStreak) bestWinStreak = currentStreak;
        roundLabel.setText("Round " + roundsPlayed); refreshScoreLabels(); refreshStreakLabel(); refreshPips(); addHistoryBadge(result);
    }

    private void refreshScoreLabels() {
        playerScoreLabel.setText(String.valueOf(playerScore)); cpuScoreLabel.setText(String.valueOf(cpuScore)); tieScoreLabel.setText(String.valueOf(tieScore));
        int contested = roundsPlayed - tieScore;
        if (contested > 0) {
            playerRateLabel.setText(String.format("%.0f%% win rate", playerScore * 100.0 / contested));
            cpuRateLabel.setText(String.format("%.0f%% win rate", cpuScore * 100.0 / contested));
        } else { playerRateLabel.setText(""); cpuRateLabel.setText(""); }
    }

    private void refreshStreakLabel() {
        if (currentStreak >= 3) { streakLabel.setText("🔥 " + currentStreak + " streak"); streakLabel.setForeground(WIN_GREEN); }
        else if (currentStreak <= -3) { streakLabel.setText("💔 " + Math.abs(currentStreak) + " streak"); streakLabel.setForeground(LOSE_RED); }
        else streakLabel.setText("");
    }

    private void refreshPips() {
        pipsPanel.removeAll(); if (gameMode == 0) { pipsPanel.revalidate(); pipsPanel.repaint(); return; }
        int needed = (gameMode + 1) / 2;
        addPipLabel("YOU ", ACCENT_PURPLE); for (int i = 0; i < needed; i++) addPip(i < matchPlayerWins, ACCENT_PURPLE);
        addPipLabel("  vs  ", TEXT_MUTED); for (int i = 0; i < needed; i++) addPip(i < matchCpuWins, ACCENT_PINK);
        addPipLabel("  CPU", ACCENT_PINK);
        pipsPanel.revalidate(); pipsPanel.repaint();
        if (!matchOver && (matchPlayerWins >= needed || matchCpuWins >= needed)) {
            matchOver = true; boolean playerWon = matchPlayerWins >= needed;
            Timer t = new Timer(500, e -> showMatchOverlay(playerWon)); t.setRepeats(false); t.start();
        }
    }

    private void addPipLabel(String text, Color col) { JLabel l = new JLabel(text); l.setFont(loadFont(10f, Font.BOLD)); l.setForeground(col); pipsPanel.add(l); }
    private void addPip(boolean filled, Color col) { JLabel p = new JLabel(filled ? "●" : "○"); p.setFont(loadFont(13f, Font.PLAIN)); p.setForeground(filled?col:TEXT_MUTED); pipsPanel.add(p); }

    private void animateResult(String result) {
        playerChoiceLabel.setText(choiceEmoji(playerChoice)); cpuChoiceLabel.setText("⏳"); resultLabel.setText("CPU is thinking…"); resultLabel.setForeground(TEXT_SECONDARY); playSound("click");
        Timer t = new Timer(700, ev -> {
            if (cpuAvatar != null) {
                cpuChoiceLabel.setIcon(null);
                cpuChoiceLabel.setText(choiceEmoji(cpuChoice));
            } else {
                cpuChoiceLabel.setText(choiceEmoji(cpuChoice));
            }
            String msg = "🤝 It's a Tie!"; Color col = TIE_YELLOW;
            if (result.equals("WIN")) { msg = "🏆 You Win!"; col = WIN_GREEN; confettiPanel.burst(false); playSound("win"); }
            if (result.equals("LOSE")) { msg = "💀 You Lose!"; col = LOSE_RED; playSound("lose"); }
            if (result.equals("TIE")) playSound("tie");
            resultLabel.setText(msg + " (" + playerChoice + " vs " + cpuChoice + ")"); resultLabel.setForeground(col);
            flashLabel(resultLabel); bumpScore(result); busy = false; setButtonsEnabled(true);
        }); t.setRepeats(false); t.start();
    }

    private void flashLabel(JLabel lbl) {
        final int[] c = {0}; Timer t = new Timer(80, null);
        t.addActionListener(e -> { c[0]++; lbl.setVisible(c[0] % 2 == 0); if (c[0] >= 6) { t.stop(); lbl.setVisible(true); } });
        t.start();
    }

    private void bumpScore(JLabel target) { // overload to prevent duplication
        final int[] step = {0}; Timer t = new Timer(25, null);
        t.addActionListener(e -> {
            step[0]++; float s = 1f + 0.25f * (float) Math.abs(Math.sin(step[0] * Math.PI / 6.0));
            target.setFont(loadFont(38f * s, Font.BOLD));
            if (step[0] >= 6) { t.stop(); target.setFont(loadFont(38f, Font.BOLD)); }
        }); t.start();
    }
    private void bumpScore(String res) { bumpScore(res.equals("WIN")?playerScoreLabel : res.equals("LOSE")?cpuScoreLabel : tieScoreLabel); }

    private void showMatchOverlay(boolean playerWon) {
        confettiPanel.burst(playerWon); JPanel overlay = new JPanel(new GridBagLayout()); overlay.setBackground(new Color(0,0,0,190));
        overlay.setBounds(0,0,arenaLayered.getWidth(),arenaLayered.getHeight());
        RoundedPanel card = new RoundedPanel(24, BG_CARD, playerWon?WIN_GREEN:LOSE_RED); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); card.setBorder(new EmptyBorder(22,44,22,44));
        Color col = playerWon?WIN_GREEN:LOSE_RED;
        addCardRow(card, playerWon?"🏆":"💀", 48, true, TEXT_PRIMARY); card.add(Box.createVerticalStrut(6));
        addCardRow(card, playerWon?"YOU WIN THE MATCH!":"CPU WINS!",17f, false, col); card.add(Box.createVerticalStrut(4));
        addCardRow(card, matchPlayerWins + " – " + matchCpuWins, 28f, false, TEXT_PRIMARY); card.add(Box.createVerticalStrut(16));
        JButton again = createTextButton("▶ Play Again"); again.setFont(loadFont(13f, Font.BOLD)); again.setForeground(col); again.setAlignmentX(Component.CENTER_ALIGNMENT);
        again.addActionListener(e -> { arenaLayered.remove(overlay); arenaLayered.repaint(); resetMatch(); }); card.add(again); overlay.add(card);
        arenaLayered.add(overlay, JLayeredPane.MODAL_LAYER); arenaLayered.revalidate(); arenaLayered.repaint();
    }
    private void addCardRow(JPanel box, String text, float size, boolean emoji, Color col) {
        JLabel l = new JLabel(text, SwingConstants.CENTER); l.setFont(emoji?new Font("Segoe UI Emoji", Font.PLAIN, (int)size):loadFont(size, Font.BOLD));
        l.setForeground(col); l.setAlignmentX(Component.CENTER_ALIGNMENT); box.add(l);
    }

    private void resetMatch() {
        matchPlayerWins=0; matchCpuWins=0; matchOver=false; busy=false; playerScore=0; cpuScore=0; tieScore=0; roundsPlayed=0; currentStreak=0;
        history.clear(); historyPanel.removeAll(); historyPanel.repaint();
        playerChoiceLabel.setText("❓"); 
        if (cpuAvatar != null) {
            cpuChoiceLabel.setText("");
            cpuChoiceLabel.setIcon(cpuAvatar);
        } else {
            cpuChoiceLabel.setText("❓");
        }
        resultLabel.setText("Choose your weapon!");
        resultLabel.setForeground(TEXT_SECONDARY); roundLabel.setText("Round 0"); streakLabel.setText("");
        refreshScoreLabels(); refreshPips(); setButtonsEnabled(true);
    }

    private void resetGame() { resetMatch(); bestWinStreak=0; for (String c:CHOICES) {playerMoveCounts.put(c,0); cpuMoveCounts.put(c,0);} moveHistory.clear(); }

    private void setMode(int mode, int idx) { gameMode=mode; for (int i=0; i<modeBtns.length; i++) setActive(modeBtns[i], i==idx); resetMatch(); }
    private void toggleDifficulty() { hardMode=!hardMode; diffBtn.setText(hardMode?"🤖 Hard":"🎲 Easy"); setActive(diffBtn, hardMode); moveHistory.clear(); }

    private void toggleMute() { muted=!muted; muteBtn.setText(muted?"🔇":"🔊"); }
    private void playSound(String type) {
        if (muted) return;
        new Thread(() -> {
            try {
                AudioFormat f = new AudioFormat(44100, 16, 1, true, false); SourceDataLine l = AudioSystem.getSourceDataLine(f);
                l.open(f, 4096); l.start(); int[] fr, d;
                switch (type) { case "win"->{fr=new int[]{523,659,784}; d=new int[]{70,70,110};} case "lose"->{fr=new int[]{392,330,262}; d=new int[]{70,70,140};}
                                case "tie"->{fr=new int[]{440,460}; d=new int[]{55,55};} default->{fr=new int[]{660}; d=new int[]{30};} }
                byte[] b = buildTone(fr,d); l.write(b,0,b.length); l.drain(); l.close();
            } catch (Exception ignored) {}
        }).start();
    }
    private byte[] buildTone(int[] fr, int[] d) {
        int total=0; for (int i:d) total+=i*44100/1000; byte[] b = new byte[total*2]; int p=0;
        for (int i=0; i<fr.length; i++) {
            int s = d[i]*44100/1000; for (int j=0; j<s; j++) {
                double f = 1.0 - (double)j/s; short v = (short)(Math.sin(2*Math.PI*fr[i]*j/44100.0)*18000*f);
                b[p++] = (byte)(v&0xFF); b[p++] = (byte)((v>>8)&0xFF);
            }
        } return b;
    }

    // UTILS
    private String choiceEmoji(String c) { return switch (c) { case "Rock"->"🪨"; case "Paper"->"📄"; case "Scissors"->"✂️"; default->"❓"; }; }
    private void setButtonsEnabled(boolean en) { rockBtn.setEnabled(en); paperBtn.setEnabled(en); scissorsBtn.setEnabled(en); }
    private void addHistoryBadge(String result) {
        String sym = result.equals("WIN")?"W":result.equals("LOSE")?"L":"T"; Color col = result.equals("WIN")?WIN_GREEN:result.equals("LOSE")?LOSE_RED:TIE_YELLOW;
        if (history.size()>=12) {history.remove(0); historyPanel.remove(0);} history.add(result);
        JLabel b = new JLabel(sym, SwingConstants.CENTER); b.setOpaque(true); b.setBackground(new Color(col.getRed(),col.getGreen(),col.getBlue(),40)); b.setForeground(col);
        b.setFont(loadFont(11f, Font.BOLD)); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(col,1), new EmptyBorder(2,8,2,8)));
        historyPanel.add(b); historyPanel.revalidate(); historyPanel.repaint();
    }
    private Font loadFont(float s, int st) { return new Font("Segoe UI", st, 1).deriveFont(st, s); }
    private JLabel cLabel(String t, float s, int st, Color c) { JLabel l = new JLabel(t, SwingConstants.CENTER); l.setFont(loadFont(s, st)); l.setForeground(c); l.setAlignmentX(Component.CENTER_ALIGNMENT); return l; }
    private JButton createTextButton(String t) {
        JButton b = new JButton(t); b.setFont(loadFont(12f, Font.BOLD)); b.setForeground(TEXT_MUTED); b.setBackground(new Color(0,0,0,0)); b.setBorderPainted(false); b.setContentAreaFilled(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e){if(b.isEnabled())b.setForeground(ACCENT_PURPLE);} public void mouseExited(MouseEvent e){b.setForeground(TEXT_MUTED);} }); return b;
    }
    private JButton createIconButton(String i) { JButton b = createTextButton(i); b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15)); return b; }
    private JButton createToggleBtn(String t, boolean a) { JButton b = new JButton(t); b.setFont(loadFont(11f, Font.BOLD)); b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); setActive(b, a); return b; }
    private void setActive(JButton b, boolean a) {
        if (a) { b.setBackground(ACCENT_PURPLE); b.setForeground(Color.WHITE); b.setBorder(BorderFactory.createLineBorder(ACCENT_PURPLE, 1)); }
        else { b.setBackground(new Color(30, 30, 50)); b.setForeground(TEXT_SECONDARY); b.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 100), 1)); }
    }

    private void startPulseAnimation() {
        new Timer(50, e -> repaint()).start();
    }

    // COPIED INNER CLASSES FOR STANDALONE
    class GradientPanel extends JPanel {
        private Color c1, c2;
        public GradientPanel(Color c1, Color c2) { this.c1 = c1; this.c2 = c2; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
            g2.fillRect(0, 0, getWidth(), getHeight()); g2.dispose();
        }
    }
    class RoundedPanel extends JPanel {
        private int arc; private Color bg, border;
        public RoundedPanel(int a, Color b, Color br) { this.arc = a; this.bg = b; this.border = br; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setColor(border); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, arc, arc);
            g2.dispose();
        }
    }
    class GlowButton extends JButton {
        private Color color; private String label, sub;
        public GlowButton(String l, String t, Color c, String s) { this.label=l; this.color=c; this.sub=s; setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean h = getModel().isRollover(); g2.setColor(h ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 40) : BG_CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); g2.setColor(h ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
            g2.setStroke(new BasicStroke(h?2.5f:1.5f)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
            g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28)); FontMetrics fm = g2.getFontMetrics();
            int lx = (getWidth()-fm.stringWidth(label))/2; int ly = getHeight()/2 - 2; g2.setColor(TEXT_PRIMARY); g2.drawString(label, lx, ly);
            g2.setFont(loadFont(10f, Font.BOLD)); fm = g2.getFontMetrics();
            int tx = (getWidth()-fm.stringWidth(sub))/2; int ty = getHeight()-15; g2.setColor(h?color:TEXT_MUTED); g2.drawString(sub, tx, ty);
            g2.dispose();
        }
    }
    class ConfettiPanel extends JPanel {
        class Particle { float x,y,vx,vy,size,r,g,b,a; }
        private java.util.List<Particle> particles = new ArrayList<>();
        private Timer timer;
        public void burst(boolean big) {
            int count = big ? 150 : 40; for (int i=0; i<count; i++) {
                Particle p = new Particle(); p.x = getWidth()/2; p.y = getHeight()/2;
                double ang = random.nextDouble()*2*Math.PI; double spd = random.nextDouble()*(big?15:8);
                p.vx = (float)(Math.cos(ang)*spd); p.vy = (float)(Math.sin(ang)*spd);
                p.size = 3+random.nextFloat()*5; p.r = random.nextFloat(); p.g = random.nextFloat(); p.b = random.nextFloat(); p.a = 1f;
                particles.add(p);
            }
            if (timer==null) { timer = new Timer(20, e -> {
                for (int i=particles.size()-1; i>=0; i--) {
                    Particle p = particles.get(i); p.x+=p.vx; p.y+=p.vy; p.vy+=0.2f; p.a-=0.02f; if(p.a<=0) particles.remove(i);
                } repaint(); if(particles.isEmpty()){timer.stop(); timer=null;}
            }); timer.start(); }
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (Particle p : new ArrayList<>(particles)) {
                g2.setColor(new Color(p.r,p.g,p.b,p.a)); g2.fillOval((int)p.x, (int)p.y, (int)p.size, (int)p.size);
            } g2.dispose();
        }
    }
}
