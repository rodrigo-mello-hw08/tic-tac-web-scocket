package com.tic.service;

import com.tic.domain.database.Player;
import com.tic.domain.pojo.GameMessages.InviteView;
import com.tic.domain.pojo.GameMessages.PlayerView;
import com.tic.domain.pojo.InviteResponse;
import com.tic.domain.pojo.InviteSent;
import com.tic.domain.pojo.PendingInvite;
import com.tic.domain.pojo.StartedRoom;
import com.tic.mapper.GameMessageMapper;
import com.tic.validator.InviteValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class InviteService {
    private final PlayerService playerService;
    private final PresenceService presenceService;
    private final RoomService roomService;
    private final GameMessageMapper mapper;
    private final InviteValidator validator;
    private final Map<String, PendingInvite> pendingInvites = new ConcurrentHashMap<>();

    public InviteSent send(Long fromPlayerId, Long toPlayerId) {
        validator.validateSendRequest(fromPlayerId, toPlayerId);
        Player from = playerService.requirePlayer(fromPlayerId, fromPlayerId);
        Player to = playerService.requirePlayer(toPlayerId, fromPlayerId);
        validator.validateRecipientOnline(fromPlayerId, presenceService.isOnline(to.getId()));

        String inviteId = UUID.randomUUID().toString();
        pendingInvites.put(inviteId, new PendingInvite(from, to));
        return new InviteSent(inviteId, from.getId(), to.getId(),
                new InviteView(inviteId, mapper.toView(from), mapper.toView(to)));
    }

    @Transactional
    public InviteResponse respond(String inviteId, Long playerId, boolean accepted) {
        PendingInvite invite = validator.validateAndConsumeResponse(inviteId, playerId, pendingInvites);

        if (!accepted) {
            return new InviteResponse(invite.from().getId(), invite.to().getId(), false,
                    mapper.toView(invite.from()), mapper.toView(invite.to()), null);
        }
        StartedRoom started = roomService.createRoom(invite.from(), invite.to());
        return new InviteResponse(invite.from().getId(), invite.to().getId(), true,
                mapper.toView(invite.from()), mapper.toView(invite.to()), started);
    }

}
