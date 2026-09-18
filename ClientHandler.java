import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable
{
    private static final ConcurrentHashMap<Integer, PrintWriter> connectedUsers = new ConcurrentHashMap<>();
    private Socket clientSocket;
    private int id;

    public ClientHandler (Socket socket, int clientId)
    {
        clientSocket = socket;
        id = clientId;
    }

    @Override 
    public void run()
    {
        try
        {
            BufferedReader in = new BufferedReader (new InputStreamReader (clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            connectedUsers.put(id, out);

            DatabaseManager.registerUser(id);
            
            List<String> history = DatabaseManager.getChatHistory(50);
            for (String msg : history) {
                out.println(msg);
            }

            broadcast("Client #"+id+" has joined the chat", id, false);

            String clientMessage;
            
            while ((clientMessage = in.readLine()) != null)
            {
                if ("exit".equalsIgnoreCase(clientMessage))
                    break;

                DatabaseManager.saveMessage(id, clientMessage);

                broadcast(clientMessage, id, true);
            }
            
        } catch (IOException e)
        {
            System.err.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        } finally 
        {
            broadcast("Client #"+id+" has left the chat", id, false);
            connectedUsers.remove(id);
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
    private void broadcast(String message, int senderId, boolean isChatMessge)
    {
        String formatted = isChatMessge ? "[Client #" + senderId + "] " + message : ">> " + message;

        for (PrintWriter values : connectedUsers.values())
            values.println(formatted);
        System.out.println(formatted);
    }
    public static void broadcastToTcpClients(String formatted, int senderId) {
    for (var entry : connectedUsers.entrySet()) {
        if (entry.getKey() != senderId) {
            entry.getValue().println(formatted);
        }
    }
}
}