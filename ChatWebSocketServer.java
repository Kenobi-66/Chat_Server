import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketServer extends WebSocketServer {
    private static final ConcurrentHashMap<Integer, WebSocket> webUsers = new ConcurrentHashMap<>();
    private static int webClientCount = 1000; // offset so web client IDs don't collide with TCP client IDs

    public ChatWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        int id = ++webClientCount;
        conn.setAttachment(id);
        webUsers.put(id, conn);

        DatabaseManager.registerUser(id);
        List<String> history = DatabaseManager.getChatHistory(50);
        for (String msg : history) {
            conn.send(msg);
        }

        broadcastToAll("Client #" + id + " has joined the chat (web)", id, false);
        System.out.println(">> Web client #" + id + " connected");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        int id = (int) conn.getAttachment();
        DatabaseManager.saveMessage(id, message);
        broadcastToAll(message, id, true);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        int id = (int) conn.getAttachment();
        webUsers.remove(id);
        broadcastToAll("Client #" + id + " has left the chat", id, false);
        System.out.println(">> Web client #" + id + " disconnected");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WebSocket] Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[WebSocket Server] Started on port " + getPort());
    }

    private void broadcastToAll(String message, int senderId, boolean isChatMessage) {
        String formatted = isChatMessage ? "[Client #" + senderId + "] " + message : ">> " + message;

        for (var entry : webUsers.entrySet()) {
            if (entry.getKey() != senderId) {
                entry.getValue().send(formatted);
            }
        }

        ClientHandler.broadcastToTcpClients(formatted, senderId);

        System.out.println(formatted);
    }
}