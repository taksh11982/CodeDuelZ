package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.Problem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDto {
    private Long testcaseId;
    private Problem problem;
    private String input;
    private String expectedOutput;
}
