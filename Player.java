/**
 * Player.java - Represents local player state
 */
public class Player {
    private String username;
    private int wins;
    private int losses;
    private int draws;
    private boolean authenticated;
    
    public Player() {
        this.authenticated = false;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setStats(int wins, int losses, int draws) {
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
    }
    
    public int getWins() {
        return wins;
    }
    
    public int getLosses() {
        return losses;
    }
    
    public int getDraws() {
        return draws;
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
}




