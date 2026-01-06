import java.io.BufferedReader;
import java.io.IOException;

/**
 * ServerListenerGUI.java - Thread that listens for incoming server messages (GUI version)
 */
public class ServerListenerGUI extends Thread {
    private BufferedReader in;
    private GameClientGUI client;
    private boolean running;
    
    public ServerListenerGUI(BufferedReader in, GameClientGUI client) {
        this.in = in;
        this.client = client;
        this.running = true;
    }
    
    @Override
    public void run() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                client.handleServerMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Connection lost: " + e.getMessage());
                client.disconnect();
            }
        }
    }
    
    public void stopListening() {
        running = false;
    }
}




