package edu.cit.Judify.TutorAvailability;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class TutorAvailabilityService {

    private final TutorAvailabilityRepository availabilityRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public TutorAvailabilityService(TutorAvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional
    public TutorAvailabilityEntity createAvailability(TutorAvailabilityEntity availability) {
        // --- FIX START: Add conflict checking before saving ---
        checkAvailabilityConflicts(availability.getTutor(), availability);
        // --- FIX END ---
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
                .orElseThrow(() -> new RuntimeException("Availability slot not found with id: " + id));

        // Set the tutor from the existing entity to ensure it's correct
        availabilityDetails.setTutor(availability.getTutor());
        availabilityDetails.setAvailabilityId(id); // Ensure ID is set for conflict check exclusion

        // --- FIX START: Add conflict checking before saving updates ---
        checkAvailabilityConflicts(availability.getTutor(), availabilityDetails);
        // --- FIX END ---

        availability.setDayOfWeek(availabilityDetails.getDayOfWeek());
        availability.setStartTime(availabilityDetails.getStartTime());
        availability.setEndTime(availabilityDetails.getEndTime());
        availability.setAdditionalNotes(availabilityDetails.getAdditionalNotes());

        return availabilityRepository.save(availability);
    }

    @Transactional
    public void deleteAvailability(Long id) {
        if (!availabilityRepository.existsById(id)) {
            throw new RuntimeException("Availability slot not found with id: " + id);
        }
        availabilityRepository.deleteById(id);
    }

    @Transactional
    public void deleteTutorAvailability(UserEntity tutor) {
        List<TutorAvailabilityEntity> tutorSlots = availabilityRepository.findByTutor(tutor);
        availabilityRepository.deleteAll(tutorSlots);
    }

    // --- FIX START: Implement conflict checking logic ---
    private void checkAvailabilityConflicts(UserEntity tutor, TutorAvailabilityEntity newAvailability) {
        List<TutorAvailabilityEntity> existingSlots = availabilityRepository.findByTutorAndDayOfWeek(tutor, newAvailability.getDayOfWeek());

        LocalTime newStart;
        LocalTime newEnd;

        try {
            newStart = LocalTime.parse(newAvailability.getStartTime(), TIME_FORMATTER);
            newEnd = LocalTime.parse(newAvailability.getEndTime(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Please use HH:mm.", e);
        }

        if (newStart.isAfter(newEnd) || newStart.equals(newEnd)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        for (TutorAvailabilityEntity existingSlot : existingSlots) {
            // If updating, skip checking against the slot itself
            if (newAvailability.getAvailabilityId() != null && newAvailability.getAvailabilityId().equals(existingSlot.getAvailabilityId())) {
                continue;
            }

            LocalTime existingStart;
            LocalTime existingEnd;
            try {
                existingStart = LocalTime.parse(existingSlot.getStartTime(), TIME_FORMATTER);
                existingEnd = LocalTime.parse(existingSlot.getEndTime(), TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                // Log or handle error for existing invalid data, but proceed with check
                System.err.println("Skipping conflict check against invalid existing slot ID " + existingSlot.getAvailabilityId() + ": " + e.getMessage());
                continue;
            }


            // Check for overlap: (StartA < EndB) and (EndA > StartB)
            boolean overlaps = newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);

            if (overlaps) {
                throw new IllegalArgumentException(String.format(
                        "The new availability slot from %s to %s conflicts with an existing slot from %s to %s on %s.",
                        newAvailability.getStartTime(), newAvailability.getEndTime(),
                        existingSlot.getStartTime(), existingSlot.getEndTime(),
                        newAvailability.getDayOfWeek()
                ));
            }
        }
    }
    // --- FIX END ---

    // Existing check method - Now uses the logic from checkAvailabilityConflicts
    // Note: This might still be useful for booking checks, but needs refinement
    public boolean isTimeSlotAvailable(UserEntity tutor, String dayOfWeek, String startTimeStr, String endTimeStr) {
        LocalTime checkStart;
        LocalTime checkEnd;
        try {
            checkStart = LocalTime.parse(startTimeStr, TIME_FORMATTER);
            checkEnd = LocalTime.parse(endTimeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid time format for checking availability: " + e.getMessage());
            return false; // Treat invalid format as unavailable
        }

        List<TutorAvailabilityEntity> existingSlots = availabilityRepository.findByTutorAndDayOfWeek(tutor, dayOfWeek);

        for (TutorAvailabilityEntity existingSlot : existingSlots) {
            LocalTime existingStart;
            LocalTime existingEnd;
            try {
                existingStart = LocalTime.parse(existingSlot.getStartTime(), TIME_FORMATTER);
                existingEnd = LocalTime.parse(existingSlot.getEndTime(), TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                continue; // Skip invalid existing slots
            }

            // Check if the requested slot [checkStart, checkEnd) overlaps with [existingStart, existingEnd)
            if (checkStart.isBefore(existingEnd) && checkEnd.isAfter(existingStart)) {
                return false; // Found a conflict
            }
        }
        return true; // No conflicts found
    }
}