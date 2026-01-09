package com.codeduelz.codeduelz.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class CreateMatchDto {
    private Long opponentUserId;
    private Long problemId;
}
