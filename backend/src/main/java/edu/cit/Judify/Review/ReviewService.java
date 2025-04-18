package edu.cit.Judify.Review;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public ReviewEntity createReview(ReviewEntity review) {
        // Add validation logic here if needed
        return reviewRepository.save(review);
    }

    public Optional<ReviewEntity> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    public List<ReviewEntity> getTutorReviews(UserEntity tutor) {
        return reviewRepository.findByTutorOrderByCreatedAtDesc(tutor);
    }

    public List<ReviewEntity> getStudentReviews(UserEntity student) {
        return reviewRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    public List<ReviewEntity> getReviewsByRating(Integer rating) {
        return reviewRepository.findByRating(rating);
    }

    public Double getTutorAverageRating(UserEntity tutor) {
        return reviewRepository.findAverageRatingByTutor(tutor);
    }

    @Transactional
    public ReviewEntity updateReview(Long id, ReviewEntity reviewDetails) {
        ReviewEntity review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setRating(reviewDetails.getRating());
        review.setComment(reviewDetails.getComment());

        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }

    // Additional methods for review analytics could be added here
    public Double calculateAverageRating(List<ReviewEntity> reviews) {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double sum = reviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .sum();
        return sum / reviews.size();
    }

    // Paginated version of getTutorReviews with sorting
    public Page<ReviewEntity> getTutorReviewsPaginated(
            UserEntity tutor, String sortBy, String direction, int page, int size) {
        
        Sort sort = direction.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : 
                Sort.by(sortBy).descending();
                
        Pageable pageable = PageRequest.of(page, size, sort);
        return reviewRepository.findByTutor(tutor, pageable);
    }
    
    // Paginated version of getStudentReviews
    public Page<ReviewEntity> getStudentReviewsPaginated(
            UserEntity student, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByStudent(student, pageable);
    }
    
    // Paginated version of getReviewsByRating
    public Page<ReviewEntity> getReviewsByRatingPaginated(
            Integer rating, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByRating(rating, pageable);
    }
} 