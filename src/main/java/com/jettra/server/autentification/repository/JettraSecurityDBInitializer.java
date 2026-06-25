package com.jettra.server.autentification.repository;

import com.jettra.server.autentification.entity.JCredential;
import com.jettra.server.autentification.entity.JRole;
import com.jettra.server.autentification.entity.JUser;
import com.jettra.server.autentification.*;
import java.time.Instant;
import java.util.*;

public class JettraSecurityDBInitializer {

    public static void initializeIfEmpty() {
        JRoleRepository roleRepo = new JRoleRepositoryImpl();
        JUserRepository userRepo = new JUserRepositoryImpl();
        JCredentialRepository credRepo = new JCredentialRepositoryImpl();

        List<JRole> roles = roleRepo.findAll();
        List<JUser> users = userRepo.findAll();
        List<JCredential> credentials = credRepo.findAll();

        if (roles.isEmpty() && users.isEmpty() && credentials.isEmpty()) {
            System.out.println("[JettraSecurityDBInitializer] No records found. Initializing default security records...");

            // 1. Create Roles
            JRole adminRole = new JRole(UUID.nameUUIDFromBytes("ADMIN".getBytes()), "ADMIN", true);
            roleRepo.save(adminRole);

            // 2. Create User admin
            Set<JRole> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);

            JUser adminUser = new JUser(
                UUID.nameUUIDFromBytes("admin".getBytes()),
                "admin",
                "admin",
                "admin@jettra.io",
                "1234567890",
                true,
                adminRoles
            );
            userRepo.save(adminUser);

            // 3. Create Credentials for user admin with password 'admin' encrypted
            String encryptedPassword = hashPassword("admin");
            JCredential adminCredential = new JCredential(
                UUID.nameUUIDFromBytes("admin-cred".getBytes()),
                adminUser,
                "admin",
                encryptedPassword,
                true,
                Instant.now()
            );
            credRepo.save(adminCredential);

            System.out.println("[JettraSecurityDBInitializer] Default security records initialized successfully.");
        } else {
            System.out.println("[JettraSecurityDBInitializer] Security records already exist. Skipping default initialization.");
        }
    }

    public static String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error hashing password", ex);
        }
    }
}
