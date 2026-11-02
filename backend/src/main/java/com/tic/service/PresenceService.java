package com.tic.service;

import com.tic.domain.pojo.GameMessages;
import com.tic.domain.pojo.GameMessages.PlayerView;
import com.tic.domain.pojo.PendingToken;
import com.tic.exception.GameRuleException;
import com.tic.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private static final long TOKEN_TTL_SECONDS = 60;
    private static final String ACTIVE_USERS_TOPIC = "/topic/users/active";

    private final Map<String, Long> sessionPlayers = new ConcurrentHashMap<>();
    private final Map<String, PendingToken> pendingTokens = new ConcurrentHashMap<>();
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messaging;
    private final ApplicationEventPublisher events;

    public String issueSessionToken(Long playerId) {
        purgeExpiredTokens();
        String token = UUID.randomUUID().toString();
        pendingTokens.put(token, new PendingToken(playerId, Instant.now().plusSeconds(TOKEN_TTL_SECONDS)));
        return token;
    }

    public Long consumeSessionToken(String token) {
        if (token == null || token.isBlank()) throw new GameRuleException(null, "Token de sessão inválido ou expirado.");
        PendingToken pending = pendingTokens.remove(token);
        if (pending == null || !pending.expiresAt().isAfter(Instant.now()))
            throw new GameRuleException(null, "Token de sessão inválido ou expirado.");
        return pending.playerId();
    }

    public void login(String sessionId, Long playerId) {
        if (sessionId == null || playerId == null) throw new GameRuleException(null, "Sessão de usuário inválida.");
        sessionPlayers.put(sessionId, playerId);
    }

    public Long requirePlayerId(String sessionId) {
        Long playerId = sessionPlayers.get(sessionId);
        if (playerId == null) throw new GameRuleException(null, "É necessário estar logado para esta operação.");
        return playerId;
    }

    public boolean isOnline(Long playerId) {
        return playerId != null && sessionPlayers.containsValue(playerId);
    }

    public boolean logout(String sessionId) {
        Long playerId = sessionId == null ? null : sessionPlayers.remove(sessionId);
        if (playerId != null) events.publishEvent(new PlayerOfflineEvent(playerId));
        return playerId != null;
    }

    public java.util.List<PlayerView> activeUsers() {
        var activeIds = java.util.Set.copyOf(sessionPlayers.values());
        return playerRepository.findAll().stream()
                .filter(player -> activeIds.contains(player.getId()))
                .map(player -> new PlayerView(player.getId(), player.getLogin()))
                .toList();
    }

    public void publishActiveUsers() {
        messaging.convertAndSend(ACTIVE_USERS_TOPIC,
                GameMessages.Event.success("ACTIVE_USERS", activeUsers()));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Long playerId = event.getSessionId() == null ? null : sessionPlayers.remove(event.getSessionId());
        if (playerId != null) {
            events.publishEvent(new PlayerOfflineEvent(playerId));
            publishActiveUsers();
        }
        // Tokens are independent of the issuing WebSocket and remain valid until consumed or expired.
        purgeExpiredTokens();
    }

    private void purgeExpiredTokens() {
        Instant now = Instant.now();
        pendingTokens.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

}
