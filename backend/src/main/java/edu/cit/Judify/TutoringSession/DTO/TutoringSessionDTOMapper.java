package edu.cit.Judify.TutoringSession.DTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.User.UserEntity;
import edu.cit.Judify.User.UserRepository;

@Component
public class TutoringSessionDTOMapper {

    @Autowired
    private UserRepository userRepository;

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
            UserEntity tutor = userRepository.findById(dto.getTutorId())
                .orElseThrow(() -> new IllegalArgumentException("Tutor not found with ID: " + dto.getTutorId()));
            entity.setTutor(tutor);
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
        return entity;
    }
} 