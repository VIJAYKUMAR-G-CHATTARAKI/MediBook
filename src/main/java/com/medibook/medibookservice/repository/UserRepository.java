package com.medibook.medibookservice.repository;

import com.medibook.medibookservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity.
 * Spring Data JPA auto-generates implementations at runtime.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * Used during login authentication.
     *
     * @param email the user's email
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the given email already exists.
     * Used during registration to prevent duplicates.
     *
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    // Custom JPQL query
    @Query("SELECT u FROM User u WHERE u.role = 'DOCTOR' AND u.enabled = true")
    List<User> findActiveDoctors();

    // Native SQL query
    @Query(value = "SELECT * FROM users WHERE created_at > NOW() - INTERVAL 7 DAY",
            nativeQuery = true)
    List<User> findRecentUsers();
}