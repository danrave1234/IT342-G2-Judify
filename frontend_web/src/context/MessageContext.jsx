import { createContext, useContext, useState } from 'react';
import { messageApi, conversationApi } from '../api/api';
import { useUser } from './UserContext';

const MessageContext = createContext(null);

export const useMessage = () => useContext(MessageContext);

export const MessageProvider = ({ children }) => {
  const { user } = useUser();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeConversation, setActiveConversation] = useState(null);
  const [messages, setMessages] = useState([]);

  const getConversations = async () => {
    if (!user) return { success: false, message: 'No user logged in', conversations: [] };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await conversationApi.getConversations(user.userId);
      return { success: true, conversations: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to load conversations';
      setError(message);
      return { success: false, message, conversations: [] };
    } finally {
      setLoading(false);
    }
  };

  const getOrCreateConversation = async (otherUserId) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!otherUserId) return { success: false, message: 'No recipient specified' };
    
    setLoading(true);
    setError(null);
    
    try {
      // First check if a conversation already exists
      const conversations = await conversationApi.getConversations(user.userId);
      
      // Find a conversation with the other user
      const existingConversation = conversations.data.find(
        conv => (conv.user1Id === otherUserId || conv.user2Id === otherUserId)
      );
      
      if (existingConversation) {
        setActiveConversation(existingConversation);
        return { success: true, conversation: existingConversation };
      }
      
      // Create a new conversation if none exists
      const conversationData = {
        user1Id: user.userId,
        user2Id: otherUserId
      };
      
      const response = await conversationApi.createConversation(conversationData);
      setActiveConversation(response.data);
      return { success: true, conversation: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to create conversation';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const loadMessages = async (conversationId, params = {}) => {
    if (!user) return { success: false, message: 'No user logged in', messages: [] };
    if (!conversationId) return { success: false, message: 'No conversation specified', messages: [] };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await messageApi.getMessages(conversationId, params);
      setMessages(response.data);
      return { success: true, messages: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to load messages';
      setError(message);
      return { success: false, message, messages: [] };
    } finally {
      setLoading(false);
    }
  };

  const sendMessage = async (conversationId, receiverId, content) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!conversationId) return { success: false, message: 'No conversation specified' };
    if (!content) return { success: false, message: 'Message cannot be empty' };
    
    setLoading(true);
    setError(null);
    
    try {
      const messageData = {
        conversationId,
        senderId: user.userId,
        receiverId,
        content,
        isRead: false
      };
      
      const response = await messageApi.sendMessage(messageData);
      
      // Add the new message to the messages array
      setMessages(prevMessages => [...prevMessages, response.data]);
      
      return { success: true, message: response.data };
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Failed to send message';
      setError(errorMessage);
      return { success: false, message: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const markMessageAsRead = async (messageId) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!messageId) return { success: false, message: 'No message specified' };
    
    setLoading(true);
    setError(null);
    
    try {
      await messageApi.markAsRead(messageId);
      
      // Update the message in the state
      setMessages(prevMessages => 
        prevMessages.map(msg => 
          msg.messageId === messageId ? { ...msg, isRead: true } : msg
        )
      );
      
      return { success: true };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to mark message as read';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  return (
    <MessageContext.Provider
      value={{
        loading,
        error,
        messages,
        activeConversation,
        setActiveConversation,
        getConversations,
        getOrCreateConversation,
        loadMessages,
        sendMessage,
        markMessageAsRead
      }}
    >
      {children}
    </MessageContext.Provider>
  );
}; 