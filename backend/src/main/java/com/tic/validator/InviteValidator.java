package com.tic.validator;

import com.tic.domain.pojo.PendingInvite;
import com.tic.exception.GameRuleException;
import org.springframework.stereotype.Component;

import java.util.Map;

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

    public PendingInvite validateAndConsumeResponse(String inviteId, Long playerId,
                                                     Map<String, PendingInvite> pendingInvites) {
        if (inviteId == null || playerId == null)
            throw new GameRuleException(playerId, "Resposta de convite inválida.");

        PendingInvite invite = pendingInvites.get(inviteId);
        if (invite == null || !invite.to().getId().equals(playerId))
            throw new GameRuleException(playerId, "Convite inexistente ou não destinado a este jogador.");
        if (!pendingInvites.remove(inviteId, invite))
            throw new GameRuleException(playerId, "Este convite já foi respondido.");

        return invite;
    }
}
