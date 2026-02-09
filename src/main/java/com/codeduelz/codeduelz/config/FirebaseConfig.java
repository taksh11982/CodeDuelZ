package com.codeduelz.codeduelz.config;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount;
            
            // Check for environment variable first (for production/Render deployment)
            String firebaseCredentials = System.getenv("FIREBASE_CREDENTIALS");
            
            if (firebaseCredentials != null && !firebaseCredentials.isEmpty()) {
                // Load from environment variable
                serviceAccount = new ByteArrayInputStream(
                        firebaseCredentials.getBytes(StandardCharsets.UTF_8));
                System.out.println("Loading Firebase credentials from environment variable");
            } else {
                // Fallback to classpath for local development
                serviceAccount = new ClassPathResource("firebase/serviceAccountKey.json")
                        .getInputStream();
                System.out.println("Loading Firebase credentials from classpath");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            System.out.println("Firebase initialized successfully");

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Firebase initialization failed", e
            );
        }
    }
}
