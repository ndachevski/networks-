import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UserManager.java - Handles user registration, authentication, and persistence
 */
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
     */
    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creating users file: " + e.getMessage());
            }
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String username = parts[0];
                    String password = parts[1];
                    int wins = Integer.parseInt(parts[2]);
                    int losses = Integer.parseInt(parts[3]);
                    int draws = Integer.parseInt(parts[4]);
                    
                    users.put(username, new User(username, password, wins, losses, draws));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }
    
    /**
     * Save users to file
     */
    private void saveUsers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (User user : users.values()) {
                writer.println(user.toFileString());
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
    
    /**
     * Register a new user
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
     */
    public boolean login(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }
    
    /**
     * Check if user is online
     */
    public boolean isOnline(String username) {
        return onlineUsers.containsKey(username);
    }
    
    /**
     * Add user to online list
     */
    public void setOnline(String username, String sessionId) {
        onlineUsers.put(username, sessionId);
    }
    
    /**
     * Remove user from online list
     */
    public void setOffline(String username) {
        onlineUsers.remove(username);
    }
    
    /**
     * Get list of online usernames
     */
    public String[] getOnlineUsers() {
        return onlineUsers.keySet().toArray(new String[0]);
    }
    
    /**
     * Get leaderboard (top players by wins)
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
     */
    public User getUser(String username) {
        return users.get(username);
    }
    
    /**
     * Update user statistics after game
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

