package edu.cit.Storix.Notification;

import edu.cit.Storix.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
    List<NotificationEntity> findByUserAndIsReadFalse(UserEntity user);
    List<NotificationEntity> findByType(String type);
} 