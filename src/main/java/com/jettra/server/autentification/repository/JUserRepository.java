package com.jettra.server.autentification.repository;

import com.jettra.server.autentification.entity.JUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JUserRepository {
    void save(JUser user);
    Optional<JUser> findById(UUID id);
    List<JUser> findAll();
    void delete(UUID id);
    List<JUser> search(String query);
}
