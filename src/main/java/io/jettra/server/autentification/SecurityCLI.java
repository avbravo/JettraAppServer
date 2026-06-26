package io.jettra.server.autentification;

import io.jettra.server.autentification.repository.JCredentialRepositoryImpl;
import io.jettra.server.autentification.repository.JUserRepositoryImpl;
import io.jettra.server.autentification.repository.JRoleRepositoryImpl;
import io.jettra.server.autentification.repository.JRoleRepository;
import io.jettra.server.autentification.repository.JCredentialRepository;
import io.jettra.server.autentification.repository.JettraSecurityDBInitializer;
import io.jettra.server.autentification.repository.JUserRepository;
import io.jettra.server.autentification.entity.JCredential;
import io.jettra.server.autentification.entity.JRole;
import io.jettra.server.autentification.entity.JUser;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class SecurityCLI {

    private static final Scanner scanner = new Scanner(System.in);
    private static final JRoleRepository roleRepo = new JRoleRepositoryImpl();
    private static final JUserRepository userRepo = new JUserRepositoryImpl();
    private static final JCredentialRepository credRepo = new JCredentialRepositoryImpl();

    public static void main(String[] args) {
        // Ensure default data exists
        JettraSecurityDBInitializer.initializeIfEmpty();

        printHeader("JettraSecurityDB - Administrative Console CLI");

        if (!login()) {
            System.out.println("\n\u001B[31mAuthentication failed. Exiting Console.\u001B[0m");
            return;
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            String option = readLine("Select an option (1-4, or 'exit'): ").trim().toLowerCase();
            switch (option) {
                case "1":
                    manageRolesMenu();
                    break;
                case "2":
                    manageUsersMenu();
                    break;
                case "3":
                    manageCredentialsMenu();
                    break;
                case "4":
                case "exit":
                case "q":
                    running = false;
                    System.out.println("\n\u001B[32mExiting administrative session. Goodbye!\u001B[0m");
                    break;
                default:
                    System.out.println("\u001B[31mInvalid option. Please choose a number between 1 and 4.\u001B[0m");
            }
        }
    }

    private static boolean login() {
        System.out.println("Please log in to continue.");
        int attempts = 3;
        while (attempts > 0) {
            String username = readLine("Username: ").trim();
            String password = readPassword("Password: ");

            // Query credentials
            List<JCredential> results = credRepo.search("username = '" + username + "'");
            if (!results.isEmpty()) {
                JCredential cred = results.get(0);
                String hashedInput = JettraSecurityDBInitializer.hashPassword(password);
                if (cred.active() && cred.passwordHash().equals(hashedInput) && cred.jUser().active()) {
                    System.out.println("\n\u001B[32mAccess Granted! Welcome, " + cred.jUser().firstName() + " " + cred.jUser().lastName() + ".\u001B[0m");
                    // Update last login
                    JCredential updated = new JCredential(
                        cred.id(),
                        cred.jUser(),
                        cred.username(),
                        cred.passwordHash(),
                        cred.active(),
                        Instant.now()
                    );
                    credRepo.save(updated);
                    return true;
                }
            }
            attempts--;
            if (attempts > 0) {
                System.out.println("\u001B[31mInvalid username, password, or inactive account. Attempts remaining: " + attempts + "\u001B[0m\n");
            }
        }
        return false;
    }

    private static void printMainMenu() {
        System.out.println("\n=================================");
        System.out.println("           MAIN MENU             ");
        System.out.println("=================================");
        System.out.println("1. Manage Roles (JRole)");
        System.out.println("2. Manage Users (JUser)");
        System.out.println("3. Manage Credentials (JCredential)");
        System.out.println("4. Exit Console");
        System.out.println("=================================");
    }

    /* ---------------- ROLES MANAGEMENT ---------------- */
    private static void manageRolesMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Manage Roles ---");
            System.out.println("1. List All Roles");
            System.out.println("2. Create New Role");
            System.out.println("3. Toggle Role Active Status");
            System.out.println("4. Search Roles");
            System.out.println("5. Back to Main Menu");
            String choice = readLine("Select action: ").trim();
            switch (choice) {
                case "1":
                    listRoles();
                    break;
                case "2":
                    createRole();
                    break;
                case "3":
                    toggleRoleStatus();
                    break;
                case "4":
                    searchRoles();
                    break;
                case "5":
                    back = true;
                    break;
                default:
                    System.out.println("\u001B[31mInvalid option.\u001B[0m");
            }
        }
    }

    private static void listRoles() {
        List<JRole> roles = roleRepo.findAll();
        System.out.println("\n------------------------------------------------------------");
        System.out.printf("%-36s | %-15s | %-8s\n", "Role ID", "Name", "Active");
        System.out.println("------------------------------------------------------------");
        for (JRole role : roles) {
            System.out.printf("%-36s | %-15s | %-8s\n", role.id(), role.name(), role.active());
        }
        System.out.println("------------------------------------------------------------");
    }

    private static void createRole() {
        String name = readLine("Role Name (min 3 chars): ").trim().toUpperCase();
        if (name.length() < 3) {
            System.out.println("\u001B[31mError: Name must be at least 3 characters.\u001B[0m");
            return;
        }
        // Check if exists
        if (!roleRepo.search("name = '" + name + "'").isEmpty()) {
            System.out.println("\u001B[31mError: Role '" + name + "' already exists.\u001B[0m");
            return;
        }
        JRole role = new JRole(UUID.randomUUID(), name, true);
        roleRepo.save(role);
        System.out.println("\u001B[32mRole '" + name + "' created successfully.\u001B[0m");
    }

    private static void toggleRoleStatus() {
        String idStr = readLine("Enter Role ID to toggle: ").trim();
        try {
            UUID id = UUID.fromString(idStr);
            Optional<JRole> roleOpt = roleRepo.findById(id);
            if (roleOpt.isPresent()) {
                JRole r = roleOpt.get();
                JRole updated = new JRole(r.id(), r.name(), !r.active());
                roleRepo.save(updated);
                System.out.println("\u001B[32mRole '" + r.name() + "' status toggled to: " + updated.active() + ".\u001B[0m");
            } else {
                System.out.println("\u001B[31mRole not found.\u001B[0m");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\u001B[31mInvalid UUID format.\u001B[0m");
        }
    }

    /* ---------------- USERS MANAGEMENT ---------------- */
    private static void manageUsersMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Manage Users ---");
            System.out.println("1. List All Users");
            System.out.println("2. Create New User");
            System.out.println("3. Delete User");
            System.out.println("4. Toggle User Active Status");
            System.out.println("5. Change User Password");
            System.out.println("6. Search Users");
            System.out.println("7. Back to Main Menu");
            String choice = readLine("Select action: ").trim();
            switch (choice) {
                case "1":
                    listUsers();
                    break;
                case "2":
                    createUser();
                    break;
                case "3":
                    deleteUser();
                    break;
                case "4":
                    toggleUserStatus();
                    break;
                case "5":
                    changeUserPassword();
                    break;
                case "6":
                    searchUsers();
                    break;
                case "7":
                    back = true;
                    break;
                default:
                    System.out.println("\u001B[31mInvalid option.\u001B[0m");
            }
        }
    }

    private static void listUsers() {
        List<JUser> users = userRepo.findAll();
        System.out.println("\n------------------------------------------------------------------------------------------------------");
        System.out.printf("%-36s | %-20s | %-20s | %-8s | %-15s\n", "User ID", "Full Name", "Email", "Active", "Roles");
        System.out.println("------------------------------------------------------------------------------------------------------");
        for (JUser u : users) {
            String fullName = u.firstName() + " " + u.lastName();
            String rolesStr = u.jRoles().stream().map(JRole::name).collect(Collectors.joining(","));
            System.out.printf("%-36s | %-20s | %-20s | %-8s | %-15s\n", 
                u.id(), 
                fullName.substring(0, Math.min(fullName.length(), 20)), 
                (u.email() != null ? u.email() : "").substring(0, Math.min(u.email() != null ? u.email().length() : 0, 20)), 
                u.active(), 
                rolesStr);
        }
        System.out.println("------------------------------------------------------------------------------------------------------");
    }

    private static void createUser() {
        String firstName = readLine("First Name (min 3 chars): ").trim();
        String lastName = readLine("Last Name (min 3 chars): ").trim();
        String email = readLine("Email: ").trim();
        String phone = readLine("Phone: ").trim();

        if (firstName.length() < 3 || lastName.length() < 3) {
            System.out.println("\u001B[31mError: First and last name must be at least 3 characters long.\u001B[0m");
            return;
        }

        // Role selection
        List<JRole> allRoles = roleRepo.findAll();
        if (allRoles.isEmpty()) {
            System.out.println("\u001B[31mError: No roles exist. Please create a role first.\u001B[0m");
            return;
        }

        System.out.println("\nAvailable Roles:");
        for (int i = 0; i < allRoles.size(); i++) {
            System.out.println((i + 1) + ". " + allRoles.get(i).name() + " (" + (allRoles.get(i).active() ? "Active" : "Inactive") + ")");
        }

        String rolesInput = readLine("Select role indices (comma separated, e.g. 1,2): ");
        Set<JRole> assignedRoles = new HashSet<>();
        try {
            for (String idxStr : rolesInput.split(",")) {
                int idx = Integer.parseInt(idxStr.trim()) - 1;
                if (idx >= 0 && idx < allRoles.size()) {
                    assignedRoles.add(allRoles.get(idx));
                }
            }
        } catch (Exception e) {
            System.out.println("\u001B[31mError parsing role indices.\u001B[0m");
            return;
        }

        if (assignedRoles.isEmpty()) {
            System.out.println("\u001B[31mError: You must assign at least one role.\u001B[0m");
            return;
        }

        JUser user = new JUser(UUID.randomUUID(), firstName, lastName, email, phone, true, assignedRoles);
        userRepo.save(user);
        System.out.println("\u001B[32mUser created successfully with ID: " + user.id() + "\u001B[0m");
    }

    private static void deleteUser() {
        String idStr = readLine("Enter User ID to delete: ").trim();
        try {
            UUID id = UUID.fromString(idStr);
            if (userRepo.findById(id).isPresent()) {
                userRepo.delete(id);
                System.out.println("\u001B[32mUser deleted successfully.\u001B[0m");
            } else {
                System.out.println("\u001B[31mUser not found.\u001B[0m");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\u001B[31mInvalid UUID format.\u001B[0m");
        }
    }

    private static void toggleUserStatus() {
        String idStr = readLine("Enter User ID to toggle: ").trim();
        try {
            UUID id = UUID.fromString(idStr);
            Optional<JUser> userOpt = userRepo.findById(id);
            if (userOpt.isPresent()) {
                JUser u = userOpt.get();
                JUser updated = new JUser(u.id(), u.firstName(), u.lastName(), u.email(), u.phone(), !u.active(), u.jRoles());
                userRepo.save(updated);
                System.out.println("\u001B[32mUser status toggled to: " + updated.active() + ".\u001B[0m");
            } else {
                System.out.println("\u001B[31mUser not found.\u001B[0m");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\u001B[31mInvalid UUID format.\u001B[0m");
        }
    }

    /* ---------------- CREDENTIALS MANAGEMENT ---------------- */
    private static void manageCredentialsMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Manage Credentials ---");
            System.out.println("1. List All Credentials");
            System.out.println("2. Create Credentials for User");
            System.out.println("3. Delete Credentials");
            System.out.println("4. Toggle Credentials Active Status");
            System.out.println("5. Search Credentials");
            System.out.println("6. Show Microservices Credentials");
            System.out.println("7. Back to Main Menu");
            String choice = readLine("Select action: ").trim();
            switch (choice) {
                case "1":
                    listCredentials();
                    break;
                case "2":
                    createCredentials();
                    break;
                case "3":
                    deleteCredentials();
                    break;
                case "4":
                    toggleCredentialsStatus();
                    break;
                case "5":
                    searchCredentials();
                    break;
                case "6":
                    showMicroservicesCredentials();
                    break;
                case "7":
                    back = true;
                    break;
                default:
                    System.out.println("\u001B[31mInvalid option.\u001B[0m");
            }
        }
    }

    private static void listCredentials() {
        List<JCredential> credentials = credRepo.findAll();
        System.out.println("\n------------------------------------------------------------------------------------------------------");
        System.out.printf("%-36s | %-20s | %-12s | %-8s | %-20s\n", "Credential ID", "Linked User", "Username", "Active", "Last Login");
        System.out.println("------------------------------------------------------------------------------------------------------");
        for (JCredential c : credentials) {
            String userName = c.jUser() != null ? c.jUser().firstName() + " " + c.jUser().lastName() : "N/A";
            System.out.printf("%-36s | %-20s | %-12s | %-8s | %-20s\n", 
                c.id(), 
                userName.substring(0, Math.min(userName.length(), 20)), 
                c.username(), 
                c.active(), 
                c.lastLogin() != null ? c.lastLogin().toString() : "Never");
        }
        System.out.println("------------------------------------------------------------------------------------------------------");
    }

    private static void createCredentials() {
        List<JUser> users = userRepo.findAll();
        if (users.isEmpty()) {
            System.out.println("\u001B[31mError: No users exist. Please create a user first.\u001B[0m");
            return;
        }

        System.out.println("\nSelect User:");
        for (int i = 0; i < users.size(); i++) {
            System.out.println((i + 1) + ". " + users.get(i).firstName() + " " + users.get(i).lastName() + " (ID: " + users.get(i).id() + ")");
        }

        String userIdxStr = readLine("Enter user index: ").trim();
        JUser selectedUser;
        try {
            int idx = Integer.parseInt(userIdxStr) - 1;
            selectedUser = users.get(idx);
        } catch (Exception e) {
            System.out.println("\u001B[31mInvalid user selection.\u001B[0m");
            return;
        }

        String username = readLine("Username (min 7 chars): ").trim();
        String password = readPassword("Password (min 6 chars): ");

        if (username.length() < 7 || password.length() < 6) {
            System.out.println("\u001B[31mError: Username must be min 7 chars and Password must be min 6 chars.\u001B[0m");
            return;
        }

        // Verify if username exists
        if (!credRepo.search("username = '" + username + "'").isEmpty()) {
            System.out.println("\u001B[31mError: Username '" + username + "' already exists.\u001B[0m");
            return;
        }

        String hashedPassword = JettraSecurityDBInitializer.hashPassword(password);
        JCredential credential = new JCredential(UUID.randomUUID(), selectedUser, username, hashedPassword, true, null);
        credRepo.save(credential);
        System.out.println("\u001B[32mCredentials created successfully.\u001B[0m");
    }

    private static void deleteCredentials() {
        String idStr = readLine("Enter Credential ID to delete: ").trim();
        try {
            UUID id = UUID.fromString(idStr);
            if (credRepo.findById(id).isPresent()) {
                credRepo.delete(id);
                System.out.println("\u001B[32mCredentials deleted successfully.\u001B[0m");
            } else {
                System.out.println("\u001B[31mCredentials not found.\u001B[0m");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\u001B[31mInvalid UUID format.\u001B[0m");
        }
    }

    private static void toggleCredentialsStatus() {
        String idStr = readLine("Enter Credential ID to toggle: ").trim();
        try {
            UUID id = UUID.fromString(idStr);
            Optional<JCredential> credOpt = credRepo.findById(id);
            if (credOpt.isPresent()) {
                JCredential c = credOpt.get();
                JCredential updated = new JCredential(c.id(), c.jUser(), c.username(), c.passwordHash(), !c.active(), c.lastLogin());
                credRepo.save(updated);
                System.out.println("\u001B[32mCredentials status toggled to: " + updated.active() + ".\u001B[0m");
            } else {
                System.out.println("\u001B[31mCredentials not found.\u001B[0m");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\u001B[31mInvalid UUID format.\u001B[0m");
        }
    }

    private static void showMicroservicesCredentials() {
        String condition = readLine("Enter search condition (e.g., username = 'admin'): ").trim();
        List<JCredential> creds = credRepo.search(condition);
        
        if (creds.isEmpty()) {
            System.out.println("\u001B[31mError: No credentials found matching the condition.\u001B[0m");
            return;
        }
        
        System.out.println("\n" + creds.size() + " credential(s) found.");

        String secretPhrase = readPassword("Enter secret phrase to encrypt credentials: ").trim();
        if (secretPhrase.isBlank()) {
            System.out.println("\u001B[31mError: Secret phrase cannot be empty.\u001B[0m");
            return;
        }

        System.out.println("\n\u001B[32m--- MICROSERVICE CREDENTIALS ENCRYPTED SUCCESSFULLY ---\u001B[0m");
        System.out.println("\u001B[33mThese values are only shown on screen and not saved to the database.\u001B[0m");
        System.out.println("\n--------------------------------------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-20s | %-65s | %-65s\n", "Field", "Current Value", "Encrypted (using your secret phrase)");
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------");
        
        for (JCredential cred : creds) {
            String currentPassword = cred.passwordHash();
            String encUsername = JettraCrypto.encrypt(cred.username(), secretPhrase);
            String encPassword = JettraCrypto.encrypt(currentPassword, secretPhrase);
            
            System.out.printf("%-20s | %-65s | %-65s\n", "Username", cred.username(), encUsername);
            System.out.printf("%-20s | %-65s | %-65s\n", "Password", currentPassword, encPassword);
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------");
        }
        System.out.println();
    }

    private static void searchRoles() {
        String condition = readLine("Enter search condition (e.g., name = 'ADMIN'): ").trim();
        List<JRole> roles = roleRepo.search(condition);
        if (roles.isEmpty()) {
            System.out.println("\u001B[33mNo roles found matching the condition.\u001B[0m");
        } else {
            System.out.println("\n------------------------------------------------------------");
            System.out.printf("%-36s | %-15s | %-8s\n", "Role ID", "Name", "Active");
            System.out.println("------------------------------------------------------------");
            for (JRole role : roles) {
                System.out.printf("%-36s | %-15s | %-8s\n", role.id(), role.name(), role.active());
            }
            System.out.println("------------------------------------------------------------");
        }
    }

    private static void changeUserPassword() {
        String idStr = readLine("Enter User ID to change password: ").trim();
        try {
            UUID id = UUID.fromString(idStr);
            Optional<JUser> userOpt = userRepo.findById(id);
            if (userOpt.isPresent()) {
                JUser u = userOpt.get();
                List<JCredential> creds = credRepo.search("juser.id = '" + u.id().toString() + "'");
                if (!creds.isEmpty()) {
                    JCredential cred = creds.get(0);
                    String newPassword = readPassword("Enter new password (min 6 chars): ");
                    if (newPassword.length() < 6) {
                        System.out.println("\u001B[31mError: Password must be min 6 chars.\u001B[0m");
                        return;
                    }
                    String hashed = JettraSecurityDBInitializer.hashPassword(newPassword);
                    JCredential updated = new JCredential(cred.id(), cred.jUser(), cred.username(), hashed, cred.active(), cred.lastLogin());
                    credRepo.save(updated);
                    System.out.println("\u001B[32mPassword changed successfully for user: " + u.firstName() + " " + u.lastName() + ".\u001B[0m");
                } else {
                    System.out.println("\u001B[31mNo credentials found for this user. Create credentials first.\u001B[0m");
                }
            } else {
                System.out.println("\u001B[31mUser not found.\u001B[0m");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\u001B[31mInvalid UUID format.\u001B[0m");
        }
    }

    private static void searchUsers() {
        String condition = readLine("Enter search condition (e.g., firstName = 'admin'): ").trim();
        List<JUser> users = userRepo.search(condition);
        if (users.isEmpty()) {
            System.out.println("\u001B[33mNo users found matching the condition.\u001B[0m");
        } else {
            System.out.println("\n------------------------------------------------------------------------------------------------------");
            System.out.printf("%-36s | %-20s | %-20s | %-8s | %-15s\n", "User ID", "Full Name", "Email", "Active", "Roles");
            System.out.println("------------------------------------------------------------------------------------------------------");
            for (JUser u : users) {
                String fullName = u.firstName() + " " + u.lastName();
                String rolesStr = u.jRoles().stream().map(JRole::name).collect(Collectors.joining(","));
                System.out.printf("%-36s | %-20s | %-20s | %-8s | %-15s\n", 
                    u.id(), 
                    fullName.substring(0, Math.min(fullName.length(), 20)), 
                    (u.email() != null ? u.email() : "").substring(0, Math.min(u.email() != null ? u.email().length() : 0, 20)), 
                    u.active(), 
                    rolesStr);
            }
            System.out.println("------------------------------------------------------------------------------------------------------");
        }
    }

    private static void searchCredentials() {
        String condition = readLine("Enter search condition (e.g., username = 'admin'): ").trim();
        List<JCredential> credentials = credRepo.search(condition);
        if (credentials.isEmpty()) {
            System.out.println("\u001B[33mNo credentials found matching the condition.\u001B[0m");
        } else {
            System.out.println("\n------------------------------------------------------------------------------------------------------");
            System.out.printf("%-36s | %-20s | %-12s | %-8s | %-20s\n", "Credential ID", "Linked User", "Username", "Active", "Last Login");
            System.out.println("------------------------------------------------------------------------------------------------------");
            for (JCredential c : credentials) {
                String userName = c.jUser() != null ? c.jUser().firstName() + " " + c.jUser().lastName() : "N/A";
                System.out.printf("%-36s | %-20s | %-12s | %-8s | %-20s\n", 
                    c.id(), 
                    userName.substring(0, Math.min(userName.length(), 20)), 
                    c.username(), 
                    c.active(), 
                    c.lastLogin() != null ? c.lastLogin().toString() : "Never");
            }
            System.out.println("------------------------------------------------------------------------------------------------------");
        }
    }

    /* ---------------- CLI UTILITIES ---------------- */
    private static void printHeader(String text) {
        String border = "=".repeat(text.length() + 8);
        System.out.println("\n" + border);
        System.out.println("    " + text);
        System.out.println(border + "\n");
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static String readPassword(String prompt) {
        if (System.console() != null) {
            char[] chars = System.console().readPassword(prompt);
            return new String(chars);
        } else {
            // Fallback for IDE terminals or processes redirecting stdin
            System.out.print(prompt);
            return scanner.nextLine();
        }
    }
}
