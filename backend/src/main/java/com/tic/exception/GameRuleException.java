package com.tic.exception;

public class GameRuleException extends RuntimeException {
    private final Long playerId;

    public GameRuleException(Long playerId, String message) {
        super(message);
        this.playerId = playerId;
    }

    public Long getPlayerId() { return playerId; }
}
