package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Leaderboard;
import com.codeduelz.codeduelz.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderboardRepo extends JpaRepository<Leaderboard,Long> {
    Optional<Leaderboard> findByUser(User user);
    List<Leaderboard> findAllByOrderByRankAsc();
}
