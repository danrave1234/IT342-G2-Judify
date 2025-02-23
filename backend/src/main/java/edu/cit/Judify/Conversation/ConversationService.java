package edu.cit.Judify.Conversation;

import edu.cit.Judify.User.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @Autowired
    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public ConversationEntity createConversation(ConversationEntity conversation) {
        return conversationRepository.save(conversation);
    }

    public Optional<ConversationEntity> getConversationById(Long id) {
        return conversationRepository.findById(id);
    }

    public List<ConversationEntity> getUserConversations(UserEntity participant) {
        return conversationRepository.findByParticipantsContaining(participant);
    }

    @Transactional
    public ConversationEntity addParticipant(Long conversationId, UserEntity participant) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        Set<UserEntity> participants = conversation.getParticipants();
        participants.add(participant);
        conversation.setParticipants(participants);
        
        return conversationRepository.save(conversation);
    }

    @Transactional
    public ConversationEntity removeParticipant(Long conversationId, UserEntity participant) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        Set<UserEntity> participants = conversation.getParticipants();
        participants.remove(participant);
        conversation.setParticipants(participants);
        
        return conversationRepository.save(conversation);
    }

    @Transactional
    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }
} 