package com.tic.service;

import com.tic.domain.database.Board;
import com.tic.domain.database.Match;
import com.tic.domain.database.Player;
import com.tic.domain.database.Room;
import com.tic.exception.GameRuleException;
import com.tic.mapper.GameMessageMapper;
import com.tic.repository.RoomRepository;
import com.tic.domain.pojo.GameMessages.RoomResult;
import com.tic.domain.pojo.GameMessages.RoundState;
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
    public RoundState move(Long roomId, Long playerId, Integer cellIndex) {
        Room room = findRoomForMove(roomId, playerId);
        Match match = findActiveMatch(room, playerId);
        validateCellIndex(cellIndex, playerId);
        Board board = match.getBoard();
        String symbol = applyMove(match, board, playerId, cellIndex);

        boolean won = hasWinner(board.getCells(), symbol);
        boolean draw = !won && board.getMoves() == 9;

        Long winnerId = finishRound(room, match, playerId, won, draw);
        StartedRoom next = createNextMatchIfFinished(room, match, won || draw);
        roomRepository.save(room);
        Long nextId = (won || draw) ? null : (playerId.equals(room.getPlayerA().getId()) ? room.getPlayerB().getId() : room.getPlayerA().getId());
        return new RoundState(roomId, mapper.toView(match), nextId, won ? "WIN" : draw ? "DRAW" : null, winnerId, next);
    }

    private Room findRoomForMove(Long roomId, Long playerId) {
        Room room = roomRepository.findByIdForUpdate(roomId).orElseThrow(() -> new GameRuleException(playerId, "Sala inexistente ou jogador inválido."));
        validator.validatePlayerCanAccess(room, playerId);
        if (room.isClosed()) throw new GameRuleException(playerId, "A sala está encerrada.");
        return room;
    }

    private Match findActiveMatch(Room room, Long playerId) {
        return room.getMatches().stream().filter(m -> "ACTIVE".equals(m.getStatus())).max(Comparator.comparing(Match::getId))
                .orElseThrow(() -> new GameRuleException(playerId, "Não há partida ativa nesta sala."));
    }

    private void validateCellIndex(Integer index, Long playerId) {
        if (index == null || index < 0 || index > 8) throw new GameRuleException(playerId, "Índice da casa inválido.");
    }

    private String applyMove(Match match, Board board, Long playerId, int index) {
        String symbol = board.getMoves() % 2 == 0 ? "X" : "O";
        Long xPlayer = "X".equals(match.getSymbolA()) ? match.getPlayerA().getId() : match.getPlayerB().getId();
        Long expected = board.getMoves() % 2 == 0 ? xPlayer : (xPlayer.equals(match.getPlayerA().getId()) ? match.getPlayerB().getId() : match.getPlayerA().getId());
        if (!expected.equals(playerId)) throw new GameRuleException(playerId, "Não é seu turno.");
        if (!board.getCells().get(index).isEmpty()) throw new GameRuleException(playerId, "Casa já ocupada.");
        board.getCells().set(index, symbol);
        board.setMoves(board.getMoves() + 1);
        return symbol;
    }

    private Long finishRound(Room room, Match match, Long playerId, boolean won, boolean draw) {
        if (!won && !draw) return null;
        match.setStatus("FINISHED");
        if (!won) return null;
        if (playerId.equals(room.getPlayerA().getId())) room.setScoreA(room.getScoreA() + 1);
        else room.setScoreB(room.getScoreB() + 1);
        return playerId;
    }

    private StartedRoom createNextMatchIfFinished(Room room, Match match, boolean finished) {
        if (!finished) return null;
        Match following = createMatch(room, match.getSymbolB(), match.getSymbolA());
        return new StartedRoom(mapper.toView(room, following), mapper.toView(following));
    }
    private boolean hasWinner(java.util.List<String> c, String s) {
        int[][] lines = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        int winningProduct = "X".equals(s) ? 27 : 125;
        for (int[] line : lines) {
            int product = cellValue(c.get(line[0])) * cellValue(c.get(line[1])) * cellValue(c.get(line[2]));
            if (product == winningProduct) return true;
        }
        return false;
    }

    private int cellValue(String cell) {
        return switch (cell) {
            case "X" -> 3;
            case "O" -> 5;
            default -> 1;
        };
    }

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
