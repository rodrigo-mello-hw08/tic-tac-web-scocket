package com.tic.websocket;

public final class GameMessages {
    private GameMessages() {}

    public record RegisterRequest(String name, String password) {}
    public record LoginRequest(Long playerId, String password) {}
    public record InviteRequest(Long fromPlayerId, Long toPlayerId) {}
    public record InviteResponseRequest(String inviteId, Long playerId, boolean accepted) {}
    public record StartNextMatchRequest(Long roomId, Long playerId) {}
    public record PlayerView(Long id, String name) {}
    public record InviteView(String inviteId, PlayerView from, PlayerView to) {}
    public record RoomView(Long roomId, PlayerView playerA, PlayerView playerB, Long activeMatchId) {}
    public record MatchView(Long matchId, Long roomId, PlayerView playerA, PlayerView playerB,
                            Integer scoreA, Integer scoreB, String status, Long boardId,
                            Integer moves, java.util.List<String> cells) {}
    public record Event(String type, Object data, String error) {
        public static Event success(String type, Object data) { return new Event(type, data, null); }
        public static Event error(String message) { return new Event("ERROR", null, message); }
    }
}
