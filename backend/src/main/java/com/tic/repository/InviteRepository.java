package com.tic.repository;

import com.tic.domain.database.Invite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, String> {
    boolean existsByRequestUser_IdAndInvitedUser_IdAndPendingTrue(Long requestUserId, Long invitedUserId);
    List<Invite> findByInvitedUser_IdAndPendingTrue(Long invitedUserId);
    @Query("select i from Invite i where i.pending = true and (i.requestUser.id = :playerId or i.invitedUser.id = :playerId)")
    List<Invite> findPendingForPlayer(@Param("playerId") Long playerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invite i where i.id = :id")
    Optional<Invite> findByIdForUpdate(@Param("id") String id);
}
