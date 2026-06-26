package io.jettra.server.autentification.entity;

import io.jettra.rules.validations.NotNull;
import io.jettra.rules.validations.Size;
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
