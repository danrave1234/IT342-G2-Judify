package edu.cit.Judify.TutorAvailability;

import edu.cit.Judify.User.UserEntity;
import jakarta.persistence.*;

/** Explanation why this entity exists (based ra gihapon nis docs)
 * TutorAvailabilityEntity represents individual availability slots for a tutor.
 *
 * Why this class exists:
 * 1. Normalization: Stores each availability slot as a separate record, making it easy to query and update.
 * 2. Flexibility: Allows for detailed scheduling (e.g., recurring sessions, blackout dates) without modifying the core tutor profile.
 * 3. Optionality: Not all tutors require explicit scheduling on the platform; they can opt-in to use this functionality.
 */

@Entity
@Table(name = "tutor_availabilities")
public class TutorAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long availabilityId;

    // Many availability slots can be linked to a tutor (referenced via UserEntity)
    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private UserEntity tutor;

    @Column(nullable = false)
    private String dayOfWeek;

    @Column(nullable = false)
    private String startTime;

    @Column(nullable = false)
    private String endTime;

    @Column(length = 1000)
    private String additionalNotes;

    // Constructors
    public TutorAvailabilityEntity() {
    }

    // Getters and Setters

    public Long getAvailabilityId() {
        return availabilityId;
    }
    public void setAvailabilityId(Long availabilityId) {
        this.availabilityId = availabilityId;
    }

    public UserEntity getTutor() {
        return tutor;
    }
    public void setTutor(UserEntity tutor) {
        this.tutor = tutor;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }
    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }
}
