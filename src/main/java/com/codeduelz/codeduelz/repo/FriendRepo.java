package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Friend;
import com.codeduelz.codeduelz.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepo extends JpaRepository<Friend, Long> {
    boolean existsByUserAndFriendUser(User user, User friendUser);

    List<Friend> findByUserAndStatus(User user, String status);

    Optional<Friend> findByUserAndFriendUserAndStatus(User user, User friendUser, String status);

    List<Friend> findByFriendUserAndStatus(User friendUser, String status);

    Optional<Friend> findByIdAndUser(Long id, User user);
}
