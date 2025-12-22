package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Match;
import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.entities.Submission;
import com.codeduelz.codeduelz.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepo extends JpaRepository<Submission,Long> {
    List<Submission> findByUser(User user);
    List<Submission> findByProblem(Problem problem);
    List<Submission> findByMatch(Match match);
    Optional<Submission> findTopByUserAndProblemOrderBySubmittedAtDesc(
            User user, Problem problem);
}
