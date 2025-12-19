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
    @Column(nullable = false,length = 2000)
    private String description;
    @Column(nullable = false)
    private String difficulty;
}
