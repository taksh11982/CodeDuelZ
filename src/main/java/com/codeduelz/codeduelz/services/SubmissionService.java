package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.SubmitCodeDto;
import com.codeduelz.codeduelz.entities.Submission;
import com.codeduelz.codeduelz.entities.User;

public interface SubmissionService {
    public void submitCode(User user, SubmitCodeDto dto);
}
