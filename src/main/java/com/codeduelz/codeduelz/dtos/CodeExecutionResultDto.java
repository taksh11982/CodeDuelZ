package com.codeduelz.codeduelz.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionResultDto {
    private String status;           // "ACCEPTED", "WRONG_ANSWER", "COMPILATION_ERROR", "RUNTIME_ERROR", "TIME_LIMIT_EXCEEDED"
    private List<TestCaseResultDto> testCaseResults;
    private String compilationError; // null if no compile error
    private int totalPassed;
    private int totalTests;
}
