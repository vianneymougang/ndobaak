package com.ndobaak.backend.booking.service;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingStateMachineTest {

    private final BookingStateMachine stateMachine = new BookingStateMachine();

    private Booking bookingWithStatus(BookingStatus status) {
        Booking booking = new Booking();
        booking.setStatus(status);
        return booking;
    }

    @Test
    void confirm_fromPending_succeeds() {
        Booking booking = bookingWithStatus(BookingStatus.PENDING);
        stateMachine.confirm(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirm_fromConfirmed_throws() {
        Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> stateMachine.confirm(booking))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("modifiée");
    }

    @Test
    void confirm_fromCancelled_throws() {
        Booking booking = bookingWithStatus(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> stateMachine.confirm(booking))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reject_fromPending_succeeds() {
        Booking booking = bookingWithStatus(BookingStatus.PENDING);
        stateMachine.reject(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void reject_fromConfirmed_throws() {
        Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> stateMachine.reject(booking))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_fromPending_succeeds() {
        Booking booking = bookingWithStatus(BookingStatus.PENDING);
        stateMachine.cancel(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_fromConfirmed_succeeds() {
        Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
        stateMachine.cancel(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_fromCancelled_throws() {
        Booking booking = bookingWithStatus(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> stateMachine.cancel(booking))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_fromCompleted_throws() {
        Booking booking = bookingWithStatus(BookingStatus.COMPLETED);
        assertThatThrownBy(() -> stateMachine.cancel(booking))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void statusIsNotChangedOnInvalidTransition() {
        Booking booking = bookingWithStatus(BookingStatus.CANCELLED);
        try {
            stateMachine.confirm(booking);
        } catch (BusinessException ignored) {
        }
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}
