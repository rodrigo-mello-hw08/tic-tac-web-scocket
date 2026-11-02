package com.tic.validator;

import com.tic.domain.database.Room;
import com.tic.exception.GameRuleException;
import org.springframework.stereotype.Component;

@Component
public class RoomValidator {
    public void validateCanStartNextMatch(Room room, Long playerId) {
        boolean playerBelongsToRoom = room.getPlayerA().getId().equals(playerId)
                || room.getPlayerB().getId().equals(playerId);
        if (!playerBelongsToRoom)
            throw new GameRuleException(playerId, "Sala inexistente ou jogador não pertence a ela.");

        if (room.getMatches().stream().anyMatch(match -> "ACTIVE".equals(match.getStatus())))
            throw new GameRuleException(playerId, "A sala já possui uma partida ativa.");
    }
}
