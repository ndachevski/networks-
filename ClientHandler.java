import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;

// this class is responsable for handling individual client connections on the server side
// each time a client connects, a new ClientHandler thread is created to manage that connection
// it handles receiving messages from the client, parsing them, and routing them to appropriate handlers
// each handler thread runs independently so multiple clients can be served simultaniously
public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UserManager userManager;
    private GameServer server;
    private String username;
    private String sessionId;
    private boolean authenticated;
    
    public ClientHandler(Socket socket, UserManager userManager, GameServer server) {
        this.socket = socket;
        this.userManager = userManager;
        this.server = server;
        this.sessionId = UUID.randomUUID().toString();
        this.authenticated = false;
        
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error setting up client handler: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + (username != null ? username : "unknown"));
        } finally {
            cleanup();
        }
    }
    
    /**
     * handle incoming messages from client
     * this method processes incoming messages from the client and routes them to the right handler
     * it parses the message using the Protocol class to extract the message type and data
     * then it uses a switch statement to dispatch the message to the apropriate handler method
     * for example: if type is "LOGIN", it calls handlelogin() method
     */
    private void handleMessage(String message) {
        Map<String, Object> msg = Protocol.parseMessage(message);
        String type = (String) msg.get("type");
        
        if (type == null) {
            sendMessage(Protocol.createErrorMessage("Invalid message format"));
            return;
        }
        
        // route the message to the correct handler based on the type
        switch (type) {
            case "REGISTER":
                // user wants to create a new account
                handleRegister(msg);
                break;
            case "LOGIN":
                // user wants to log into their account
                handleLogin(msg);
                break;
            case "LIST_PLAYERS":
                // user wants to see who is online
                handleListPlayers();
                break;
            case "CHALLENGE":
                // user wants to challenge another player to a game
                handleChallenge(msg);
                break;
            case "CHALLENGE_RESPONSE":
                // user accepts or rejects a challenge
                handleChallengeResponse(msg);
                break;
            case "MOVE":
                // user makes a move during a game
                handleMove(msg);
                break;
            case "LOGOUT":
                // user wants to log out
                handleLogout();
                break;
            case "REMATCH_REQUEST":
                // user wants to play another game with the same opponent
                handleRematchRequest(msg);
                break;
            case "REMATCH_RESPONSE":
                // user accepts or rejects a rematch request
                handleRematchResponse(msg);
                break;
            case "LEADERBOARD":
                // user wants to see the rankings
                handleLeaderboard();
                break;
            default:
                sendMessage(Protocol.createErrorMessage("Unknown message type"));
        }
    }
    
    private void handleRegister(Map<String, Object> msg) {
        // extract username and password from the incoming message
        String username = (String) msg.get("username");
        String password = (String) msg.get("password");
        
        // validate that both username and password were provided
        if (username == null || password == null) {
            sendMessage(Protocol.createErrorMessage("Username and password required"));
            return;
        }
        
        // try to register the new user with userManager
        // userManager checks if username is alredy taken
        if (userManager.register(username, password)) {
            // if registration was sucessful, send success message back to client
            sendMessage(Protocol.createSuccessMessage("Registration successful"));
        } else {
            // if username alredy exists, send error message
            sendMessage(Protocol.createErrorMessage("Username already exists"));
        }
    }
    
    // this handles when a user logs in
    // it checks if the username and password are correct
    // then it marks the user as logged in and online
    private void handleLogin(Map<String, Object> msg) {
        String username = (String) msg.get("username");
        String password = (String) msg.get("password");
        
        // check if username and password are provided
        if (username == null || password == null) {
            sendMessage(Protocol.createErrorMessage("Username and password required"));
            return;
        }
        
        // try to authenticate the user with provided credentials
        if (userManager.login(username, password)) {
            // dont allow the same user to login twice at the same time
            if (userManager.isOnline(username)) {
                sendMessage(Protocol.createErrorMessage("User already logged in"));
                return;
            }
            
            // save the username and mark as authenticated
            this.username = username;
            this.authenticated = true;
            // add user to the online users list
            userManager.setOnline(username, sessionId);
            // tell the server about this new login
            server.addClient(this);
            
            // get the user's stats and send back success message
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("type", "LOGIN_SUCCESS");
            response.put("username", username);
            UserManager.User user = userManager.getUser(username);
            // include the user's game statistics
            response.put("wins", String.valueOf(user.getWins()));
            response.put("losses", String.valueOf(user.getLosses()));
            response.put("draws", String.valueOf(user.getDraws()));
            sendMessage(Protocol.createMessage(response));
            
            // tell all other players about the new login
            server.broadcastPlayerList();
        } else {
            // send error if password or username is wrong
            sendMessage(Protocol.createErrorMessage("Incorrect credentials"));
        }
    }
    
    private void handleListPlayers() {
        // verify that the user is logged in before sending player list
        if (!authenticated) {
            sendMessage(Protocol.createErrorMessage("Not authenticated"));
            return;
        }
        
        // get all the online users from the userManager
        String[] onlineUsers = userManager.getOnlineUsers();
        // build a comma-seperated list of online players (excluding self)
        StringBuilder playersList = new StringBuilder();
        for (String user : onlineUsers) {
            // skip the current user so they dont see themselves in the list
            if (!user.equals(username)) {
                // add a comma between players for seperation
                if (playersList.length() > 0) {
                    playersList.append(",");
                }
                playersList.append(user);
            }
        }
        
        // create response message with the list of players
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("type", "PLAYERS_LIST");
        response.put("players", playersList.toString());
        // send the player list back to the client
        sendMessage(Protocol.createMessage(response));
    }
    
    private void handleChallenge(Map<String, Object> msg) {
        // check if user is authenticated before allowing to send challenge
        if (!authenticated) {
            sendMessage(Protocol.createErrorMessage("Not authenticated"));
            return;
        }
        
        // get the username of the opponent that this player wants to challange
        String opponent = (String) msg.get("opponent");
        if (opponent == null) {
            sendMessage(Protocol.createErrorMessage("Opponent username required"));
            return;
        }
        
        // verify that the opponent is online and not offline or disconnected
        if (!userManager.isOnline(opponent)) {
            sendMessage(Protocol.createErrorMessage("User not available"));
            return;
        }
        
        // prevent a player from challenging themself (that wouldnt make sense)
        if (opponent.equals(username)) {
            sendMessage(Protocol.createErrorMessage("Cannot challenge yourself"));
            return;
        }
        
        // send the challenge to the opponent through the server
        server.sendChallenge(opponent, username);
    }
    
    private void handleChallengeResponse(Map<String, Object> msg) {
        // verify that user is logged in before handling challenge response
        if (!authenticated) {
            sendMessage(Protocol.createErrorMessage("Not authenticated"));
            return;
        }
        
        // get the username of the person who sent the original challenge
        String challenger = (String) msg.get("challenger");
        // get the response which should be either "ACCEPT" or "REJECT"
        String response = (String) msg.get("response");
        
        // make sure both values are provided in the message
        if (challenger == null || response == null) {
            sendMessage(Protocol.createErrorMessage("Invalid challenge response"));
            return;
        }
        
        // send the response to the server to process and notify the challenger
        server.handleChallengeResponse(challenger, username, response);
    }
    
    // this processes a game move sent by the player
    // it gets the game id and the x,y coordinates for the move
    // it checks if everything is valid and sends the move to the server
    private void handleMove(Map<String, Object> msg) {
        if (!authenticated) {
            sendMessage(Protocol.createErrorMessage("Not authenticated"));
            return;
        }
        
        // get the game id that this move is for
        String gameId = (String) msg.get("gameId");
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) msg.get("data");
        
        // check if game id and data are provided
        if (gameId == null || data == null) {
            sendMessage(Protocol.createErrorMessage("Invalid move format"));
            return;
        }
        
        // get the move coordinates from the data
        String xStr = data.get("x");
        String yStr = data.get("y");
        
        // check if both coordinates are provided
        if (xStr == null || yStr == null) {
            sendMessage(Protocol.createErrorMessage("Move coordinates required"));
            return;
        }
        
        // convert coordinates to numbers and send the move to the server
        try {
            int x = Integer.parseInt(xStr);
            int y = Integer.parseInt(yStr);
            // tell the server to process this move
            server.processMove(gameId, username, x, y);
        } catch (NumberFormatException e) {
            // send error if the coordinates are not valid numbers
            sendMessage(Protocol.createErrorMessage("Invalid move coordinates"));
        }
    }
    
    private void handleLogout() {
        cleanup();
    }
    
    private void handleRematchRequest(Map<String, Object> msg) {
        // check if user is logged in before allowing rematch request
        if (!authenticated) {
            sendMessage(Protocol.createErrorMessage("Not authenticated"));
            return;
        }
        
        // get the opponent username from the message
        String opponent = (String) msg.get("opponent");
        // if no opponent provided, try to get the last opponent from server history
        if (opponent == null) {
            opponent = server.getLastOpponent(username);
            // if no previous opponent found, we cant send rematch request
            if (opponent == null) {
                sendMessage(Protocol.createErrorMessage("No previous opponent found"));
                return;
            }
        }
        
        // verify that the opponent is online right now
        if (!userManager.isOnline(opponent)) {
            sendMessage(Protocol.createErrorMessage("User not available"));
            return;
        }
        
        // send the rematch request to the opponent through the server
        server.sendRematchRequest(opponent, username);
    }
    
    private void handleRematchResponse(Map<String, Object> msg) {
        // verify that user is logged in before handling rematch response
        if (!authenticated) {
            sendMessage(Protocol.createErrorMessage("Not authenticated"));
            return;
        }
        
        // get the opponent who requested the rematch
        String opponent = (String) msg.get("opponent");
        // get the response which should be "ACCEPT" or "REJECT"
        String response = (String) msg.get("response");
        
        // validate that both fields are provided
        if (opponent == null || response == null) {
            sendMessage(Protocol.createErrorMessage("Invalid rematch response"));
            return;
        }
        
        // send the response to the server to handle and notify the opponent
        server.handleRematchResponse(opponent, username, response);
    }
    
    private void handleLeaderboard() {
        // make sure user is logged in before viewing leaderboard
        if (!authenticated) {
            sendMessage(Protocol.createErrorMessage("Not authenticated"));
            return;
        }
        
        // request the server to send the leaderboard to this client
        server.sendLeaderboard(this);
    }
    
    public void sendMessage(String message) {
        // send a message to the client through the output writer
        // the message is automatically flushed because out is created with auto-flush=true
        if (out != null) {
            out.println(message);
        }
    }
    
    public String getUsername() {
        // return the username of the client currently connected
        return username;
    }
    
    public String getSessionId() {
        // return the unique session id for this client connection
        return sessionId;
    }
    
    public boolean isAuthenticated() {
        // return whether the user has successfully logged in or not
        return authenticated;
    }
    
    // this cleans up when a user disconnects
    // it removes them from the online list and closes the socket
    private void cleanup() {
        if (username != null) {
            // mark the user as offline
            userManager.setOffline(username);
            // remove this client from the server's client list
            server.removeClient(this);
            // tell all players about the updated player list
            server.broadcastPlayerList();
        }
        
        // close the socket connection
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}

