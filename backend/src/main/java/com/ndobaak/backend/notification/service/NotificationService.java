package com.ndobaak.backend.notification.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               AuthService authService,
                               NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public void createNotification(Long userId, NotificationType type, String title, String message,
                                   Long referenceId, String referenceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Pageable pageable) {
        User currentUser = authService.getAuthenticatedUser();
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        return notifications.map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User currentUser = authService.getAuthenticatedUser();
        return notificationRepository.countByUserIdAndReadFalse(currentUser.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        User currentUser = authService.getAuthenticatedUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Vous ne pouvez pas modifier les notifications d'un autre utilisateur.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        User currentUser = authService.getAuthenticatedUser();
        notificationRepository.markAllAsReadByUserId(currentUser.getId());
    }
}
