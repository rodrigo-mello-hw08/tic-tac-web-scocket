package com.tic.domain.pojo;

import java.time.Instant;

public record PendingToken(Long playerId, Instant expiresAt) {}
