package com.codeduelz.codeduelz.services;

import com.codeduelz.codeduelz.entities.Leaderboard;
import com.codeduelz.codeduelz.repo.LeaderboardRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {
    @Autowired
    private LeaderboardRepo leaderboardRepo;
    public List<Leaderboard> getLeaderboard(){
        return leaderboardRepo.findAllByOrderByRankAsc();
    }
}
