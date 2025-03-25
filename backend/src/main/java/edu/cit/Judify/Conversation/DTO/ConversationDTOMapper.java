package edu.cit.Judify.Conversation.DTO;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.User.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConversationDTOMapper {

    public ConversationDTO toDTO(ConversationEntity entity) {
        if (entity == null) {
            return null;
        }

        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(entity.getConversationId());
        dto.setParticipantIds(entity.getParticipants().stream()
                .map(UserEntity::getUserId)
                .collect(Collectors.toSet()));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public ConversationEntity toEntity(ConversationDTO dto, Set<UserEntity> participants) {
        if (dto == null) {
            return null;
        }

        ConversationEntity entity = new ConversationEntity();
        entity.setConversationId(dto.getConversationId());
        entity.setParticipants(participants);
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
} 