package com.ndobaak.backend.booking.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.dto.BookingResponse;
import com.ndobaak.backend.booking.dto.CreateBookingRequest;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.mapper.BookingMapper;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private Student buildStudent(Long id) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEmail("john@test.com");
        return student;
    }

    private Landlord buildLandlord(Long id) {
        Landlord landlord = new Landlord();
        landlord.setId(id);
        landlord.setFirstName("Jane");
        landlord.setLastName("Smith");
        landlord.setEmail("jane@test.com");
        return landlord;
    }

    private Listing buildListing(Long id, Landlord landlord) {
        return Listing.builder()
                .id(id)
                .title("Studio Eyang")
                .monthlyPrice(new BigDecimal("35000"))
                .status(ListingStatus.PUBLISHED)
                .landlord(landlord)
                .build();
    }

    private Booking buildBooking(Long id, Student student, Listing listing, BookingStatus status) {
        Booking booking = Booking.builder()
                .student(student)
                .listing(listing)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 7, 1))
                .monthlyRent(new BigDecimal("35000"))
                .totalAmount(new BigDecimal("105000"))
                .status(status)
                .build();
        booking.setId(id);
        return booking;
    }

    private CreateBookingRequest buildRequest(Long listingId) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setListingId(listingId);
        request.setStartDate(LocalDate.of(2026, 4, 1));
        request.setEndDate(LocalDate.of(2026, 7, 1));
        return request;
    }

    private BookingResponse buildDummyResponse(Long id) {
        return BookingResponse.builder().id(id).build();
    }

    @Test
    void createBookingShouldPersistAndNotifyLandlord() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        CreateBookingRequest request = buildRequest(100L);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByStudentIdAndListingIdAndStatusIn(eq(1L), eq(100L), any()))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(buildDummyResponse(1L));

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response.getId()).isEqualTo(1L);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        Booking saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getMonthlyRent()).isEqualByComparingTo(new BigDecimal("35000"));
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("105000"));

        verify(notificationService).createNotification(eq(10L), any(), anyString(), anyString(), anyLong(), eq("BOOKING"));
    }

    @Test
    void createBookingShouldCalculateMinimumOneMonth() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setListingId(100L);
        request.setStartDate(LocalDate.of(2026, 4, 1));
        request.setEndDate(LocalDate.of(2026, 4, 15));

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByStudentIdAndListingIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any())).thenReturn(buildDummyResponse(1L));

        bookingService.createBooking(request);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(new BigDecimal("35000"));
    }

    @Test
    void createBookingShouldThrowWhenListingNotFound() {
        when(authService.getAuthenticatedUser()).thenReturn(buildStudent(1L));
        when(listingRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        CreateBookingRequest request = buildRequest(999L);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createBookingShouldThrowWhenListingNotPublished() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        listing.setStatus(ListingStatus.DRAFT);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.createBooking(buildRequest(100L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disponible");
    }

    @Test
    void createBookingShouldThrowWhenEndDateBeforeStartDate() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);

        CreateBookingRequest request = new CreateBookingRequest();
        request.setListingId(100L);
        request.setStartDate(LocalDate.of(2026, 7, 1));
        request.setEndDate(LocalDate.of(2026, 4, 1));

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("date de fin");
    }

    @Test
    void createBookingShouldThrowWhenDuplicateActiveBooking() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(listingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(listing));
        when(bookingRepository.existsByStudentIdAndListingIdAndStatusIn(eq(1L), eq(100L), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(buildRequest(100L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("réservation active");
    }

    @Test
    void getBookingByIdShouldReturnWhenAuthorized() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(buildDummyResponse(1L));

        BookingResponse response = bookingService.getBookingById(1L);
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getBookingByIdShouldThrowWhenNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBookingByIdShouldThrowWhenUnauthorized() {
        Student student = buildStudent(1L);
        Student otherStudent = buildStudent(99L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(otherStudent);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingById(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getStudentBookingsShouldReturnAllWhenNoStatusFilter() {
        Student student = buildStudent(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findByStudentId(1L, pageable)).thenReturn(page);

        Page<BookingResponse> result = bookingService.getStudentBookings(null, pageable);

        assertThat(result).isEmpty();
        verify(bookingRepository).findByStudentId(1L, pageable);
        verify(bookingRepository, never()).findByStudentIdAndStatus(anyLong(), any(), any());
    }

    @Test
    void getStudentBookingsShouldFilterByStatus() {
        Student student = buildStudent(1L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findByStudentIdAndStatus(1L, BookingStatus.PENDING, pageable)).thenReturn(page);

        bookingService.getStudentBookings(BookingStatus.PENDING, pageable);

        verify(bookingRepository).findByStudentIdAndStatus(1L, BookingStatus.PENDING, pageable);
    }

    @Test
    void getLandlordBookingsShouldReturnAllWhenNoStatusFilter() {
        Landlord landlord = buildLandlord(10L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(bookingRepository.findByListingLandlordId(10L, pageable)).thenReturn(page);

        bookingService.getLandlordBookings(null, pageable);

        verify(bookingRepository).findByListingLandlordId(10L, pageable);
    }

    @Test
    void getLandlordBookingsShouldFilterByStatus() {
        Landlord landlord = buildLandlord(10L);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(), pageable, 0);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(bookingRepository.findByListingLandlordIdAndStatus(10L, BookingStatus.CONFIRMED, pageable))
                .thenReturn(page);

        bookingService.getLandlordBookings(BookingStatus.CONFIRMED, pageable);

        verify(bookingRepository).findByListingLandlordIdAndStatus(10L, BookingStatus.CONFIRMED, pageable);
    }

    @Test
    void confirmBookingShouldSetStatusConfirmedAndNotifyStudent() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any())).thenReturn(buildDummyResponse(1L));

        bookingService.confirmBooking(1L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getLandlordResponseDate()).isNotNull();
        verify(notificationService).createNotification(eq(1L), any(), anyString(), anyString(), anyLong(), eq("BOOKING"));
    }

    @Test
    void confirmBookingShouldThrowWhenNotLandlord() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Landlord otherLandlord = buildLandlord(99L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(otherLandlord);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void confirmBookingShouldThrowWhenNotPending() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.CONFIRMED);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en attente");
    }

    @Test
    void rejectBookingShouldSetCancelledWithReasonAndNotify() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any())).thenReturn(buildDummyResponse(1L));

        bookingService.rejectBooking(1L, "Logement indisponible");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancellationReason()).isEqualTo("Logement indisponible");
        assertThat(booking.getLandlordResponseDate()).isNotNull();
        verify(notificationService).createNotification(eq(1L), any(), anyString(), anyString(), anyLong(), eq("BOOKING"));
    }

    @Test
    void rejectBookingShouldThrowWhenNotLandlord() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Landlord otherLandlord = buildLandlord(99L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(otherLandlord);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rejectBooking(1L, "raison"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectBookingShouldThrowWhenNotPending() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.CONFIRMED);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.rejectBooking(1L, "raison"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelBookingByStudentShouldNotifyLandlord() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any())).thenReturn(buildDummyResponse(1L));

        bookingService.cancelBooking(1L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(notificationService).createNotification(eq(10L), any(), anyString(), anyString(), anyLong(), eq("BOOKING"));
    }

    @Test
    void cancelBookingByLandlordShouldNotifyStudent() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.CONFIRMED);

        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toResponse(any())).thenReturn(buildDummyResponse(1L));

        bookingService.cancelBooking(1L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(notificationService).createNotification(eq(1L), any(), anyString(), anyString(), anyLong(), eq("BOOKING"));
    }

    @Test
    void cancelBookingShouldThrowWhenCompletedStatus() {
        Student student = buildStudent(1L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.COMPLETED);

        when(authService.getAuthenticatedUser()).thenReturn(student);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en attente ou confirmées");
    }

    @Test
    void cancelBookingShouldThrowWhenUnauthorized() {
        Student student = buildStudent(1L);
        Student otherStudent = buildStudent(99L);
        Landlord landlord = buildLandlord(10L);
        Listing listing = buildListing(100L, landlord);
        Booking booking = buildBooking(1L, student, listing, BookingStatus.PENDING);

        when(authService.getAuthenticatedUser()).thenReturn(otherStudent);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                .isInstanceOf(ForbiddenException.class);
    }
}
