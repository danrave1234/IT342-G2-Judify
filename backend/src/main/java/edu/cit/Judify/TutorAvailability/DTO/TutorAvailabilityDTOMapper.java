package edu.cit.Judify.TutorAvailability.DTO;

import edu.cit.Judify.TutorAvailability.TutorAvailabilityEntity;
import edu.cit.Judify.User.UserEntity;
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

        // Set tutor if tutorId is provided
        if (dto.getTutorId() != null) {
            UserEntity tutor = new UserEntity();
            tutor.setUserId(dto.getTutorId());
            entity.setTutor(tutor);
        }

        entity.setDayOfWeek(dto.getDayOfWeek());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setAdditionalNotes(dto.getAdditionalNotes());
        return entity;
    }
} 
