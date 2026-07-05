package io.jettra.server.jwt;

import io.jettra.jwt.request.LoginRequest;
import io.jettra.jwt.request.LoginResponse;
import io.jettra.server.autentification.entity.JCredential;
import io.jettra.server.autentification.entity.JRole;
import io.jettra.server.autentification.entity.JUser;
import io.jettra.server.autentification.repository.JCredentialRepository;
import io.jettra.server.openapi.annotations.OpenApi;
import io.jettra.server.openapi.annotations.Operation;
import io.jettra.server.openapi.annotations.RequestBody;
import io.jettra.server.openapi.annotations.ApiResponse;
import io.jettra.core.inject.annotation.Inject;
import io.jettra.jwt.JettraJWT;
import io.jettra.rest.annotations.Consumes;
import io.jettra.rest.annotations.POST;
import io.jettra.rest.annotations.Path;
import io.jettra.rest.annotations.PermitAll;
import io.jettra.rest.annotations.Produces;
import io.jettra.rest.core.Response;
import io.jettra.server.config.JettraConfigProperty;
import io.jettra.server.discoverer.Discovered;

import java.util.HashMap;

@Discovered
@OpenApi(title = "Authentication API", version = "v1.0", description = "API for Authentication")
@Path("/auth")
public class AuthController {
    @JettraConfigProperty(name = "server.JWT_SECRET")
    private String JWT_SECRET;
    @JettraConfigProperty(name = "server.JWT_EXPIRATION")   
    private Integer JWT_EXPIRATION;

    
    // Para GeminiAPI por favor cambiarlas por las propiedades de
//    private static final String JWT_SECRET = "default_secret_key_jettra_rest_2026";
//    private static final long JWT_EXPIRATION = 3600000; // 1 hour

    @Inject
    JCredentialRepository jCredentialRepository;
    @POST
    @Path("/login")
    @Consumes("application/json")
    @Produces("application/json")
    @PermitAll
    @Operation(summary = "Login and get JWT", description = "Authenticates a user and returns a Bearer token")
    @ApiResponse(responseCode = "200", description = "Successful authentication")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public Response login(@RequestBody(description = "Login credentials", required = true) LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null 
            || request.getUsername().trim().isEmpty() || request.getPassword().trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Invalid credentials\"}").build();
        }

        java.util.Optional<JCredential> optCred = jCredentialRepository.findByUsernamePassword(request.getUsername(), request.getPassword());

        if (optCred.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Invalid credentials\"}").build();
        }

        JUser jUser = optCred.get().jUser();
        java.util.List<String> roleNames = new java.util.ArrayList<>();
        if (jUser != null && jUser.jRoles()!= null) {
            for (JRole r : jUser.jRoles()) {
                roleNames.add(r.name());
            }
        }
        
        String secret = (JWT_SECRET != null) ? JWT_SECRET : "default_secret_key_jettra_rest_2026";
        long expiration = (JWT_EXPIRATION != null) ? JWT_EXPIRATION : 3600000;

        JettraJWT jwt = new JettraJWT(secret, expiration);
        java.util.Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roleNames);
        String token = "Bearer " + jwt.generateToken(claims, request.getUsername());

        return Response.ok(new LoginResponse(token)).build();
    }
}
