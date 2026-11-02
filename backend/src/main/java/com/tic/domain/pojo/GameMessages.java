package com.tic.domain.pojo;

public final class GameMessages {
    private GameMessages() {}

    public record RegisterRequest(String login, String password) {}
    public record HealthRequest(String message) {}
    public record LoginRequest(String login, String password) {}
    public record SessionRequest(String sessionToken) {}
    public record EmptyRequest() {}
    public record InviteRequest(Long fromPlayerId, Long toPlayerId) {}
    public record InviteResponseRequest(String inviteId, Long playerId, boolean accepted) {}
    public record StartNextMatchRequest(Long roomId, Long playerId) {}
    public record RoomActionRequest(Long roomId) {}
    public record MoveRequest(Long roomId, Integer cellIndex) {}
    public record PlayerView(Long id, String login) {}
    public record SessionView(Long id, String login, String sessionToken) {}
    public record InviteView(String inviteId, PlayerView from, PlayerView to) {}
    public record RoomView(Long roomId, PlayerView playerA, PlayerView playerB, Long activeMatchId) {}
    public record MatchView(Long matchId, Long roomId, PlayerView playerA, PlayerView playerB,
                            Integer scoreA, Integer scoreB, String status, Long boardId,
                            Integer moves, java.util.List<String> cells, String symbolA, String symbolB) {}
    public record RoomResult(Long roomId, MatchView match, boolean roomClosed, Long winnerPlayerId,
                             StartedRoom nextMatch) {}
    public record RoundState(Long roomId, MatchView match, Long nextPlayerId, String result,
                             Long winnerPlayerId, StartedRoom nextMatch) {}
    public record RoomHistoryEntry(Long roomId, String playerName, Integer playerScore,
                                   String opponentName, Integer opponentScore, String result) {}
    public record Event(String type, Object data, String error) {
        public static Event success(String type, Object data) { return new Event(type, data, null); }
        public static Event error(String message) { return new Event("ERROR", null, message); }
    }
}
