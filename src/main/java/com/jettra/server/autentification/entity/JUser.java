package com.jettra.server.autentification.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.UUID;
import java.util.Set;

public record JUser(
        @NotNull
        @Size(min = 3)
        UUID id,
        @NotNull
        @Size(min = 3)
        String firstName,
        @NotNull
        @Size(min = 3)
        String lastName,
        String email,
        String phone,
        Boolean active,
        @NotNull
        Set<JRole> jRoles
        ) implements Serializable {
}
