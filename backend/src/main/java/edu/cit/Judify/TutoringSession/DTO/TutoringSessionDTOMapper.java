package edu.cit.Judify.TutoringSession.DTO;

import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import org.springframework.stereotype.Component;

@Component
public class TutoringSessionDTOMapper {

    public TutoringSessionDTO toDTO(TutoringSessionEntity entity) {
        if (entity == null) {
            return null;
        }

        TutoringSessionDTO dto = new TutoringSessionDTO();
        dto.setSessionId(entity.getSessionId());
        dto.setTutorId(entity.getTutor().getUserId());
        dto.setStudentId(entity.getStudent().getUserId());
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
        // Note: Tutor and Student should be set separately as they require UserEntity objects
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