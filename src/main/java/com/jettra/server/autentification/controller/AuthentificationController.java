package com.jettra.server.autentification.controller;

import com.jettra.core.inject.annotation.Inject;
import com.jettra.jwt.JettraJWT;
import com.jettra.jwt.request.LoginResponse;
import com.jettra.rest.annotations.GET;
import com.jettra.rest.annotations.Path;
import com.jettra.rest.annotations.PermitAll;
import com.jettra.rest.annotations.Produces;
import com.jettra.rest.annotations.QueryParam;
import com.jettra.rest.core.Response;
import com.jettra.server.autentification.entity.JCredential;
import com.jettra.server.autentification.entity.JRole;
import com.jettra.server.autentification.entity.JUser;
import com.jettra.server.autentification.repository.JCredentialRepository;
import com.jettra.server.autentification.repository.JUserRepository;
import com.jettra.server.openapi.annotations.OpenApi;
import com.jettra.server.openapi.annotations.Operation;
import com.jettra.server.openapi.annotations.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/autentification/auth")
@OpenApi(title = "Library API", version = "v1.0", description = "API for Library management")
public class AuthentificationController {

    private static final String JWT_SECRET = "default_secret_key_jettra_rest_2026";
    private static final long JWT_EXPIRATION = 3600000;

        @Inject
    private JCredentialRepository jCredentialRepository;
        
        @Inject
    private JUserRepository jUserRepository;
        
        
    
    @GET
    @Produces("application/json")
    @PermitAll
    @Operation(summary = "Login and get JWT", description = "Authenticates a user and returns a Bearer token")
    public Response login(
            @Parameter(name = "username", description = "Username for authentication", required = true) @QueryParam("username") String username,
            @Parameter(name = "password", description = "Password for authentication", required = true) @QueryParam("password") String password) {

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Invalid credentials\"}").build();
        }

//        Optional<Credential> optCred =  credentialRepository.findAll().stream()
//                .filter(c -> c.username().equals(username) && c.passwordHash().equals(password))
//                .findFirst();
        
        Optional<JCredential> optCred =  jCredentialRepository.findByUsernamePassword(username, password);
               
        
        
        

        if (optCred.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Invalid credentials\"}").build();
        }

        // Fetch User using UserRepository to validate/hydrate User
      //   UUID uuid = UUID.fromString(id);
        Optional<JUser> optUser = jUserRepository.findById(UUID.fromString(optCred.get().jUser().id().toString()));
        if (optUser.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"User not found in UserRepository\"}").build();
        }

        JUser user = optUser.get();
        List<String> roleNames = new ArrayList<>();
        if (user.jRoles() != null) {
            for (JRole r : user.jRoles()) {
                roleNames.add(r.name());
            }
        }

        JettraJWT jwt = new JettraJWT(JWT_SECRET, JWT_EXPIRATION);
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleNames);
        String token = "Bearer " + jwt.generateToken(claims, username);

        return Response.ok(new LoginResponse(token)).build();
    }
}
