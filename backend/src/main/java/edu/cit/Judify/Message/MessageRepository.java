package edu.cit.Judify.Message;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByConversationOrderByTimestampDesc(ConversationEntity conversation);
    List<MessageEntity> findBySenderAndIsReadFalse(UserEntity sender);
    List<MessageEntity> findByConversationAndIsReadFalse(ConversationEntity conversation);
} 