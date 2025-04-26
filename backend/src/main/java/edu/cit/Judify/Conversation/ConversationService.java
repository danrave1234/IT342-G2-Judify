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
     * Find or create a conversation between two users
     * @param user1 The first user
     * @param user2 The second user
     * @return The existing or newly created conversation
     */
    @Transactional
    public ConversationEntity findOrCreateConversation(UserEntity user1, UserEntity user2) {
        // First, try to find an existing conversation with exactly these users
        // The order of users doesn't matter - check both ways
        List<ConversationEntity> existingConversations = 
            conversationRepository.findConversationBetweenUsers(user1, user2);

        // If found, return the first one
        if (!existingConversations.isEmpty()) {
            return existingConversations.get(0);
        }

        // Otherwise, create a new conversation
        ConversationEntity conversation = new ConversationEntity();
        conversation.setUser1(user1);
        conversation.setUser2(user2);
        return conversationRepository.save(conversation);
    }
    
    /**
     * Find a conversation between two users by their user IDs
     * @param user1Id The ID of the first user
     * @param user2Id The ID of the second user
     * @param user1 The first user entity
     * @param user2 The second user entity
     * @return The existing or newly created conversation
     */
    @Transactional
    public ConversationEntity findOrCreateConversationByUserIds(Long user1Id, Long user2Id, UserEntity user1, UserEntity user2) {
        if (user1 == null || user2 == null) {
            throw new IllegalArgumentException("Both user entities must be provided");
        }
        
        if (!user1.getUserId().equals(user1Id) || !user2.getUserId().equals(user2Id)) {
            throw new IllegalArgumentException("User IDs must match the provided user entities");
        }
        
        return findOrCreateConversation(user1, user2);
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
        return conversationRepository.findByUser1OrUser2(user, user);
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
