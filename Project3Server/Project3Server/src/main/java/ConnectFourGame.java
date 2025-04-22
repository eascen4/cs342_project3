import java.util.Arrays;
import java.util.List;

public class ConnectFourGame {
    final int ROWS = 6;
    final int COLUMNS = 7;

    final int[][] DIRECTIONS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

    /**
     * The status of the current game, where 0 is empty
     * and any integer is the player id that has their piece
     * in the space
      */
    private final int[][] board;
    private GameStatus status;


    // The index of the player that has its turn
    private int currentPlayerIndex;

    // List of players a part of the game
    private List<Integer> players;


    public enum GameStatus {
        NOT_STARTED,
        PLAYING,
        PLAYER_WON,
        STALEMATE;
    }

    public ConnectFourGame(int[] players) {
        board = new int[ROWS][COLUMNS];

        players = players.clone();

        for (var row : board) {
            Arrays.fill(row, 0);
        }

        currentPlayerIndex = 0;
        status = GameStatus.NOT_STARTED;
    }

    public synchronized boolean dropPiece(int column) {
        if(status != GameStatus.PLAYING) return false;
        if(!isColumnDroppable(column)) return false;

        for(int row = ROWS - 1; row >= 0; row--) {
            if(board[row][column] == 0) {
                board[row][column] = players.get(currentPlayerIndex);

                if(checkForWin(row, column)) status = GameStatus.PLAYER_WON;

                return true;
            }
        }
        return false;
    }

    // Check for win in the previous move
    private boolean checkForWin(int row, int column) {
        int playerId = board[row][column];
        if(playerId == 0) return false;

        for(var direction : DIRECTIONS) {
            int count = 1;

            count += countPieces(playerId, row, column, direction[0], direction[1]);
            count += countPieces(playerId, row, column, -direction[0], -direction[1]);

            if(count >= 4) return true;
        }
        return false;
    }

    private boolean isColumnDroppable(int column) {
        return column < COLUMNS &&
                column >= 0 &&
                board[0][column] == 0;
    }

    private int countPieces(int playerId, int r, int c, int dr, int dc) {
        int count = 0;

        int currRow = r;
        int currCol = c;

        while(
            currRow >= 0 && currRow < ROWS &&
            currCol >= 0 && currCol < COLUMNS &&
            board[currRow][currCol] == playerId
        ) {
            count++;
            currRow += dr;
            currCol += dc;
        }

        return count;
    }







}
