package com.tic.config;

import com.tic.domain.database.Player;
import com.tic.repository.PlayerRepository;
import com.tic.domain.database.Room;
import com.tic.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultUsersInitializer implements ApplicationRunner {
    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("admin", "admin");
        createIfMissing("rodrigo", "1234");
        Player pedro = createPedroIfMissing();
        seedRoomHistory(pedro);
    }

    private void createIfMissing(String login, String password) {
        if (!playerRepository.existsByLogin(login)) playerRepository.save(new Player(login, password));
    }

    private Player createPedroIfMissing() {
        return playerRepository.findByLogin("pedro").orElseGet(() -> {
            Player pedro = new Player("pedro", "1234");
            return playerRepository.save(pedro);
        });
    }

    private void seedRoomHistory(Player pedro) {
        Player rodrigo = playerRepository.findByLogin("rodrigo").orElse(null);
        if (rodrigo == null) return;
        createHistoryRoomIfMissing(pedro, rodrigo, 3, 2);
        createHistoryRoomIfMissing(rodrigo, pedro, 7, 1);
    }

    private void createHistoryRoomIfMissing(Player playerA, Player playerB, int scoreA, int scoreB) {
        if (roomRepository.existsByClosedTrueAndPlayerA_IdAndPlayerB_IdAndScoreAAndScoreB(
                playerA.getId(), playerB.getId(), scoreA, scoreB)) return;
        Room room = new Room();
        room.setPlayerA(playerA);
        room.setPlayerB(playerB);
        room.setScoreA(scoreA);
        room.setScoreB(scoreB);
        room.setClosed(true);
        roomRepository.save(room);
    }
}
