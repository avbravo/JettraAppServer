package com.jettra.server.autentification.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.UUID;

public record JRole(
        @NotNull
        UUID id,
        @NotNull
        @Size(min = 3)
        String name,
        @NotNull
        Boolean active
        ) implements Serializable {
}
