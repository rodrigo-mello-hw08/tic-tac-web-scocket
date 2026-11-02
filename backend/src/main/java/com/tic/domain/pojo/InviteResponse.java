package com.tic.domain.pojo;

public record InviteResponse(String inviteId, Long fromPlayerId, Long toPlayerId, boolean accepted,
                             GameMessages.PlayerView fromPlayer, GameMessages.PlayerView toPlayer,
                             StartedRoom startedRoom) {}
