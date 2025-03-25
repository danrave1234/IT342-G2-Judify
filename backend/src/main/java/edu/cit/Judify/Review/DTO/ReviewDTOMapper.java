package edu.cit.Judify.Review.DTO;

import edu.cit.Judify.Review.ReviewEntity;
import org.springframework.stereotype.Component;

@Component
public class ReviewDTOMapper {

    public ReviewDTO toDTO(ReviewEntity entity) {
        if (entity == null) {
            return null;
        }

        ReviewDTO dto = new ReviewDTO();
        dto.setReviewId(entity.getReviewId());
        dto.setTutorId(entity.getTutor().getUserId());
        dto.setStudentId(entity.getStudent().getUserId());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public ReviewEntity toEntity(ReviewDTO dto) {
        if (dto == null) {
            return null;
        }

        ReviewEntity entity = new ReviewEntity();
        entity.setReviewId(dto.getReviewId());
        // Note: Tutor and Student should be set separately as they require UserEntity objects
        entity.setRating(dto.getRating());
        entity.setComment(dto.getComment());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }
} 