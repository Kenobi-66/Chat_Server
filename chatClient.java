import java.io.*;
import java.net.*;
import java.util.Scanner;

public class chatClient
{
    public static void main (String[] args) throws IOException
    {
        try(
            Socket clientSocket = new Socket("localhost", 5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in)
        )
        {    

            System.out.println("Type 'exit' to quit the connection.");

            Thread listener = new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null)
                        System.out.println(serverResponse);
                } catch (IOException e) {
                    
                }
            });
            listener.start();

            String userMessage;

            while (true)
            {
                userMessage = scanner.nextLine();

                out.println(userMessage);
                
                if ("exit".equalsIgnoreCase(userMessage))
                    break;
            }
        } catch (IOException e) 
        {
            System.err.println("Error Message: " + e.getMessage());
            e.printStackTrace();
        } finally
        {
            System.out.println("Disconnecting from server");
        }
    }
}