package com.ndobaak.backend.payment.repository;

import com.ndobaak.backend.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    Page<Payment> findByBookingStudentId(Long studentId, Pageable pageable);

    Page<Payment> findByBookingListingLandlordId(Long landlordId, Pageable pageable);
}
