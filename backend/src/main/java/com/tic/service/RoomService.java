package com.tic.service;

import com.tic.domain.database.Board;
import com.tic.domain.database.Match;
import com.tic.domain.database.Player;
import com.tic.domain.database.Room;
import com.tic.exception.GameRuleException;
import com.tic.mapper.GameMessageMapper;
import com.tic.repository.RoomRepository;
import com.tic.domain.pojo.GameMessages.MatchView;
import com.tic.domain.pojo.GameMessages.RoomView;
import com.tic.domain.pojo.StartedRoom;
import com.tic.validator.RoomValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Match match = createMatch(room);
        roomRepository.save(room);
        return new StartedRoom(mapper.toView(room, match), mapper.toView(match));
    }

    @Transactional
    public StartedRoom startNextMatch(Long roomId, Long playerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GameRuleException(playerId, "Sala inexistente ou jogador não pertence a ela."));
        validator.validateCanStartNextMatch(room, playerId);

        Match match = createMatch(room);
        roomRepository.save(room);
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
}
