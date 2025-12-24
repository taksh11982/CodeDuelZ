package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Submission;

public interface SubmissionService {
    public Submission submit(Submission submission);
    public void updateStatus(Submission submission, String status);
}
