import java.io.*;
import java.net.Socket;
import java.util.Map;

// this is the main game client class that handles all socket communication with the server
// it manages the game state, player information, and processes server messages
// this class is the core of the client-side logic for the tic-tac-toe game
// it maintains the current game board, opponent info, and handles all client operations
public class GameClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ServerListener listener;
    private Player player;
    private GameUI ui;
    private String currentGameId;
    private String currentOpponent;
    private char[][] currentBoard;
    private String currentPlayer;
    private boolean inGame;
    
    public GameClient() {
        player = new Player();
        ui = new GameUI(this);
        currentBoard = new char[3][3];
        inGame = false;
    }
    
    public boolean connect() {
        try {
            // create a socket connection to the server on the specified host and port
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            // create input stream reader to recieve messages from the server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // create output stream writer to send messages to the server
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // start a new listener thread to listen for incoming messages from server
            listener = new ServerListener(in, this);
            listener.start();
            
            // return true to indicate sucessful connection
            return true;
        } catch (IOException e) {
            // if connection fails, print error message
            System.err.println("Error connecting to server: " + e.getMessage());
            // return false to indicate connection failed
            return false;
        }
    }
    
    public void handleServerMessage(String message) {
        Map<String, Object> msg = Protocol.parseMessage(message);
        String type = (String) msg.get("type");
        
        if (type == null) {
            return;
        }
        
        switch (type) {
            case "LOGIN_SUCCESS":
                handleLoginSuccess(msg);
                break;
            case "SUCCESS":
                ui.showMessage((String) msg.get("message"));
                break;
            case "ERROR":
                ui.showError((String) msg.get("message"));
                break;
            case "PLAYERS_LIST":
                handlePlayersList(msg);
                break;
            case "CHALLENGE":
                handleChallenge(msg);
                break;
            case "CHALLENGE_RESPONSE":
                handleChallengeResponse(msg);
                break;
            case "START_GAME":
                handleStartGame(msg);
                break;
            case "UPDATE":
                handleUpdate(msg);
                break;
            case "RESULT":
                handleResult(msg);
                break;
            case "OPPONENT_DISCONNECTED":
                handleOpponentDisconnected(msg);
                break;
            case "REMATCH_REQUEST":
                handleRematchRequest(msg);
                break;
            case "REMATCH_RESPONSE":
                handleRematchResponse(msg);
                break;
            case "LEADERBOARD":
                handleLeaderboard(msg);
                break;
        }
    }
    
    private void handleLoginSuccess(Map<String, Object> msg) {
        player.setUsername((String) msg.get("username"));
        player.setStats(
            Integer.parseInt((String) msg.get("wins")),
            Integer.parseInt((String) msg.get("losses")),
            Integer.parseInt((String) msg.get("draws"))
        );
        player.setAuthenticated(true);
        ui.showMessage("Login successful! Welcome " + player.getUsername());
        ui.showStats();
    }
    
    private void handlePlayersList(Map<String, Object> msg) {
        String playersStr = (String) msg.get("players");
        if (playersStr != null && !playersStr.isEmpty()) {
            String[] players = playersStr.split(",");
            ui.updatePlayersList(players);
        } else {
            ui.updatePlayersList(new String[0]);
        }
    }
    
    private void handleChallenge(Map<String, Object> msg) {
        String challenger = (String) msg.get("challenger");
        ui.showChallenge(challenger);
    }
    
    private void handleRematchRequest(Map<String, Object> msg) {
        String requester = (String) msg.get("requester");
        ui.showMessage(requester + " wants a rematch! Type 'accept' to accept or 'reject' to reject");
        // Store as pending rematch requester for accept/reject commands
        ui.setPendingRematchRequester(requester);
    }
    
    private void handleRematchResponse(Map<String, Object> msg) {
        String response = (String) msg.get("response");
        String opponent = (String) msg.get("opponent");
        
        if ("ACCEPT".equals(response)) {
            ui.showMessage(opponent + " accepted your rematch request!");
        } else {
            ui.showMessage(opponent + " declined your rematch request.");
        }
    }
    
    private void handleLeaderboard(Map<String, Object> msg) {
        String data = (String) msg.get("data");
        if (data == null || data.isEmpty()) {
            ui.showError("No leaderboard data available");
            return;
        }
        
        String[] entries = data.split("\\|");
        System.out.println("\n=== LEADERBOARD ===");
        System.out.println("Rank | Player     | Wins | Losses | Draws");
        System.out.println("-----|------------|------|--------|------");
        
        for (String entry : entries) {
            String[] parts = entry.split(",");
            if (parts.length >= 5) {
                System.out.printf("%-4s | %-10s | %-4s | %-6s | %-5s%n", 
                    parts[0], parts[1], parts[2], parts[3], parts[4]);
            }
        }
        System.out.println("=====================\n");
    }
    
    private void handleChallengeResponse(Map<String, Object> msg) {
        String response = (String) msg.get("response");
        String opponent = (String) msg.get("opponent");
        
        if ("ACCEPT".equals(response)) {
            ui.showMessage(opponent + " accepted your challenge!");
        } else {
            ui.showMessage(opponent + " rejected your challenge.");
        }
    }
    
    private void handleStartGame(Map<String, Object> msg) {
        currentGameId = (String) msg.get("gameId");
        String player1 = (String) msg.get("player1");
        String player2 = (String) msg.get("player2");
        currentPlayer = (String) msg.get("currentPlayer");
        
        currentOpponent = player1.equals(player.getUsername()) ? player2 : player1;
        inGame = true;
        
        // Initialize board
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                currentBoard[i][j] = ' ';
            }
        }
        
        ui.showMessage("Game started! You are playing against " + currentOpponent);
        ui.showBoard();
        if (currentPlayer.equals(player.getUsername())) {
            ui.showMessage("Your turn!");
        } else {
            ui.showMessage("Waiting for " + currentOpponent + "'s move...");
        }
    }
    
    // this updates the board when the server sends a move update
    // it rebuilds the board from a map and updates whose turn it is
    private void handleUpdate(Map<String, Object> msg) {
        @SuppressWarnings("unchecked")
        Map<String, String> boardMap = (Map<String, String>) msg.get("board");
        currentPlayer = (String) msg.get("currentPlayer");
        
        // rebuild the 2d board from the map of coordinates
        if (boardMap != null) {
            for (Map.Entry<String, String> entry : boardMap.entrySet()) {
                // the key is "x,y" so we split it to get the coordinates
                String[] coords = entry.getKey().split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                // put the piece on the board
                currentBoard[x][y] = entry.getValue().charAt(0);
            }
        }
        
        // show the updated board to the player
        ui.showBoard();
        // tell the player if it's their turn or not
        if (currentPlayer.equals(player.getUsername())) {
            ui.showMessage("Your turn!");
        } else {
            ui.showMessage("Waiting for " + currentOpponent + "'s move...");
        }
    }
    
    // this handles when the game ends
    // it shows the final board and tells the player if they won, lost, or drew
    private void handleResult(Map<String, Object> msg) {
        String result = (String) msg.get("result");
        @SuppressWarnings("unchecked")
        Map<String, String> boardMap = (Map<String, String>) msg.get("board");
        
        // rebuild the final board state from the map
        if (boardMap != null) {
            for (Map.Entry<String, String> entry : boardMap.entrySet()) {
                String[] coords = entry.getKey().split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                currentBoard[x][y] = entry.getValue().charAt(0);
            }
        }
        
        // show the final board
        ui.showBoard();
        // mark that the game is over
        inGame = false;
        
        // display the game result
        if ("WIN".equals(result)) {
            ui.showMessage("Congratulations! You won!");
        } else if ("LOSS".equals(result)) {
            ui.showMessage("You lost. Better luck next time!");
        } else {
            ui.showMessage("It's a draw!");
        }
        
        // reset game info for the next game
        currentGameId = null;
        currentOpponent = null;
    }
    
    private void handleOpponentDisconnected(Map<String, Object> msg) {
        ui.showError("Your opponent disconnected. Game ended.");
        inGame = false;
        currentGameId = null;
        currentOpponent = null;
    }
    
    public void sendMessage(String message) {
        // send a message string to the server through the output writer
        if (out != null) {
            out.println(message);
        }
    }
    
    public void register(String username, String password) {
        // create a registration request message
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "REGISTER");
        msg.put("username", username);
        msg.put("password", password);
        // send the registration message to the server
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void login(String username, String password) {
        // create a login request message
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "LOGIN");
        msg.put("username", username);
        msg.put("password", password);
        // send the login message to the server
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void listPlayers() {
        // request a list of online players from the server
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "LIST_PLAYERS");
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void challenge(String opponent) {
        // send a challenge request to the specified opponent
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "CHALLENGE");
        msg.put("opponent", opponent);
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void respondToChallenge(String challenger, String response) {
        // respond to a challenge request (ACCEPT or REJECT)
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "CHALLENGE_RESPONSE");
        msg.put("challenger", challenger);
        msg.put("response", response);
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void makeMove(int x, int y) {
        // check if player is actually in a game before sending move
        if (currentGameId == null || !inGame) {
            ui.showError("Not in a game");
            return;
        }
        
        // create a data map for the move coordinates (x and y positions)
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("x", String.valueOf(x));
        data.put("y", String.valueOf(y));
        
        // create the complete move message with game id and player info
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "MOVE");
        msg.put("gameId", currentGameId);
        msg.put("player", player.getUsername());
        msg.put("data", data);
        
        // send the move message to the server
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void requestRematch() {
        // check if there was a previous opponent
        if (currentOpponent == null) {
            ui.showError("No previous opponent found");
            return;
        }
        
        // create a rematch request message with the opponent
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "REMATCH_REQUEST");
        msg.put("opponent", currentOpponent);
        // send the rematch request to the server
        sendMessage(Protocol.createMessage(msg));
        ui.showMessage("Requesting rematch with " + currentOpponent + "...");
    }
    
    public void respondToRematch(String requester, String response) {
        // send the rematch response (ACCEPT or REJECT) to the opponent
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "REMATCH_RESPONSE");
        msg.put("opponent", requester);
        msg.put("response", response);
        // send the response to the server
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void requestLeaderboard() {
        // request the leaderboard from the server
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "LEADERBOARD");
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void logout() {
        // create a logout message to tell the server this player is leaving
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "LOGOUT");
        // send the logout message to the server
        sendMessage(Protocol.createMessage(msg));
        // close the socket connection after sending logout message
        disconnect();
    }
    
    public void disconnect() {
        // stop the listener thread that receives messages from server
        if (listener != null) {
            listener.stopListening();
        }
        
        try {
            // close the socket connection if it's open
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // print error if closing socket fails
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
    
    public Player getPlayer() {
        // return the player object for this client
        return player;
    }
    
    public char[][] getCurrentBoard() {
        // return the current game board state
        return currentBoard;
    }
    
    public boolean isInGame() {
        // check if the player is currently in an active game
        return inGame;
    }
    
    public String getCurrentPlayer() {
        // return the username of whose turn it is to move
        return currentPlayer;
    }
    
    public static void main(String[] args) {
        // create a new game client instance
        GameClient client = new GameClient();
        
        // try to connect to the server
        if (!client.connect()) {
            System.err.println("Failed to connect to server");
            return;
        }
        
        // start the console ui which lets the player type commands
        client.ui.run();
        
        // when the player is done, disconnect from the server
        client.disconnect();
    }
}

