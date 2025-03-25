package edu.cit.Judify.Message.DTO;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.Message.MessageEntity;
import edu.cit.Judify.User.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class MessageDTOMapper {

    public MessageDTO toDTO(MessageEntity entity) {
        if (entity == null) {
            return null;
        }

        MessageDTO dto = new MessageDTO();
        dto.setMessageId(entity.getMessageId());
        dto.setConversationId(entity.getConversation().getConversationId());
        dto.setSenderId(entity.getSender().getUserId());
        dto.setContent(entity.getContent());
        dto.setCreatedAt(entity.getTimestamp());
        dto.setIsRead(entity.getIsRead());
        return dto;
    }

    public MessageEntity toEntity(MessageDTO dto, ConversationEntity conversation, UserEntity sender) {
        if (dto == null) {
            return null;
        }

        MessageEntity entity = new MessageEntity();
        entity.setMessageId(dto.getMessageId());
        entity.setConversation(conversation);
        entity.setSender(sender);
        entity.setContent(dto.getContent());
        entity.setTimestamp(dto.getCreatedAt());
        entity.setIsRead(dto.getIsRead());
        return entity;
    }
} 