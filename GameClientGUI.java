import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Map;

// this class implements the graphical user interface for the game client using java swing
// it creates windows for login, main menu, and game board using GUI components
// it handles all user interactions through buttons, text fields, and list components
// the GUI communicates with the server through the same protocol as the console client
public class GameClientGUI {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ServerListenerGUI listener;
    private Player player;
    
    // GUI Components
    private JFrame loginFrame;
    private JFrame mainFrame;
    private JFrame gameFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private DefaultListModel<String> playersListModel;
    private JList<String> playersList;
    private JButton[][] boardButtons;
    private JLabel statusLabel;
    private JLabel currentPlayerLabel;
    private String currentGameId;
    private String currentOpponent;
    private char[][] currentBoard;
    private String currentPlayer;
    private boolean inGame;
    
    public GameClientGUI() {
        // create a player object to store this client's info
        player = new Player();
        // initialize the game board (3x3 for tic-tac-toe)
        currentBoard = new char[3][3];
        // we are not in a game yet
        inGame = false;
        // create the model for the players list
        playersListModel = new DefaultListModel<>();
        
        // show the login window first
        createLoginWindow();
    }
    
    private void createLoginWindow() {
        loginFrame = new JFrame("Tic-Tac-Toe - Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(350, 250);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Tic-Tac-Toe Game", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(titleLabel, gbc);
        
        // Username
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        usernameField = new JTextField(15);
        mainPanel.add(usernameField, gbc);
        
        // Password
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(15);
        mainPanel.add(passwordField, gbc);
        
        // Buttons
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> handleRegister());
        mainPanel.add(registerButton, gbc);
        
        gbc.gridx = 1;
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> handleLogin());
        mainPanel.add(loginButton, gbc);
        
        // Status label
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel, gbc);
        
        loginFrame.add(mainPanel, BorderLayout.CENTER);
        loginFrame.setVisible(true);
        
        // Enter key to login
        passwordField.addActionListener(e -> handleLogin());
    }
    
    // this handles registration in the gui
    // it gets the username and password from the text fields
    // connects to the server and sends the registration message
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // check that username and password are not empty
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter username and password", Color.RED);
            return;
        }
        
        // connect to the server
        if (!connect()) {
            showStatus("Failed to connect to server", Color.RED);
            return;
        }
        
        // send the registration message
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "REGISTER");
        msg.put("username", username);
        msg.put("password", password);
        sendMessage(Protocol.createMessage(msg));
    }
    
    // this handles login in the gui
    // it gets the username and password, connects to the server, and sends login message
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // check that username and password are not empty
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter username and password", Color.RED);
            return;
        }
        
        // connect to the server
        if (!connect()) {
            showStatus("Failed to connect to server", Color.RED);
            return;
        }
        
        // send the login message with credentials
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "LOGIN");
        msg.put("username", username);
        msg.put("password", password);
        sendMessage(Protocol.createMessage(msg));
    }
    
    // this makes a connection to the server
    // it checks if we already have a connection and reuses it
    // if not connected, it creates a socket and starts a listener thread
    private boolean connect() {
        // if already connected, reuse the connection
        if (socket != null && !socket.isClosed()) {
            return true;
        }
        
        // try to create a new socket connection
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            // set up the input and output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // start a listener thread to recieve messages from the server
            listener = new ServerListenerGUI(in, this);
            listener.start();
            
            return true;
        } catch (IOException e) {
            // show error if connection fails
            showStatus("Connection error: " + e.getMessage(), Color.RED);
            return false;
        }
    }
    
    public void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
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
                    showStatus((String) msg.get("message"), Color.GREEN);
                    break;
                case "ERROR":
                    showStatus((String) msg.get("message"), Color.RED);
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
        });
    }
    
    private void handleLoginSuccess(Map<String, Object> msg) {
        player.setUsername((String) msg.get("username"));
        player.setStats(
            Integer.parseInt((String) msg.get("wins")),
            Integer.parseInt((String) msg.get("losses")),
            Integer.parseInt((String) msg.get("draws"))
        );
        player.setAuthenticated(true);
        
        loginFrame.setVisible(false);
        createMainWindow();
        listPlayers();
    }
    
    private void createMainWindow() {
        mainFrame = new JFrame("Tic-Tac-Toe - " + player.getUsername());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(500, 600);
        mainFrame.setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel - Stats and controls
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Your Statistics"));
        JLabel statsLabel = new JLabel(String.format("Wins: %d | Losses: %d | Draws: %d", 
            player.getWins(), player.getLosses(), player.getDraws()));
        statsPanel.add(statsLabel);
        topPanel.add(statsPanel);
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Refresh Players");
        refreshButton.addActionListener(e -> listPlayers());
        JButton leaderboardButton = new JButton("Leaderboard");
        leaderboardButton.addActionListener(e -> requestLeaderboard());
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        controlPanel.add(refreshButton);
        controlPanel.add(leaderboardButton);
        controlPanel.add(logoutButton);
        topPanel.add(controlPanel);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // Center - Players list
        JPanel playersPanel = new JPanel(new BorderLayout());
        playersPanel.setBorder(BorderFactory.createTitledBorder("Online Players"));
        playersList = new JList<>(playersListModel);
        playersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(playersList);
        playersPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Challenge button
        JButton challengeButton = new JButton("Challenge Selected Player");
        challengeButton.addActionListener(e -> {
            String selected = playersList.getSelectedValue();
            if (selected != null) {
                challenge(selected);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Please select a player to challenge");
            }
        });
        playersPanel.add(challengeButton, BorderLayout.SOUTH);
        
        mainPanel.add(playersPanel, BorderLayout.CENTER);
        
        // Status area
        statusLabel = new JLabel("Connected. Waiting for players...", JLabel.CENTER);
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
    }
    
    private void createGameWindow() {
        if (gameFrame != null) {
            gameFrame.dispose();
        }
        
        gameFrame = new JFrame("Tic-Tac-Toe - vs " + currentOpponent);
        gameFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        gameFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // Allow closing by going back to lobby
                if (gameFrame != null) {
                    gameFrame.dispose();
                    gameFrame = null;
                }
                if (mainFrame != null) {
                    mainFrame.setVisible(true);
                    mainFrame.toFront();
                }
                listPlayers();
                inGame = false;
                currentGameId = null;
                currentOpponent = null;
            }
        });
        gameFrame.setSize(400, 550);
        gameFrame.setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Current player label
        currentPlayerLabel = new JLabel("Your turn!", JLabel.CENTER);
        currentPlayerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        currentPlayerLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        mainPanel.add(currentPlayerLabel, BorderLayout.NORTH);
        
        // Game board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        boardPanel.setBorder(BorderFactory.createTitledBorder("Game Board"));
        boardButtons = new JButton[3][3];
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int x = i;
                final int y = j;
                JButton button = new JButton(" ");
                button.setFont(new Font("Arial", Font.BOLD, 48));
                button.setPreferredSize(new Dimension(100, 100));
                button.addActionListener(e -> makeMove(x, y));
                boardButtons[i][j] = button;
                boardPanel.add(button);
            }
        }
        
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        
        // Button panel (for after game ends) - Make it more visible
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Game Options"));
        buttonPanel.setBackground(Color.WHITE);
        rematchButton = new JButton("Request Rematch");
        rematchButton.setEnabled(false); // Enabled after game ends
        rematchButton.setPreferredSize(new Dimension(160, 35));
        rematchButton.setFont(new Font("Arial", Font.BOLD, 12));
        rematchButton.setBackground(new Color(70, 130, 200));
        rematchButton.setForeground(Color.BLACK);
        rematchButton.setOpaque(true);
        rematchButton.setBorderPainted(false);
        rematchButton.setFocusPainted(false);
        rematchButton.addActionListener(e -> {
            if (currentOpponent != null) {
                requestRematch();
            } else {
                showStatus("No opponent to rematch", Color.RED);
            }
        });
        lobbyButton = new JButton("Back to Lobby");
        lobbyButton.setEnabled(false); // Enabled after game ends
        lobbyButton.setPreferredSize(new Dimension(160, 35));
        lobbyButton.setFont(new Font("Arial", Font.BOLD, 12));
        lobbyButton.setBackground(new Color(200, 70, 70));
        lobbyButton.setForeground(Color.BLACK);
        lobbyButton.setOpaque(true);
        lobbyButton.setBorderPainted(false);
        lobbyButton.setFocusPainted(false);
        lobbyButton.addActionListener(e -> goBackToLobby());
        buttonPanel.add(rematchButton);
        buttonPanel.add(lobbyButton);
        
        // Status label
        statusLabel = new JLabel("Game in progress", JLabel.CENTER);
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        
        gameFrame.add(mainPanel);
        gameFrame.setVisible(true);
        
        updateBoardDisplay();
    }
    
    private JButton rematchButton;
    private JButton lobbyButton;
    
    private void updateBoardDisplay() {
        if (boardButtons == null) return;
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                char cell = currentBoard[i][j];
                JButton button = boardButtons[i][j];
                
                if (cell == ' ') {
                    button.setText(" ");
                    button.setEnabled(inGame && currentPlayer != null && currentPlayer.equals(player.getUsername()));
                } else {
                    button.setText(String.valueOf(cell));
                    button.setEnabled(false);
                }
            }
        }
        
        if (currentPlayerLabel != null && currentPlayer != null) {
            if (currentPlayer.equals(player.getUsername())) {
                currentPlayerLabel.setText("Your turn!");
                currentPlayerLabel.setForeground(Color.GREEN);
            } else {
                currentPlayerLabel.setText(currentOpponent + "'s turn");
                currentPlayerLabel.setForeground(Color.RED);
            }
        }
    }
    
    private void handlePlayersList(Map<String, Object> msg) {
        String playersStr = (String) msg.get("players");
        playersListModel.clear();
        if (playersStr != null && !playersStr.isEmpty()) {
            String[] players = playersStr.split(",");
            for (String p : players) {
                if (!p.equals(player.getUsername())) {
                    playersListModel.addElement(p);
                }
            }
        }
    }
    
    private void handleChallenge(Map<String, Object> msg) {
        String challenger = (String) msg.get("challenger");
        int response = JOptionPane.showConfirmDialog(
            mainFrame,
            challenger + " has challenged you to a game!\nDo you accept?",
            "Challenge Received",
            JOptionPane.YES_NO_OPTION
        );
        
        respondToChallenge(challenger, response == JOptionPane.YES_OPTION ? "ACCEPT" : "REJECT");
    }
    
    private void handleChallengeResponse(Map<String, Object> msg) {
        String response = (String) msg.get("response");
        String opponent = (String) msg.get("opponent");
        
        if ("ACCEPT".equals(response)) {
            showStatus(opponent + " accepted your challenge!", Color.GREEN);
        } else {
            showStatus(opponent + " rejected your challenge.", Color.ORANGE);
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
        
        createGameWindow();
        showStatus("Game started! Playing against " + currentOpponent, Color.GREEN);
    }
    
    private void handleUpdate(Map<String, Object> msg) {
        @SuppressWarnings("unchecked")
        Map<String, String> boardMap = (Map<String, String>) msg.get("board");
        currentPlayer = (String) msg.get("currentPlayer");
        
        if (boardMap != null) {
            for (Map.Entry<String, String> entry : boardMap.entrySet()) {
                String[] coords = entry.getKey().split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                currentBoard[x][y] = entry.getValue().charAt(0);
            }
        }
        
        updateBoardDisplay();
    }
    
    private void handleResult(Map<String, Object> msg) {
        String result = (String) msg.get("result");
        @SuppressWarnings("unchecked")
        Map<String, String> boardMap = (Map<String, String>) msg.get("board");
        
        if (boardMap != null) {
            for (Map.Entry<String, String> entry : boardMap.entrySet()) {
                String[] coords = entry.getKey().split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                currentBoard[x][y] = entry.getValue().charAt(0);
            }
        }
        
        inGame = false;
        
        // Disable all board buttons
        if (boardButtons != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (boardButtons[i][j] != null) {
                        boardButtons[i][j].setEnabled(false);
                    }
                }
            }
        }
        
        // Update board display
        updateBoardDisplay();
        
        String message;
        Color color;
        if ("WIN".equals(result)) {
            message = "Congratulations! You won!";
            color = Color.GREEN;
        } else if ("LOSS".equals(result)) {
            message = "You lost. Better luck next time!";
            color = Color.RED;
        } else {
            message = "It's a draw!";
            color = Color.ORANGE;
        }
        
        showStatus(message, color);
        currentPlayerLabel.setText(message);
        
        currentGameId = null;
        // Keep currentOpponent for rematch
        
        // Enable buttons IMMEDIATELY on EDT
        SwingUtilities.invokeLater(() -> {
            if (rematchButton != null) {
                rematchButton.setEnabled(true);
                rematchButton.setFocusable(true);
                rematchButton.setVisible(true);
            }
            if (lobbyButton != null) {
                lobbyButton.setEnabled(true);
                lobbyButton.setFocusable(true);
                lobbyButton.setVisible(true);
            }
            
            // Force immediate UI update
            if (gameFrame != null) {
                gameFrame.revalidate();
                gameFrame.repaint();
                gameFrame.toFront();
            }
        });
    }
    
    private void handleRematchRequest(Map<String, Object> msg) {
        String requester = (String) msg.get("requester");
        int response = JOptionPane.showConfirmDialog(
            gameFrame != null ? gameFrame : mainFrame,
            requester + " wants a rematch!\nDo you accept?",
            "Rematch Request",
            JOptionPane.YES_NO_OPTION
        );
        
        respondToRematch(requester, response == JOptionPane.YES_OPTION ? "ACCEPT" : "REJECT");
    }
    
    private void handleRematchResponse(Map<String, Object> msg) {
        String response = (String) msg.get("response");
        String opponent = (String) msg.get("opponent");
        
        if ("ACCEPT".equals(response)) {
            showStatus(opponent + " accepted your rematch request!", Color.GREEN);
        } else {
            showStatus(opponent + " declined your rematch request.", Color.ORANGE);
            // Option to go back to lobby
            if (gameFrame != null) {
                int choice = JOptionPane.showConfirmDialog(
                    gameFrame,
                    "Return to lobby?",
                    "Rematch Declined",
                    JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    gameFrame.dispose();
                    gameFrame = null;
                    if (mainFrame != null) {
                        mainFrame.setVisible(true);
                    }
                    listPlayers();
                }
            }
        }
    }
    
    private void handleLeaderboard(Map<String, Object> msg) {
        String data = (String) msg.get("data");
        if (data == null || data.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "No leaderboard data available");
            return;
        }
        
        String[] entries = data.split("\\|");
        StringBuilder leaderboardText = new StringBuilder();
        leaderboardText.append("<html><body><table border='1' cellpadding='5'>");
        leaderboardText.append("<tr><th>Rank</th><th>Player</th><th>Wins</th><th>Losses</th><th>Draws</th></tr>");
        
        for (String entry : entries) {
            String[] parts = entry.split(",");
            if (parts.length >= 5) {
                leaderboardText.append("<tr>");
                leaderboardText.append("<td>").append(parts[0]).append("</td>");
                leaderboardText.append("<td>").append(parts[1]).append("</td>");
                leaderboardText.append("<td>").append(parts[2]).append("</td>");
                leaderboardText.append("<td>").append(parts[3]).append("</td>");
                leaderboardText.append("<td>").append(parts[4]).append("</td>");
                leaderboardText.append("</tr>");
            }
        }
        
        leaderboardText.append("</table></body></html>");
        
        JLabel label = new JLabel(leaderboardText.toString());
        JScrollPane scrollPane = new JScrollPane(label);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        JOptionPane.showMessageDialog(mainFrame, scrollPane, "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void handleOpponentDisconnected(Map<String, Object> msg) {
        showStatus("Your opponent disconnected. Game ended.", Color.RED);
        inGame = false;
        if (gameFrame != null) {
            gameFrame.dispose();
            gameFrame = null;
        }
        currentGameId = null;
        currentOpponent = null;
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    public void listPlayers() {
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "LIST_PLAYERS");
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void challenge(String opponent) {
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "CHALLENGE");
        msg.put("opponent", opponent);
        sendMessage(Protocol.createMessage(msg));
        showStatus("Challenging " + opponent + "...", Color.BLUE);
    }
    
    public void respondToChallenge(String challenger, String response) {
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "CHALLENGE_RESPONSE");
        msg.put("challenger", challenger);
        msg.put("response", response);
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void makeMove(int x, int y) {
        if (currentGameId == null || !inGame) {
            showStatus("Not in a game", Color.RED);
            return;
        }
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("x", String.valueOf(x));
        data.put("y", String.valueOf(y));
        
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "MOVE");
        msg.put("gameId", currentGameId);
        msg.put("player", player.getUsername());
        msg.put("data", data);
        
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void requestRematch() {
        if (currentOpponent == null) {
            showStatus("No previous opponent found", Color.RED);
            JOptionPane.showMessageDialog(gameFrame != null ? gameFrame : mainFrame, 
                "No previous opponent found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (rematchButton != null) {
            rematchButton.setEnabled(false);
        }
        
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "REMATCH_REQUEST");
        msg.put("opponent", currentOpponent);
        sendMessage(Protocol.createMessage(msg));
        showStatus("Requesting rematch with " + currentOpponent + "...", Color.BLUE);
    }
    
    public void respondToRematch(String requester, String response) {
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "REMATCH_RESPONSE");
        msg.put("opponent", requester);
        msg.put("response", response);
        sendMessage(Protocol.createMessage(msg));
    }
    
    public void requestLeaderboard() {
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "LEADERBOARD");
        sendMessage(Protocol.createMessage(msg));
    }
    
    private void goBackToLobby() {
        // Close game window
        if (gameFrame != null) {
            gameFrame.dispose();
            gameFrame = null;
        }
        
        // Show main window
        if (mainFrame != null) {
            mainFrame.setVisible(true);
            mainFrame.toFront();
            mainFrame.requestFocus();
        }
        
        // Reset game state
        inGame = false;
        currentGameId = null;
        // Keep currentOpponent for potential rematch later
        
        // Refresh player list
        listPlayers();
        
        showStatus("Returned to lobby", Color.BLUE);
    }
    
    public void logout() {
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "LOGOUT");
        sendMessage(Protocol.createMessage(msg));
        disconnect();
        
        if (mainFrame != null) mainFrame.dispose();
        if (gameFrame != null) gameFrame.dispose();
        System.exit(0);
    }
    
    public void disconnect() {
        if (listener != null) {
            listener.stopListening();
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
    
    private void showStatus(String message, Color color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        }
    }
    
    public static void main(String[] args) {
        // use invokeLater to run the gui on the swing event thread
        // this is required for thread safety in swing applications
        SwingUtilities.invokeLater(() -> {
            try {
                // use the system look and feel so it matches the os style
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // create a new gui client instance which shows the login window
            new GameClientGUI();
        });
    }
}

