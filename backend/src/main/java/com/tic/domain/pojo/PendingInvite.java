package com.tic.domain.pojo;

import com.tic.domain.database.Player;

public record PendingInvite(Player from, Player to) {}
