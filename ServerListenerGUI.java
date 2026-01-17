import java.io.BufferedReader;
import java.io.IOException;

// this is the gui version of the serverlistener thread
// it works the same way as serverlistener but integrates with the swing gui
// it reads messages from the server and forwards them to gameclientgui for processing
// using a separate listener thread allows the gui to remain responsive to user input
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
        // this is the listening loop for the gui client version
        // it runs in a seperate thread and reads messages from the server
        try {
            String message;
            // keep listening for messages while the thread is running
            while (running && (message = in.readLine()) != null) {
                // pass each message to the gui client for processing
                client.handleServerMessage(message);
            }
        } catch (IOException e) {
            // if there's an error listening, handle it gracefully
            if (running) {
                // only show error if the connection wasnt closed on purpose
                System.err.println("Connection lost: " + e.getMessage());
                // tell the gui client to disconnect
                client.disconnect();
            }
        }
    }
    
    public void stopListening() {
        running = false;
    }
}




