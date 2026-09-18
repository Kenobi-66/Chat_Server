import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class chatServer 
{
    public static void main(String[] args) throws IOException
    {
        try(ServerSocket serverSocket = new ServerSocket(5000)) {
            ExecutorService threadpool = Executors.newFixedThreadPool(5);
            int clientCount = 0;

            while(true)
            {
                Socket clientSocket = serverSocket.accept();
                clientCount++;

                ClientHandler handler = new ClientHandler(clientSocket, clientCount);
                threadpool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        }
        
    }
}