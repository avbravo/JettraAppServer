package com.jettra.server.db.security;

import com.jettra.server.autentification.entity.JCredential;
import com.jettra.server.autentification.entity.JRole;
import com.jettra.server.autentification.entity.JUser;
import com.jettra.server.autentification.repository.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JettraSecurityDBTest {

    
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Running JettraSecurityDB & Repositories Tests...");
        System.out.println("==================================================");

        try {
            // Clean up existing files before starting
            cleanDatabaseDirectory();

            testInitializationAndCrud();
            testCustomQueryLanguage();
            testConcurrencyAndLocking();

            System.out.println("\n==================================================");
            System.out.println("ALL TESTS PASSED SUCCESSFULLY!");
            System.out.println("==================================================");
        } catch (Throwable t) {
            System.err.println("\n==================================================");
            System.err.println("TEST FAILURE!");
            t.printStackTrace();
            System.err.println("==================================================");
            System.exit(1);
        }
    }

    private static void cleanDatabaseDirectory() {
        File dbDir = new File(System.getProperty("user.dir") + File.separator + "db" + File.separator + "securitydb");
        if (dbDir.exists()) {
            deleteDirectory(dbDir);
        }
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    private static void testInitializationAndCrud() {
        System.out.println("\n--- Testing Initialization and CRUD ---");

        // Run Initializer
        JettraSecurityDBInitializer.initializeIfEmpty();

        com.jettra.server.autentification.repository.JRoleRepository roleRepo = new JRoleRepositoryImpl();
        JUserRepository userRepo = new JUserRepositoryImpl();
        JCredentialRepository credRepo = new JCredentialRepositoryImpl();

        // 1. Verify default roles
        List<JRole> roles = roleRepo.findAll();
        assertCondition(roles.size() == 3, "Should have 3 default roles (ADMIN, MANAGER, DEMO), found: " + roles.size());
        
        boolean hasAdminRole = roles.stream().anyMatch(r -> r.name().equals("ADMIN"));
        boolean hasManagerRole = roles.stream().anyMatch(r -> r.name().equals("MANAGER"));
        boolean hasDemoRole = roles.stream().anyMatch(r -> r.name().equals("DEMO"));
        assertCondition(hasAdminRole && hasManagerRole && hasDemoRole, "Default roles names do not match.");

        // 2. Verify admin user
        List<JUser> users = userRepo.findAll();
        assertCondition(users.size() == 1, "Should have 1 default user (admin)");
        JUser adminUser = users.get(0);
        assertCondition(adminUser.firstName().equals("admin") && adminUser.lastName().equals("admin"), "Default user is not admin");
        assertCondition(adminUser.jRoles().size() == 1, "Default user should have exactly 1 role");
        assertCondition(adminUser.jRoles().iterator().next().name().equals("ADMIN"), "Default user role should be ADMIN");

        // 3. Verify credentials
        List<JCredential> credentials = credRepo.findAll();
        assertCondition(credentials.size() == 1, "Should have 1 default credential");
        JCredential adminCred = credentials.get(0);
        assertCondition(adminCred.username().equals("admin"), "Default credential username is not admin");
        assertCondition(adminCred.jUser().id().equals(adminUser.id()), "Credential should link to admin user");
        
        String expectedHash = JettraSecurityDBInitializer.hashPassword("admin");
        assertCondition(adminCred.passwordHash().equals(expectedHash), "Credential password hash does not match expected");

        // 4. Test CRUD (Creation)
        JRole newRole = new JRole(UUID.randomUUID(), "SUPPORT", true);
        roleRepo.save(newRole);
        
        Optional<JRole> retrievedRole = roleRepo.findById(newRole.id());
        assertCondition(retrievedRole.isPresent(), "Saved role 'SUPPORT' could not be retrieved");
        assertCondition(retrievedRole.get().name().equals("SUPPORT"), "Retrieved role name mismatch");

        // 5. Test CRUD (Delete)
        roleRepo.delete(newRole.id());
        Optional<JRole> deletedRole = roleRepo.findById(newRole.id());
        assertCondition(deletedRole.isEmpty(), "Deleted role should not be present");

        System.out.println("CRUD and Seeding validation passed!");
    }

    private static void testCustomQueryLanguage() {
        System.out.println("\n--- Testing Custom Query Language Parser ---");

        JRoleRepository roleRepo = new JRoleRepositoryImpl();
        JUserRepository userRepo = new JUserRepositoryImpl();
        JCredentialRepository credRepo = new JCredentialRepositoryImpl();

        // 1. Role simple queries
        List<JRole> adminRoles = roleRepo.search("name = 'ADMIN'");
        assertCondition(adminRoles.size() == 1, "Expected 1 ADMIN role from search, got: " + adminRoles.size());

        List<JRole> activeRoles = roleRepo.search("active = true");
        assertCondition(activeRoles.size() == 3, "Expected 3 active roles, got: " + activeRoles.size());

        // 2. User queries with nested collections
        List<JUser> adminUsers = userRepo.search("jRoles.name = 'ADMIN'");
        assertCondition(adminUsers.size() == 1, "Expected 1 admin user, got: " + adminUsers.size());

        List<JUser> managerUsers = userRepo.search("jRoles.name = 'MANAGER'");
        assertCondition(managerUsers.size() == 0, "Expected 0 manager users, got: " + managerUsers.size());

        // 3. Credential queries with nested objects
        List<JCredential> adminCreds = credRepo.search("juser.firstName = 'admin' AND active = true");
        assertCondition(adminCreds.size() == 1, "Expected 1 admin credential, got: " + adminCreds.size());

        List<JCredential> fakeCreds = credRepo.search("juser.firstName = 'nonexistent' OR username = 'nonexistent'");
        assertCondition(fakeCreds.size() == 0, "Expected 0 credentials, got: " + fakeCreds.size());

        // 4. Complex parser tests
        List<JRole> complexRoles = roleRepo.search("(name = 'ADMIN' OR name = 'MANAGER') AND active = true");
        assertCondition(complexRoles.size() == 2, "Expected 2 roles matching complex filter, got: " + complexRoles.size());

        System.out.println("Custom Query Language evaluation passed!");
    }

    private static void testConcurrencyAndLocking() throws InterruptedException {
        System.out.println("\n--- Testing Concurrency and Locking (Cloned Cluster Simulation) ---");

        int threadCount = 10;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        JUserRepository userRepo = new JUserRepositoryImpl();

        JUser baseUser = userRepo.findAll().get(0);

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Perform concurrent writes on the same database
                        JUser temp = new JUser(
                            UUID.randomUUID(),
                            "User_" + threadId + "_" + j,
                            "LastName",
                            "user_" + threadId + "_" + j + "@jettra.io",
                            "phone",
                            true,
                            baseUser.jRoles()
                        );
                        userRepo.save(temp);
                    }
                } catch (Exception e) {
                    System.err.println("Exception in concurrent writer thread " + threadId + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        long duration = System.currentTimeMillis() - start;

        // Verify total records
        List<JUser> allUsers = userRepo.findAll();
        int expectedCount = 1 + (threadCount * operationsPerThread); // 1 is original admin
        System.out.println("Executed " + (threadCount * operationsPerThread) + " concurrent writes in " + duration + "ms");
        System.out.println("Total users stored in database: " + allUsers.size() + " (Expected: " + expectedCount + ")");

        assertCondition(allUsers.size() == expectedCount, "Data loss occurred under concurrent writes! Stored count: " + allUsers.size());
        System.out.println("Concurrency and Locking validation passed!");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
