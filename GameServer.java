import java.io.*;
import java.net.*;

/**
 * GameServer — runs on the host's machine.
 * Accepts exactly 2 TCP connections, collects choices from both,
 * evaluates the result, and broadcasts it to both players simultaneously.
 *
 * Protocol (plain text, newline-delimited):
 *   Server → Client : ROLE:1 | ROLE:2 | OPPONENT_JOINED | RESULT:WIN|myChoice|theirChoice
 *                     OPPONENT_REMATCH | OPPONENT_QUIT
 *   Client → Server : CHOICE:Rock | REMATCH | QUIT
 */
public class GameServer {

    public static final int DEFAULT_PORT = 9876;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    public interface ServerListener {
        void onPlayerConnected(int playerNum);   // called on a background thread
        void onBothReady();
        void onPlayerDisconnected(int playerNum);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private ServerListener listener;

    private PrintWriter p1Out, p2Out;
    private String p1Choice = null, p2Choice = null;

    public GameServer(int port) { this.port = port; }
    public void setListener(ServerListener l) { this.listener = l; }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        running = true;
        Thread t = new Thread(this::acceptLoop, "GameServer-Accept");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
    }

    // ── Accept loop ───────────────────────────────────────────────────────────
    private void acceptLoop() {
        try {
            // ── Player 1 ──────────────────────────────────────────────────────
            Socket s1 = serverSocket.accept();
            p1Out = new PrintWriter(s1.getOutputStream(), true);
            p1Out.println("ROLE:1");
            if (listener != null) listener.onPlayerConnected(1);

            // ── Player 2 ──────────────────────────────────────────────────────
            Socket s2 = serverSocket.accept();
            p2Out = new PrintWriter(s2.getOutputStream(), true);
            p2Out.println("ROLE:2");
            p1Out.println("OPPONENT_JOINED");
            if (listener != null) listener.onBothReady();

            // ── Start reader threads ───────────────────────────────────────────
            spawnReader(s1, p1Out, p2Out, 1);
            spawnReader(s2, p2Out, p1Out, 2);

        } catch (IOException e) {
            if (running) e.printStackTrace();
        }
    }

    private void spawnReader(Socket sock, PrintWriter myOut, PrintWriter otherOut, int num) {
        Thread t = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("CHOICE:")) {
                        handleChoice(num, line.substring(7));
                    } else if (line.equals("REMATCH")) {
                        resetChoices();
                        otherOut.println("OPPONENT_REMATCH");
                    } else if (line.equals("QUIT")) {
                        otherOut.println("OPPONENT_QUIT");
                        if (listener != null) listener.onPlayerDisconnected(num);
                        break;
                    }
                }
            } catch (IOException e) {
                if (running) {
                    otherOut.println("OPPONENT_QUIT");
                    if (listener != null) listener.onPlayerDisconnected(num);
                }
            }
        }, "GameServer-P" + num);
        t.setDaemon(true);
        t.start();
    }

    // ── Choice handling ───────────────────────────────────────────────────────
    private synchronized void handleChoice(int playerNum, String choice) {
        if (playerNum == 1) p1Choice = choice;
        else                p2Choice = choice;

        if (p1Choice != null && p2Choice != null) {
            // Both submitted — evaluate and broadcast
            p1Out.println("RESULT:" + evaluate(p1Choice, p2Choice) + "|" + p1Choice + "|" + p2Choice);
            p2Out.println("RESULT:" + evaluate(p2Choice, p1Choice) + "|" + p2Choice + "|" + p1Choice);
            p1Choice = null; p2Choice = null;
        }
    }

    private synchronized void resetChoices() {
        p1Choice = null; p2Choice = null;
    }

    private static String evaluate(String mine, String theirs) {
        if (mine.equals(theirs)) return "TIE";
        if ((mine.equals("Rock")     && theirs.equals("Scissors")) ||
            (mine.equals("Paper")    && theirs.equals("Rock"))     ||
            (mine.equals("Scissors") && theirs.equals("Paper")))   return "WIN";
        return "LOSE";
    }

    // ── Utility ───────────────────────────────────────────────────────────────
    public static String getLocalIP() {
        try {
            // Prefer non-loopback address
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public int getPort() { return port; }
}
