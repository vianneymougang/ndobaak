package com.ndobaak.backend.payment.repository;

import com.ndobaak.backend.payment.entity.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    Optional<PaymentSchedule> findByBookingId(Long bookingId);
}
