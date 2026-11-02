package com.tic.service;

import com.tic.domain.database.Player;
import com.tic.exception.GameRuleException;
import com.tic.mapper.GameMessageMapper;
import com.tic.repository.PlayerRepository;
import com.tic.domain.pojo.GameMessages.PlayerView;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final GameMessageMapper mapper;

    @Transactional
    public PlayerView register(String login, String password) {
        if (blank(login) || blank(password)) throw new GameRuleException(null, "Login e senha são obrigatórios.");
        String normalizedLogin = login.trim();
        if (playerRepository.existsByLogin(normalizedLogin)) throw new GameRuleException(null, "Este login já está em uso.");
        try {
            return mapper.toView(playerRepository.saveAndFlush(new Player(normalizedLogin, password)));
        } catch (DataIntegrityViolationException exception) {
            throw new GameRuleException(null, "Este login já está em uso.");
        }
    }

    @Transactional(readOnly = true)
    public PlayerView login(String login, String password) {
        if (blank(login) || password == null)
            throw new GameRuleException(null, "Login e senha são obrigatórios.");
        Player player = playerRepository.findByLogin(login.trim())
                .filter(candidate -> candidate.getPassword().equals(password))
                .orElseThrow(() -> new GameRuleException(null, "Login ou senha inválidos."));
        return mapper.toView(player);
    }

    @Transactional(readOnly = true)
    public Player requirePlayer(Long playerId, Long errorRecipient) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new GameRuleException(errorRecipient, "Um dos jogadores não existe."));
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
