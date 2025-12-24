package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.entities.TestCase;

import java.util.List;

public interface TestCaseService {
    public TestCase addTestCase(TestCase testCase);
    public List<TestCase> getTestCasesByProblem(Problem problem);
}
