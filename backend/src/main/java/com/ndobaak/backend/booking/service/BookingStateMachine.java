package com.ndobaak.backend.booking.service;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BookingStateMachine {

    public void confirm(Booking booking) {
        requireStatus(booking, Set.of(BookingStatus.PENDING));
        booking.setStatus(BookingStatus.CONFIRMED);
    }

    public void reject(Booking booking) {
        requireStatus(booking, Set.of(BookingStatus.PENDING));
        booking.setStatus(BookingStatus.CANCELLED);
    }

    public void cancel(Booking booking) {
        requireStatus(booking, Set.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        booking.setStatus(BookingStatus.CANCELLED);
    }

    private void requireStatus(Booking booking, Set<BookingStatus> allowed) {
        if (!allowed.contains(booking.getStatus())) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATUS);
        }
    }
}
