package edu.cit.Storix.Review;

import edu.cit.Storix.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewEntity> createReview(@RequestBody ReviewEntity review) {
        return ResponseEntity.ok(reviewService.createReview(review));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewEntity> getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<ReviewEntity>> getTutorReviews(@PathVariable UserEntity tutor) {
        return ResponseEntity.ok(reviewService.getTutorReviews(tutor));
    }

    @GetMapping("/learner/{learnerId}")
    public ResponseEntity<List<ReviewEntity>> getLearnerReviews(@PathVariable UserEntity learner) {
        return ResponseEntity.ok(reviewService.getLearnerReviews(learner));
    }

    @GetMapping("/rating/{rating}")
    public ResponseEntity<List<ReviewEntity>> getReviewsByRating(@PathVariable Integer rating) {
        return ResponseEntity.ok(reviewService.getReviewsByRating(rating));
    }

    @GetMapping("/tutor/{tutorId}/average")
    public ResponseEntity<Double> getTutorAverageRating(@PathVariable UserEntity tutor) {
        return ResponseEntity.ok(reviewService.getTutorAverageRating(tutor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewEntity> updateReview(
            @PathVariable Long id,
            @RequestBody ReviewEntity reviewDetails) {
        return ResponseEntity.ok(reviewService.updateReview(id, reviewDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/calculate-average")
    public ResponseEntity<Double> calculateAverageRating(@RequestBody List<ReviewEntity> reviews) {
        return ResponseEntity.ok(reviewService.calculateAverageRating(reviews));
    }
} 