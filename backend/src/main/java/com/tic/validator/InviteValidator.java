package com.tic.validator;

import com.tic.exception.GameRuleException;
import org.springframework.stereotype.Component;

@Component
public class InviteValidator {
    public void validateSendRequest(Long fromPlayerId, Long toPlayerId) {
        if (fromPlayerId == null || toPlayerId == null || fromPlayerId.equals(toPlayerId))
            throw new GameRuleException(fromPlayerId, "Convite inválido.");
    }

    public void validateRecipientOnline(Long fromPlayerId, boolean recipientOnline) {
        if (!recipientOnline)
            throw new GameRuleException(fromPlayerId, "O jogador convidado não está conectado.");
    }
}
