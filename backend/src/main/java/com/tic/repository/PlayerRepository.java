package com.tic.repository;

import com.tic.domain.database.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByLogin(String login);
    java.util.Optional<Player> findByLogin(String login);
}
