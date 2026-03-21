package com.ndobaak.backend.notification.repository;

import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.notification.entity.NotificationTypePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTypePreferenceRepository extends JpaRepository<NotificationTypePreference, Long> {

    List<NotificationTypePreference> findAllByUserId(Long userId);

    Optional<NotificationTypePreference> findByUserIdAndNotificationType(Long userId, NotificationType type);

    void deleteAllByUserId(Long userId);
}
