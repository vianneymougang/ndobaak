package com.ndobaak.backend.payment.repository;

import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    Page<Payment> findByBookingStudentId(Long studentId, Pageable pageable);

    Page<Payment> findByBookingListingLandlordId(Long landlordId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.booking.listing.landlord.id = :landlordId AND p.status = :status")
    BigDecimal sumAmountByLandlordIdAndStatus(@Param("landlordId") Long landlordId, @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:type IS NULL OR p.paymentType = :type) " +
           "ORDER BY p.createdAt DESC")
    Page<Payment> findAllFiltered(@Param("status") PaymentStatus status, @Param("type") com.ndobaak.backend.common.enums.PaymentType type, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT FUNCTION('TO_CHAR', p.createdAt, 'YYYY-MM'), COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.createdAt >= :since GROUP BY FUNCTION('TO_CHAR', p.createdAt, 'YYYY-MM') ORDER BY 1")
    List<Object[]> sumMonthlyRevenueByStatus(@Param("status") PaymentStatus status, @Param("since") java.time.LocalDateTime since);
}
