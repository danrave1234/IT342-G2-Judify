package edu.cit.Judify.Review;

import edu.cit.Judify.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByTutorOrderByCreatedAtDesc(UserEntity tutor);
    List<ReviewEntity> findByStudentOrderByCreatedAtDesc(UserEntity student);
    List<ReviewEntity> findByRating(Integer rating);
    
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.tutor = :tutor")
    Double findAverageRatingByTutor(@Param("tutor") UserEntity tutor);
} 