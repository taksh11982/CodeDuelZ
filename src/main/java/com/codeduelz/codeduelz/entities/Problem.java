package com.codeduelz.codeduelz.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "problems")
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long problemId;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    // LeetCode specific fields
    private String leetcodeId;    // e.g. "1"
    private String problemSlug;   // e.g. "two-sum"
    private String source;        // "LEETCODE"
}
