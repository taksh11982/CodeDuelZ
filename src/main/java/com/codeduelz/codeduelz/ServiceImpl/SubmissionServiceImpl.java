package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.entities.Submission;
import com.codeduelz.codeduelz.repo.SubmissionRepo;
import com.codeduelz.codeduelz.services.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubmissionServiceImpl implements SubmissionService {
    @Autowired
    private SubmissionRepo submissionRepository;
    public Submission submit(Submission submission) {
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus("PENDING");
        return submissionRepository.save(submission);
    }
    public void updateStatus(Submission submission, String status) {
        submission.setStatus(status);
        submissionRepository.save(submission);
    }
}
