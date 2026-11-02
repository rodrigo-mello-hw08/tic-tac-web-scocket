package com.tic.domain.pojo;

public record InviteSent(String inviteId, Long fromPlayerId, Long toPlayerId, GameMessages.InviteView view) {}
