package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByTenantId(Long tenantId);
}
