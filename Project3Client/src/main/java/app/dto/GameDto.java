package app.dto;

import lombok.*;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {
    private String gameId;
    private List<PlayerInfo> players;
    private int[][] board;
    private int currentPlayerId;
    private GameStatus status;
    private int winnerId;

    public enum GameStatus { ACTIVE, WIN, DRAW, FORFEIT }
}
