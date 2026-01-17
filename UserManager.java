import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// this class manages all user-related operations on the server side
// it handles user registration, login authentication, and account persistence
// user data is stored in a file called users.txt and loaded into memory when the server starts
// it also tracks which users are currently online and provides leaderboard functionality
public class UserManager {
    private static final String USERS_FILE = "users.txt";
    private Map<String, User> users;
    private Map<String, String> onlineUsers; // username -> sessionId
    
    public UserManager() {
        users = new ConcurrentHashMap<>();
        onlineUsers = new ConcurrentHashMap<>();
        loadUsers();
    }
    
    /**
     * Load users from file
     * this method reads the users.txt file and loads all user accounts into memory
     * each line in the file contains comma-separated user data: username, password, wins, losses, draws
     * it parses each line and creates a User object for each account
     * if the file doesn't exist, it creates an empty one
     */
    private void loadUsers() {
        // create a file object for the users file
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            // if the file doesn't exist, try to create it
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating users file: " + e.getMessage());
            }
            return;
        }
        
        // read the file line by line
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // split each line by commas to get the user data
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String username = parts[0];
                    String password = parts[1];
                    int wins = Integer.parseInt(parts[2]);
                    int losses = Integer.parseInt(parts[3]);
                    int draws = Integer.parseInt(parts[4]);
                    
                    // create a user object and add it to the map
                    users.put(username, new User(username, password, wins, losses, draws));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }
    
    /**
     * Save users to file
     * this method writes all user data back to the users.txt file
     * it's called whenever user data changes (new registration or stats updated)
     * it overwrites the entire file with current user data
     * this ensures user data persists between server restarts
     */
    private void saveUsers() {
        // create a new file writer that overwrites the existing file
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE))) {
            // write each user account to the file
            for (User user : users.values()) {
                // toFileString() formats the user data as comma-separated values
                writer.println(user.toFileString());
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
    
    /**
     * Register a new user
     * this method handles user registration for new accounts
     * it checks if the username already exists to prevent duplicates
     * if the username is available, it creates a new user with zero statistics
     * returns true if registration succeeds, false if username already taken
     */
    public boolean register(String username, String password) {
        if (users.containsKey(username)) {
            return false; // Username already exists
        }
        
        users.put(username, new User(username, password, 0, 0, 0));
        saveUsers();
        return true;
    }
    
    /**
     * Authenticate a user
     * this method authenticates a user during login
     * it retrieves the user account and compares the provided password with the stored one
     * returns true if username exists and password matches, false otherwise
     * note: passwords are stored as plain text (not secure, should use hashing in production)
     */
    public boolean login(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }
    
    /**
     * Check if user is online
     * this method checks if a user is currently logged in and online
     * returns true if the user has an active session, false otherwise
     * this is used to prevent duplicate logins and to find available opponents
     */
    public boolean isOnline(String username) {
        return onlineUsers.containsKey(username);
    }
    
    /**
     * Add user to online list
     * this method adds a user to the list of currently online users
     * it's called when a user sucessfully logs in
     * the sessionid is stored to track individual user sessions
     * this allows the server to manage multiple connections from the same user
     */
    public void setOnline(String username, String sessionId) {
        onlineUsers.put(username, sessionId);
    }
    
    /**
     * Remove user from online list
     * this method removes a user from the online users list
     * it's called when a user logs out or disconnects
     * after this, the user will appear as offline to other players
     * the user's account and statistics are preserved for future logins
     */
    public void setOffline(String username) {
        onlineUsers.remove(username);
    }
    
    /**
     * Get list of online usernames
     * this method returns an array of all currently online usernames
     * it's used to build the player list that clients can see
     * clients can challenge any player in this list
     * returns an empty array if no one is online
     */
    public String[] getOnlineUsers() {
        return onlineUsers.keySet().toArray(new String[0]);
    }
    
    /**
     * Get leaderboard (top players by wins)
     * this method generates a leaderboard sorted by player wins
     * it retrieves all users and sorts them by number of wins (descending)
     * if two players have the same wins, it sorts by total games played
     * the limit parameter restricts the leaderboard to top n players
     */
    public java.util.List<User> getLeaderboard(int limit) {
        java.util.List<User> leaderboard = new java.util.ArrayList<>(users.values());
        leaderboard.sort((a, b) -> {
            // Sort by wins (descending), then by total games
            int winDiff = b.getWins() - a.getWins();
            if (winDiff != 0) return winDiff;
            int totalA = a.getWins() + a.getLosses() + a.getDraws();
            int totalB = b.getWins() + b.getLosses() + b.getDraws();
            return totalB - totalA;
        });
        
        if (limit > 0 && limit < leaderboard.size()) {
            return leaderboard.subList(0, limit);
        }
        return leaderboard;
    }
    
    /**
     * Get user statistics
     * this method retrieves a user account by username
     * it returns the User object containing username, password, and game statistics
     * returns null if the user doesn't exist
     * used to access user data for display or updates
     */
    public User getUser(String username) {
        return users.get(username);
    }
    
    /**
     * Update user statistics after game
     * this method updates a player's statistics after a game completes
     * the result parameter should be "WIN", "LOSS", or "DRAW"
     * it increments the apropriate counter in the user's account
     * it also saves the updated data to the users.txt file
     */
    public void updateStats(String username, String result) {
        User user = users.get(username);
        if (user != null) {
            if ("WIN".equals(result)) {
                user.incrementWins();
            } else if ("LOSS".equals(result)) {
                user.incrementLosses();
            } else if ("DRAW".equals(result)) {
                user.incrementDraws();
            }
            saveUsers();
        }
    }
    
    /**
     * User data class
     * this inner class represents a user account with all its statistics
     * it stores username, password, and win/loss/draw game statistics
     * each user instance is identified by their unique username
     * this data is persisted to the users.txt file for long-term storage
     */
    public static class User {
        private String username;
        private String password;
        private int wins;
        private int losses;
        private int draws;
        
        public User(String username, String password, int wins, int losses, int draws) {
            this.username = username;
            this.password = password;
            this.wins = wins;
            this.losses = losses;
            this.draws = draws;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public int getWins() { return wins; }
        public int getLosses() { return losses; }
        public int getDraws() { return draws; }
        
        public void incrementWins() { wins++; }
        public void incrementLosses() { losses++; }
        public void incrementDraws() { draws++; }
        
        public String toFileString() {
            return username + "," + password + "," + wins + "," + losses + "," + draws;
        }
    }
}

