package edu.cit.Judify.Conversation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.cit.Judify.User.UserEntity;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    List<ConversationEntity> findByUser1OrUser2(UserEntity user1, UserEntity user2);
    
    /**
     * Find a conversation between two specific users
     */
    @Query("SELECT c FROM ConversationEntity c WHERE (c.user1 = :user1 AND c.user2 = :user2) OR (c.user1 = :user2 AND c.user2 = :user1)")
    List<ConversationEntity> findConversationBetweenUsers(@Param("user1") UserEntity user1, @Param("user2") UserEntity user2);
} 
