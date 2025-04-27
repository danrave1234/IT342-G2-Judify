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
        dto.setUser1Id(entity.getStudent().getUserId());
        dto.setUser2Id(entity.getTutor().getUserId());

        // Set student and tutor names with proper display formatting
        String studentName = entity.getStudent().getFirstName() + " " + entity.getStudent().getLastName();
        String tutorName = entity.getTutor().getFirstName() + " " + entity.getTutor().getLastName();
        
        // If the tutor has expertise, include it in the display name
        if (entity.getTutor().getRole() == edu.cit.Judify.User.UserRole.TUTOR && 
            entity.getTutor().getTutorProfile() != null && 
            entity.getTutor().getTutorProfile().getExpertise() != null && 
            !entity.getTutor().getTutorProfile().getExpertise().isEmpty()) {
            tutorName = tutorName + " (" + entity.getTutor().getTutorProfile().getExpertise() + ")";
        }
        
        dto.setUser1Name(studentName);
        dto.setUser2Name(tutorName);
        
        // Log the names to debug
        System.out.println("DEBUG: Mapped conversation with studentName: " + studentName + ", tutorName: " + tutorName);

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

    public ConversationEntity toEntity(ConversationDTO dto, UserEntity student, UserEntity tutor) {
        if (dto == null) {
            return null;
        }

        ConversationEntity entity = new ConversationEntity();
        entity.setConversationId(dto.getConversationId());
        entity.setStudent(student);
        entity.setTutor(tutor);
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
} 
