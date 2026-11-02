package com.tic.service;

import com.tic.domain.database.Board;
import com.tic.domain.database.Match;
import com.tic.domain.database.Player;
import com.tic.domain.database.Room;
import com.tic.exception.GameRuleException;
import com.tic.mapper.GameMessageMapper;
import com.tic.repository.RoomRepository;
import com.tic.domain.pojo.GameMessages.RoomResult;
import com.tic.domain.pojo.StartedRoom;
import com.tic.validator.RoomValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@AllArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final GameMessageMapper mapper;
    private final RoomValidator validator;

    @Transactional
    public StartedRoom createRoom(Player playerA, Player playerB) {
        Room room = new Room();
        room.setPlayerA(playerA);
        room.setPlayerB(playerB);
        room.setScoreA(0);
        room.setScoreB(0);
        Match match = createMatch(room, "X", "O");
        roomRepository.save(room);
        return new StartedRoom(mapper.toView(room, match), mapper.toView(match));
    }

    @Transactional
    public StartedRoom startNextMatch(Long roomId, Long playerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GameRuleException(playerId, "Sala inexistente ou jogador não pertence a ela."));
        validator.validateCanStartNextMatch(room, playerId);

        Match previous = latestMatch(room, playerId);
        Match match = createMatch(room, previous.getSymbolB(), previous.getSymbolA());
        roomRepository.save(room);
        return new StartedRoom(mapper.toView(room, match), mapper.toView(match));
    }

    @Transactional
    public RoomResult forfeit(Long roomId, Long playerId, boolean closeRoom) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new GameRuleException(playerId, "Sala inexistente ou jogador não pertence a ela."));
        validator.validatePlayerCanAccess(room, playerId);

        Match match = room.getMatches().stream()
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
                .findFirst()
                .orElseThrow(() -> new GameRuleException(playerId, "Não há partida ativa nesta sala."));
        Long winnerId;
        if (match.getPlayerA().getId().equals(playerId)) {
            room.setScoreB(room.getScoreB() + 1);
            winnerId = match.getPlayerB().getId();
        } else {
            room.setScoreA(room.getScoreA() + 1);
            winnerId = match.getPlayerA().getId();
        }
        match.setStatus("FINISHED");
        Match followingMatch = null;
        StartedRoom nextMatch = null;
        if (closeRoom) {
            room.setClosed(true);
        } else {
            followingMatch = createMatch(room, match.getSymbolB(), match.getSymbolA());
        }
        roomRepository.save(room);
        if (followingMatch != null)
            nextMatch = new StartedRoom(mapper.toView(room, followingMatch), mapper.toView(followingMatch));
        return new RoomResult(room.getId(), mapper.toView(match), room.isClosed(), winnerId, nextMatch);
    }

    private Match latestMatch(Room room, Long playerId) {
        return room.getMatches().stream()
                .max(Comparator.comparing(Match::getId))
                .orElseThrow(() -> new GameRuleException(playerId, "A sala ainda não possui partidas."));
    }

    private Match createMatch(Room room, String symbolA, String symbolB) {
        Match match = new Match();
        match.setPlayerA(room.getPlayerA());
        match.setPlayerB(room.getPlayerB());
        match.setSymbolA(symbolA);
        match.setSymbolB(symbolB);
        match.setBoard(new Board());
        room.addMatch(match);
        return match;
    }
}
