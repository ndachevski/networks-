// this class represents the current logged-in player on the client side
// it stores the player's username, win/loss/draw statistics, and authentication status
// this is different from the user class in UserManager which stores server-side player data
// this class is primarly used by the UI to display player information and track session state
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
        // set the username of the current player
        this.username = username;
    }
    
    public String getUsername() {
        // return the username of the current player
        return username;
    }
    
    public void setStats(int wins, int losses, int draws) {
        // update the player's game statistics all at once
        // this method is usually called after receiving stats from server
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
    }
    
    public int getWins() {
        // return the number of games won by the player
        return wins;
    }
    
    public int getLosses() {
        // return the number of games lost by the player
        return losses;
    }
    
    public int getDraws() {
        // return the number of games that ended in a draw
        return draws;
    }
    
    public void setAuthenticated(boolean authenticated) {
        // mark whether the player is currently logged in or not
        this.authenticated = authenticated;
    }
    
    public boolean isAuthenticated() {
        // check if the player is logged in or not
        return authenticated;
    }
}




