package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.entities.Difficulty;
import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.repo.ProblemRepo;
import com.codeduelz.codeduelz.services.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class ProblemServiceImpl implements ProblemService {
    @Autowired
    private ProblemRepo problemRepository;
    public Problem createProblem(Problem problem) {
        return problemRepository.save(problem);
    }

    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    public List<Problem> getProblemsByDifficulty(Difficulty difficulty) {
        return problemRepository.findByDifficulty(difficulty);
    }

    public Problem getProblemById(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
    }

    @Override
    public Problem getRandomProblem(Difficulty difficulty) {
        List<Problem> problems = problemRepository.findByDifficulty(difficulty);

        if (problems.isEmpty()) {
            throw new RuntimeException("No problems found for difficulty: " + difficulty);
        }

        Random random = new Random();
        int index = random.nextInt(problems.size());

        return problems.get(index);
    }
}
