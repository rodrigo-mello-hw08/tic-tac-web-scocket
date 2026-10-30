package com.tic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.CollectionTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
@Getter @Setter @NoArgsConstructor
public class Board {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer moves = 0;

    @ElementCollection
    @CollectionTable(name = "board_cells", joinColumns = @JoinColumn(name = "board_id"))
    @Column(name = "cell_value", length = 1)
    @OrderColumn(name = "cell_index")
    private List<String> cells = new ArrayList<>(List.of("", "", "", "", "", "", "", "", ""));
}
