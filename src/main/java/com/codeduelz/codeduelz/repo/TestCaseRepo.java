package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.entities.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepo extends JpaRepository<TestCase,Long> {
    List<TestCase> findByProblem(Problem problem);
}
