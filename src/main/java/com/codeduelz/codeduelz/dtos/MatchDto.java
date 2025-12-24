package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
    private User player1;
    private User player2;
    private Problem problem;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
