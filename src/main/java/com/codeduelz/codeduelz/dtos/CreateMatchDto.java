package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.Difficulty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMatchDto {
    private Long opponentUserId;
    private Difficulty difficulty;
}
