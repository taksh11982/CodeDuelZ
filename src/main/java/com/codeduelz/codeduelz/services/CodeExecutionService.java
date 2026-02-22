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
            // Auto-wrap code in main method/function if needed
            if ("java".equalsIgnoreCase(language)) {
                String originalCode = sourceCode;
                sourceCode = wrapJavaCodeIfNeeded(sourceCode, methodName, testInput);
                System.out.println("[DEBUG] Original code: " + originalCode);
                System.out.println("[DEBUG] Wrapped code: " + sourceCode);
            } else if ("cpp".equalsIgnoreCase(language) && methodName != null && !methodName.isEmpty()
                    && testInput != null) {
                String originalCode = sourceCode;
                sourceCode = wrapCppCodeIfNeeded(sourceCode, methodName, testInput);
                System.out.println("[DEBUG] Original C++ code: " + originalCode);
                System.out.println("[DEBUG] Wrapped C++ code: " + sourceCode);
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
            // For Java and C++ with methodName, pass testInput for smart wrapping
            Map<String, Object> execResult;
            boolean needsWrapping = methodName != null && !methodName.isEmpty();
            if (needsWrapping && ("java".equalsIgnoreCase(language) || "cpp".equalsIgnoreCase(language))) {
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
     * Normalize output for comparison: trim whitespace, normalize line endings,
     * and strip spaces after commas/inside brackets so [1, 2, 3] == [1,2,3].
     */
    private String normalizeOutput(String output) {
        if (output == null)
            return "";
        return output
                .replaceAll("\\r\\n", "\n") // normalize line endings
                .replaceAll(",\\s+", ",") // "1, 2, 3" -> "1,2,3"
                .replaceAll("\\[\\s+", "[") // "[ 1" -> "[1"
                .replaceAll("\\s+\\]", "]") // "1 ]" -> "1]"
                .replaceAll("\\s+$", "")
                .trim();
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
     * Wrap C++ Solution class with a main() function if it doesn't have one.
     * Always prepends standard headers so 'string', 'vector' etc. are available.
     */
    private String wrapCppCodeIfNeeded(String code, String methodName, String testInput) {
        String trimmed = code.trim();

        // Always prepend standard headers if not already present
        String headers = "";
        if (!trimmed.contains("#include")) {
            headers = "#include <bits/stdc++.h>\nusing namespace std;\n\n";
        }

        // If it already has a main function, just add headers and return
        if (trimmed.contains("int main(") || trimmed.contains("int main (")) {
            return headers + trimmed;
        }

        return generateCppWrapper(trimmed, methodName, testInput);
    }

    /**
     * Generate a C++ main() that instantiates Solution, feeds test input, and
     * prints result.
     */
    private String generateCppWrapper(String solutionCode, String methodName, String testInput) {
        String[] parsed = parseTestInputMulti(testInput);
        String declarations = parsed[0];
        String argList = parsed[1];

        // Convert Java-style declarations to C++ style:
        // int arg0 = 31; -> same (valid C++)
        // int[] arg0 = ... -> int arg0[] = ...
        // int[][] arg0 = ... -> (handled as vector, but flatten for simple cases)
        String cppDecls = declarations
                .replace("int[] ", "int[] ") // keep, handled below
                .replace("int[][] ", "int[][] "); // keep, handled below
        // Replace Java array syntax with C++ array syntax
        cppDecls = cppDecls.replaceAll("int\\[\\]\\[\\] (\\w+) = \\{(.*?)\\};",
                "// 2D array input not fully auto-typed in C++ — pass manually");
        cppDecls = cppDecls.replaceAll("int\\[\\] (\\w+) = \\{(.*?)\\};",
                "vector<int> $1 = {$2};");
        // Remove semicolons on blank/comment lines
        cppDecls = cppDecls.replace("String ", "string ");

        StringBuilder sb = new StringBuilder();
        sb.append("#include <bits/stdc++.h>\n");
        sb.append("using namespace std;\n\n");
        sb.append(solutionCode).append("\n\n");
        sb.append("int main() {\n");
        sb.append("    Solution sol;\n");
        for (String decl : cppDecls.split("\n")) {
            sb.append("    ").append(decl).append("\n");
        }
        sb.append("    auto result = sol.").append(methodName).append("(").append(argList).append(");\n");
        // Print result — for vectors use a loop, for primitives use cout
        sb.append("    cout << result << endl;\n");
        sb.append("    return 0;\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Generate smart wrapper for LeetCode-style Solution classes.
     * Creates a Main class that instantiates Solution and calls the method with
     * test input.
     * Supports multi-parameter methods by splitting multi-line test inputs.
     */
    private String generateSmartWrapper(String solutionCode, String methodName, String testInput) {
        String className = "Solution";

        // Split test input lines (one per parameter)
        String[] inputLines = (testInput == null || testInput.trim().isEmpty())
                ? new String[0]
                : testInput.trim().split("\\r?\\n");

        // Extract actual parameter types from the solution method signature
        List<String> paramTypes = extractParamTypes(solutionCode, methodName);

        // Build declarations and argument list using real types
        StringBuilder decls = new StringBuilder();
        StringBuilder argList = new StringBuilder();
        for (int i = 0; i < inputLines.length; i++) {
            String argName = "arg" + i;
            String rawValue = inputLines[i].trim();
            String type = (i < paramTypes.size()) ? paramTypes.get(i) : null;
            String decl = buildTypedDeclaration(type, rawValue, argName);
            if (i > 0) {
                decls.append("\n");
                argList.append(", ");
            }
            decls.append(decl);
            argList.append(argName);
        }

        StringBuilder wrapper = new StringBuilder();
        wrapper.append("import java.util.*;\n");
        wrapper.append("import java.util.Arrays;\n\n");
        wrapper.append(solutionCode).append("\n\n");
        wrapper.append("public class Main {\n");
        // Helper: prints 1D/2D arrays and Lists properly
        wrapper.append("    static void printResult(Object r) {\n");
        wrapper.append("        if (r instanceof int[][]) {\n");
        wrapper.append("            System.out.print(Arrays.deepToString((int[][]) r));\n");
        wrapper.append("        } else if (r instanceof int[]) {\n");
        wrapper.append("            System.out.print(Arrays.toString((int[]) r));\n");
        wrapper.append("        } else if (r instanceof String[]) {\n");
        wrapper.append("            System.out.print(Arrays.toString((String[]) r));\n");
        wrapper.append("        } else {\n");
        wrapper.append("            System.out.print(r);\n");
        wrapper.append("        }\n");
        wrapper.append("    }\n");
        wrapper.append("    public static void main(String[] args) {\n");
        wrapper.append("        ").append(className).append(" sol = new ").append(className).append("();\n");
        wrapper.append("        \n");
        wrapper.append("        // Test case input\n");
        if (decls.length() > 0) {
            for (String decl : decls.toString().split("\n")) {
                wrapper.append("        ").append(decl).append("\n");
            }
        }
        wrapper.append("        \n");
        wrapper.append("        Object result = sol.").append(methodName).append("(").append(argList).append(");\n");
        wrapper.append("        printResult(result);\n");
        wrapper.append("    }\n");
        wrapper.append("}\n");

        return wrapper.toString();
    }

    /**
     * Extract ordered parameter types from the method signature inside
     * solutionCode.
     * Handles generics like List<Integer>, Map<Integer,Integer> correctly.
     * Returns empty list if the method signature can't be found.
     */
    private List<String> extractParamTypes(String solutionCode, String methodName) {
        List<String> types = new ArrayList<>();
        // Match: <anything> methodName(<params>)
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "[\\w<>\\[\\],\\s]+?\\s+" + java.util.regex.Pattern.quote(methodName) + "\\s*\\(([^)]*?)\\)",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(solutionCode);
        if (!m.find())
            return types;

        String paramStr = m.group(1).trim();
        if (paramStr.isEmpty())
            return types;

        // Split parameters by comma, but ignore commas inside angle brackets (generics)
        List<String> params = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char ch : paramStr.toCharArray()) {
            if (ch == '<')
                depth++;
            else if (ch == '>')
                depth--;
            else if (ch == ',' && depth == 0) {
                params.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        if (current.length() > 0)
            params.add(current.toString().trim());

        // Each param is like "List<Integer> nums" or "int[] arr" — extract the type
        // part
        for (String param : params) {
            // Last token is the variable name; everything before is the type
            String[] parts = param.trim().split("\\s+(?=[\\w]+$)");
            if (parts.length >= 2) {
                types.add(parts[0].trim()); // e.g. "List<Integer>" or "int[]"
            } else if (parts.length == 1) {
                types.add(parts[0].trim()); // fallback
            }
        }
        return types;
    }

    /**
     * Build a Java variable declaration for argName using the known type and raw
     * value.
     * Falls back to parseSingleValue() when type is null or unknown.
     */
    private String buildTypedDeclaration(String type, String rawValue, String argName) {
        if (type == null || type.isEmpty()) {
            return parseSingleValue(rawValue, argName);
        }

        String t = type.trim();

        // Strip LeetCode-style "varname = " prefix if present (e.g. "nums = [2,3,5,7]"
        // -> "[2,3,5,7]")
        String valueOnly = rawValue.trim();
        if (valueOnly.matches("\\w+\\s*=\\s*\\[.*")) {
            valueOnly = valueOnly.substring(valueOnly.indexOf('[')).trim();
        } else if (valueOnly.matches("\\w+\\s*=\\s*.*")) {
            valueOnly = valueOnly.substring(valueOnly.indexOf('=') + 1).trim();
        }

        // Strip outer [ ] to get just the CSV content (for non-2D arrays)
        String inner = valueOnly;
        if (inner.startsWith("[") && inner.endsWith("]") && !inner.startsWith("[[")) {
            inner = inner.substring(1, inner.length() - 1);
        }

        // List<Integer> / List<Long> / List<String>
        if (t.startsWith("List<")) {
            String genericType = t.substring(5, t.length() - 1); // e.g. "Integer"
            // Build: Arrays.asList(1,2,3) works for List<Integer>
            return "List<" + genericType + "> " + argName + " = new ArrayList<>(Arrays.asList("
                    + wrapElements(inner, genericType) + "));";
        }

        // int[]
        if (t.equals("int[]")) {
            // Handle 2D input [[1,2],[3,4]]
            if (valueOnly.startsWith("[[")) {
                String i2 = valueOnly.substring(1, valueOnly.length() - 1)
                        .replaceAll("\\[", "{").replaceAll("\\]", "}");
                return "int[][] " + argName + " = {" + i2 + "};";
            }
            return "int[] " + argName + " = {" + inner + "};";
        }

        // int[][]
        if (t.equals("int[][]")) {
            String i2 = valueOnly.substring(1, valueOnly.length() - 1)
                    .replaceAll("\\[", "{").replaceAll("\\]", "}");
            return "int[][] " + argName + " = {" + i2 + "};";
        }

        // String (unquoted in test input)
        if (t.equals("String")) {
            String v = valueOnly.startsWith("\"") ? valueOnly : "\"" + valueOnly + "\"";
            return "String " + argName + " = " + v + ";";
        }

        // Primitive types: int, long, double, boolean, char
        if (t.matches("int|long|double|float|boolean|char")) {
            return t + " " + argName + " = " + valueOnly + ";";
        }

        // Fallback: parseSingleValue infers type from value
        return parseSingleValue(valueOnly, argName);
    }

    /**
     * Wrap comma-separated elements for Arrays.asList().
     * For Integer/Long, values are kept as-is.
     * For String, wrap each element in quotes if not already quoted.
     */
    private String wrapElements(String csv, String genericType) {
        if (csv.trim().isEmpty())
            return "";
        if (!genericType.equals("String"))
            return csv; // Integers/Longs work as-is
        // Wrap each token in quotes if not already
        String[] tokens = csv.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String tok = tokens[i].trim();
            if (!tok.startsWith("\""))
                tok = "\"" + tok + "\"";
            if (i > 0)
                sb.append(", ");
            sb.append(tok);
        }
        return sb.toString();
    }

    /**
     * Parse test input (single or multi-line) into Java variable declarations and
     * an argument list.
     *
     * Returns String[2]:
     * [0] = newline-separated variable declarations (e.g. "int arg0 = 31;\nint arg1
     * = 8;")
     * [1] = comma-separated argument names (e.g. "arg0, arg1")
     *
     * Each line of testInput is treated as one argument.
     * Supports: plain ints, decimals, quoted strings, arrays ([1,2,3]), and plain
     * text.
     */
    private String[] parseTestInputMulti(String testInput) {
        if (testInput == null || testInput.trim().isEmpty()) {
            return new String[] { "int arg0 = 0;", "arg0" };
        }

        // Split on newlines — each line is one parameter
        String[] lines = testInput.trim().split("\\r?\\n");

        // Single-line shortcut keeps behaviour identical to before
        if (lines.length == 1) {
            String decl = parseSingleValue(lines[0].trim(), "arg0");
            return new String[] { decl, "arg0" };
        }

        // Multi-parameter: parse each line independently
        StringBuilder decls = new StringBuilder();
        StringBuilder argList = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String argName = "arg" + i;
            String decl = parseSingleValue(lines[i].trim(), argName);
            if (i > 0) {
                decls.append("\n");
                argList.append(", ");
            }
            decls.append(decl);
            argList.append(argName);
        }
        return new String[] { decls.toString(), argList.toString() };
    }

    /**
     * Parse a single value token into a typed Java variable declaration.
     * Supports: plain int, decimal, quoted string, array literal [1,2,3], plain
     * text.
     */
    private String parseSingleValue(String token, String varName) {
        if (token == null || token.isEmpty()) {
            return "int " + varName + " = 0;";
        }

        // Already valid Java type declaration — return as-is (edge case)
        if (token.matches(".*\\b(int|long|double|float|String|boolean|char)\\b.*")) {
            return token.endsWith(";") ? token : token + ";";
        }

        // 2D Array: "[[1,2],[3,4]]" — must be checked BEFORE 1D array
        if (token.startsWith("[[") && token.endsWith("]]")) {
            // Convert [[1,2],[3,4]] → {{1,2},{3,4}}
            String inner = token.substring(1, token.length() - 1); // [1,2],[3,4]
            // Replace each inner [...] with {...}
            inner = inner.replaceAll("\\[", "{").replaceAll("\\]", "}");
            return "int[][] " + varName + " = {" + inner + "};";
        }

        // 1D Array: "[1,2,3]" or "nums = [1,2,3]"
        String arrayContent = null;
        if (token.matches("\\w+\\s*=\\s*\\[.*\\]")) {
            arrayContent = token.substring(token.indexOf('[') + 1, token.lastIndexOf(']'));
        } else if (token.startsWith("[") && token.endsWith("]")) {
            arrayContent = token.substring(1, token.length() - 1);
        }
        if (arrayContent != null) {
            return "int[] " + varName + " = {" + arrayContent + "};";
        }

        // Quoted string: "hello"
        if (token.startsWith("\"") && token.endsWith("\"")) {
            return "String " + varName + " = " + token + ";";
        }

        // Plain integer
        if (token.matches("-?\\d+")) {
            return "int " + varName + " = " + token + ";";
        }

        // Decimal number
        if (token.matches("-?\\d+\\.\\d+")) {
            return "double " + varName + " = " + token + ";";
        }

        // Boolean
        if (token.equals("true") || token.equals("false")) {
            return "boolean " + varName + " = " + token + ";";
        }

        // Fallback: treat as String literal
        return "String " + varName + " = \"" + token + "\";";
    }
}
