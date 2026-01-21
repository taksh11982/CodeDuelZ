package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.dtos.SubmitCodeDto;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.services.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {
    @Autowired
    private SubmissionService submissionService;
    @PostMapping("/submissions/submit")
    public void submitCode(
            @AuthenticationPrincipal User user,
            @RequestBody SubmitCodeDto dto) {

        submissionService.submitCode(user, dto);
    }

}
