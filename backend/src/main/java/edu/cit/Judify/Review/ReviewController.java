package edu.cit.Judify.Review;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.Judify.Review.DTO.ReviewDTO;
import edu.cit.Judify.Review.DTO.ReviewDTOMapper;
import edu.cit.Judify.User.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
@Tag(name = "Reviews", description = "Review management endpoints")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewDTOMapper reviewDTOMapper;

    @Autowired
    public ReviewController(ReviewService reviewService, ReviewDTOMapper reviewDTOMapper) {
        this.reviewService = reviewService;
        this.reviewDTOMapper = reviewDTOMapper;
    }

    @Operation(summary = "Create a new review", description = "Creates a new review for a tutoring session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/createReview")
    public ResponseEntity<ReviewDTO> createReview(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Review data to create", required = true)
            @RequestBody ReviewDTO reviewDTO) {
        ReviewEntity review = reviewDTOMapper.toEntity(reviewDTO);
        return ResponseEntity.ok(reviewDTOMapper.toDTO(reviewService.createReview(review)));
    }

    @Operation(summary = "Get review by ID", description = "Returns a review by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the review"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @GetMapping("/findById/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(
            @Parameter(description = "Review ID") @PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(reviewDTOMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get reviews by user", description = "Returns all reviews for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user reviews")
    })
    @GetMapping("/findByUser/{userId}")
    public ResponseEntity<List<ReviewDTO>> getUserReviews(
            @Parameter(description = "User ID") @PathVariable UserEntity user) {
        return ResponseEntity.ok(reviewService.getTutorReviews(user)
                .stream()
                .map(reviewDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get reviews by student", description = "Returns all reviews submitted by a specific student")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved student reviews")
    })
    @GetMapping("/findByStudent/{learnerId}")
    public ResponseEntity<List<ReviewDTO>> getLearnerReviews(
            @Parameter(description = "Student ID") @PathVariable UserEntity learner) {
        return ResponseEntity.ok(reviewService.getStudentReviews(learner)
                .stream()
                .map(reviewDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get reviews by rating", description = "Returns all reviews with a specific rating")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved reviews by rating")
    })
    @GetMapping("/findByRating/{rating}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByRating(
            @Parameter(description = "Rating value (1-5)") @PathVariable Integer rating) {
        return ResponseEntity.ok(reviewService.getReviewsByRating(rating)
                .stream()
                .map(reviewDTOMapper::toDTO)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Calculate average rating for user", description = "Returns the average rating for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully calculated average rating")
    })
    @GetMapping("/calculateAverageForUser/{userId}")
    public ResponseEntity<Double> getUserAverageRating(
            @Parameter(description = "User ID") @PathVariable UserEntity user) {
        return ResponseEntity.ok(reviewService.getTutorAverageRating(user));
    }

    @Operation(summary = "Update a review", description = "Updates an existing review")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review successfully updated"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PutMapping("/updateReview/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
            @Parameter(description = "Review ID") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated review data", required = true)
            @RequestBody ReviewDTO reviewDTO) {
        ReviewEntity reviewDetails = reviewDTOMapper.toEntity(reviewDTO);
        return ResponseEntity.ok(reviewDTOMapper.toDTO(
                reviewService.updateReview(id, reviewDetails)));
    }

    @Operation(summary = "Delete a review", description = "Deletes a review by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @DeleteMapping("/deleteReview/{id}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Review ID") @PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Calculate average rating from a list of reviews", description = "Returns the average rating from a list of reviews")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully calculated average rating")
    })
    @GetMapping("/calculateAverage")
    public ResponseEntity<Double> calculateAverageRating(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of reviews to calculate average from", required = true)
            @RequestBody List<ReviewDTO> reviewDTOs) {
        List<ReviewEntity> reviews = reviewDTOs.stream()
                .map(reviewDTOMapper::toEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reviewService.calculateAverageRating(reviews));
    }

    @Operation(summary = "Get sorted user reviews with pagination", description = "Returns a paginated and sorted list of reviews for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved sorted user reviews")
    })
    @GetMapping("/findByUserSorted/{userId}")
    public ResponseEntity<Page<ReviewDTO>> getUserReviewsSorted(
            @Parameter(description = "User ID") @PathVariable UserEntity user,
            @Parameter(description = "Field to sort by (e.g., createdAt, rating)") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "DESC") String direction,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        Page<ReviewEntity> reviews = reviewService.getTutorReviewsPaginated(
                user, sortBy, direction, page, size);
                
        Page<ReviewDTO> reviewDTOs = reviews.map(reviewDTOMapper::toDTO);
        return ResponseEntity.ok(reviewDTOs);
    }
    
    @Operation(summary = "Get student reviews with pagination", description = "Returns a paginated list of reviews submitted by a specific student")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated student reviews")
    })
    @GetMapping("/findByStudentPaginated/{learnerId}")
    public ResponseEntity<Page<ReviewDTO>> getStudentReviewsPaginated(
            @Parameter(description = "Student ID") @PathVariable UserEntity learner,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        Page<ReviewEntity> reviews = reviewService.getStudentReviewsPaginated(
                learner, page, size);
                
        Page<ReviewDTO> reviewDTOs = reviews.map(reviewDTOMapper::toDTO);
        return ResponseEntity.ok(reviewDTOs);
    }
    
    @Operation(summary = "Get reviews by rating with pagination", description = "Returns a paginated list of reviews with a specific rating")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated reviews by rating")
    })
    @GetMapping("/findByRatingPaginated/{rating}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByRatingPaginated(
            @Parameter(description = "Rating value (1-5)") @PathVariable Integer rating,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        Page<ReviewEntity> reviews = reviewService.getReviewsByRatingPaginated(
                rating, page, size);
                
        Page<ReviewDTO> reviewDTOs = reviews.map(reviewDTOMapper::toDTO);
        return ResponseEntity.ok(reviewDTOs);
    }
} 