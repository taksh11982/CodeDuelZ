package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Difficulty;
import com.codeduelz.codeduelz.entities.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemRepo extends JpaRepository<Problem,Long> {
    List<Problem> findByDifficulty(Difficulty difficulty);
    Optional<Problem> findByLeetcodeId(String leetcodeId);
}
