package com.tic.websocket;

import com.tic.domain.GameMessages;
import com.tic.exception.GameRuleException;
import com.tic.service.InviteService;
import com.tic.service.PlayerService;
import com.tic.service.PresenceService;
import com.tic.service.RoomService;
import com.tic.domain.GameMessages.Event;
import com.tic.domain.GameMessages.InviteRequest;
import com.tic.domain.GameMessages.InviteResponseRequest;
import com.tic.domain.GameMessages.LoginRequest;
import com.tic.domain.GameMessages.RegisterRequest;
import com.tic.domain.GameMessages.StartNextMatchRequest;
import com.tic.domain.GameMessages.HealthRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {
    private final PlayerService players;
    private final PresenceService presence;
    private final InviteService invites;
    private final RoomService rooms;
    private final SimpMessagingTemplate messaging;

    public GameWebSocketController(PlayerService players, PresenceService presence, InviteService invites,
                                   RoomService rooms, SimpMessagingTemplate messaging) {
        this.players = players;
        this.presence = presence;
        this.invites = invites;
        this.rooms = rooms;
        this.messaging = messaging;
    }

    @MessageMapping("/ping-pong")
    @SendTo("/topic/ping-pong")
    public Event health(HealthRequest request) {
        if (request == null || !"ping".equalsIgnoreCase(request.message())) {
            return Event.error("Expected message 'ping'");
        }
        return Event.success("PONG", java.util.Map.of("message", "pong"));
    }

    @MessageMapping("/players/register")
    public void register(RegisterRequest request) {
        if (request == null) return;
        handle(
                null,
                () -> publishPlayer(players.register(request.name(), request.password()),
                    "REGISTERED")
        );
    }

    @MessageMapping("/players/login")
    public void login(LoginRequest request, SimpMessageHeaderAccessor headers) {
        if (request == null) return;
        handle(request.playerId(), () -> {
            var player = players.login(request.playerId(), request.password());
            presence.login(headers.getSessionId(), player.id());
            publishPlayer(player, "LOGGED_IN");
        });
    }

    @MessageMapping("/invites/send")
    public void sendInvite(InviteRequest request) {
        if (request == null) return;
        handle(request.fromPlayerId(), () -> {
            var invite = invites.send(request.fromPlayerId(), request.toPlayerId());
            messaging.convertAndSend(playerTopic(invite.toPlayerId()), Event.success("INVITE_RECEIVED", invite.view()));
            messaging.convertAndSend(playerTopic(invite.fromPlayerId()), Event.success("INVITE_SENT", invite.view()));
        });
    }

    @MessageMapping("/invites/respond")
    public void respondToInvite(InviteResponseRequest request) {
        if (request == null) return;
        handle(request.playerId(), () -> {
            var response = invites.respond(request.inviteId(), request.playerId(), request.accepted());
            if (!response.accepted()) {
                messaging.convertAndSend(playerTopic(response.fromPlayerId()),
                        Event.success("INVITE_DECLINED", response.toPlayer()));
                messaging.convertAndSend(playerTopic(response.toPlayerId()),
                        Event.success("INVITE_DECLINED", response.fromPlayer()));
                return;
            }

            var started = response.startedRoom();
            messaging.convertAndSend(roomTopic(started.room().roomId()), Event.success("MATCH_STARTED", started.match()));
            messaging.convertAndSend(playerTopic(response.fromPlayerId()), Event.success("ROOM_JOINED", started.room()));
            messaging.convertAndSend(playerTopic(response.toPlayerId()), Event.success("ROOM_JOINED", started.room()));
        });
    }

    @MessageMapping("/rooms/matches/next")
    public void startNextMatch(StartNextMatchRequest request) {
        if (request == null) return;
        handle(request.playerId(), () -> {
            var started = rooms.startNextMatch(request.roomId(), request.playerId());
            messaging.convertAndSend(roomTopic(started.room().roomId()), Event.success("MATCH_STARTED", started.match()));
        });
    }

    private void publishPlayer(GameMessages.PlayerView player, String eventType) {
        messaging.convertAndSend(playerTopic(player.id()), Event.success(eventType, player));
    }

    private void handle(Long fallbackPlayerId, Runnable action) {
        try {
            action.run();
        } catch (GameRuleException exception) {
            Long playerId = exception.getPlayerId() != null ? exception.getPlayerId() : fallbackPlayerId;
            if (playerId != null) messaging.convertAndSend(playerTopic(playerId), Event.error(exception.getMessage()));
        }
    }

    private static String playerTopic(Long id) { return "/topic/players/" + id; }
    private static String roomTopic(Long id) { return "/topic/rooms/" + id; }
}
