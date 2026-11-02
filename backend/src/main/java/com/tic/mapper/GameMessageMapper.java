package com.tic.mapper;

import com.tic.domain.database.Board;
import com.tic.domain.database.Match;
import com.tic.domain.database.Player;
import com.tic.domain.database.Room;
import com.tic.domain.pojo.GameMessages.MatchView;
import com.tic.domain.pojo.GameMessages.PlayerView;
import com.tic.domain.pojo.GameMessages.RoomView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameMessageMapper {
    public PlayerView toView(Player player) {
        return new PlayerView(player.getId(), player.getLogin());
    }

    public RoomView toView(Room room, Match activeMatch) {
        return new RoomView(room.getId(), toView(room.getPlayerA()), toView(room.getPlayerB()),
                activeMatch == null ? null : activeMatch.getId());
    }

    public MatchView toView(Match match) {
        Board board = match.getBoard();
        return new MatchView(match.getId(), match.getRoom().getId(), toView(match.getPlayerA()),
                toView(match.getPlayerB()), match.getRoom().getScoreA(), match.getRoom().getScoreB(), match.getStatus(),
                board.getId(), board.getMoves(), List.copyOf(board.getCells()), match.getSymbolA(), match.getSymbolB());
    }
}
