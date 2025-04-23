package edu.cit.Judify.Conversation;

import edu.cit.Judify.User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
    List<ConversationEntity> findByParticipantsContaining(UserEntity participant);
}

