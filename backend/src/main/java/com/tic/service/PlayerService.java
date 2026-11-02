package com.tic.service;

import com.tic.domain.Player;
import com.tic.exception.GameRuleException;
import com.tic.repository.PlayerRepository;
import com.tic.domain.GameMessages.PlayerView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {
    private final PlayerRepository players;
    private final GameMessageMapper mapper;

    public PlayerService(PlayerRepository players, GameMessageMapper mapper) {
        this.players = players;
        this.mapper = mapper;
    }

    @Transactional
    public PlayerView register(String name, String password) {
        if (blank(name) || blank(password)) throw new GameRuleException(null, "Nome e senha são obrigatórios.");
        return mapper.toView(players.save(new Player(name.trim(), password)));
    }

    @Transactional(readOnly = true)
    public PlayerView login(Long playerId, String password) {
        if (playerId == null || password == null)
            throw new GameRuleException(playerId, "ID e senha são obrigatórios.");
        Player player = players.findById(playerId)
                .filter(candidate -> candidate.getPassword().equals(password))
                .orElseThrow(() -> new GameRuleException(playerId, "ID ou senha inválidos."));
        return mapper.toView(player);
    }

    @Transactional(readOnly = true)
    public Player requirePlayer(Long playerId, Long errorRecipient) {
        return players.findById(playerId)
                .orElseThrow(() -> new GameRuleException(errorRecipient, "Um dos jogadores não existe."));
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
