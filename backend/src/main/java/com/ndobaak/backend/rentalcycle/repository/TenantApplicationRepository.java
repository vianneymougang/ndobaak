package com.ndobaak.backend.rentalcycle.repository;

import com.ndobaak.backend.rentalcycle.entity.TenantApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantApplicationRepository extends JpaRepository<TenantApplication, Long> {
    Optional<TenantApplication> findByBookingId(Long bookingId);
}
