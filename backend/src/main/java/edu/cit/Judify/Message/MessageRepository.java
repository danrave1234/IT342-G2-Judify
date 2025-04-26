package edu.cit.Judify.Message;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.User.UserEntity;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    /**
     * Find messages by sender
     */
    List<MessageEntity> findBySender(UserEntity sender);
    
    /**
     * Find messages by receiver
     */
    List<MessageEntity> findByReceiver(UserEntity receiver);
    
    /**
     * Find messages by conversation
     */
    List<MessageEntity> findByConversation(ConversationEntity conversation);
    
    /**
     * Find messages by conversation ordered by timestamp (descending)
     */
    List<MessageEntity> findByConversationOrderByTimestampDesc(ConversationEntity conversation);
    
    /**
     * Find messages by sender and receiver
     */
    List<MessageEntity> findBySenderAndReceiver(UserEntity sender, UserEntity receiver);
    
    /**
     * Find unread messages by receiver
     */
    List<MessageEntity> findByReceiverAndIsReadFalse(UserEntity receiver);
    
    /**
     * Find unread messages by sender
     */
    List<MessageEntity> findBySenderAndIsReadFalse(UserEntity sender);
    
    /**
     * Find unread messages by conversation
     */
    List<MessageEntity> findByConversationAndIsReadFalse(ConversationEntity conversation);
    
    /**
     * Find unread messages by sender and receiver
     */
    List<MessageEntity> findBySenderAndReceiverAndIsReadFalse(UserEntity sender, UserEntity receiver);
    
    /**
     * Find paginated unread messages by sender
     */
    Page<MessageEntity> findBySenderAndIsReadFalse(UserEntity sender, Pageable pageable);
    
    /**
     * Find paginated unread messages by conversation
     */
    Page<MessageEntity> findByConversationAndIsReadFalse(ConversationEntity conversation, Pageable pageable);
    
    /**
     * Find paginated messages by conversation
     */
    Page<MessageEntity> findByConversation(ConversationEntity conversation, Pageable pageable);

    /**
     * Find all messages in a conversation ordered by timestamp (ascending)
     */
    List<MessageEntity> findByConversationConversationIdOrderByTimestampAsc(Long conversationId);
    
    /**
     * Find all messages in a conversation ordered by timestamp (descending) with pagination
     */
    Page<MessageEntity> findByConversationConversationIdOrderByTimestampDesc(Long conversationId, Pageable pageable);
    
    /**
     * Count unread messages in a conversation for a specific user
     */
    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.conversation.conversationId = :conversationId AND m.receiver.userId = :userId AND m.isRead = false")
    Long countUnreadMessagesInConversationForUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
    
    /**
     * Mark all messages as read in a conversation for a specific user
     */
    @Modifying
    @Query("UPDATE MessageEntity m SET m.isRead = true WHERE m.conversation.conversationId = :conversationId AND m.receiver = :user AND m.isRead = false")
    int markAllAsReadInConversationForUser(@Param("conversationId") Long conversationId, @Param("user") UserEntity user);
    
    /**
     * Find the latest message in each conversation for a user
     */
    @Query(value = "SELECT m.* FROM messages m " +
           "INNER JOIN (" +
           "  SELECT conversation_id, MAX(timestamp) as latest_time " +
           "  FROM messages " +
           "  GROUP BY conversation_id" +
           ") latest ON m.conversation_id = latest.conversation_id AND m.timestamp = latest.latest_time " +
           "INNER JOIN conversations c ON m.conversation_id = c.conversation_id " +
           "WHERE c.user1_id = :userId OR c.user2_id = :userId " +
           "ORDER BY m.timestamp DESC", 
           nativeQuery = true)
    List<MessageEntity> findLatestMessagesForUser(@Param("userId") Long userId);
} 