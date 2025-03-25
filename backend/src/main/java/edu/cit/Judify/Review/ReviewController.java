package edu.cit.Judify.Review;

import edu.cit.Judify.Review.DTO.ReviewDTO;
import edu.cit.Judify.Review.DTO.ReviewDTOMapper;
import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewDTOMapper reviewDTOMapper;

    @Autowired
    public ReviewController(ReviewService reviewService, ReviewDTOMapper reviewDTOMapper) {
        this.reviewService = reviewService;
        this.reviewDTOMapper = reviewDTOMapper;
    }

    @PostMapping("/createReview")
    public ResponseEntity<ReviewDTO> createReview(@RequestBody ReviewDTO reviewDTO) {
        ReviewEntity review = reviewDTOMapper.toEntity(reviewDTO);
        return ResponseEntity.ok(reviewDTOMapper.toDTO(reviewService.createReview(review)));
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(reviewDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/findByTutor/{tutorId}")
    public ResponseEntity<List<ReviewDTO>> getTutorReviews(@PathVariable UserEntity tutor) {
        return ResponseEntity.ok(reviewService.getTutorReviews(tutor)
                .stream()
                .map(reviewDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByStudent/{learnerId}")
    public ResponseEntity<List<ReviewDTO>> getLearnerReviews(@PathVariable UserEntity learner) {
        return ResponseEntity.ok(reviewService.getStudentReviews(learner)
                .stream()
                .map(reviewDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/findByRating/{rating}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByRating(@PathVariable Integer rating) {
        return ResponseEntity.ok(reviewService.getReviewsByRating(rating)
                .stream()
                .map(reviewDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/calculateAverageForTutor/{tutorId}")
    public ResponseEntity<Double> getTutorAverageRating(@PathVariable UserEntity tutor) {
        return ResponseEntity.ok(reviewService.getTutorAverageRating(tutor));
    }

    @PutMapping("/updateReview/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long id,
            @RequestBody ReviewDTO reviewDTO) {
        ReviewEntity reviewDetails = reviewDTOMapper.toEntity(reviewDTO);
        return ResponseEntity.ok(reviewDTOMapper.toDTO(
                reviewService.updateReview(id, reviewDetails)));
    }

    @DeleteMapping("/deleteReview/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/calculateAverage")
    public ResponseEntity<Double> calculateAverageRating(@RequestBody List<ReviewDTO> reviewDTOs) {
        List<ReviewEntity> reviews = reviewDTOs.stream()
                .map(reviewDTOMapper::toEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reviewService.calculateAverageRating(reviews));
    }

    @GetMapping("/findByTutorSorted/{tutorId}")
    public ResponseEntity<Page<ReviewDTO>> getTutorReviewsSorted(
            @PathVariable UserEntity tutor,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ReviewEntity> reviews = reviewService.getTutorReviewsPaginated(
                tutor, sortBy, direction, page, size);
                
        Page<ReviewDTO> reviewDTOs = reviews.map(reviewDTOMapper::toDTO);
        return ResponseEntity.ok(reviewDTOs);
    }
    
    @GetMapping("/findByStudentPaginated/{learnerId}")
    public ResponseEntity<Page<ReviewDTO>> getStudentReviewsPaginated(
            @PathVariable UserEntity learner,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ReviewEntity> reviews = reviewService.getStudentReviewsPaginated(
                learner, page, size);
                
        Page<ReviewDTO> reviewDTOs = reviews.map(reviewDTOMapper::toDTO);
        return ResponseEntity.ok(reviewDTOs);
    }
    
    @GetMapping("/findByRatingPaginated/{rating}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByRatingPaginated(
            @PathVariable Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ReviewEntity> reviews = reviewService.getReviewsByRatingPaginated(
                rating, page, size);
                
        Page<ReviewDTO> reviewDTOs = reviews.map(reviewDTOMapper::toDTO);
        return ResponseEntity.ok(reviewDTOs);
    }
} 