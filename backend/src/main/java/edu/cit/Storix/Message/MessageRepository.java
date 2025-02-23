package edu.cit.Storix.Message;

import edu.cit.Storix.Conversation.ConversationEntity;
import edu.cit.Storix.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByConversationOrderByTimestampDesc(ConversationEntity conversation);
    List<MessageEntity> findBySenderAndIsReadFalse(UserEntity sender);
    List<MessageEntity> findByConversationAndIsReadFalse(ConversationEntity conversation);
} 