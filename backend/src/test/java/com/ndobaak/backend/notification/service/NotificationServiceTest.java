package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.NotificationChannel;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.dto.NotificationResponse;
import com.ndobaak.backend.notification.entity.Notification;
import com.ndobaak.backend.notification.mapper.NotificationMapper;
import com.ndobaak.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private static final NotificationResponse DUMMY_RESPONSE = NotificationResponse.builder()
            .id(1L)
            .type(NotificationType.BOOKING_CREATED)
            .title("Nouvelle réservation")
            .message("Une réservation a été créée.")
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();

    private User buildUser(Long id) {
        Student user = new Student();
        user.setId(id);
        user.setEmail("user@test.com");
        return user;
    }

    private Notification buildNotification(Long id, User user) {
        return Notification.builder()
                .id(id)
                .user(user)
                .type(NotificationType.BOOKING_CREATED)
                .title("Nouvelle réservation")
                .message("Une réservation a été créée.")
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createNotificationShouldSaveNotification() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        notificationService.createNotification(1L, NotificationType.BOOKING_CREATED,
                "Nouvelle réservation", "Une réservation a été créée.", 10L, "BOOKING");

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotificationShouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.createNotification(999L,
                NotificationType.BOOKING_CREATED, "Test", "Test", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserNotificationsShouldReturnPaginatedResults() {
        User user = buildUser(1L);
        Notification notification = buildNotification(1L, user);
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(page);
        when(notificationMapper.toResponse(notification)).thenReturn(DUMMY_RESPONSE);

        Page<NotificationResponse> result = notificationService.getUserNotifications(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getType()).isEqualTo(NotificationType.BOOKING_CREATED);
    }

    @Test
    void getUnreadCountShouldReturnCount() {
        User user = buildUser(1L);
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount();

        assertThat(count).isEqualTo(5);
    }

    @Test
    void markAsReadShouldUpdateNotification() {
        User user = buildUser(1L);
        Notification notification = buildNotification(1L, user);

        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadShouldThrowWhenNotificationNotFound() {
        User user = buildUser(1L);
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsReadShouldThrowWhenNotOwner() {
        User currentUser = buildUser(1L);
        User otherUser = buildUser(2L);
        Notification notification = buildNotification(1L, otherUser);

        when(authService.getAuthenticatedUser()).thenReturn(currentUser);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void markAllAsReadShouldCallRepository() {
        User user = buildUser(1L);
        when(authService.getAuthenticatedUser()).thenReturn(user);

        notificationService.markAllAsRead();

        verify(notificationRepository).markAllAsReadByUserId(1L);
    }
}
