import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Rock Paper Scissors — Main Entry & Mode Selector
 * 
 * This class serves as the container for the 3 game modes:
 *  1. 🤖 vs CPU        (CPUGamePanel)
 *  2. 👥 Local PvP     (LocalPvPPanel)
 *  3. 🌐 Online PvP    (OnlinePanel)
 */
public class RockPaperScissors extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainContainer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new RockPaperScissors().setVisible(true);
        });
    }

    public RockPaperScissors() {
        super("⚡ Rock · Paper · Scissors — Ultimate Edition");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 750);
        setMinimumSize(new Dimension(860, 680));
        setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setOpaque(false);

        // Build Home Screen
        mainContainer.add(new HomePanel(), "home");

        // Build Game Panels (Lazy loading or pre-loading)
        // We'll pre-load them to make transitions snappy
        mainContainer.add(new CPUGamePanel(() -> showHome()), "cpu");
        mainContainer.add(new LocalPvPPanel(() -> showHome()), "local");
        mainContainer.add(new OnlinePanel(() -> showHome()), "online");

        getContentPane().setBackground(new Color(10, 10, 20));
        getContentPane().add(mainContainer);
    }

    private void showHome() {
        cardLayout.show(mainContainer, "home");
    }

    // ─── Home Screen Panel ────────────────────────────────────────────────────
    class HomePanel extends JPanel {
        
        public HomePanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
            
            // Header
            JLabel title = new JLabel("ROCK · PAPER · SCISSORS", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 42));
            title.setForeground(new Color(240, 240, 255));
            title.setBorder(new EmptyBorder(60, 0, 10, 0));
            
            JLabel sub = new JLabel("Select your game mode", SwingConstants.CENTER);
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            sub.setForeground(new Color(160, 160, 200));
            
            JPanel top = new JPanel(new GridLayout(2, 1)); top.setOpaque(false);
            top.add(title); top.add(sub);
            
            // Cards Container
            JPanel cards = new JPanel(new GridBagLayout());
            cards.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(20, 20, 20, 20);
            
            cards.add(modeCard("🤖 VS CPU", "Practice your skills against our AI with pattern-recognition.", new Color(139, 92, 246), "cpu"), gbc);
            cards.add(modeCard("👥 LOCAL PVP", "Play with a friend on the same keyboard/machine.", new Color(34, 211, 238), "local"), gbc);
            cards.add(modeCard("🌐 ONLINE PvP", "Host or Join a game over Wi-Fi/Local Network.", new Color(244, 114, 182), "online"), gbc);
            
            add(top, BorderLayout.NORTH);
            add(cards, BorderLayout.CENTER);
            
            JLabel footer = new JLabel("v2.0 Ultimate Edition — Developed by Amar Kumar Mandal", SwingConstants.CENTER);
            footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            footer.setForeground(new Color(100, 100, 140));
            footer.setBorder(new EmptyBorder(0, 0, 30, 0));
            add(footer, BorderLayout.SOUTH);
        }

        private JPanel modeCard(String title, String desc, Color accent, String target) {
            JPanel card = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(18, 18, 35));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 24, 24);
                    g2.dispose();
                }
            };
            card.setPreferredSize(new Dimension(240, 320));
            card.setLayout(new BorderLayout());
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(new EmptyBorder(40, 20, 40, 20));
            
            JLabel icon = new JLabel();
                    ImageIcon ii;
                    java.net.URL imgUrl = null;
                    if (target.equals("cpu")) {
                        imgUrl = getClass().getResource("/CPU image.png");
                    } else if (target.equals("local")) {
                        imgUrl = getClass().getResource("/verses.jpg");
                    }

                    if (imgUrl != null) {
                        ii = new ImageIcon(imgUrl);
                        Image img = ii.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                        icon.setIcon(new ImageIcon(img));
                    } else {
                        icon.setText(title.split(" ")[0]);
                        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
                    }
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel tl = new JLabel(title.substring(title.indexOf(" ")).trim(), SwingConstants.CENTER);
            tl.setFont(new Font("Segoe UI", Font.BOLD, 18));
            tl.setForeground(accent);
            tl.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JTextArea ds = new JTextArea(desc);
            ds.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            ds.setForeground(new Color(160, 160, 200));
            ds.setLineWrap(true); ds.setWrapStyleWord(true);
            ds.setEditable(false); ds.setFocusable(false); ds.setOpaque(false);
            ds.setAlignmentX(Component.CENTER_ALIGNMENT);
            ds.setMargin(new Insets(15, 0, 0, 0));
            
            content.add(icon);
            content.add(Box.createVerticalStrut(15));
            content.add(tl);
            content.add(Box.createVerticalStrut(10));
            content.add(ds);
            
            card.add(content, BorderLayout.CENTER);
            
            card.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { cardLayout.show(mainContainer, target); }
                public void mouseEntered(MouseEvent e) { card.setBorder(BorderFactory.createLineBorder(accent, 2)); }
                public void mouseExited(MouseEvent e) { card.setBorder(null); }
            });
            
            return card;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, new Color(10, 10, 20), 0, getHeight(), new Color(5, 5, 15)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
