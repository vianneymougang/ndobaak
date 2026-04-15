package com.ndobaak.backend.auth.repository;

import com.ndobaak.backend.auth.entity.Admin;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.common.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByOauthProviderId(String oauthProviderId);

    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @Query("SELECT u FROM User u WHERE TYPE(u) = Admin")
    List<Admin> findAllAdmins();

    @Query("SELECT COUNT(u) FROM User u WHERE u.registrationDate >= :since")
    long countByRegistrationDateAfter(@Param("since") LocalDateTime since);

    long countByStatus(UserStatus status);
}
