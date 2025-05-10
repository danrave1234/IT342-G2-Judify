import { messageApi, conversationApi } from '../api';

/**
 * Simple messaging service for fetching conversation messages
 * 
 * This is a simplified version without complex polling or state tracking
 */
export const simpleMessageService = {
  /**
   * Get details for a specific conversation
   * @param {number|string} conversationId - ID of the conversation
   * @returns {Promise<Object>} Conversation details
   */
  getConversation: async (conversationId) => {
    if (!conversationId) {
      console.error('Cannot get conversation: conversationId is undefined');
      return Promise.reject(new Error('Conversation ID is required'));
    }

    try {
      const response = await conversationApi.getConversation(conversationId);
      return response.data;
    } catch (error) {
      console.error(`Error getting conversation ${conversationId}:`, error);
      // Return minimal object to prevent UI from breaking
      return { conversationId };
    }
  },

  /**
   * Get messages for a specific conversation
   * @param {number|string} conversationId - ID of the conversation
   * @param {Object} params - Optional pagination parameters (page, size)
   * @returns {Promise<Array>} Messages
   */
  getMessages: async (conversationId, params = { page: 0, size: 20 }) => {
    if (!conversationId) {
      console.error('Cannot get messages: conversationId is undefined');
      return Promise.reject(new Error('Conversation ID is required'));
    }

    try {
      const response = await messageApi.getMessages(conversationId, params);
      return response.data || [];
    } catch (error) {
      console.error(`Error getting messages for conversation ${conversationId}:`, error);
      return [];
    }
  },

  /**
   * Send a message to a conversation
   * @param {Object} messageData - Message data (conversationId, senderId, receiverId, content)
   * @returns {Promise<Object>} Sent message
   */
  sendMessage: async (messageData) => {
    if (!messageData.conversationId || !messageData.senderId || !messageData.content) {
      console.error('Cannot send message: missing required data');
      return Promise.reject(new Error('Missing required message data'));
    }

    try {
      const response = await messageApi.sendMessage(messageData);
      return response.data;
    } catch (error) {
      console.error(`Error sending message to conversation ${messageData.conversationId}:`, error);
      throw error;
    }
  },

  /**
   * Mark all messages in a conversation as read
   * @param {number|string} conversationId - ID of the conversation
   * @param {number|string} userId - ID of the user
   */
  markAllAsRead: async (conversationId, userId) => {
    if (!conversationId || !userId) {
      return;
    }

    try {
      await messageApi.markAllAsRead(conversationId, userId);
    } catch (error) {
      // Non-critical, so just log it
      console.warn(`Could not mark messages as read for conversation ${conversationId}`);
    }
  }
};

export default simpleMessageService; 