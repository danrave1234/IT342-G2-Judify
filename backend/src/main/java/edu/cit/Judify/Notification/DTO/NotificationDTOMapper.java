package edu.cit.Judify.Notification.DTO;

import edu.cit.Judify.Notification.NotificationEntity;
import edu.cit.Judify.User.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationDTOMapper {

    public NotificationDTO toDTO(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }

        NotificationDTO dto = new NotificationDTO();
        dto.setNotificationId(entity.getNotificationId());
        dto.setUserId(entity.getUser().getUserId());
        dto.setType(entity.getType());
        dto.setContent(entity.getContent());
        dto.setIsRead(entity.getIsRead());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public NotificationEntity toEntity(NotificationDTO dto, UserEntity user) {
        if (dto == null) {
            return null;
        }

        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId(dto.getNotificationId());
        entity.setUser(user);
        entity.setType(dto.getType());
        entity.setContent(dto.getContent());
        entity.setIsRead(dto.getIsRead());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }
} 