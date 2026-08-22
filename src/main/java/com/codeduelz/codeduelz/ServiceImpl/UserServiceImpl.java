package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.UserDto;
import com.codeduelz.codeduelz.entities.AuthProvider;
import com.codeduelz.codeduelz.entities.Role;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.UserRepo;
import com.codeduelz.codeduelz.services.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    @Override
    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER); // default role
        return userRepo.save(user);
    }
    @Override
    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    @Override
    public UserDto getUserProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return modelMapper.map(user, UserDto.class);
    }
    @Override
    public User findOrCreateJwtUser(String uuid, String email) {
        // Try to find by UUID (providerId)
        User user = userRepo.findByProviderId(uuid).orElse(null);

        if (user != null) {
            // Update email if it changed
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                user = userRepo.save(user);
            }

            // Set online status
            user.setIsOnline(true);
            user.setLastSeen(LocalDateTime.now());
            return userRepo.save(user);
        }

        // Not found by UUID, try to find by email (for existing users migrated from Firebase)
        if (email != null) {
            user = userRepo.findByEmail(email).orElse(null);
            if (user != null) {
                // Link this JWT (UUID) to existing user
                user.setProviderId(uuid);
                user.setProvider(AuthProvider.FIREBASE);
                user.setIsOnline(true);
                user.setLastSeen(LocalDateTime.now());
                return userRepo.save(user);
            }
        }

        // Create new user from JWT
        User newUser = new User();
        newUser.setProviderId(uuid);
        newUser.setEmail(email);

        String baseName = email != null ? email.split("@")[0] : "user_" + uuid.substring(0, 6);
        String tempName = baseName;
        int suffix = 1;
        while (userRepo.existsByUserName(tempName)) {
            tempName = baseName + suffix;
            suffix++;
        }
        newUser.setUserName(tempName);
        newUser.setProvider(AuthProvider.FIREBASE);
        newUser.setRole(Role.USER);
        newUser.setIsOnline(true);
        newUser.setLastSeen(LocalDateTime.now());

        // Retry with next suffix if concurrent request claimed this username
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return userRepo.save(newUser);
            } catch (DataIntegrityViolationException ex) {
                log.error("Username collision for '{}', retrying with suffix", newUser.getUsername());
                tempName = baseName + (suffix + attempt);
                newUser.setUserName(tempName);
            }
        }

        return userRepo.save(newUser);
    }

}
