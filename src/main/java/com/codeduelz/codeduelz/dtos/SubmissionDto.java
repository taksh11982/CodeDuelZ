package com.codeduelz.codeduelz.dtos;

import com.codeduelz.codeduelz.entities.Match;
import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDto {
    private Long submissionId;
    private User user;
    private Problem problem;
    private Match match;
    private String code;
    private String language;
    private String status;
    private LocalDateTime submittedAt;
}
