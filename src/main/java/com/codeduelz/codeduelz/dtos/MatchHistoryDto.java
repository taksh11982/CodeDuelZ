package com.codeduelz.codeduelz.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class MatchHistoryDto {
    private Long matchId;
    private Long opponentId;
    private String opponentName;
    private Long problemId;
    private String problemTitle;
    private String status;   // COMPLETED
    private String result;   // WIN / LOSS
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
