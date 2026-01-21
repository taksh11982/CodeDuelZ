package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Difficulty;
import com.codeduelz.codeduelz.entities.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemRepo extends JpaRepository<Problem,Long> {
    List<Problem> findByDifficulty(Difficulty difficulty);
}
