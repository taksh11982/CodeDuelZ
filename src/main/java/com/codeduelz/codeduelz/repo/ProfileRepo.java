package com.codeduelz.codeduelz.repo;

import com.codeduelz.codeduelz.entities.Profile;
import com.codeduelz.codeduelz.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepo extends JpaRepository<Profile,Long> {
    Optional<Profile> findByUser(User user);
}
