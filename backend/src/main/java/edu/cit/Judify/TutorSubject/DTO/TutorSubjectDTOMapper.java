package edu.cit.Judify.TutorSubject.DTO;

import edu.cit.Judify.TutorSubject.TutorSubjectEntity;
import org.springframework.stereotype.Component;

@Component
public class TutorSubjectDTOMapper {

    public TutorSubjectDTO toDTO(TutorSubjectEntity entity) {
        if (entity == null) {
            return null;
        }

        TutorSubjectDTO dto = new TutorSubjectDTO();
        dto.setId(entity.getId());
        dto.setTutorProfileId(entity.getTutorProfile().getId());
        dto.setSubject(entity.getSubject());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public TutorSubjectEntity toEntity(TutorSubjectDTO dto) {
        if (dto == null) {
            return null;
        }

        TutorSubjectEntity entity = new TutorSubjectEntity();
        entity.setId(dto.getId());
        entity.setSubject(dto.getSubject());
        entity.setCreatedAt(dto.getCreatedAt());
        // Note: tutorProfile must be set separately
        return entity;
    }
} 