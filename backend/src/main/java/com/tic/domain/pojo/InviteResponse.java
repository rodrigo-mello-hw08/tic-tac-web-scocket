package com.tic.domain.pojo;

public record InviteResponse(Long fromPlayerId, Long toPlayerId, boolean accepted,
                             GameMessages.PlayerView fromPlayer, GameMessages.PlayerView toPlayer,
                             StartedRoom startedRoom) {}
