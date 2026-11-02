package com.tic.websocket;

import com.tic.domain.pojo.GameMessages.EmptyRequest;
import com.tic.domain.pojo.GameMessages.Event;
import com.tic.domain.pojo.GameMessages.LoginRequest;
import com.tic.domain.pojo.GameMessages.RegisterRequest;
import com.tic.domain.pojo.GameMessages.SessionRequest;
import com.tic.domain.pojo.GameMessages.SessionView;
import com.tic.exception.GameRuleException;
import com.tic.service.PlayerService;
import com.tic.service.PresenceService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class UserWebSocketController {
    private final PlayerService playerService;
    private final PresenceService presenceService;

    @MessageMapping("/users/register")
    @SendToUser(value = "/queue/users", broadcast = false)
    public Event register(RegisterRequest request) {
        if (request == null) throw new GameRuleException(null, "Dados de cadastro inválidos.");
        return Event.success("REGISTERED", playerService.register(request.login(), request.password()));
    }

    @MessageMapping("/users/login")
    @SendToUser(value = "/queue/users", broadcast = false)
    public Event login(LoginRequest request, SimpMessageHeaderAccessor headers) {
        if (request == null) throw new GameRuleException(null, "Dados de login inválidos.");
        var user = playerService.login(request.login(), request.password());
        String sessionId = headers.getSessionId();
        presenceService.login(sessionId, user.id());
        String sessionToken = presenceService.issueSessionToken(user.id());
        presenceService.publishActiveUsers();
        return Event.success("LOGGED_IN", new SessionView(user.id(), user.login(), sessionToken));
    }

    @MessageMapping("/users/session")
    @SendToUser(value = "/queue/users", broadcast = false)
    public Event authenticateDashboard(SessionRequest request, SimpMessageHeaderAccessor headers) {
        Long playerId = presenceService.consumeSessionToken(request == null ? null : request.sessionToken());
        presenceService.login(headers.getSessionId(), playerId);
        var user = playerService.requirePlayer(playerId, null);
        presenceService.publishActiveUsers();
        return Event.success("LOGGED_IN", new SessionView(user.getId(), user.getLogin(), null));
    }

    @MessageMapping("/users/active")
    @SendToUser(value = "/queue/users", broadcast = false)
    public Event activeUsers(EmptyRequest request, SimpMessageHeaderAccessor headers) {
        presenceService.requirePlayerId(headers.getSessionId());
        return Event.success("ACTIVE_USERS", presenceService.activeUsers());
    }

    @MessageMapping("/users/logout")
    @SendToUser(value = "/queue/users", broadcast = false)
    public Event logout(EmptyRequest request, SimpMessageHeaderAccessor headers) {
        if (!presenceService.logout(headers.getSessionId()))
            throw new GameRuleException(null, "A sessão não está autenticada.");
        presenceService.publishActiveUsers();
        return Event.success("LOGGED_OUT", null);
    }

}
