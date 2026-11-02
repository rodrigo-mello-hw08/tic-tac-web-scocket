package com.tic.websocket;

import com.tic.domain.pojo.GameMessages.EmptyRequest;
import com.tic.domain.pojo.GameMessages.Event;
import com.tic.domain.pojo.GameMessages.RoomHistoryEntry;
import com.tic.repository.RoomRepository;
import com.tic.service.PresenceService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class HistoryWebSocketController {
    private final PresenceService presence;
    private final RoomRepository rooms;

    @MessageMapping("/history/list")
    @SendToUser(value = "/queue/users", broadcast = false)
    public Event history(EmptyRequest request, SimpMessageHeaderAccessor headers) {
        Long playerId = presence.requirePlayerId(headers.getSessionId());
        var entries = rooms.findClosedRoomsForPlayer(playerId).stream()
                .map(room -> {
                    boolean isPlayerA = room.getPlayerA().getId().equals(playerId);
                    String playerName = isPlayerA ? room.getPlayerA().getLogin() : room.getPlayerB().getLogin();
                    Integer playerScore = isPlayerA ? room.getScoreA() : room.getScoreB();
                    String opponentName = isPlayerA ? room.getPlayerB().getLogin() : room.getPlayerA().getLogin();
                    Integer opponentScore = isPlayerA ? room.getScoreB() : room.getScoreA();
                    String result = playerScore.equals(opponentScore) ? "Empate"
                            : playerScore > opponentScore ? "Vencedor" : "Perdedor";
                    return new RoomHistoryEntry(room.getId(), playerName, playerScore, opponentName, opponentScore, result);
                })
                .toList();
        return Event.success("ROOM_HISTORY", entries);
    }
}
