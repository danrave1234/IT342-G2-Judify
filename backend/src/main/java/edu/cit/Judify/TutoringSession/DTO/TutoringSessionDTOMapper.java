package edu.cit.Judify.TutoringSession.DTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.Conversation.ConversationRepository;
import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.TutorProfile.TutorProfileRepository;
import edu.cit.Judify.TutorProfile.TutorProfileService;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;

@Component
public class TutoringSessionDTOMapper {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private TutorProfileService tutorProfileService;

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    public TutoringSessionDTO toDTO(TutoringSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        TutoringSessionDTO dto = new TutoringSessionDTO();
        dto.setSessionId(entity.getSessionId());
        dto.setTutorId(entity.getTutor() != null ? entity.getTutor().getUserId() : null);
        dto.setStudentId(entity.getStudent() != null ? entity.getStudent().getUserId() : null);
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setSubject(entity.getSubject());
        dto.setStatus(entity.getStatus());
        dto.setPrice(entity.getPrice());
        dto.setNotes(entity.getNotes());
        dto.setLocationData(entity.getLocationData());
        dto.setMeetingLink(entity.getMeetingLink());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setTutorAccepted(entity.getTutorAccepted());
        dto.setStudentAccepted(entity.getStudentAccepted());

        // Set conversation ID if conversation exists
        if (entity.getConversation() != null) {
            dto.setConversationId(entity.getConversation().getConversationId());
        }

        // Set tutor name if tutor exists
        if (entity.getTutor() != null) {
            // Special case for tutor with ID 2
            if (entity.getTutor().getUserId() != null && entity.getTutor().getUserId() == 2) {
                dto.setTutorName("Danrave Tutor");
            } else {
                String firstName = entity.getTutor().getFirstName() != null ? entity.getTutor().getFirstName().trim() : "";
                String lastName = entity.getTutor().getLastName() != null ? entity.getTutor().getLastName().trim() : "";
                String fullName = (firstName + " " + lastName).trim();

                // If the name is empty or just "Tutor User", try to use username or email as fallback
                if (fullName.isEmpty() || fullName.equals("Tutor User")) {
                    if (entity.getTutor().getUsername() != null && !entity.getTutor().getUsername().isEmpty()) {
                        fullName = entity.getTutor().getUsername();
                    } else if (entity.getTutor().getEmail() != null && !entity.getTutor().getEmail().isEmpty()) {
                        // Use email but remove domain part for privacy
                        String email = entity.getTutor().getEmail();
                        fullName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
                    } else {
                        fullName = "Tutor #" + entity.getTutor().getUserId();
                    }
                }

                dto.setTutorName(fullName);
            }
        }

        // Set student name if student exists
        if (entity.getStudent() != null) {
            String firstName = entity.getStudent().getFirstName() != null ? entity.getStudent().getFirstName().trim() : "";
            String lastName = entity.getStudent().getLastName() != null ? entity.getStudent().getLastName().trim() : "";
            String fullName = (firstName + " " + lastName).trim();

            // If the name is empty, try to use username or email as fallback
            if (fullName.isEmpty()) {
                if (entity.getStudent().getUsername() != null && !entity.getStudent().getUsername().isEmpty()) {
                    fullName = entity.getStudent().getUsername();
                } else if (entity.getStudent().getEmail() != null && !entity.getStudent().getEmail().isEmpty()) {
                    // Use email but remove domain part for privacy
                    String email = entity.getStudent().getEmail();
                    fullName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
                } else {
                    fullName = "Student #" + entity.getStudent().getUserId();
                }
            }

            dto.setStudentName(fullName);
        }

        return dto;
    }

    public TutoringSessionEntity toEntity(TutoringSessionDTO dto) {
        if (dto == null) {
            return null;
        }

        TutoringSessionEntity entity = new TutoringSessionEntity();
        entity.setSessionId(dto.getSessionId());

        // Set Tutor and Student entities from IDs
        if (dto.getTutorId() != null) {
            try {
                // First try to find the user directly (assuming tutorId is a userId)
                UserEntity tutor = userRepository.findById(dto.getTutorId()).orElse(null);

                // If not found, it might be a tutorProfileId instead of a userId
                if (tutor == null) {
                    System.out.println("User not found with ID: " + dto.getTutorId() + ". Trying to convert from tutorProfileId to userId...");

                    try {
                        // Try to get the userId from the tutorId
                        Long tutorUserId = tutorProfileService.getUserIdFromTutorId(dto.getTutorId());
                        System.out.println("Converted tutorProfileId " + dto.getTutorId() + " to userId " + tutorUserId);

                        // Now try to find the user with the converted ID
                        tutor = userRepository.findById(tutorUserId)
                            .orElseThrow(() -> new IllegalArgumentException("Tutor user not found with converted ID: " + tutorUserId));
                    } catch (Exception e) {
                        System.out.println("Error converting tutorProfileId to userId: " + e.getMessage());
                        throw new IllegalArgumentException("Failed to find tutor with ID: " + dto.getTutorId() + ". Error: " + e.getMessage());
                    }
                }

                entity.setTutor(tutor);
            } catch (Exception e) {
                System.out.println("Error setting tutor: " + e.getMessage());
                throw new IllegalArgumentException("Tutor not found with ID: " + dto.getTutorId() + ". Error: " + e.getMessage());
            }
        }

        if (dto.getStudentId() != null) {
            UserEntity student = userRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + dto.getStudentId()));
            entity.setStudent(student);
        } else {
            throw new IllegalArgumentException("Student ID must not be null");
        }

        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setSubject(dto.getSubject());
        entity.setStatus(dto.getStatus());
        entity.setPrice(dto.getPrice());
        entity.setNotes(dto.getNotes());
        entity.setLocationData(dto.getLocationData());
        entity.setMeetingLink(dto.getMeetingLink());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setTutorAccepted(dto.getTutorAccepted());
        entity.setStudentAccepted(dto.getStudentAccepted());

        // Set conversation if conversationId exists
        if (dto.getConversationId() != null) {
            ConversationEntity conversation = conversationRepository.findById(dto.getConversationId())
                .orElse(null);
            entity.setConversation(conversation);
        }

        return entity;
    }
} 
