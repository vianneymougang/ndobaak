package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.admin.dto.AdminUserDetailResponse;
import com.ndobaak.backend.admin.dto.AdminUserListResponse;
import com.ndobaak.backend.admin.dto.DeactivateUserRequest;
import com.ndobaak.backend.auth.entity.Admin;
import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.UserStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.notification.service.NotificationService;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminUserService adminUserService;

    private Landlord landlord;
    private Student student;
    private Admin adminUser;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        landlord = Landlord.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .email("jean@example.com")
                .phone("655000001")
                .status(UserStatus.ACTIVE)
                .build();
        landlord.setId(1L);

        student = Student.builder()
                .firstName("Marie")
                .lastName("Curie")
                .email("marie@example.com")
                .phone("655000002")
                .status(UserStatus.ACTIVE)
                .build();
        student.setId(2L);

        adminUser = Admin.builder()
                .firstName("Root")
                .lastName("Admin")
                .email("admin@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        adminUser.setId(99L);

        pageable = PageRequest.of(0, 20);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersShouldReturnPageWithLandlordListingCount() {
        Page<User> userPage = new PageImpl<>(List.of(landlord));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(listingRepository.countByLandlordId(1L)).thenReturn(3L);

        Page<AdminUserListResponse> result = adminUserService.listUsers(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getListingCount()).isEqualTo(3L);
        verify(listingRepository).countByLandlordId(1L);
        verify(bookingRepository, never()).countByStudentId(anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersShouldReturnPageWithStudentBookingCount() {
        Page<User> userPage = new PageImpl<>(List.of(student));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(bookingRepository.countByStudentId(2L)).thenReturn(5L);

        Page<AdminUserListResponse> result = adminUserService.listUsers(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBookingCount()).isEqualTo(5L);
        verify(bookingRepository).countByStudentId(2L);
        verify(listingRepository, never()).countByLandlordId(anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsersShouldDelegateFiltersToRepository() {
        Page<User> userPage = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

        adminUserService.listUsers("LANDLORD", "ACTIVE", "jean", pageable);

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getUserDetailShouldReturnLandlordDetailWithRevenueAndListingCount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(listingRepository.countByLandlordId(1L)).thenReturn(4L);
        when(paymentRepository.sumAmountByLandlordIdAndStatus(1L, PaymentStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(150000));
        when(reviewRepository.countByStudentId(1L)).thenReturn(0L);

        AdminUserDetailResponse result = adminUserService.getUserDetail(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getListingCount()).isEqualTo(4L);
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(150000));
    }

    @Test
    void getUserDetailShouldReturnZeroRevenueWhenPaymentReturnsNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(listingRepository.countByLandlordId(1L)).thenReturn(2L);
        when(paymentRepository.sumAmountByLandlordIdAndStatus(1L, PaymentStatus.COMPLETED)).thenReturn(null);
        when(reviewRepository.countByStudentId(1L)).thenReturn(0L);

        AdminUserDetailResponse result = adminUserService.getUserDetail(1L);

        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getUserDetailShouldReturnStudentDetailWithBookingCounts() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(bookingRepository.countByStudentId(2L)).thenReturn(7L);
        when(bookingRepository.countByStudentIdAndStatus(2L, BookingStatus.COMPLETED)).thenReturn(3L);
        when(reviewRepository.countByStudentId(2L)).thenReturn(2L);

        AdminUserDetailResponse result = adminUserService.getUserDetail(2L);

        assertThat(result.getBookingCount()).isEqualTo(7L);
        assertThat(result.getCompletedBookingCount()).isEqualTo(3L);
        assertThat(result.getReviewCount()).isEqualTo(2L);
    }

    @Test
    void getUserDetailNotFoundShouldThrow() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserDetail(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivateUserShouldSetDeactivatedAndNotify() {
        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        DeactivateUserRequest request = new DeactivateUserRequest();
        request.setReason("Violation des conditions d'utilisation");

        adminUserService.deactivateUser(1L, request);

        verify(userRepository).save(argThat(u -> u.isDeactivated()
                && "Violation des conditions d'utilisation".equals(u.getDeactivationReason())));
        verify(notificationService).createNotification(
                eq(1L), eq(NotificationType.ACCOUNT_DEACTIVATED),
                anyString(), anyString(), eq(1L), eq("USER"));
    }

    @Test
    void deactivateUserAlreadyDeactivatedShouldThrow() {
        landlord.setDeactivated(true);
        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));

        DeactivateUserRequest request = new DeactivateUserRequest();
        request.setReason("Test");

        assertThatThrownBy(() -> adminUserService.deactivateUser(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà désactivé");
    }

    @Test
    void deactivateAdminUserShouldThrow() {
        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        DeactivateUserRequest request = new DeactivateUserRequest();
        request.setReason("Test");

        assertThatThrownBy(() -> adminUserService.deactivateUser(99L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("administrateur");
    }

    @Test
    void reactivateUserShouldClearDeactivationFields() {
        landlord.setDeactivated(true);
        landlord.setDeactivationReason("Violation");
        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        adminUserService.reactivateUser(1L);

        verify(userRepository).save(argThat(u -> !u.isDeactivated()
                && u.getDeactivationReason() == null));
    }

    @Test
    void reactivateUserNotDeactivatedShouldThrow() {
        when(authService.getAuthenticatedUser()).thenReturn(adminUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));

        assertThatThrownBy(() -> adminUserService.reactivateUser(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pas désactivé");
    }
}
