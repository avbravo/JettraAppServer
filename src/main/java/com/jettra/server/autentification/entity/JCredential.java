package com.jettra.server.autentification.entity;

import com.jettra.server.autentification.entity.JUser;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record JCredential(
        @NotNull
        @Size(min = 3)
        UUID id,
        @NotNull
        JUser jUser,
        @NotNull
        @Size(min = 7)
        String username,
        @NotNull
        @Size(min = 10)
        String passwordHash,
        Boolean active,
        Instant lastLogin
        ) implements Serializable {
}
