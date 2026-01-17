import java.io.BufferedReader;
import java.io.IOException;

// this class is a thread that listens for incoming messages from the server
// it runs on the client side and continuously reads messages from the socket
// when a message arrives, it forwards it to the gameclient for processing
// it allows the client to recieve server messages without blocking user input
public class ServerListener extends Thread {
    private BufferedReader in;
    private GameClient client;
    private boolean running;
    
    public ServerListener(BufferedReader in, GameClient client) {
        this.in = in;
        this.client = client;
        this.running = true;
    }
    
    @Override
    public void run() {
        // this is the main listening loop that runs in a seperate thread
        // it continuously reads messages from the server socket
        try {
            String message;
            // keep reading messages as long as the thread is running
            while (running && (message = in.readLine()) != null) {
                // when we get a message, send it to the game client to process it
                client.handleServerMessage(message);
            }
        } catch (IOException e) {
            // if we get an error while listening, handle it
            if (running) {
                // only print error if we didnt close the connection on purpose
                System.out.println("Connection lost: " + e.getMessage());
                // tell the client that the connection was lost
                client.disconnect();
            }
        }
    }
    
    public void stopListening() {
        running = false;
    }
}

