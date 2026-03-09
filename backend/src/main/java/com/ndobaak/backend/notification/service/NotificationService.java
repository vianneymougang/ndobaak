package com.ndobaak.backend.notification.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.enums.NotificationChannel;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.notification.entity.Notification;
import com.ndobaak.backend.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
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
}
