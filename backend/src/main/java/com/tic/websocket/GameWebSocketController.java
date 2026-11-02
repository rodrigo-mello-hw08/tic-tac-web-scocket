package com.tic.websocket;

import com.tic.exception.GameRuleException;
import com.tic.service.PresenceService;
import com.tic.service.RoomService;
import com.tic.domain.pojo.GameMessages.Event;
import com.tic.domain.pojo.GameMessages.StartNextMatchRequest;
import com.tic.domain.pojo.GameMessages.RoomActionRequest;
import com.tic.domain.pojo.GameMessages.MoveRequest;
import com.tic.domain.pojo.StartedRoom;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class GameWebSocketController {
    private static String roomTopic(Long id) { return "/topic/rooms/" + id; }
    private static String playerTopic(Long id) { return "/topic/players/" + id; }
    private final PresenceService presence;
    private final RoomService rooms;
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/rooms/move")
    public void move(MoveRequest request, SimpMessageHeaderAccessor headers) {
        Long playerId = presence.requirePlayerId(headers.getSessionId());
        if (request == null || request.roomId() == null || request.cellIndex() == null) {
            messaging.convertAndSend(playerTopic(playerId), Event.error("Jogada inválida."));
            return;
        }
        try {
            var state = rooms.move(request.roomId(), playerId, request.cellIndex());
            messaging.convertAndSend(roomTopic(state.roomId()), Event.success("ROUND_STATE", state));
        } catch (GameRuleException exception) {
            messaging.convertAndSend(playerTopic(playerId), Event.error(exception.getMessage()));
        }
    }

    @MessageMapping("/rooms/matches/next")
    public void startNextMatch(StartNextMatchRequest request, SimpMessageHeaderAccessor headers) {
        if (request == null) throw new GameRuleException(null, "Dados da partida inválidos.");
        Long playerId = presence.requirePlayerId(headers.getSessionId());
        if (!playerId.equals(request.playerId())) {
            messaging.convertAndSend(playerTopic(playerId), Event.error("Identidade do jogador inválida."));
            return;
        }
        try {
            StartedRoom started = rooms.startNextMatch(request.roomId(), playerId);
            messaging.convertAndSend(roomTopic(started.room().roomId()), Event.success("MATCH_STARTED", started.match()));
        } catch (GameRuleException exception) {
            messaging.convertAndSend(playerTopic(exception.getPlayerId() == null ? playerId : exception.getPlayerId()), Event.error(exception.getMessage()));
        }
    }

    @MessageMapping("/rooms/resign")
    public void resign(RoomActionRequest request, SimpMessageHeaderAccessor headers) {
        endByForfeit(request, headers, false);
    }

    @MessageMapping("/rooms/close")
    public void closeRoom(RoomActionRequest request, SimpMessageHeaderAccessor headers) {
        endByForfeit(request, headers, true);
    }

    private void endByForfeit(RoomActionRequest request, SimpMessageHeaderAccessor headers, boolean closeRoom) {
        Long playerId = presence.requirePlayerId(headers.getSessionId());
        if (request == null || request.roomId() == null) {
            messaging.convertAndSend(playerTopic(playerId), Event.error("Sala inválida."));
            return;
        }
        try {
            var result = rooms.forfeit(request.roomId(), playerId, closeRoom);
            messaging.convertAndSend(roomTopic(result.roomId()), Event.success("ROOM_RESULT", result));
        } catch (GameRuleException exception) {
            messaging.convertAndSend(playerTopic(playerId), Event.error(exception.getMessage()));
        }
    }
}
