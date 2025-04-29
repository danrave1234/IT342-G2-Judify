package edu.cit.Judify.Conversation;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.Judify.User.UserEntity;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @Autowired
    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    /**
     * Find or create a conversation between a student and a tutor
     * @param student The student user
     * @param tutor The tutor user
     * @return The existing or newly created conversation
     */
    @Transactional
    public ConversationEntity findOrCreateConversation(UserEntity student, UserEntity tutor) {
        // First, try to find an existing conversation with exactly these users
        // The order of users doesn't matter - check both ways
        List<ConversationEntity> existingConversations = 
            conversationRepository.findConversationBetweenUsers(student, tutor);

        // If found, return the first one
        if (!existingConversations.isEmpty()) {
            return existingConversations.get(0);
        }

        // Otherwise, create a new conversation
        ConversationEntity conversation = new ConversationEntity();
        conversation.setStudent(student);
        conversation.setTutor(tutor);
        return conversationRepository.save(conversation);
    }

    /**
     * Find a conversation between a student and a tutor by their user IDs
     * @param studentId The ID of the student user
     * @param tutorId The ID of the tutor user
     * @param student The student user entity
     * @param tutor The tutor user entity
     * @return The existing or newly created conversation
     */
    @Transactional
    public ConversationEntity findOrCreateConversationByUserIds(Long studentId, Long tutorId, UserEntity student, UserEntity tutor) {
        if (student == null || tutor == null) {
            throw new IllegalArgumentException("Both user entities must be provided");
        }

        if (!student.getUserId().equals(studentId) || !tutor.getUserId().equals(tutorId)) {
            throw new IllegalArgumentException("User IDs must match the provided user entities");
        }

        return findOrCreateConversation(student, tutor);
    }

    /**
     * Create a new conversation
     * @param conversation The conversation to create
     * @return The created conversation
     */
    @Transactional
    public ConversationEntity createConversation(ConversationEntity conversation) {
        return conversationRepository.save(conversation);
    }

    /**
     * Get a conversation by ID
     * @param id The conversation ID
     * @return Optional containing the conversation if found
     */
    public Optional<ConversationEntity> getConversationById(Long id) {
        return conversationRepository.findById(id);
    }

    /**
     * Get all conversations for a user
     * @param user The user
     * @return List of conversations
     */
    public List<ConversationEntity> getUserConversations(UserEntity user) {
        return conversationRepository.findByStudentOrTutor(user, user);
    }

    /**
     * Delete a conversation by ID
     * @param id The conversation ID
     */
    @Transactional
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }

    /**
     * Find or create a conversation between a student and a tutor
     * @param studentUser The student user entity
     * @param tutorUser The tutor user entity
     * @return The existing or newly created conversation
     */
    @Transactional
    public ConversationEntity findOrCreateStudentTutorConversation(UserEntity studentUser, UserEntity tutorUser) {
        // This is essentially the same as findOrCreateConversation, but with specific naming
        // for clarity when used in the student-tutor context
        return findOrCreateConversation(studentUser, tutorUser);
    }
} 
