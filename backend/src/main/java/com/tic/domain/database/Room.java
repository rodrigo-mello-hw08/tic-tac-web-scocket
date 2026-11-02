package com.tic.domain.database;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter @Setter @NoArgsConstructor
public class Room {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "player_a_id", nullable = false)
    private Player playerA;

    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "player_b_id", nullable = false)
    private Player playerB;

    private boolean closed = false;
    private Integer scoreA = 0;
    private Integer scoreB = 0;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Match> matches = new ArrayList<>();

    public void addMatch(Match match) {
        matches.add(match);
        match.setRoom(this);
    }
}
