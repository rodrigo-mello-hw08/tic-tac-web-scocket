package com.tic.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    private final Map<String, Long> sessionPlayers = new ConcurrentHashMap<>();

    public void login(String sessionId, Long playerId) {
        if (sessionId != null) sessionPlayers.put(sessionId, playerId);
    }

    public boolean isOnline(Long playerId) {
        return sessionPlayers.containsValue(playerId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (event.getSessionId() != null) sessionPlayers.remove(event.getSessionId());
    }
}
