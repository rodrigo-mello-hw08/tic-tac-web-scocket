package com.tic.repository;

import com.tic.domain.database.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByClosedTrueAndPlayerA_IdAndPlayerB_IdAndScoreAAndScoreB(
            Long playerAId, Long playerBId, Integer scoreA, Integer scoreB);

    @Query("select r from Room r where r.closed = true and (r.playerA.id = :playerId or r.playerB.id = :playerId) order by r.id desc")
    List<Room> findClosedRoomsForPlayer(@Param("playerId") Long playerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
