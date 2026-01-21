package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Difficulty;
import com.codeduelz.codeduelz.entities.Problem;

import java.util.List;

public interface ProblemService {
    public Problem createProblem(Problem problem);
    public List<Problem> getProblemsByDifficulty(Difficulty difficulty);
    public Problem getProblemById(Long id);
    public Problem getRandomProblem(Difficulty difficulty);
}
