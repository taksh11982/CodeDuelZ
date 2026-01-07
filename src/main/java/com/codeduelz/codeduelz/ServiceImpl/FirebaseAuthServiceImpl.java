package com.codeduelz.codeduelz.ServiceImpl;

import com.codeduelz.codeduelz.services.FirebaseAuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthServiceImpl implements FirebaseAuthService {
    @Override
    public FirebaseToken verifyToken(String token) {
        try{
            return FirebaseAuth.getInstance().verifyIdToken(token);
        } catch (Exception e){
            throw new RuntimeException("Invalid token");
        }
    }
}
