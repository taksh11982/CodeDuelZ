package com.codeduelz.codeduelz.services;

import com.google.firebase.auth.FirebaseToken;

public interface FirebaseAuthService {
    public FirebaseToken verifyToken(String token);
}
