import java.util.Scanner;

// this class provides the command-line interface for the game client
// it handles displaying messages and prompts to the user
// it also reads user input from the console and processes game commands like move, challenge, etc
// it works together with gameclient to provide a text-based gaming experience
public class GameUI {
    private GameClient client;
    private Scanner scanner;
    private String pendingChallenger;
    private String pendingRematchRequester;
    
    public GameUI(GameClient client) {
        this.client = client;
        this.scanner = new Scanner(System.in);
        this.pendingChallenger = null;
        this.pendingRematchRequester = null;
    }
    
    public void run() {
        System.out.println("=== Tic-Tac-Toe Game Client ===");
        System.out.println("Type 'help' for commands");
        
        // main game loop that reads commands from user until they quit or exit
        while (true) {
            System.out.print("> ");
            // read a command line from the user
            String command = scanner.nextLine().trim();
            
            // skip empty input lines and show the prompt again
            if (command.isEmpty()) {
                continue;
            }
            
            // split the command into parts seperated by spaces
            String[] parts = command.split("\\s+");
            // get the first part which is the command name
            String cmd = parts[0].toLowerCase();
            
            // process each command by dispatching to apropriate handler
            switch (cmd) {
                case "register":
                    // user wants to create a new account
                    if (parts.length >= 3) {
                        client.register(parts[1], parts[2]);
                    } else {
                        showError("Usage: register <username> <password>");
                    }
                    break;
                    
                case "login":
                    // user wants to log into their account
                    if (parts.length >= 3) {
                        client.login(parts[1], parts[2]);
                    } else {
                        showError("Usage: login <username> <password>");
                    }
                    break;
                    
                case "list":
                    // user wants to see who is online
                    if (client.getPlayer().isAuthenticated()) {
                        client.listPlayers();
                    } else {
                        showError("Please login first");
                    }
                    break;
                    
                case "challenge":
                    // user wants to challenge another player to a game
                    if (parts.length >= 2) {
                        if (client.getPlayer().isAuthenticated()) {
                            client.challenge(parts[1]);
                        } else {
                            showError("Please login first");
                        }
                    } else {
                        showError("Usage: challenge <username>");
                    }
                    break;
                    
                case "accept":
                    // user accepts a pending challenge or rematch request
                    if (pendingChallenger != null) {
                        client.respondToChallenge(pendingChallenger, "ACCEPT");
                        pendingChallenger = null;
                    } else if (pendingRematchRequester != null) {
                        client.respondToRematch(pendingRematchRequester, "ACCEPT");
                        pendingRematchRequester = null;
                    } else {
                        showError("No pending challenge or rematch");
                    }
                    break;
                    
                case "reject":
                    // user rejects a pending challenge or rematch request
                    if (pendingChallenger != null) {
                        client.respondToChallenge(pendingChallenger, "REJECT");
                        pendingChallenger = null;
                    } else if (pendingRematchRequester != null) {
                        client.respondToRematch(pendingRematchRequester, "REJECT");
                        pendingRematchRequester = null;
                    } else {
                        showError("No pending challenge or rematch");
                    }
                    break;
                    
                case "move":
                    // user wants to make a move on the game board
                    if (parts.length >= 3) {
                        if (client.isInGame()) {
                            try {
                                // parse the x and y coordinates from the command
                                int x = Integer.parseInt(parts[1]);
                                int y = Integer.parseInt(parts[2]);
                                // verify coordinates are within valid range (0-2)
                                if (x >= 0 && x < 3 && y >= 0 && y < 3) {
                                    client.makeMove(x, y);
                                } else {
                                    showError("Coordinates must be 0-2");
                                }
                            } catch (NumberFormatException e) {
                                showError("Invalid coordinates");
                            }
                        } else {
                            showError("Not in a game");
                        }
                    } else {
                        showError("Usage: move <x> <y> (0-2)");
                    }
                    break;
                    
                case "board":
                    // user wants to see the current game board
                    if (client.isInGame()) {
                        showBoard();
                    } else {
                        showError("Not in a game");
                    }
                    break;
                    
                case "stats":
                    // display the user's game statistics (wins, losses, draws)
                    showStats();
                    break;
                    
                case "rematch":
                    // user wants to play another game with the same opponent
                    client.requestRematch();
                    break;
                    
                case "leaderboard":
                    // user wants to see the top ranked players
                    client.requestLeaderboard();
                    break;
                    
                case "logout":
                    // user is done playing and wants to disconnect
                    client.logout();
                    System.out.println("Logged out. Goodbye!");
                    return;
                    
                case "quit":
                case "exit":
                    // user wants to exit the program
                    client.logout();
                    System.out.println("Goodbye!");
                    return;
                    
                case "help":
                    // display available commands
                    showHelp();
                    break;
                    
                default:
                    // unknown command
                    showError("Unknown command. Type 'help' for commands");
            }
        }
    }
    
    public void showMessage(String message) {
        System.out.println("[INFO] " + message);
    }
    
    public void showError(String error) {
        System.out.println("[ERROR] " + error);
    }
    
    public void showStats() {
        // get the current player's stats from the client
        Player player = client.getPlayer();
        // only show stats if the player is logged in
        if (player.isAuthenticated()) {
            System.out.println("\n=== Your Statistics ===");
            // display the player's username
            System.out.println("Username: " + player.getUsername());
            // display the number of games the player has won
            System.out.println("Wins: " + player.getWins());
            // display the number of games the player has lost
            System.out.println("Losses: " + player.getLosses());
            // display the number of games that ended in a draw
            System.out.println("Draws: " + player.getDraws());
            System.out.println("=====================\n");
        } else {
            showError("Not logged in");
        }
    }
    
    public void updatePlayersList(String[] players) {
        // check if there are any players online besides the current user
        if (players.length > 0) {
            // display the list of online players
            System.out.println("\n=== Online Players ===");
            // iterate through each player and display their name
            for (String player : players) {
                System.out.println("- " + player);
            }
            System.out.println("======================\n");
        } else {
            // message when no other players are online
            System.out.println("\nNo other players online\n");
        }
    }
    
    public void showChallenge(String challenger) {
        // store the name of the player who sent the challenge
        this.pendingChallenger = challenger;
        System.out.println("\n=== CHALLENGE ===");
        // display who is challenging the player
        System.out.println(challenger + " has challenged you to a game!");
        // prompt the player to accept or reject
        System.out.println("Type 'accept' to accept or 'reject' to reject");
        System.out.println("==================\n");
    }
    
    public void setPendingChallenger(String challenger) {
        this.pendingChallenger = challenger;
    }
    
    public void setPendingRematchRequester(String requester) {
        this.pendingRematchRequester = requester;
    }
    
    public void showBoard() {
        // get the current game board from the client
        char[][] board = client.getCurrentBoard();
        // print column numbers at the top
        System.out.println("\n  0   1   2");
        // iterate through each row of the board
        for (int i = 0; i < 3; i++) {
            // print the row number
            System.out.print(i + " ");
            // iterate through each column in the row
            for (int j = 0; j < 3; j++) {
                // get the current cell value
                char cell = board[i][j];
                // if empty, show a space; otherwise show X or O
                if (cell == ' ') {
                    System.out.print(" ");
                } else {
                    System.out.print(cell);
                }
                // print seperator between columns except after last column
                if (j < 2) {
                    System.out.print(" | ");
                }
            }
            System.out.println();
            // print horizontal line between rows except after last row
            if (i < 2) {
                System.out.println("  ---------");
            }
        }
        System.out.println();
    }
    
    private void showHelp() {
        System.out.println("\n=== Available Commands ===");
        System.out.println("register <username> <password> - Register a new account");
        System.out.println("login <username> <password>    - Login to your account");
        System.out.println("list                           - List online players");
        System.out.println("challenge <username>           - Challenge a player");
        System.out.println("accept                         - Accept a challenge");
        System.out.println("reject                         - Reject a challenge");
        System.out.println("move <x> <y>                   - Make a move (0-2)");
        System.out.println("board                          - Show current board");
        System.out.println("stats                          - Show your statistics");
        System.out.println("rematch                        - Request rematch with last opponent");
        System.out.println("leaderboard                    - Show top players leaderboard");
        System.out.println("logout                         - Logout and disconnect");
        System.out.println("quit/exit                      - Exit the client");
        System.out.println("help                           - Show this help");
        System.out.println("============================\n");
    }
}

