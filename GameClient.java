import java.io.*;
import java.net.*;

/**
 * GameClient — used by both host (connects to localhost) and guest (connects to host IP).
 * Fires all incoming messages to a MessageListener on a background reader thread.
 * Callers must dispatch UI updates to the EDT themselves.
 */
public class GameClient {

    public interface MessageListener {
        void onMessage(String message);   // raw protocol line
        void onDisconnected();
        void onError(String message);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;
    private MessageListener listener;

    public void setListener(MessageListener l) { this.listener = l; }
    public boolean isConnected()               { return connected; }

    // ── Connect ───────────────────────────────────────────────────────────────
    public void connect(String host, int port) throws IOException {
        socket    = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000); // 5s timeout
        out       = new PrintWriter(socket.getOutputStream(), true);
        connected = true;
        startReader();
    }

    private void startReader() {
        Thread t = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    if (listener != null) listener.onMessage(msg);
                }
            } catch (IOException e) {
                if (connected && listener != null) listener.onDisconnected();
            } finally {
                connected = false;
            }
        }, "GameClient-Reader");
        t.setDaemon(true);
        t.start();
    }

    // ── Send helpers ──────────────────────────────────────────────────────────
    public void sendChoice(String choice) { send("CHOICE:" + choice); }
    public void sendRematch()             { send("REMATCH"); }
    public void sendQuit()                { send("QUIT"); }

    private void send(String msg) {
        if (out != null && connected) out.println(msg);
    }

    // ── Disconnect ────────────────────────────────────────────────────────────
    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}
