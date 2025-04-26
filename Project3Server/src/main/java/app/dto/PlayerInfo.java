package app.dto;

import lombok.*;

@Builder
@Data
public class PlayerInfo {
    private String username;
    private PlayerStatus status;

    public enum PlayerStatus { AVAILABLE, IN_GAME, CHALLENGED }
}
