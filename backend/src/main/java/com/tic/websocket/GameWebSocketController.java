package com.tic.websocket;

import com.tic.exception.GameRuleException;
import com.tic.service.InviteService;
import com.tic.service.PresenceService;
import com.tic.service.RoomService;
import com.tic.domain.pojo.GameMessages.Event;
import com.tic.domain.pojo.GameMessages.HealthRequest;
import com.tic.domain.pojo.GameMessages.InviteRequest;
import com.tic.domain.pojo.GameMessages.InviteResponseRequest;
import com.tic.domain.pojo.GameMessages.StartNextMatchRequest;
import com.tic.domain.pojo.InviteResponse;
import com.tic.domain.pojo.InviteSent;
import com.tic.domain.pojo.StartedRoom;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class GameWebSocketController {

    private static String playerTopic(Long id) { return "/topic/players/" + id; }
    private static String roomTopic(Long id) { return "/topic/rooms/" + id; }

    private final PresenceService presence;
    private final InviteService invites;
    private final RoomService rooms;
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/invites/send")
    public void sendInvite(InviteRequest request, SimpMessageHeaderAccessor headers) {
        if (request == null) throw new GameRuleException(null, "Dados do convite inválidos.");
        Long authenticatedPlayerId = presence.requirePlayerId(headers.getSessionId());
        if (!authenticatedPlayerId.equals(request.fromPlayerId())) {
            messaging.convertAndSend(playerTopic(authenticatedPlayerId), Event.error("Identidade do jogador inválida."));
            return;
        }
        handle(authenticatedPlayerId, () -> {
            InviteSent invite = invites.send(authenticatedPlayerId, request.toPlayerId());
            messaging.convertAndSend(playerTopic(invite.toPlayerId()), Event.success("INVITE_RECEIVED", invite.view()));
            messaging.convertAndSend(playerTopic(invite.fromPlayerId()), Event.success("INVITE_SENT", invite.view()));
        });
    }

    @MessageMapping("/invites/respond")
    public void respondToInvite(InviteResponseRequest request, SimpMessageHeaderAccessor headers) {
        if (request == null) throw new GameRuleException(null, "Dados da resposta inválidos.");
        Long authenticatedPlayerId = presence.requirePlayerId(headers.getSessionId());
        if (!authenticatedPlayerId.equals(request.playerId())) {
            messaging.convertAndSend(playerTopic(authenticatedPlayerId), Event.error("Identidade do jogador inválida."));
            return;
        }
        handle(authenticatedPlayerId, () -> {
            InviteResponse response = invites.respond(request.inviteId(), authenticatedPlayerId, request.accepted());
            if (!response.accepted()) {
                messaging.convertAndSend(playerTopic(response.fromPlayerId()),
                        Event.success("INVITE_DECLINED", response.toPlayer()));
                messaging.convertAndSend(playerTopic(response.toPlayerId()),
                        Event.success("INVITE_DECLINED", response.fromPlayer()));
                return;
            }

            StartedRoom started = response.startedRoom();
            messaging.convertAndSend(roomTopic(started.room().roomId()), Event.success("MATCH_STARTED", started.match()));
            messaging.convertAndSend(playerTopic(response.fromPlayerId()), Event.success("ROOM_JOINED", started.room()));
            messaging.convertAndSend(playerTopic(response.toPlayerId()), Event.success("ROOM_JOINED", started.room()));
        });
    }

    @MessageMapping("/rooms/matches/next")
    public void startNextMatch(StartNextMatchRequest request, SimpMessageHeaderAccessor headers) {
        if (request == null) throw new GameRuleException(null, "Dados da partida inválidos.");
        Long authenticatedPlayerId = presence.requirePlayerId(headers.getSessionId());
        if (!authenticatedPlayerId.equals(request.playerId())) {
            messaging.convertAndSend(playerTopic(authenticatedPlayerId), Event.error("Identidade do jogador inválida."));
            return;
        }
        handle(authenticatedPlayerId, () -> {
            StartedRoom started = rooms.startNextMatch(request.roomId(), authenticatedPlayerId);
            messaging.convertAndSend(roomTopic(started.room().roomId()), Event.success("MATCH_STARTED", started.match()));
        });
    }

    private void handle(Long fallbackPlayerId, Runnable action) {
        try {
            action.run();
        } catch (GameRuleException exception) {
            Long playerId = exception.getPlayerId() != null ? exception.getPlayerId() : fallbackPlayerId;
            if (playerId != null) messaging.convertAndSend(playerTopic(playerId), Event.error(exception.getMessage()));
        }
    }
}
