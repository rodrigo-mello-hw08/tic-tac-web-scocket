package com.tic.service;

import com.tic.domain.Player;
import com.tic.exception.GameRuleException;
import com.tic.websocket.GameMessages.InviteView;
import com.tic.websocket.GameMessages.PlayerView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InviteService {
    private final PlayerService players;
    private final PresenceService presence;
    private final RoomService rooms;
    private final GameMessageMapper mapper;
    private final Map<String, PendingInvite> pendingInvites = new ConcurrentHashMap<>();

    public InviteService(PlayerService players, PresenceService presence, RoomService rooms, GameMessageMapper mapper) {
        this.players = players;
        this.presence = presence;
        this.rooms = rooms;
        this.mapper = mapper;
    }

    public InviteSent send(Long fromPlayerId, Long toPlayerId) {
        if (fromPlayerId == null || toPlayerId == null || fromPlayerId.equals(toPlayerId))
            throw new GameRuleException(fromPlayerId, "Convite inválido.");
        Player from = players.requirePlayer(fromPlayerId, fromPlayerId);
        Player to = players.requirePlayer(toPlayerId, fromPlayerId);
        if (!presence.isOnline(to.getId()))
            throw new GameRuleException(fromPlayerId, "O jogador convidado não está conectado.");

        String inviteId = UUID.randomUUID().toString();
        pendingInvites.put(inviteId, new PendingInvite(from, to));
        return new InviteSent(inviteId, from.getId(), to.getId(),
                new InviteView(inviteId, mapper.toView(from), mapper.toView(to)));
    }

    @Transactional
    public InviteResponse respond(String inviteId, Long playerId, boolean accepted) {
        if (inviteId == null || playerId == null)
            throw new GameRuleException(playerId, "Resposta de convite inválida.");
        PendingInvite invite = pendingInvites.get(inviteId);
        if (invite == null || !invite.to().getId().equals(playerId))
            throw new GameRuleException(playerId, "Convite inexistente ou não destinado a este jogador.");
        if (!pendingInvites.remove(inviteId, invite))
            throw new GameRuleException(playerId, "Este convite já foi respondido.");

        if (!accepted) {
            return new InviteResponse(invite.from().getId(), invite.to().getId(), false,
                    mapper.toView(invite.from()), mapper.toView(invite.to()), null);
        }
        RoomService.StartedRoom started = rooms.createRoom(invite.from(), invite.to());
        return new InviteResponse(invite.from().getId(), invite.to().getId(), true,
                mapper.toView(invite.from()), mapper.toView(invite.to()), started);
    }

    private record PendingInvite(Player from, Player to) {}

    public record InviteSent(String inviteId, Long fromPlayerId, Long toPlayerId, InviteView view) {}

    public record InviteResponse(Long fromPlayerId, Long toPlayerId, boolean accepted,
                                 PlayerView fromPlayer, PlayerView toPlayer, RoomService.StartedRoom startedRoom) {}
}
