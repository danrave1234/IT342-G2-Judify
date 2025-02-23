package edu.cit.Storix.Review;

import edu.cit.Storix.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByTutorOrderByCreatedAtDesc(UserEntity tutor);
    List<ReviewEntity> findByLearnerOrderByCreatedAtDesc(UserEntity learner);
    List<ReviewEntity> findByRating(Integer rating);
    Double findAverageRatingByTutor(UserEntity tutor);
} 