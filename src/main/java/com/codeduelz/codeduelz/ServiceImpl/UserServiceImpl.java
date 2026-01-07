package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.dtos.UserDto;
import com.codeduelz.codeduelz.entities.AuthProvider;
import com.codeduelz.codeduelz.entities.Role;
import com.codeduelz.codeduelz.entities.User;
import com.codeduelz.codeduelz.repo.UserRepo;
import com.codeduelz.codeduelz.services.UserService;
import com.google.firebase.auth.FirebaseToken;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    @Autowired
    private final UserRepo userRepo;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
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
    public User findOrCreateFirebaseUser(FirebaseToken token) {

        String email = token.getEmail();
        String uid = token.getUid();

        return userRepo.findByProviderId(uid)
                .orElseGet(() -> {
                    User user = new User();
                    user.setProviderId(uid);              // Firebase UID
                    user.setEmail(email);
                    user.setUserName(
                            email != null
                                    ? email.split("@")[0]
                                    : "user_" + uid.substring(0, 6)
                    );
                    user.setProvider(AuthProvider.FIREBASE);
                    user.setRole(Role.USER);
                    return userRepo.save(user);
                });
    }

}
