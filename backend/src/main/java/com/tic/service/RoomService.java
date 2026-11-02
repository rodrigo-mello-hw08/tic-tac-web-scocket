package com.tic.service;

import com.tic.domain.Board;
import com.tic.domain.Match;
import com.tic.domain.Player;
import com.tic.domain.Room;
import com.tic.exception.GameRuleException;
import com.tic.repository.RoomRepository;
import com.tic.domain.GameMessages.MatchView;
import com.tic.domain.GameMessages.RoomView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
    private final RoomRepository rooms;
    private final GameMessageMapper mapper;

    public RoomService(RoomRepository rooms, GameMessageMapper mapper) {
        this.rooms = rooms;
        this.mapper = mapper;
    }

    @Transactional
    public StartedRoom createRoom(Player playerA, Player playerB) {
        Room room = new Room();
        room.setPlayerA(playerA);
        room.setPlayerB(playerB);
        Match match = createMatch(room);
        rooms.save(room);
        return new StartedRoom(mapper.toView(room, match), mapper.toView(match));
    }

    @Transactional
    public StartedRoom startNextMatch(Long roomId, Long playerId) {
        Room room = rooms.findById(roomId)
                .orElseThrow(() -> new GameRuleException(playerId, "Sala inexistente ou jogador não pertence a ela."));
        if (!room.getPlayerA().getId().equals(playerId) && !room.getPlayerB().getId().equals(playerId))
            throw new GameRuleException(playerId, "Sala inexistente ou jogador não pertence a ela.");
        if (room.getMatches().stream().anyMatch(match -> "ACTIVE".equals(match.getStatus())))
            throw new GameRuleException(playerId, "A sala já possui uma partida ativa.");

        Match match = createMatch(room);
        rooms.save(room);
        return new StartedRoom(mapper.toView(room, match), mapper.toView(match));
    }

    private Match createMatch(Room room) {
        Match match = new Match();
        match.setPlayerA(room.getPlayerA());
        match.setPlayerB(room.getPlayerB());
        match.setBoard(new Board());
        room.addMatch(match);
        return match;
    }

    public record StartedRoom(RoomView room, MatchView match) {}
}
