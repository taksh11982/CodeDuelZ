package com.codeduelz.codeduelz.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "matches")
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchId;
    @ManyToOne
    @JoinColumn(name = "player1_id",nullable = false)
    private User player1;
    @ManyToOne
    @JoinColumn(name = "player2_id",nullable = false)
    private User player2;
    @OneToOne
    @JoinColumn(name = "problem_id",nullable = false)
    private Problem problem;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MatchStatus status;
}
