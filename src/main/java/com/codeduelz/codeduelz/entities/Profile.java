package com.codeduelz.codeduelz.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "profiles")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;
    @OneToOne
    @JoinColumn(name = "user_id",nullable = false,unique = true)
    private User user;
    @Column(nullable = false)
    private Integer totalMatches=0;
    @Column(nullable = false)
    private Integer wins=0;
    @Column(length = 500)
    private String bio;
    private String avatar;
}
