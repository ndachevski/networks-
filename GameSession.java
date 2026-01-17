import java.util.HashMap;
import java.util.Map;

// this class represents a single active game of tic-tac-toe between two players
// it manages the game board state, player turns, move validation, and win/draw detection
// each game instance has its own unique game id and tracks both players
// it also contains the logic for checking win conditions and managing turn rotation
public class GameSession {
    private String gameId;
    private String player1;
    private String player2;
    private char[][] board;
    private String currentPlayer;
    private String winner;
    private boolean gameOver;
    private int moveCount;
    
    public GameSession(String gameId, String player1, String player2) {
        // store the game's unique identifier
        this.gameId = gameId;
        // store the two players
        this.player1 = player1;
        this.player2 = player2;
        // create an empty 3x3 game board
        this.board = new char[3][3];
        // player1 goes first
        this.currentPlayer = player1;
        // game is not over yet
        this.gameOver = false;
        // no moves have been made yet
        this.moveCount = 0;
        
        // fill the board with spaces to represent empty squares
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public String getPlayer1() {
        return player1;
    }
    
    public String getPlayer2() {
        return player2;
    }
    
    public String getCurrentPlayer() {
        return currentPlayer;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
    
    public String getWinner() {
        return winner;
    }
    
    /**
     * Make a move on the board
     * this method processes a move attempt on the game board
     * it validates that the move is legal (correct player, valid coordinates, empty cell)
     * if the move is valid, it updates the board and checks for win/draw conditions
     * it also switches the current player after a valid move is made
     */
    public boolean makeMove(String player, int x, int y) {
        // check if game is already over or if it's not this player's turn
        if (gameOver || !player.equals(currentPlayer)) {
            return false;
        }
        
        // check if the coordinates are valid (within 0-2) and the square is empty
        if (x < 0 || x >= 3 || y < 0 || y >= 3 || board[x][y] != ' ') {
            return false;
        }
        
        // place the player's symbol (X for player1, O for player2) on the board
        board[x][y] = (player.equals(player1)) ? 'X' : 'O';
        // increment the move counter
        moveCount++;
        
        // check if this move resulted in a win
        if (checkWin(x, y)) {
            gameOver = true;
            winner = player;
            return true;
        }
        
        // check if the board is full (9 moves = 9 squares filled = draw)
        if (moveCount == 9) {
            gameOver = true;
            winner = "DRAW";
            return true;
        }
        
        // switch to the other player's turn
        currentPlayer = (currentPlayer.equals(player1)) ? player2 : player1;
        return true;
    }
    
    /**
     * Check if the last move resulted in a win
     * this method checks if the last move at position (x, y) resulted in a winning condition
     * it checks four possible win patterns: row, column, and two diagonals
     * returns true if a win is detected, false otherwise
     * it only needs to check from the position of the last move for efficiancy
     */
    private boolean checkWin(int x, int y) {
        char symbol = board[x][y];
        
        // check if all three squares in the same row are the same symbol
        if (board[x][0] == symbol && board[x][1] == symbol && board[x][2] == symbol) {
            return true;
        }
        
        // check if all three squares in the same column are the same symbol
        if (board[0][y] == symbol && board[1][y] == symbol && board[2][y] == symbol) {
            return true;
        }
        
        // check the diagonal from top-left to bottom-right
        // only check this diagonal if the move was on it
        if (x == y && board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) {
            return true;
        }
        
        // check the diagonal from top-right to bottom-left
        // only check this diagonal if the move was on it
        if (x + y == 2 && board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get board state as string representation
     */
    public String getBoardString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sb.append(board[i][j]);
                if (j < 2) sb.append("|");
            }
            if (i < 2) sb.append("\n-+-+-\n");
        }
        return sb.toString();
    }
    
    /**
     * Get board state as map for JSON
     */
    public Map<String, String> getBoardMap() {
        Map<String, String> boardMap = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardMap.put(i + "," + j, String.valueOf(board[i][j]));
            }
        }
        return boardMap;
    }
    
    /**
     * Get result for a specific player
     */
    public String getResultFor(String player) {
        if (!gameOver) {
            return "ONGOING";
        }
        
        if (winner.equals("DRAW")) {
            return "DRAW";
        } else if (winner.equals(player)) {
            return "WIN";
        } else {
            return "LOSS";
        }
    }
}




