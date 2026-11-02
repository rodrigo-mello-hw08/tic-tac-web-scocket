package com.tic.domain.database;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "matches")
@Getter @Setter @NoArgsConstructor
public class Match {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "player_a_id")
    private Player playerA;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "player_b_id")
    private Player playerB;
    private Integer scoreA = 0;
    private Integer scoreB = 0;
    private String status = "ACTIVE";
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id")
    private Room room;
}
