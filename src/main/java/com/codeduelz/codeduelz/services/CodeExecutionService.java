package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.dtos.CodeExecutionResultDto;
import com.codeduelz.codeduelz.dtos.TestCaseResultDto;
import com.codeduelz.codeduelz.entities.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class CodeExecutionService {

    // Using JDoodle API for code execution
    @Value("${jdoodle.api.url}")
    private String jdoodleApiUrl;

    @Value("${jdoodle.client.id}")
    private String clientId;

    @Value("${jdoodle.client.secret}")
    private String clientSecret;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Map frontend language keys to JDoodle language names and version indices
    private static final Map<String, String[]> LANGUAGE_MAP = Map.of(
            "cpp", new String[] { "cpp17", "0" },
            "python", new String[] { "python3", "4" },
            "java", new String[] { "java", "4" },
            "javascript", new String[] { "nodejs", "4" });

    /**
     * Execute code with the given stdin input using JDoodle API.
     * Returns a map with keys: stdout, stderr, exitCode, error
     */
    public Map<String, Object> executeCode(String sourceCode, String language, String stdin) {
        return executeCode(sourceCode, language, stdin, null, null);
    }

    /**
     * Execute code with methodName and testInput for smart Java wrapping.
     */
    public Map<String, Object> executeCode(String sourceCode, String language, String stdin,
            String methodName, String testInput) {
        try {
            // Auto-wrap Java code in main method if needed
            if ("java".equalsIgnoreCase(language)) {
                String originalCode = sourceCode;
                sourceCode = wrapJavaCodeIfNeeded(sourceCode, methodName, testInput);
                System.out.println("[DEBUG] Original code: " + originalCode);
                System.out.println("[DEBUG] Wrapped code: " + sourceCode);
            }
            String[] langInfo = LANGUAGE_MAP.getOrDefault(language, LANGUAGE_MAP.get("cpp"));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("clientId", clientId);
            requestBody.put("clientSecret", clientSecret);
            requestBody.put("script", sourceCode);
            requestBody.put("language", langInfo[0]);
            requestBody.put("versionIndex", langInfo[1]);
            if (stdin != null && !stdin.isEmpty()) {
                requestBody.put("stdin", stdin);
            }

            String json = objectMapper.writeValueAsString(requestBody);
            System.out.println("[DEBUG] JDoodle request: " + json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jdoodleApiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[DEBUG] JDoodle response status: " + response.statusCode());
            System.out.println("[DEBUG] JDoodle response body: " + response.body());

            if (response.statusCode() != 200) {
                return Map.of("error", "JDoodle API error: HTTP " + response.statusCode(),
                        "stdout", "", "stderr", "", "exitCode", -1);
            }

            JsonNode root = objectMapper.readTree(response.body());

            // JDoodle response format:
            // { "output": "...", "statusCode": 200, "memory": "...", "cpuTime": "..." }
            // For compilation errors, output contains the error message
            String output = root.has("output") ? root.get("output").asText() : "";
            int statusCode = root.has("statusCode") ? root.get("statusCode").asInt() : -1;

            // Check for compilation or runtime errors
            // JDoodle returns statusCode 200 for successful execution
            // Non-zero statusCode or error messages in output indicate failures
            boolean hasError = statusCode != 200;
            boolean isCompilationError = false;
            String stderr = "";
            String stdout = output;

            // Detect compilation errors by checking for common error patterns
            if (hasError || output.contains("error:") || output.contains("Error:") ||
                    output.contains("Exception") || output.contains("SyntaxError") ||
                    output.contains("compilation terminated")) {

                // Check if it's a compilation error vs runtime error
                if (output.contains("error:") && !output.contains("runtime error") ||
                        output.contains("compilation terminated") ||
                        output.contains("SyntaxError") ||
                        (language.equals("java") && output.contains("error:"))) {
                    isCompilationError = true;
                    stderr = output;
                    stdout = "";
                } else if (hasError) {
                    // Runtime error
                    stderr = output;
                    stdout = "";
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("stdout", stdout);
            result.put("stderr", stderr);
            result.put("exitCode", hasError ? 1 : 0);
            result.put("timedOut", false); // JDoodle handles timeouts internally
            result.put("compilationError", isCompilationError);
            System.out.println("[DEBUG] Execution result - stdout: '" + stdout + "', stderr: '" + stderr
                    + "', exitCode: " + (hasError ? 1 : 0));
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
    public CodeExecutionResultDto evaluateAgainstTestCases(String sourceCode, String language,
            List<TestCase> testCases) {
        return evaluateAgainstTestCases(sourceCode, language, testCases, null);
    }

    /**
     * Evaluate code against a list of test cases with smart Java wrapping.
     */
    public CodeExecutionResultDto evaluateAgainstTestCases(String sourceCode, String language,
            List<TestCase> testCases, String methodName) {
        List<TestCaseResultDto> results = new ArrayList<>();
        int passed = 0;
        String overallStatus = "ACCEPTED";
        String compilationError = null;

        for (TestCase tc : testCases) {
            // For Java with methodName, pass testInput for smart wrapping
            Map<String, Object> execResult;
            if ("java".equalsIgnoreCase(language) && methodName != null && !methodName.isEmpty()) {
                execResult = executeCode(sourceCode, language, tc.getInput(), methodName, tc.getInput());
            } else {
                execResult = executeCode(sourceCode, language, tc.getInput());
            }

            // Check for compilation error
            if (Boolean.TRUE.equals(execResult.get("compilationError"))) {
                compilationError = (String) execResult.get("stderr");
                overallStatus = "COMPILATION_ERROR";
                results.add(new TestCaseResultDto(
                        tc.getInput(), tc.getExpectedOutput(),
                        "Compilation Error: " + compilationError, false));
                // All remaining tests will also fail to compile, so break
                for (int i = results.size(); i < testCases.size(); i++) {
                    TestCase remaining = testCases.get(i);
                    results.add(new TestCaseResultDto(
                            remaining.getInput(), remaining.getExpectedOutput(),
                            "Compilation Error", false));
                }
                break;
            }

            // Check for timeout
            if (Boolean.TRUE.equals(execResult.get("timedOut"))) {
                overallStatus = "TIME_LIMIT_EXCEEDED";
                results.add(new TestCaseResultDto(
                        tc.getInput(), tc.getExpectedOutput(),
                        "Time Limit Exceeded", false));
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
                        "Runtime Error: " + stderr, false));
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
                    tc.getInput(), tc.getExpectedOutput(), actualOutput, isCorrect));
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
        if (output == null)
            return "";
        return output.replaceAll("\\r\\n", "\n").replaceAll("\\s+$", "").trim();
    }

    /**
     * Automatically wrap Java code in a Main class with main method if it doesn't
     * already have one.
     * This allows users to write code without boilerplate.
     * 
     * Examples:
     * - Input: "System.out.println(\"Hello\");"
     * Output: Full class with main method
     * - Input: "public static void main(String[] args) { ... }"
     * Output: Wrapped in class (if not already)
     * - Input: "public class Solution { ... }"
     * Output: Unchanged (already has class)
     */
    private String wrapJavaCodeIfNeeded(String code) {
        return wrapJavaCodeIfNeeded(code, null, null);
    }

    private String wrapJavaCodeIfNeeded(String code, String methodName, String testInput) {
        String trimmedCode = code.trim();

        // Check if code already has a class definition
        if (trimmedCode.matches("(?s).*\\b(class|interface|enum)\\s+\\w+.*")) {
            // Code already has a class/interface/enum definition
            // Check if it has a main method
            if (!trimmedCode.matches("(?s).*public\\s+static\\s+void\\s+main\\s*\\(.*")) {
                // Has class but no main method - generate smart wrapper
                if (methodName != null && !methodName.isEmpty() && testInput != null) {
                    // Smart wrapping: instantiate Solution and call the method
                    return generateSmartWrapper(trimmedCode, methodName, testInput);
                } else {
                    // Fallback: basic wrapping (old behavior)
                    return "public class Main {\n" +
                            "    public static void main(String[] args) {\n" +
                            "        " + trimmedCode + "\n" +
                            "    }\n" +
                            "}";
                }
            }
            // Has both class and main method
            return trimmedCode;
        }

        // Check if code has main method but no class wrapper
        if (trimmedCode.matches("(?s).*public\\s+static\\s+void\\s+main\\s*\\(.*")) {
            // Has main method but no class - wrap in Main class
            return "public class Main {\n" +
                    "    " + trimmedCode + "\n" +
                    "}";
        }

        // Code is just statements/expressions - wrap in Main class with main method
        return "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        " + trimmedCode + "\n" +
                "    }\n" +
                "}";
    }

    /**
     * Generate smart wrapper for LeetCode-style Solution classes.
     * Creates a Main class that instantiates Solution and calls the method with
     * test input.
     */
    private String generateSmartWrapper(String solutionCode, String methodName, String testInput) {
        // Extract class name from the solution code (usually "Solution")
        String className = "Solution";

        // Parse and convert test input to valid Java code
        String javaInput = parseTestInput(testInput);

        // Build the wrapper code
        StringBuilder wrapper = new StringBuilder();
        wrapper.append("import java.util.*;\n\n");
        wrapper.append(solutionCode).append("\n\n");
        wrapper.append("public class Main {\n");
        wrapper.append("    public static void main(String[] args) {\n");
        wrapper.append("        ").append(className).append(" sol = new ").append(className).append("();\n");
        wrapper.append("        \n");
        wrapper.append("        // Test case input\n");
        wrapper.append("        ").append(javaInput).append("\n");
        wrapper.append("        \n");
        wrapper.append("        Object result = sol.").append(methodName).append("(input);\n");
        wrapper.append("        System.out.print(result);\n");
        wrapper.append("    }\n");
        wrapper.append("}\n");

        return wrapper.toString();
    }

    /**
     * Parse test input and convert to valid Java code.
     */
    private String parseTestInput(String testInput) {
        if (testInput == null || testInput.trim().isEmpty()) {
            return "int input = 0;";
        }

        String trimmed = testInput.trim();

        // If it already looks like valid Java code, return as-is
        if (trimmed.matches(".*\\b(int|long|double|float|String|boolean|char)\\b.*")) {
            return trimmed.endsWith(";") ? trimmed : trimmed + ";";
        }

        // Parse "varName = [...]" format (array)
        if (trimmed.matches("\\w+\\s*=\\s*\\[.*\\]")) {
            String arrayContent = trimmed.substring(trimmed.indexOf('[') + 1, trimmed.lastIndexOf(']'));
            return "int[] input = {" + arrayContent + "};";
        }

        // Parse "[...]" format (array without variable name)
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String arrayContent = trimmed.substring(1, trimmed.length() - 1);
            return "int[] input = {" + arrayContent + "};";
        }

        // Parse quoted string
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return "String input = " + trimmed + ";";
        }

        // Parse plain number
        if (trimmed.matches("-?\\d+")) {
            return "int input = " + trimmed + ";";
        }

        // Parse decimal number
        if (trimmed.matches("-?\\d+\\.\\d+")) {
            return "double input = " + trimmed + ";";
        }

        // Fallback: treat as string
        return "String input = \"" + trimmed + "\";";
    }
}
