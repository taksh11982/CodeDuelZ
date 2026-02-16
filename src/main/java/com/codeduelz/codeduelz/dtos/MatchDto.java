package com.codeduelz.codeduelz.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchDto {
    private Long matchId;
    private Long player1Id;
    private String player1Name;
    private Long player2Id;
    private String player2Name;
    private Long problemId;
    private String problemTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
