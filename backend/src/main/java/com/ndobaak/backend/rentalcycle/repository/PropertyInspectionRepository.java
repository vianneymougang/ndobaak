package com.ndobaak.backend.rentalcycle.repository;

import com.ndobaak.backend.common.enums.InspectionStatus;
import com.ndobaak.backend.common.enums.InspectionType;
import com.ndobaak.backend.rentalcycle.entity.PropertyInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyInspectionRepository extends JpaRepository<PropertyInspection, Long> {

    List<PropertyInspection> findByBookingId(Long bookingId);

    Optional<PropertyInspection> findByBookingIdAndTypeAndStatus(
            Long bookingId, InspectionType type, InspectionStatus status);

    boolean existsByBookingIdAndType(Long bookingId, InspectionType type);
}
