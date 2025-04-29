package edu.cit.Judify.Conversation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.cit.Judify.User.UserEntity;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    List<ConversationEntity> findByParticipantsContaining(UserEntity participant);

    List<ConversationEntity> findByStudentOrTutor(UserEntity student, UserEntity tutor);

    /**
     * Find a conversation between a student and a tutor
     */
    @Query("SELECT c FROM ConversationEntity c WHERE (c.student = :student AND c.tutor = :tutor) OR (c.student = :tutor AND c.tutor = :student)")
    List<ConversationEntity> findConversationBetweenUsers(@Param("student") UserEntity student, @Param("tutor") UserEntity tutor);
}

