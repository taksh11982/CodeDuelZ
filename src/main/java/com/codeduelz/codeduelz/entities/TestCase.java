package com.codeduelz.codeduelz.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "test_cases")
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long testcaseId;
    @ManyToOne
    @JoinColumn(name = "problem_id",nullable = false)
    private Problem problem;
    @Column(nullable = false,length = 1000)
    private String input;
    @Column(nullable = false,length = 1000)
    private String expectedOutput;
}
