package edu.cit.Judify.TutorSubject;

import edu.cit.Judify.TutorProfile.TutorProfileEntity;
import edu.cit.Judify.TutorProfile.TutorProfileRepository;
import edu.cit.Judify.TutorSubject.DTO.TutorSubjectDTO;
import edu.cit.Judify.TutorSubject.DTO.TutorSubjectDTOMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TutorSubjectService {

    private final TutorSubjectRepository tutorSubjectRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final TutorSubjectDTOMapper dtoMapper;

    @Autowired
    public TutorSubjectService(TutorSubjectRepository tutorSubjectRepository,
                             TutorProfileRepository tutorProfileRepository,
                             TutorSubjectDTOMapper dtoMapper) {
        this.tutorSubjectRepository = tutorSubjectRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.dtoMapper = dtoMapper;
    }

    public List<TutorSubjectDTO> getSubjectsByTutorProfileId(Long tutorProfileId) {
        List<TutorSubjectEntity> subjects = tutorSubjectRepository.findByTutorProfileId(tutorProfileId);
        return subjects.stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<TutorSubjectDTO> searchSubjects(String query) {
        List<TutorSubjectEntity> subjects = tutorSubjectRepository.findBySubjectContainingIgnoreCase(query);
        return subjects.stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TutorSubjectDTO addSubject(TutorSubjectDTO subjectDTO) {
        TutorProfileEntity tutorProfile = tutorProfileRepository.findById(subjectDTO.getTutorProfileId())
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found with id: " + subjectDTO.getTutorProfileId()));

        TutorSubjectEntity entity = dtoMapper.toEntity(subjectDTO);
        entity.setTutorProfile(tutorProfile);
        
        TutorSubjectEntity savedEntity = tutorSubjectRepository.save(entity);
        return dtoMapper.toDTO(savedEntity);
    }

    @Transactional
    public TutorSubjectDTO updateSubject(Long id, TutorSubjectDTO subjectDTO) {
        TutorSubjectEntity existingSubject = tutorSubjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found with id: " + id));

        // Only update the subject name, not the relationship
        existingSubject.setSubject(subjectDTO.getSubject());
        
        TutorSubjectEntity updatedEntity = tutorSubjectRepository.save(existingSubject);
        return dtoMapper.toDTO(updatedEntity);
    }

    @Transactional
    public void deleteSubject(Long id) {
        if (!tutorSubjectRepository.existsById(id)) {
            throw new EntityNotFoundException("Subject not found with id: " + id);
        }
        tutorSubjectRepository.deleteById(id);
    }

    @Transactional
    public List<TutorSubjectDTO> addSubjectsForTutor(Long tutorProfileId, List<String> subjects) {
        TutorProfileEntity tutorProfile = tutorProfileRepository.findById(tutorProfileId)
                .orElseThrow(() -> new EntityNotFoundException("TutorProfile not found with id: " + tutorProfileId));

        List<TutorSubjectEntity> subjectEntities = subjects.stream()
                .map(subject -> new TutorSubjectEntity(tutorProfile, subject))
                .collect(Collectors.toList());
        
        List<TutorSubjectEntity> savedEntities = tutorSubjectRepository.saveAll(subjectEntities);
        
        return savedEntities.stream()
                .map(dtoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAllSubjectsForTutor(Long tutorProfileId) {
        // Check if profile exists
        if (!tutorProfileRepository.existsById(tutorProfileId)) {
            throw new EntityNotFoundException("TutorProfile not found with id: " + tutorProfileId);
        }
        
        tutorSubjectRepository.deleteByTutorProfileId(tutorProfileId);
    }
} 