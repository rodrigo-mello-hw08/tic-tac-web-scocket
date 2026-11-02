package com.tic.websocket;

import com.tic.domain.pojo.GameMessages.Event;
import com.tic.domain.pojo.GameMessages.InviteRequest;
import com.tic.domain.pojo.GameMessages.InviteResponseRequest;
import com.tic.domain.pojo.InviteResponse;
import com.tic.domain.pojo.InviteSent;
import com.tic.domain.pojo.StartedRoom;
import com.tic.exception.GameRuleException;
import com.tic.service.InviteService;
import com.tic.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class InviteWebSocketController {
    private final InviteService invites;
    private final PresenceService presence;
    private final SimpMessagingTemplate messaging;

    @MessageMapping("/invites/send")
    public void sendInvite(InviteRequest request, SimpMessageHeaderAccessor headers) {
        if (request == null) throw new GameRuleException(null, "Dados do convite inválidos.");
        Long playerId = presence.requirePlayerId(headers.getSessionId());
        try {
            InviteSent invite = invites.send(playerId, request.toPlayerId());
            messaging.convertAndSend(topic(invite.toPlayerId()), Event.success("INVITE_RECEIVED", invite.view()));
            messaging.convertAndSend(topic(invite.fromPlayerId()), Event.success("INVITE_SENT", invite.view()));
        } catch (GameRuleException exception) {
            messaging.convertAndSend(topic(playerId), Event.success("INVITE_SEND_FAILED",
                    java.util.Map.of("toPlayerId", request.toPlayerId(), "message", exception.getMessage())));
        }
    }

    @MessageMapping("/invites/respond")
    public void respondToInvite(InviteResponseRequest request, SimpMessageHeaderAccessor headers) {
        if (request == null) throw new GameRuleException(null, "Dados da resposta inválidos.");
        Long playerId = presence.requirePlayerId(headers.getSessionId());
        try {
            InviteResponse response = invites.respond(request.inviteId(), playerId, request.accepted());
            if (!response.accepted()) {
                messaging.convertAndSend(topic(response.fromPlayerId()), Event.success("INVITE_DECLINED", response));
                messaging.convertAndSend(topic(response.toPlayerId()), Event.success("INVITE_DECLINED", response));
                return;
            }
            StartedRoom started = response.startedRoom();
            messaging.convertAndSend(topic(response.fromPlayerId()), Event.success("ROOM_JOINED", started.room()));
            messaging.convertAndSend(topic(response.toPlayerId()), Event.success("ROOM_JOINED", started.room()));
        } catch (GameRuleException exception) {
            messaging.convertAndSend(topic(playerId), Event.error(exception.getMessage()));
        }
    }

    @MessageMapping("/invites/pending")
    @SendToUser(value = "/queue/users", broadcast = false)
    public Event pendingInvites(SimpMessageHeaderAccessor headers) {
        Long playerId = presence.requirePlayerId(headers.getSessionId());
        return Event.success("INVITES_PENDING", invites.pendingForInvitedUser(playerId));
    }

    private static String topic(Long playerId) { return "/topic/players/" + playerId; }
}
