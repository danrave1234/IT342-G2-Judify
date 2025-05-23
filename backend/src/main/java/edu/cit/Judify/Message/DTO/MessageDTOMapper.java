package edu.cit.Judify.Message.DTO;

import org.springframework.stereotype.Component;

import edu.cit.Judify.Conversation.ConversationEntity;
import edu.cit.Judify.Message.MessageEntity;
import edu.cit.Judify.TutoringSession.TutoringSessionEntity;
import edu.cit.Judify.User.UserEntity;

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
        dto.setReceiverId(entity.getReceiver().getUserId());
        dto.setContent(entity.getContent());
        dto.setCreatedAt(entity.getTimestamp());
        dto.setIsRead(entity.getIsRead());
        dto.setMessageType(entity.getMessageType());

        // Set sessionId if session is not null
        if (entity.getSession() != null) {
            dto.setSessionId(entity.getSession().getSessionId());
        }

        return dto;
    }

    public MessageEntity toEntity(MessageDTO dto, ConversationEntity conversation, UserEntity sender, UserEntity receiver) {
        if (dto == null) {
            return null;
        }

        MessageEntity entity = new MessageEntity();
        entity.setMessageId(dto.getMessageId());
        entity.setConversation(conversation);
        entity.setSender(sender);
        entity.setReceiver(receiver);
        entity.setContent(dto.getContent());
        entity.setTimestamp(dto.getCreatedAt());
        entity.setIsRead(dto.getIsRead());
        entity.setMessageType(dto.getMessageType());
        return entity;
    }

    public MessageEntity toEntity(MessageDTO dto, ConversationEntity conversation, UserEntity sender, UserEntity receiver, TutoringSessionEntity session) {
        MessageEntity entity = toEntity(dto, conversation, sender, receiver);
        if (entity != null && session != null) {
            entity.setSession(session);
        }
        return entity;
    }
} 
