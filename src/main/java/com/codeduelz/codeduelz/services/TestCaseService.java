package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.entities.TestCase;
import com.codeduelz.codeduelz.repo.TestCaseRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestCaseService {
    @Autowired
    private TestCaseRepo testCaseRepository;
    public TestCase addTestCase(TestCase testCase) {
        return testCaseRepository.save(testCase);
    }

    public List<TestCase> getTestCasesByProblem(Problem problem) {
        return testCaseRepository.findByProblem(problem);
    }
}
