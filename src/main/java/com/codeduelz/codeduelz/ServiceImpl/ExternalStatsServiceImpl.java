package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.ExternalStatsDto;
import com.codeduelz.codeduelz.services.ExternalStatsService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class ExternalStatsServiceImpl implements ExternalStatsService {
    private final RestTemplate rest = new RestTemplate();

    @Override
    public ExternalStatsDto getUserStats(String lc, String cf, String cc) {
        ExternalStatsDto stats = new ExternalStatsDto();
        if (lc != null && !lc.isBlank()) stats.setLeetCode(getLeetCode(lc));
        if (cf != null && !cf.isBlank()) stats.setCodeforces(getCodeforces(cf));
        if (cc != null && !cc.isBlank()) stats.setCodeChef(getCodeChef(cc));
        return stats;
    }

    private ExternalStatsDto.LeetCodeStats getLeetCode(String user) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            String q = "{\"query\":\"{matchedUser(username:\\\"" + user + "\\\"){submitStats:submitStatsGlobal{acSubmissionNum{difficulty count}}}}\"}";
            Map res = rest.postForObject("https://leetcode.com/graphql", new HttpEntity<>(q, h), Map.class);
            Map data = (Map) res.get("data");
            if (data.get("matchedUser") == null) return null;
            List<Map> subs = (List<Map>) ((Map) ((Map) data.get("matchedUser")).get("submitStats")).get("acSubmissionNum");
            ExternalStatsDto.LeetCodeStats lc = new ExternalStatsDto.LeetCodeStats();
            for (Map s : subs) {
                int c = (Integer) s.get("count");
                String d = (String) s.get("difficulty");
                if ("All".equals(d)) lc.setTotalSolved(c);
                else if ("Easy".equals(d)) lc.setEasySolved(c);
                else if ("Medium".equals(d)) lc.setMediumSolved(c);
                else if ("Hard".equals(d)) lc.setHardSolved(c);
            }
            return lc;
        } catch (Exception e) { return null; }
    }

    private ExternalStatsDto.CodeforcesStats getCodeforces(String user) {
        try {
            Map res = rest.getForObject("https://codeforces.com/api/user.info?handles=" + user, Map.class);
            if (!"OK".equals(res.get("status"))) return null;
            Map u = ((List<Map>) res.get("result")).get(0);
            ExternalStatsDto.CodeforcesStats cf = new ExternalStatsDto.CodeforcesStats();
            cf.setRating((Integer) u.get("rating"));
            cf.setMaxRating((Integer) u.get("maxRating"));
            cf.setRank((String) u.get("rank"));
            return cf;
        } catch (Exception e) { return null; }
    }

    private ExternalStatsDto.CodeChefStats getCodeChef(String user) {
        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect("https://www.codechef.com/users/" + user).get();
            ExternalStatsDto.CodeChefStats cc = new ExternalStatsDto.CodeChefStats();
            try { cc.setCurrentRating(Integer.parseInt(doc.select(".rating-number").text())); } catch (Exception e) {}
            try { cc.setStars(doc.select(".rating-star span").text()); } catch (Exception e) {}
            return cc;
        } catch (Exception e) { return null; }
    }
}
