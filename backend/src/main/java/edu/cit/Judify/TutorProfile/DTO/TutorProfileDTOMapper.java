package edu.cit.Judify.TutorProfile.DTO;

import edu.cit.Judify.TutorProfile.TutorProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class TutorProfileDTOMapper {

    public TutorProfileDTO toDTO(TutorProfileEntity entity) {
        if (entity == null) {
            return null;
        }

        TutorProfileDTO dto = new TutorProfileDTO();
        dto.setProfileId(entity.getId());
        dto.setUserId(entity.getUser().getUserId());
        dto.setUsername(entity.getUser().getUsername());
        dto.setBio(entity.getBiography());
        dto.setExpertise(entity.getExpertise());
        dto.setHourlyRate(entity.getHourlyRate());
        dto.setSubjects(entity.getSubjects());
        dto.setRating(entity.getRating());
        dto.setTotalReviews(entity.getTotalReviews());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public TutorProfileEntity toEntity(TutorProfileDTO dto) {
        if (dto == null) {
            return null;
        }

        TutorProfileEntity entity = new TutorProfileEntity();
        entity.setId(dto.getProfileId());
        entity.setBiography(dto.getBio());
        entity.setExpertise(dto.getExpertise());
        entity.setHourlyRate(dto.getHourlyRate());
        entity.setSubjects(dto.getSubjects());
        entity.setRating(dto.getRating());
        entity.setTotalReviews(dto.getTotalReviews());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }
} 