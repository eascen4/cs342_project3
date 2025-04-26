package app;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ConnectFourGame {
    final int ROWS = 6;
    final int COLUMNS = 7;
    final int EMPTY_SPACE = 0;

    final int[][] DIRECTIONS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

    /**
     * The status of the current game, where 0 is empty
     * and any integer is the player id that has their piece
     * in the space
      */
    private final int[][] board;
    private GameStatus status;
    private int winnerId; // 0 if draw
    private int numberOfMoves;

    // The index of the player that has its turn
    private int currentPlayerIndex;
    // List of players a part of the game
    @Getter
    private List<Integer> playerIds;


    public enum GameStatus {
        NOT_STARTED,
        PLAYING,
        FINISHED
    }

    public ConnectFourGame(List<Integer> playerIds) {
        this.playerIds = List.copyOf(playerIds);

        board = new int[ROWS][COLUMNS];

        currentPlayerIndex = 0;
        status = GameStatus.NOT_STARTED;
        winnerId = 0;
        numberOfMoves = 0;
        log.debug("Game initialized for players {}", this.playerIds);
    }

    public synchronized boolean dropPiece(int playerId, int column) {
        if(status != GameStatus.PLAYING) return false;
        if(playerId != getCurrentPlayerId()) return false;
        if(!isColumnDroppable(column)) return false;

        for(int row = ROWS - 1; row >= 0; row--) {
            if(board[row][column] == EMPTY_SPACE) {
                board[row][column] = playerId;
                numberOfMoves++;

                if(checkForWin(row, column, playerId)) {
                    status = GameStatus.FINISHED;
                    winnerId = playerId;
                }
                else if (isBoardFull()) {
                    status = GameStatus.FINISHED;
                    winnerId = EMPTY_SPACE;
                }
                else {
                    nextTurn();
                }
                return true;
            }
        }
        return false;
    }

    private void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % playerIds.size();
    }

    // Check for win in the previous move
    private boolean checkForWin(int row, int column, int playerId) {
        if(playerId == EMPTY_SPACE) return false;
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
                board[0][column] == EMPTY_SPACE;
    }

    private int countPieces(int playerId, int r, int c, int dr, int dc) {
        int count = 0;
        int currRow = r + dr;
        int currCol = c + dc;

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

    public boolean isBoardFull() {
        return numberOfMoves == ROWS * COLUMNS;
    }

    public synchronized int[][] getBoard() {
        var boardCopy = new int[ROWS][COLUMNS];
        for(int row = 0; row < ROWS; row++) {
            boardCopy[row] = Arrays.copyOf(board[row], COLUMNS);
        }
        return boardCopy;
    }

    public synchronized GameStatus getStatus() { return status; }
    public synchronized void setStatus(GameStatus status) {
        this.status = status;
    }

    public synchronized int getCurrentPlayerId() {
        return playerIds.get(currentPlayerIndex);
    }

    public synchronized int getWinnerId() { return winnerId; }
    public synchronized void setWinnerId(int winnerId) {
        log.warn("Winner ID explicitly set to: {}", winnerId);
        this.winnerId = winnerId;
    }

    public synchronized void startGame() {
        if(status == GameStatus.NOT_STARTED) status = GameStatus.PLAYING;
    }

    public synchronized void resetGame() {
        log.info("Resetting game for players: {}", playerIds);
        for (int[] row : board) {
            Arrays.fill(row, EMPTY_SPACE);
        }

        currentPlayerIndex = 0;
        status = GameStatus.PLAYING;
        winnerId = EMPTY_SPACE;
        numberOfMoves = 0;
    }
}
