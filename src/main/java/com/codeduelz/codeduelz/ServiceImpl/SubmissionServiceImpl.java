package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.SubmitCodeDto;
import com.codeduelz.codeduelz.entities.Match;
import com.codeduelz.codeduelz.entities.Submission;
import com.codeduelz.codeduelz.entities.SubmissionStatus;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.MatchRepo;
import com.codeduelz.codeduelz.repo.SubmissionRepo;
import com.codeduelz.codeduelz.services.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubmissionServiceImpl implements SubmissionService {
    @Autowired
    private SubmissionRepo submissionRepo;
    @Autowired
    private MatchRepo matchRepo;
    @Override
    public void submitCode(User user, SubmitCodeDto dto) {
        Match match = matchRepo.findById(dto.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Submission submission = new Submission();
        submission.setUser(user);
        submission.setMatch(match);
        submission.setProblem(match.getProblem()); // 👈 important
        submission.setCode(dto.getCode());
        submission.setLanguage(dto.getLanguage());
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(LocalDateTime.now());

        submissionRepo.save(submission);
    }
//    public Submission submit(Submission submission) {
//        submission.setSubmittedAt(LocalDateTime.now());
//        submission.setStatus("PENDING");
//        return submissionRepository.save(submission);
//    }

}
