package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Difficulty;
import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.repo.ProblemRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LeetCodeProblemService {
    private final ProblemRepo problemRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory index: difficulty -> list of parsed JSON nodes
    private final Map<Difficulty, List<JsonNode>> problemsByDifficulty = new EnumMap<>(Difficulty.class);
    private final Random random = new Random();

    @PostConstruct
    public void init() {
        for (Difficulty d : Difficulty.values()) {
            problemsByDifficulty.put(d, new ArrayList<>());
        }

        File mergedFile = new File("merged_problems.json");
        if (!mergedFile.exists()) {
            System.err.println("WARNING: merged_problems.json not found at: " + mergedFile.getAbsolutePath());
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(mergedFile);
            JsonNode questions = root.get("questions");
            if (questions == null || !questions.isArray()) {
                System.err.println("WARNING: merged_problems.json does not contain a 'questions' array");
                return;
            }

            int loaded = 0;
            for (JsonNode question : questions) {
                String diffStr = question.has("difficulty") ? question.get("difficulty").asText() : "";
                Difficulty difficulty = mapDifficulty(diffStr);
                problemsByDifficulty.get(difficulty).add(question);
                loaded++;
            }

            System.out.println("LeetCodeProblemService: Indexed " + loaded + " problems from merged_problems.json");
            for (Difficulty d : Difficulty.values()) {
                System.out.println("  " + d + ": " + problemsByDifficulty.get(d).size() + " problems");
            }
        } catch (IOException e) {
            System.err.println("Failed to load merged_problems.json: " + e.getMessage());
        }
    }

    /**
     * Get a random LeetCode problem for the given difficulty.
     * Returns the Problem entity (saved to DB) and the raw JSON data
     * packaged together as a Map for the WebSocket payload.
     */
    public Map<String, Object> getRandomProblemData(Difficulty difficulty) {
        List<JsonNode> problems = problemsByDifficulty.get(difficulty);
        if (problems.isEmpty()) {
            problems = problemsByDifficulty.values().stream()
                .flatMap(List::stream)
                .toList();
        }
        if (problems.isEmpty()) {
            throw new RuntimeException("No LeetCode problems available");
        }

        JsonNode node = problems.get(random.nextInt(problems.size()));
        return loadProblemData(node);
    }

    private Map<String, Object> loadProblemData(JsonNode root) {
        String leetcodeId = root.has("problem_id") ? root.get("problem_id").asText() : "";

        // Load or create the Problem entity
        Problem problem = problemRepo.findByLeetcodeId(leetcodeId).orElse(null);
        if (problem == null) {
            problem = new Problem();
            problem.setLeetcodeId(leetcodeId);
            problem.setTitle(root.has("title") ? root.get("title").asText() : "Unknown");
            problem.setProblemSlug(root.has("problem_slug") ? root.get("problem_slug").asText() : "");
            problem.setDifficulty(mapDifficulty(root.has("difficulty") ? root.get("difficulty").asText() : "Medium"));
            problem.setSource("LEETCODE");
            problem.setDescription(root.has("description") ? root.get("description").asText() : "");
            problem = problemRepo.save(problem);
        }

        // Build structured data for WebSocket payload
        List<Map<String, Object>> examples = new ArrayList<>();
        if (root.has("examples") && root.get("examples").isArray()) {
            for (JsonNode ex : root.get("examples")) {
                Map<String, Object> example = new HashMap<>();
                example.put("num", ex.has("example_num") ? ex.get("example_num").asInt() : 0);
                example.put("text", ex.has("example_text") ? ex.get("example_text").asText() : "");
                examples.add(example);
            }
        }

        List<String> constraints = new ArrayList<>();
        if (root.has("constraints") && root.get("constraints").isArray()) {
            for (JsonNode c : root.get("constraints")) {
                constraints.add(c.asText());
            }
        }

        Map<String, String> codeSnippets = new HashMap<>();
        if (root.has("code_snippets") && root.get("code_snippets").isObject()) {
            root.get("code_snippets").fields().forEachRemaining(entry ->
                codeSnippets.put(entry.getKey(), entry.getValue().asText())
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("problem", problem);
        result.put("examples", examples);
        result.put("constraints", constraints);
        result.put("codeSnippets", codeSnippets);
        return result;
    }

    private Difficulty mapDifficulty(String diffStr) {
        if (diffStr == null) return Difficulty.MEDIUM;
        return switch (diffStr.toLowerCase()) {
            case "easy" -> Difficulty.EASY;
            case "hard" -> Difficulty.HARD;
            default -> Difficulty.MEDIUM;
        };
    }
}
