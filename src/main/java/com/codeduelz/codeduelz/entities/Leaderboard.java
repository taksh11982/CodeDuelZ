package com.codeduelz.codeduelz.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "leaderboard")
public class Leaderboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leaderboard_id")
    private Long leaderboardId;
    @OneToOne
    @JoinColumn(name = "user_id",nullable = false,unique = true)
    private User user;
    @Column(nullable = false)
    private Integer rank;
}
