package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.CodeExecutionResultDto;
import com.codeduelz.codeduelz.dtos.TestCaseResultDto;
import com.codeduelz.codeduelz.entities.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class CodeExecutionService {

    private static final String PISTON_URL = "https://emkc.org/api/v2/piston/execute";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Map frontend language keys to Piston language names and versions
    private static final Map<String, String[]> LANGUAGE_MAP = Map.of(
            "cpp",        new String[]{"c++",        "10.2.0"},
            "python",     new String[]{"python",     "3.10.0"},
            "java",       new String[]{"java",       "15.0.2"},
            "javascript", new String[]{"javascript", "18.15.0"}
    );

    /**
     * Execute code with the given stdin input using Piston API.
     * Returns a map with keys: stdout, stderr, exitCode, error
     */
    public Map<String, Object> executeCode(String sourceCode, String language, String stdin) {
        try {
            String[] langInfo = LANGUAGE_MAP.getOrDefault(language, LANGUAGE_MAP.get("cpp"));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("language", langInfo[0]);
            requestBody.put("version", langInfo[1]);
            requestBody.put("files", List.of(Map.of("content", sourceCode)));
            if (stdin != null && !stdin.isEmpty()) {
                requestBody.put("stdin", stdin);
            }
            // Timeouts: 10s compile, 5s run
            requestBody.put("compile_timeout", 10000);
            requestBody.put("run_timeout", 5000);

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PISTON_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return Map.of("error", "Piston API error: HTTP " + response.statusCode(),
                              "stdout", "", "stderr", "", "exitCode", -1);
            }

            JsonNode root = objectMapper.readTree(response.body());

            // Check for compile stage errors
            JsonNode compile = root.get("compile");
            if (compile != null && compile.has("code") && compile.get("code").asInt() != 0) {
                String compileStderr = compile.has("stderr") ? compile.get("stderr").asText() : "";
                String compileOutput = compile.has("output") ? compile.get("output").asText() : "";
                return Map.of(
                    "stdout", "",
                    "stderr", compileStderr.isEmpty() ? compileOutput : compileStderr,
                    "exitCode", compile.get("code").asInt(),
                    "compilationError", true
                );
            }

            // Get run stage results
            JsonNode run = root.get("run");
            String stdout = run != null && run.has("stdout") ? run.get("stdout").asText() : "";
            String stderr = run != null && run.has("stderr") ? run.get("stderr").asText() : "";
            int exitCode = run != null && run.has("code") ? run.get("code").asInt() : -1;

            // Check if it was killed (timeout)
            boolean timedOut = run != null && run.has("signal") &&
                    "SIGKILL".equals(run.get("signal").asText());

            Map<String, Object> result = new HashMap<>();
            result.put("stdout", stdout);
            result.put("stderr", stderr);
            result.put("exitCode", exitCode);
            result.put("timedOut", timedOut);
            result.put("compilationError", false);
            return result;

        } catch (Exception e) {
            return Map.of("error", "Execution failed: " + e.getMessage(),
                          "stdout", "", "stderr", e.getMessage(), "exitCode", -1);
        }
    }

    /**
     * Evaluate code against a list of test cases.
     * Runs the code once per test case, comparing stdout to expected output.
     */
    public CodeExecutionResultDto evaluateAgainstTestCases(String sourceCode, String language, List<TestCase> testCases) {
        List<TestCaseResultDto> results = new ArrayList<>();
        int passed = 0;
        String overallStatus = "ACCEPTED";
        String compilationError = null;

        for (TestCase tc : testCases) {
            Map<String, Object> execResult = executeCode(sourceCode, language, tc.getInput());

            // Check for compilation error
            if (Boolean.TRUE.equals(execResult.get("compilationError"))) {
                compilationError = (String) execResult.get("stderr");
                overallStatus = "COMPILATION_ERROR";
                results.add(new TestCaseResultDto(
                    tc.getInput(), tc.getExpectedOutput(),
                    "Compilation Error: " + compilationError, false
                ));
                // All remaining tests will also fail to compile, so break
                for (int i = results.size(); i < testCases.size(); i++) {
                    TestCase remaining = testCases.get(i);
                    results.add(new TestCaseResultDto(
                        remaining.getInput(), remaining.getExpectedOutput(),
                        "Compilation Error", false
                    ));
                }
                break;
            }

            // Check for timeout
            if (Boolean.TRUE.equals(execResult.get("timedOut"))) {
                overallStatus = "TIME_LIMIT_EXCEEDED";
                results.add(new TestCaseResultDto(
                    tc.getInput(), tc.getExpectedOutput(),
                    "Time Limit Exceeded", false
                ));
                continue;
            }

            // Check for runtime error
            String stderr = (String) execResult.get("stderr");
            int exitCode = (int) execResult.get("exitCode");
            if (exitCode != 0 && !stderr.isEmpty()) {
                if (!"WRONG_ANSWER".equals(overallStatus)) {
                    overallStatus = "RUNTIME_ERROR";
                }
                results.add(new TestCaseResultDto(
                    tc.getInput(), tc.getExpectedOutput(),
                    "Runtime Error: " + stderr, false
                ));
                continue;
            }

            // Compare output
            String actualOutput = ((String) execResult.get("stdout")).trim();
            String expectedOutput = tc.getExpectedOutput().trim();
            boolean isCorrect = normalizeOutput(actualOutput).equals(normalizeOutput(expectedOutput));

            if (isCorrect) {
                passed++;
            } else {
                overallStatus = "WRONG_ANSWER";
            }

            results.add(new TestCaseResultDto(
                tc.getInput(), tc.getExpectedOutput(), actualOutput, isCorrect
            ));
        }

        if (passed == testCases.size() && !testCases.isEmpty()) {
            overallStatus = "ACCEPTED";
        }

        return new CodeExecutionResultDto(overallStatus, results, compilationError, passed, testCases.size());
    }

    /**
     * Normalize output for comparison: trim whitespace, normalize line endings
     */
    private String normalizeOutput(String output) {
        if (output == null) return "";
        return output.replaceAll("\\r\\n", "\n").replaceAll("\\s+$", "").trim();
    }
}
