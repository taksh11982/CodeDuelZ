package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.entities.Leaderboard;
import com.codeduelz.codeduelz.repo.LeaderboardRepo;
import com.codeduelz.codeduelz.services.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {
    @Autowired
    private LeaderboardRepo leaderboardRepo;

    @Override
    public List<Leaderboard> getLeaderboard() {
        return new ArrayList<>(leaderboardRepo.findAllByOrderByRankAsc());
    }
}
