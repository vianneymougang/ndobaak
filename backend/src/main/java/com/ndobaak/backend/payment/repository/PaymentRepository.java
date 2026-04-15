package com.ndobaak.backend.payment.repository;

import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByBookingId(Long bookingId);

    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByProviderReference(String providerReference);

    Page<Payment> findByBookingStudentId(Long studentId, Pageable pageable);

    Page<Payment> findByBookingListingLandlordId(Long landlordId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.booking.listing.landlord.id = :landlordId AND p.status = :status")
    BigDecimal sumAmountByLandlordIdAndStatus(@Param("landlordId") Long landlordId, @Param("status") PaymentStatus status);


    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT FUNCTION('TO_CHAR', p.createdAt, 'YYYY-MM'), COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.createdAt >= :since GROUP BY FUNCTION('TO_CHAR', p.createdAt, 'YYYY-MM') ORDER BY 1")
    List<Object[]> sumMonthlyRevenueByStatus(@Param("status") PaymentStatus status, @Param("since") java.time.LocalDateTime since);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.booking.listing.landlord.id = :landlordId AND p.status = :status AND p.createdAt BETWEEN :from AND :to")
    BigDecimal sumAmountByLandlordIdAndStatusAndPeriod(@Param("landlordId") Long landlordId,
                                                       @Param("status") PaymentStatus status,
                                                       @Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT TO_CHAR(p.created_at, 'YYYY-MM') AS month,
                   COALESCE(SUM(p.amount), 0)        AS revenue,
                   COUNT(p.id)                        AS booking_count
            FROM payment p
            JOIN booking b ON p.booking_id = b.id
            JOIN listing l ON b.listing_id = l.id
            WHERE l.landlord_id = :landlordId
              AND p.status = 'COMPLETED'
              AND p.created_at >= :from
            GROUP BY TO_CHAR(p.created_at, 'YYYY-MM')
            ORDER BY month ASC
            """, nativeQuery = true)
    List<Object[]> findMonthlyRevenue(@Param("landlordId") Long landlordId,
                                      @Param("from") LocalDateTime from);
}
