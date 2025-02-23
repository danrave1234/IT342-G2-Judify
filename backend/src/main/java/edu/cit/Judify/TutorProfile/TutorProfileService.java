package edu.cit.Judify.TutorProfile;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TutorProfileService {

    private final TutorProfileRepository tutorProfileRepository;

    @Autowired
    public TutorProfileService(TutorProfileRepository tutorProfileRepository) {
        this.tutorProfileRepository = tutorProfileRepository;
    }

    @Transactional
    public TutorProfileEntity createTutorProfile(TutorProfileEntity profile) {
        return tutorProfileRepository.save(profile);
    }

    public Optional<TutorProfileEntity> getTutorProfileById(Long id) {
        return tutorProfileRepository.findById(id);
    }

    public TutorProfileEntity getTutorProfileByUser(UserEntity user) {
        return tutorProfileRepository.findByUser(user);
    }

    public List<TutorProfileEntity> findTutorsByExpertise(String expertise) {
        return tutorProfileRepository.findBySubjectsExpertiseLike("%" + expertise + "%");
    }

    public List<TutorProfileEntity> findTutorsByPriceRange(Double minRate, Double maxRate) {
        return tutorProfileRepository.findByHourlyRateBetween(minRate, maxRate);
    }

    public List<TutorProfileEntity> findTopRatedTutors(Double minRating) {
        return tutorProfileRepository.findByAverageRatingGreaterThanEqual(minRating);
    }

    @Transactional
    public TutorProfileEntity updateTutorProfile(Long id, TutorProfileEntity profileDetails) {
        TutorProfileEntity profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));

        profile.setBiography(profileDetails.getBiography());
        profile.setCertifications(profileDetails.getCertifications());
        profile.setSubjectsExpertise(profileDetails.getSubjectsExpertise());
        profile.setHourlyRate(profileDetails.getHourlyRate());
        profile.setAvailabilitySchedule(profileDetails.getAvailabilitySchedule());
        profile.setLocation(profileDetails.getLocation());

        return tutorProfileRepository.save(profile);
    }

    @Transactional
    public void deleteTutorProfile(Long id) {
        tutorProfileRepository.deleteById(id);
    }

    @Transactional
    public TutorProfileEntity updateTutorRating(Long id, Double newRating, Integer newCount) {
        TutorProfileEntity profile = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tutor profile not found"));
        
        profile.setAverageRating(newRating);
        profile.setRatingsCount(newCount);
        
        return tutorProfileRepository.save(profile);
    }
} 