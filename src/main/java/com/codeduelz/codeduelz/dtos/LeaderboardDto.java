package com.codeduelz.codeduelz.dtos;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDto {
    private Long userId;
    private String userName;
    private Integer rating;
    private Integer rank;
    private String avatar;
}
