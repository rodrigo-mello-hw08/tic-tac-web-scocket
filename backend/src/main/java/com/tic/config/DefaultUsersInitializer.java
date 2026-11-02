package com.tic.config;

import com.tic.domain.database.Player;
import com.tic.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultUsersInitializer implements ApplicationRunner {
    private final PlayerRepository playerRepository;

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("admin", "admin");
        createIfMissing("rodrigo", "1234");
    }

    private void createIfMissing(String login, String password) {
        if (!playerRepository.existsByLogin(login)) playerRepository.save(new Player(login, password));
    }
}
