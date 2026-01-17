// this is the main client entry point file that handles starting the game client application
// its purpose is to act as a wrapper that delegates to the gameclient main method
// basically just launches the game client GUI or console version
// the Client class is very simple beacuse it just forwards everything to gameclient
public class Client {
    public static void main(String[] args) {
        // this line calls the gameclient.main method to start the actual game client
        // it passes command-line arguments along to the gameclient
        GameClient.main(args);
    }
}

