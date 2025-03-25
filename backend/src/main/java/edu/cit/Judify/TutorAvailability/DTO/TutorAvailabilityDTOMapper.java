package edu.cit.Judify.TutorAvailability.DTO;

import edu.cit.Judify.TutorAvailability.TutorAvailabilityEntity;
import org.springframework.stereotype.Component;

@Component
public class TutorAvailabilityDTOMapper {

    public TutorAvailabilityDTO toDTO(TutorAvailabilityEntity entity) {
        if (entity == null) {
            return null;
        }

        TutorAvailabilityDTO dto = new TutorAvailabilityDTO();
        dto.setAvailabilityId(entity.getAvailabilityId());
        dto.setTutorId(entity.getTutor().getUserId());
        dto.setDayOfWeek(entity.getDayOfWeek());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setAdditionalNotes(entity.getAdditionalNotes());
        return dto;
    }

    public TutorAvailabilityEntity toEntity(TutorAvailabilityDTO dto) {
        if (dto == null) {
            return null;
        }

        TutorAvailabilityEntity entity = new TutorAvailabilityEntity();
        entity.setAvailabilityId(dto.getAvailabilityId());
        // Note: Tutor should be set separately as it requires UserEntity object
        entity.setDayOfWeek(dto.getDayOfWeek());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setAdditionalNotes(dto.getAdditionalNotes());
        return entity;
    }
} 