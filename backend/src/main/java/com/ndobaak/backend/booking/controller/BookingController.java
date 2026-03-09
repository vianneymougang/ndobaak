package com.ndobaak.backend.booking.controller;

import com.ndobaak.backend.booking.dto.BookingResponse;
import com.ndobaak.backend.booking.dto.BookingStatusUpdateRequest;
import com.ndobaak.backend.booking.dto.CreateBookingRequest;
import com.ndobaak.backend.booking.service.BookingService;
import com.ndobaak.backend.common.enums.BookingStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingService.getStudentBookings(status, pageable));
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<Page<BookingResponse>> getLandlordBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingService.getLandlordBookings(status, pageable));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirmBooking(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long id,
            @RequestBody(required = false) BookingStatusUpdateRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(bookingService.rejectBooking(id, reason));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}
