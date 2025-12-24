package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDto {
    private Long leaderboardId;
    private User user;
    private Integer rank;
}
