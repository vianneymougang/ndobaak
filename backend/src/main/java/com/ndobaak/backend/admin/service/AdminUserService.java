package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.admin.dto.AdminUserDetailResponse;
import com.ndobaak.backend.admin.dto.AdminUserListResponse;
import com.ndobaak.backend.admin.dto.DeactivateUserRequest;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.UserRole;
import com.ndobaak.backend.common.enums.UserStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.ReviewRepository;
import com.ndobaak.backend.notification.service.NotificationService;
import com.ndobaak.backend.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final AuthService authService;

    public AdminUserService(UserRepository userRepository,
                            ListingRepository listingRepository,
                            BookingRepository bookingRepository,
                            PaymentRepository paymentRepository,
                            ReviewRepository reviewRepository,
                            NotificationService notificationService,
                            AuthService authService) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserListResponse> listUsers(String roleFilter, String statusFilter,
                                                  String search, Pageable pageable) {
        Specification<User> spec = buildSpec(roleFilter, statusFilter, search);
        return userRepository.findAll(spec, pageable).map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toDetailResponse(user);
    }

    @Transactional
    public void deactivateUser(Long userId, DeactivateUserRequest request) {
        User admin = authService.getAuthenticatedUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.isDeactivated()) {
            throw new BusinessException("Ce compte est déjà désactivé.");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Impossible de désactiver un compte administrateur.");
        }

        user.setDeactivated(true);
        user.setDeactivationReason(request.getReason());
        userRepository.save(user);

        notificationService.createNotification(
                user.getId(),
                NotificationType.ACCOUNT_DEACTIVATED,
                "Compte désactivé",
                "Votre compte a été désactivé. Motif : " + request.getReason(),
                user.getId(),
                "USER"
        );

        log.info("User {} deactivated by admin {}. Reason: {}", userId, admin.getId(), request.getReason());
    }

    @Transactional
    public void reactivateUser(Long userId) {
        User admin = authService.getAuthenticatedUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!user.isDeactivated()) {
            throw new BusinessException("Ce compte n'est pas désactivé.");
        }

        user.setDeactivated(false);
        user.setDeactivationReason(null);
        userRepository.save(user);

        log.info("User {} reactivated by admin {}", userId, admin.getId());
    }

    private Specification<User> buildSpec(String roleFilter, String statusFilter, String search) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (roleFilter != null && !roleFilter.isBlank()) {
                predicates.add(cb.equal(root.type().as(String.class), roleFilter.toUpperCase()));
            }

            if (statusFilter != null && !statusFilter.isBlank()) {
                try {
                    UserStatus status = UserStatus.valueOf(statusFilter.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private AdminUserListResponse toListResponse(User user) {
        AdminUserListResponse.AdminUserListResponseBuilder builder = AdminUserListResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .deactivated(user.isDeactivated())
                .registrationDate(user.getRegistrationDate());

        if (user.getRole() == UserRole.LANDLORD) {
            builder.listingCount(listingRepository.countByLandlordId(user.getId()));
        } else if (user.getRole() == UserRole.STUDENT) {
            builder.bookingCount(bookingRepository.countByStudentId(user.getId()));
        }

        return builder.build();
    }

    private AdminUserDetailResponse toDetailResponse(User user) {
        AdminUserDetailResponse.AdminUserDetailResponseBuilder builder = AdminUserDetailResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .deactivated(user.isDeactivated())
                .deactivationReason(user.getDeactivationReason())
                .registrationDate(user.getRegistrationDate())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .reviewCount(reviewRepository.countByStudentId(user.getId()));

        if (user.getRole() == UserRole.LANDLORD) {
            long listingCount = listingRepository.countByLandlordId(user.getId());
            BigDecimal revenue = paymentRepository.sumAmountByLandlordIdAndStatus(
                    user.getId(), PaymentStatus.COMPLETED);
            builder.listingCount(listingCount)
                   .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO);
        } else if (user.getRole() == UserRole.STUDENT) {
            long bookingCount = bookingRepository.countByStudentId(user.getId());
            long completedCount = bookingRepository.countByStudentIdAndStatus(
                    user.getId(), BookingStatus.COMPLETED);
            builder.bookingCount(bookingCount)
                   .completedBookingCount(completedCount);
        }

        return builder.build();
    }
}
