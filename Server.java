// this is the entry point for running the game server
// it simply creates a gameserver instance and starts it
// once started, the server will run indefinately, listening for client connections
// this main method is what you run to start the tic-tac-toe game server
public class Server {
    public static void main(String[] args) {
        // create a new game server that will handle all connections
        GameServer server = new GameServer();
        
        // start the server so it listens for client connections
        // this call blocks forever unless the program is stopped
        server.start();
    }
}




