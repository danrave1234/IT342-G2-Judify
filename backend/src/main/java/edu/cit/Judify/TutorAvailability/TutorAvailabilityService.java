package edu.cit.Judify.TutorAvailability;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TutorAvailabilityService {

    private final TutorAvailabilityRepository availabilityRepository;

    @Autowired
    public TutorAvailabilityService(TutorAvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional
    public TutorAvailabilityEntity createAvailability(TutorAvailabilityEntity availability) {
        // Add validation logic here if needed
        return availabilityRepository.save(availability);
    }

    public Optional<TutorAvailabilityEntity> getAvailabilityById(Long id) {
        return availabilityRepository.findById(id);
    }

    public List<TutorAvailabilityEntity> getTutorAvailability(UserEntity tutor) {
        return availabilityRepository.findByTutor(tutor);
    }

    public List<TutorAvailabilityEntity> getAvailabilityByDay(String dayOfWeek) {
        return availabilityRepository.findByDayOfWeek(dayOfWeek);
    }

    public List<TutorAvailabilityEntity> getTutorAvailabilityByDay(UserEntity tutor, String dayOfWeek) {
        return availabilityRepository.findByTutorAndDayOfWeek(tutor, dayOfWeek);
    }

    @Transactional
    public TutorAvailabilityEntity updateAvailability(Long id, TutorAvailabilityEntity availabilityDetails) {
        TutorAvailabilityEntity availability = availabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Availability slot not found"));

        availability.setDayOfWeek(availabilityDetails.getDayOfWeek());
        availability.setStartTime(availabilityDetails.getStartTime());
        availability.setEndTime(availabilityDetails.getEndTime());
        availability.setAdditionalNotes(availabilityDetails.getAdditionalNotes());

        return availabilityRepository.save(availability);
    }

    @Transactional
    public void deleteAvailability(Long id) {
        availabilityRepository.deleteById(id);
    }

    @Transactional
    public void deleteTutorAvailability(UserEntity tutor) {
        List<TutorAvailabilityEntity> tutorSlots = availabilityRepository.findByTutor(tutor);
        availabilityRepository.deleteAll(tutorSlots);
    }

    // Additional methods for availability management could be added here
    public boolean isTimeSlotAvailable(UserEntity tutor, String dayOfWeek, String startTime, String endTime) {
        List<TutorAvailabilityEntity> existingSlots = availabilityRepository.findByTutorAndDayOfWeek(tutor, dayOfWeek);
        
        // Add time slot conflict checking logic here
        return true; // Placeholder return
    }
} 