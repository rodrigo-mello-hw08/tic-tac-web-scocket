package com.tic.service;

import com.tic.domain.Board;
import com.tic.domain.Match;
import com.tic.domain.Player;
import com.tic.domain.Room;
import com.tic.domain.GameMessages.MatchView;
import com.tic.domain.GameMessages.PlayerView;
import com.tic.domain.GameMessages.RoomView;
import org.springframework.stereotype.Component;

@Component
public class GameMessageMapper {
    public PlayerView toView(Player player) {
        return new PlayerView(player.getId(), player.getName());
    }

    public RoomView toView(Room room, Match activeMatch) {
        return new RoomView(room.getId(), toView(room.getPlayerA()), toView(room.getPlayerB()),
                activeMatch == null ? null : activeMatch.getId());
    }

    public MatchView toView(Match match) {
        Board board = match.getBoard();
        return new MatchView(match.getId(), match.getRoom().getId(), toView(match.getPlayerA()),
                toView(match.getPlayerB()), match.getScoreA(), match.getScoreB(), match.getStatus(),
                board.getId(), board.getMoves(), board.getCells());
    }
}
