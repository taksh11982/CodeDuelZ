package com.codeduelz.codeduelz.controller;

import com.codeduelz.codeduelz.entities.Difficulty;
import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.services.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RequestMapping("/problems")
@RestController
public class ProblemController {
    @Autowired
    private ProblemService problemService;
    @GetMapping("/random")
    public Problem getRandomProblem(@RequestParam Difficulty difficulty) {
        return problemService.getRandomProblem(difficulty);
    }

}
