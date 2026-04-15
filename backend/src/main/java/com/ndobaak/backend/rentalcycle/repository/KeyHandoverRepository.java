package com.ndobaak.backend.rentalcycle.repository;

import com.ndobaak.backend.rentalcycle.entity.KeyHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeyHandoverRepository extends JpaRepository<KeyHandover, Long> {

    Optional<KeyHandover> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}
