package com.ndobaak.backend.subscription.repository;

import com.ndobaak.backend.common.enums.SubscriptionStatus;
import com.ndobaak.backend.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByLandlordId(Long landlordId);

    Optional<Subscription> findByLandlordIdAndStatus(Long landlordId, SubscriptionStatus status);

    boolean existsByLandlordIdAndStatus(Long landlordId, SubscriptionStatus status);

    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDate date);

    List<Subscription> findByStatusAndEndDateBetween(SubscriptionStatus status, LocalDate from, LocalDate to);

    Optional<Subscription> findByLastPaymentId(Long paymentId);

    long countByStatus(SubscriptionStatus status);
}
