package com.codeduelz.codeduelz.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitCodeDto {
    private Long matchId;
    private String code;
    private String language;
}

