package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.Difficulty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchProblemDto {
    private Long problemId;
    private String title;
    private String description;
    private Difficulty difficulty;
}
