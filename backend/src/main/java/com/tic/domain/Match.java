package com.tic.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Match {
    private Long id;
    private Player playerA;
    private Player playerB;
    private Integer scoreA;
    private Integer scoreB;
    private Board board;
}

