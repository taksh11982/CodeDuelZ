package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Friend;
import com.codeduelz.codeduelz.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRepo extends JpaRepository<Friend,Long> {
    boolean existsByUserAndFriendUser(User user, User friendUser);
    List<Friend> findByUserAndStatus(User user, String status);
}
