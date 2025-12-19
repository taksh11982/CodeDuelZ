package com.codeduelz.codeduelz.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "friends",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id","friend_user_id"})
        }
)
public class Friend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
    @ManyToOne
    @JoinColumn(name = "friend_user_id",nullable = false)
    private User friendUser;
    @Column(nullable = false)
    private String Status;
}
