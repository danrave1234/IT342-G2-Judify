package edu.cit.Judify.Conversation.DTO;

import org.springframework.stereotype.Component;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.User.UserEntity;

@Component
public class ConversationDTOMapper {

    public ConversationDTO toDTO(ConversationEntity entity) {
        if (entity == null) {
            return null;
        }

        ConversationDTO dto = new ConversationDTO();
        dto.setConversationId(entity.getConversationId());
        dto.setUser1Id(entity.getUser1().getUserId());
        dto.setUser2Id(entity.getUser2().getUserId());

        // Set user names with special handling for tutors
        String user1Name = getUserDisplayName(entity.getUser1());
        String user2Name = getUserDisplayName(entity.getUser2());
        dto.setUser1Name(user1Name);
        dto.setUser2Name(user2Name);

        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    /**
     * Get a display name for a user, with special handling for tutors
     * @param user The user entity
     * @return A display name for the user
     */
    private String getUserDisplayName(UserEntity user) {
        // Basic name is first name + last name
        String displayName = user.getFirstName() + " " + user.getLastName();

        // If the user is a tutor and has a tutor profile, enhance the display name
        if (user.getRole() == edu.cit.Judify.User.UserRole.TUTOR && user.getTutorProfile() != null) {
            // If the tutor has expertise, include it in their display name
            if (user.getTutorProfile().getExpertise() != null && !user.getTutorProfile().getExpertise().isEmpty()) {
                displayName = displayName + " (" + user.getTutorProfile().getExpertise() + ")";
            }
        }

        return displayName;
    }

    public ConversationEntity toEntity(ConversationDTO dto, UserEntity user1, UserEntity user2) {
        if (dto == null) {
            return null;
        }

        ConversationEntity entity = new ConversationEntity();
        entity.setConversationId(dto.getConversationId());
        entity.setUser1(user1);
        entity.setUser2(user2);
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
} 
