package edu.cit.Judify.Message;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.User.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByConversationOrderByTimestampDesc(ConversationEntity conversation);
    List<MessageEntity> findBySenderAndIsReadFalse(UserEntity sender);
    List<MessageEntity> findByConversationAndIsReadFalse(ConversationEntity conversation);
    
    Page<MessageEntity> findByConversation(ConversationEntity conversation, Pageable pageable);
    Page<MessageEntity> findBySenderAndIsReadFalse(UserEntity sender, Pageable pageable);
    Page<MessageEntity> findByConversationAndIsReadFalse(ConversationEntity conversation, Pageable pageable);
} 