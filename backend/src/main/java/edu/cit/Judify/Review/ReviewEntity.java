package edu.cit.Judify.Review;

import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.User.UserEntity;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "reviews")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    // One-to-one relationship with a tutoring session
    @OneToOne
    @JoinColumn(name = "session_id", nullable = false)
    private TutoringSessionEntity session;

    // Reviewer (learner) and review target (tutor)
    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private UserEntity tutor;

    @ManyToOne
    @JoinColumn(name = "learner_id", nullable = false)
    private UserEntity learner;

    @Column(nullable = false)
    private Integer rating;  // e.g., 1-5 stars

    @Column(length = 2000)
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    // Constructors
    public ReviewEntity() {
    }

    // Getters and Setters

    public Long getReviewId() {
        return reviewId;
    }
    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public TutoringSessionEntity getSession() {
        return session;
    }
    public void setSession(TutoringSessionEntity session) {
        this.session = session;
    }

    public UserEntity getTutor() {
        return tutor;
    }
    public void setTutor(UserEntity tutor) {
        this.tutor = tutor;
    }

    public UserEntity getLearner() {
        return learner;
    }
    public void setLearner(UserEntity learner) {
        this.learner = learner;
    }

    public Integer getRating() {
        return rating;
    }
    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
