package com.ndobaak.backend.booking.repository;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByStudentId(Long studentId, Pageable pageable);

    Page<Booking> findByListingId(Long listingId, Pageable pageable);

    Page<Booking> findByStudentIdAndStatus(Long studentId, BookingStatus status, Pageable pageable);

    Page<Booking> findByListingIdAndStatus(Long listingId, BookingStatus status, Pageable pageable);

    boolean existsByStudentIdAndListingIdAndStatusIn(Long studentId, Long listingId, List<BookingStatus> statuses);

    Page<Booking> findByListingLandlordId(Long landlordId, Pageable pageable);

    Page<Booking> findByListingLandlordIdAndStatus(Long landlordId, BookingStatus status, Pageable pageable);
}
