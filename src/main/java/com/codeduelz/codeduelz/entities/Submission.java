package com.codeduelz.codeduelz.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "submissions")
public class    Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionId;
    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
    @ManyToOne
    @JoinColumn(name = "problem_id",nullable = false)
    private Problem problem;
    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;
    @Column(nullable = false,columnDefinition = "TEXT")
    private String code;
    @Column(nullable = false)
    private String language;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;
    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private Integer testCasesPassed;
    private Integer testCasesTotal;

    @Column(columnDefinition = "TEXT")
    private String executionOutput;
}
