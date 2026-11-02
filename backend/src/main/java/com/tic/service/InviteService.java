package com.tic.service;

import com.tic.domain.database.Invite;
import com.tic.domain.database.Player;
import com.tic.domain.pojo.GameMessages.Event;
import com.tic.domain.pojo.GameMessages.InviteView;
import com.tic.domain.pojo.InviteResponse;
import com.tic.domain.pojo.InviteSent;
import com.tic.domain.pojo.StartedRoom;
import com.tic.exception.GameRuleException;
import com.tic.mapper.GameMessageMapper;
import com.tic.repository.InviteRepository;
import com.tic.validator.InviteValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InviteService {
    private final InviteRepository inviteRepository;
    private final PlayerService playerService;
    private final PresenceService presenceService;
    private final RoomService roomService;
    private final GameMessageMapper mapper;
    private final InviteValidator validator;
    private final SimpMessagingTemplate messaging;

    @Transactional
    public InviteSent send(Long requestUserId, Long invitedUserId) {
        validator.validateSendRequest(requestUserId, invitedUserId);
        Player requestUser = playerService.requirePlayer(requestUserId, requestUserId);
        Player invitedUser = playerService.requirePlayer(invitedUserId, requestUserId);
        validator.validateRecipientOnline(requestUserId, presenceService.isOnline(invitedUser.getId()));
        if (inviteRepository.existsByRequestUser_IdAndInvitedUser_IdAndPendingTrue(requestUserId, invitedUserId))
            throw new GameRuleException(requestUserId, "Já existe um convite pendente para este jogador.");

        Invite invite = new Invite();
        invite.setRequestUser(requestUser);
        invite.setInvitedUser(invitedUser);
        invite.setPending(true);
        invite.setAccepted(false);
        invite = inviteRepository.save(invite);
        InviteView view = toView(invite);
        return new InviteSent(invite.getId(), requestUserId, invitedUserId, view);
    }

    @Transactional
    public InviteResponse respond(String inviteId, Long playerId, boolean accepted) {
        if (inviteId == null || playerId == null)
            throw new GameRuleException(playerId, "Resposta de convite inválida.");
        Invite invite = inviteRepository.findByIdForUpdate(inviteId)
                .filter(Invite::isPending)
                .orElseThrow(() -> new GameRuleException(playerId, "Convite inexistente ou já respondido."));
        if (!invite.getInvitedUser().getId().equals(playerId))
            throw new GameRuleException(playerId, "Este convite não foi destinado a este jogador.");

        invite.setPending(false);
        invite.setAccepted(accepted);
        inviteRepository.save(invite);
        StartedRoom started = accepted
                ? roomService.createRoom(invite.getRequestUser(), invite.getInvitedUser()) : null;
        return new InviteResponse(invite.getId(), invite.getRequestUser().getId(), invite.getInvitedUser().getId(),
                accepted, mapper.toView(invite.getRequestUser()), mapper.toView(invite.getInvitedUser()), started);
    }

    @Transactional(readOnly = true)
    public List<InviteView> pendingForInvitedUser(Long playerId) {
        return inviteRepository.findByInvitedUser_IdAndPendingTrue(playerId).stream().map(this::toView).toList();
    }

    @EventListener
    @Transactional
    public void cancelForOfflinePlayer(PlayerOfflineEvent event) {
        for (Invite invite : inviteRepository.findPendingForPlayer(event.playerId())) {
            invite.setPending(false);
            invite.setAccepted(false);
            inviteRepository.save(invite);
            Map<String, Object> payload = Map.of("inviteId", invite.getId(), "otherPlayerId", invite.getInvitedUser().getId());
            messaging.convertAndSend(topic(invite.getRequestUser().getId()), Event.success("INVITE_CANCELLED", payload));
            messaging.convertAndSend(topic(invite.getInvitedUser().getId()), Event.success("INVITE_CANCELLED", payload));
        }
    }

    private InviteView toView(Invite invite) {
        return new InviteView(invite.getId(), mapper.toView(invite.getRequestUser()), mapper.toView(invite.getInvitedUser()));
    }

    private static String topic(Long playerId) { return "/topic/players/" + playerId; }
}
