package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Difficulty;
import com.codeduelz.codeduelz.entities.Problem;
import com.codeduelz.codeduelz.repo.ProblemRepo;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CodeforcesService {
    private final ProblemRepo problemRepo;

    public Problem getOrFetchProblem(Integer contestId, String index) {
        // Check DB first
        Optional<Problem> existing = problemRepo.findByContestIdAndProblemIndex(contestId, index);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Fetch from Codeforces
        return fetchAndSaveProblem(contestId, index);
    }

    private Problem fetchAndSaveProblem(Integer contestId, String index) {
        String title = "Problem " + contestId + index;
        Difficulty difficulty = Difficulty.MEDIUM;
        String descriptionHtml = "Could not load description. <a href='https://codeforces.com/problemset/problem/" + contestId + "/" + index + "' target='_blank'>View on Codeforces</a>";
        
        try {
            String url = "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
            // Add User-Agent and Referer to look like a real browser
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Referer", "https://codeforces.com/problemset")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(10000)
                .get();

            Element statement = doc.selectFirst(".problem-statement");
            if (statement == null) statement = doc.selectFirst(".ttypography");
            
            if (statement != null) {
                // Fix images to be absolute URLs
                for (Element img : statement.select("img")) {
                    img.attr("src", img.absUrl("src"));
                }
                descriptionHtml = statement.html();
                
                // Try to get title from the statement itself
                Element titleEl = statement.selectFirst(".title");
                if (titleEl != null) {
                    title = titleEl.text();
                    if (title.matches("^[A-Z]\\.\\s.*")) {
                        title = title.substring(3);
                    }
                }
            } else {
                System.err.println("Could not find problem statement element for " + url);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SCRAPE FAILED: " + e.getMessage());
        }
        
        // Save whatever we got (even if just default failure message)
        // This ensures the match doesn't crash
        Problem problem = new Problem();
        problem.setContestId(contestId);
        problem.setProblemIndex(index);
        problem.setTitle(title);
        problem.setDescription(descriptionHtml);
        problem.setDifficulty(difficulty);
        problem.setSource("CODEFORCES");
        
        try {
            return problemRepo.save(problem);
        } catch (Exception dbEx) {
             // If save fails (maybe unique constraint race condition), try to return existing
             return problemRepo.findByContestIdAndProblemIndex(contestId, index).orElseThrow(() -> new RuntimeException("DB Save Failed and not found"));
        }
    }

}
