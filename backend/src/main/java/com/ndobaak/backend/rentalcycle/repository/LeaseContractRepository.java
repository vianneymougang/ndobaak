package com.ndobaak.backend.rentalcycle.repository;

import com.ndobaak.backend.rentalcycle.entity.LeaseContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaseContractRepository extends JpaRepository<LeaseContract, Long> {
    Optional<LeaseContract> findByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
}
