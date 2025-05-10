import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import simpleMessageService from '../services/simpleMessageService';
import useInterval from '../hooks/useInterval';
import { useAuth } from './AuthContext';
import { toast } from 'react-toastify';
import PropTypes from 'prop-types';

const MessageContext = createContext(null);

// Polling interval in milliseconds
const POLLING_INTERVAL = 5000;

export const useMessages = () => {
  const context = useContext(MessageContext);
  if (!context) {
    throw new Error('useMessages must be used within a MessageProvider');
  }
  return context;
};

export const MessageProvider = ({ children }) => {
  const { user } = useAuth();
  const [conversations, setConversations] = useState([]);
  const [activeConversation, setActiveConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMoreMessages, setHasMoreMessages] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalMessages, setTotalMessages] = useState(0);
  const [isSending, setIsSending] = useState(false);
  const [unreadMessages, setUnreadMessages] = useState({});
  const [error, setError] = useState(null);
  const messagePageSize = 20;
  
  // Track if polling should be active
  const [isPollingActive, setIsPollingActive] = useState(false);
  
  // Track the last message timestamp to detect new messages
  const lastMessageTimestampRef = useRef(null);
  
  // Track if we're currently joining a conversation
  const joiningRef = useRef(false);

  // Function to fetch new messages for the active conversation
  const fetchMessages = useCallback(async () => {
    if (!activeConversation || !isPollingActive) return;
    
    try {
      const fetchedMessages = await simpleMessageService.getMessages(activeConversation, {
        page: 0,
        size: messagePageSize
      });
      
      if (fetchedMessages.length > 0) {
        // Sort messages by timestamp
        const sortedMessages = [...fetchedMessages].sort((a, b) => {
          const timeA = new Date(a.timestamp || a.createdAt || a.sentAt).getTime();
          const timeB = new Date(b.timestamp || b.createdAt || b.sentAt).getTime();
          return timeA - timeB;
        });
        
        // Get latest message timestamp
        const latestMessage = sortedMessages[sortedMessages.length - 1];
        const latestTimestamp = new Date(
          latestMessage.timestamp || latestMessage.createdAt || latestMessage.sentAt
        ).getTime();
        
        // Check if we have new messages based on timestamp
        if (!lastMessageTimestampRef.current || latestTimestamp > lastMessageTimestampRef.current) {
          // Update the latest timestamp
          lastMessageTimestampRef.current = latestTimestamp;
          
          // Update messages state with new messages
          setMessages(sortedMessages);
          
          // Mark messages as read if we have a user
          if (user && user.userId) {
            simpleMessageService.markAllAsRead(activeConversation, user.userId);
          }
        }
      }
    } catch (error) {
      console.error('Error fetching messages:', error);
    }
  }, [activeConversation, isPollingActive, messagePageSize, user]);
  
  // Set up the polling interval
  useInterval(fetchMessages, isPollingActive ? POLLING_INTERVAL : null);
  
  // Cleanup when component unmounts
  useEffect(() => {
    return () => {
      // Reset polling state when unmounting
      setIsPollingActive(false);
    };
  }, []);

  /**
   * Leave the current conversation
   */
  const leaveConversation = useCallback(() => {
    if (activeConversation) {
      console.log(`Leaving conversation ${activeConversation}`);
      
      // Stop polling
      setIsPollingActive(false);
      
      // Reset conversation state
      setActiveConversation(null);
      setMessages([]);
      setCurrentPage(0);
      setTotalPages(0);
      setTotalMessages(0);
      setHasMoreMessages(false);
      lastMessageTimestampRef.current = null;
    }
  }, [activeConversation]);

  /**
   * Load messages for a conversation
   * @param {number} conversationId The ID of the conversation
   */
  const loadMessages = useCallback(async (conversationId) => {
    if (!conversationId) {
      console.error('Cannot load messages: conversationId is undefined');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const messagesData = await simpleMessageService.getMessages(conversationId, { 
        page: 0, 
        size: messagePageSize 
      });
      
      // Sort messages by timestamp (oldest to newest)
      const sortedMessages = messagesData.sort((a, b) => {
        const timeA = new Date(a.timestamp || a.createdAt || a.sentAt).getTime();
        const timeB = new Date(b.timestamp || b.createdAt || b.sentAt).getTime();
        return timeA - timeB;
      });
      
      setMessages(sortedMessages);
      setCurrentPage(0);
      setHasMoreMessages(messagesData.length >= messagePageSize);
      
      // Store the latest message timestamp
      if (sortedMessages.length > 0) {
        const latestMessage = sortedMessages[sortedMessages.length - 1];
        lastMessageTimestampRef.current = new Date(
          latestMessage.timestamp || latestMessage.createdAt || latestMessage.sentAt
        ).getTime();
      }
      
      // After loading messages, mark them all as read if we have a user
      if (user && user.userId) {
        try {
          await simpleMessageService.markAllAsRead(conversationId, user.userId);
        } catch (markError) {
          console.warn('Error marking messages as read:', markError);
        }
      }
    } catch (error) {
      console.error('Error loading messages:', error);
      setError('Failed to load messages. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }, [user, messagePageSize]);

  /**
   * Join a conversation to start receiving messages
   * @param {number} conversationId The ID of the conversation to join
   */
  const joinConversation = useCallback(async (conversationId) => {
    // Validate conversation ID
    if (!conversationId) {
      console.error('Cannot join conversation: conversationId is undefined');
      return;
    }

    // Convert to string for consistency in comparisons
    const conversationIdStr = String(conversationId);
    
    // First check if we're already in this conversation
    if (activeConversation === conversationIdStr) {
      console.log(`Already in conversation ${conversationIdStr}, skipping join`);
      return;
    }
    
    // Check if we're already in the process of joining a conversation
    if (joiningRef.current) {
      console.log('Already joining a conversation, ignoring duplicate request');
      return;
    }
    
    // Set joining flag to prevent concurrent join attempts
    joiningRef.current = true;

    // If we're in a different conversation, leave it first
    if (activeConversation && activeConversation !== conversationIdStr) {
      await leaveConversation();
    }

    setIsLoading(true);
    setError(null);

    try {
      // Get conversation details
      await simpleMessageService.getConversation(conversationIdStr);
      
      // Set as active conversation
      setActiveConversation(conversationIdStr);
      
      // Load initial messages
      await loadMessages(conversationIdStr);
      
      // Start polling for this conversation
      setIsPollingActive(true);
    } catch (error) {
      console.error('Error joining conversation:', error);
      setError('Failed to join conversation. Please try again.');
    } finally {
      setIsLoading(false);
      // Reset joining flag
      joiningRef.current = false;
    }
  }, [activeConversation, leaveConversation, loadMessages]);

  /**
   * Load more messages (pagination)
   * @param {number} conversationId The ID of the conversation
   */
  const loadMoreMessages = useCallback(async (conversationId) => {
    if (!conversationId || isLoadingMore) {
      return;
    }

    // Convert to string for consistency
    const conversationIdStr = String(conversationId);

    // Make sure we have an active conversation that matches
    if (activeConversation !== conversationIdStr) {
      console.warn('Cannot load more messages: No active conversation or ID mismatch');
      return;
    }

    setIsLoadingMore(true);
    setError(null);

    try {
      const nextPage = currentPage + 1;
      
      // Load messages for the next page
      const moreMessagesData = await simpleMessageService.getMessages(conversationIdStr, {
        page: nextPage,
        size: messagePageSize
      });
      
      if (moreMessagesData.length > 0) {
        // Sort new messages by timestamp (oldest to newest)
        const sortedNewMessages = moreMessagesData.sort((a, b) => {
          const timeA = new Date(a.timestamp || a.createdAt || a.sentAt).getTime();
          const timeB = new Date(b.timestamp || b.createdAt || b.sentAt).getTime();
          return timeA - timeB;
        });
        
        // Combine with existing messages, avoiding duplicates
        setMessages(prevMessages => {
          // Get IDs of existing messages
          const existingIds = new Set(prevMessages.map(msg => msg.messageId || msg.id));
          
          // Filter out duplicates
          const uniqueNewMessages = sortedNewMessages.filter(
            msg => !existingIds.has(msg.messageId || msg.id)
          );
          
          // Prepend unique new messages to the existing ones
          return [...uniqueNewMessages, ...prevMessages];
        });
        
        setCurrentPage(nextPage);
        setHasMoreMessages(moreMessagesData.length >= messagePageSize);
      } else {
        setHasMoreMessages(false);
      }
    } catch (error) {
      console.error('Error loading more messages:', error);
      setError('Failed to load more messages. Please try again.');
    } finally {
      setIsLoadingMore(false);
    }
  }, [currentPage, isLoadingMore, activeConversation, messagePageSize]);

  /**
   * Send a message to the current conversation
   * @param {string} content The message content
   */
  const sendMessage = useCallback(async (content) => {
    if (!activeConversation || !content) {
      console.error('Cannot send message: missing conversation or content');
      return null;
    }
    
    if (!user || !user.userId) {
      console.error('Cannot send message: user is not authenticated');
      setError('You must be logged in to send messages');
      return null;
    }

    setIsSending(true);
    setError(null);

    try {
      // Get conversation details
      const conversationDetails = await simpleMessageService.getConversation(activeConversation);
      
      // Handle case where conversation details might be incomplete
      if (!conversationDetails) {
        console.error('Cannot determine receiver ID: missing conversation details');
        throw new Error('Failed to identify message recipient');
      }
      
      // Determine the receiver ID based on the conversation structure
      let receiverId;
      
      // Check if we have user1Id/user2Id format
      if (conversationDetails.user1Id !== undefined && conversationDetails.user2Id !== undefined) {
        // If the current user is user1, then the receiver is user2, and vice versa
        receiverId = conversationDetails.user1Id === user.userId 
          ? conversationDetails.user2Id 
          : conversationDetails.user1Id;
      } 
      // Check if we have studentId/tutorId format
      else if (conversationDetails.studentId !== undefined || conversationDetails.tutorId !== undefined) {
        // If one of the IDs matches the current user, use the other one as receiver
        if (conversationDetails.studentId === user.userId) {
          receiverId = conversationDetails.tutorId;
        } else if (conversationDetails.tutorId === user.userId) {
          receiverId = conversationDetails.studentId;
        } else {
          // Fallback: use studentId as receiver if user is tutor or tutorId if user is student
          receiverId = user.role === 'TUTOR' 
            ? conversationDetails.studentId 
            : conversationDetails.tutorId;
        }
      } else {
        // No valid IDs found
        console.error('Cannot determine receiver ID from conversation details:', conversationDetails);
        throw new Error('Failed to identify message recipient');
      }
      
      // Final validation of receiverId
      if (!receiverId) {
        console.error('Receiver ID is undefined or null:', { conversationDetails, userId: user.userId });
        throw new Error('Invalid message recipient');
      }
      
      // Prepare message data
      const messageData = {
        conversationId: activeConversation,
        senderId: user.userId,
        receiverId: receiverId,
        content: content,
        timestamp: new Date().toISOString()
      };

      // Send the message
      const sentMessage = await simpleMessageService.sendMessage(messageData);
      
      // Update the local messages state with the sent message
      setMessages(prevMessages => [...prevMessages, sentMessage]);
      
      // Update the latest message timestamp
      lastMessageTimestampRef.current = new Date(
        sentMessage.timestamp || sentMessage.createdAt || sentMessage.sentAt
      ).getTime();
      
      // Immediately fetch messages to get any other new messages
      fetchMessages();
      
      return sentMessage;
    } catch (error) {
      console.error('Error sending message:', error);
      setError('Failed to send message. Please try again.');
      toast.error('Failed to send message. Please try again.');
      return null;
    } finally {
      setIsSending(false);
    }
  }, [activeConversation, user, fetchMessages]);

  /**
   * Mark a message as read
   * @param {number} messageId The ID of the message to mark as read
   */
  const markAsRead = useCallback(async (messageId) => {
    if (!messageId) {
      console.warn('Cannot mark message as read: messageId is undefined');
      return;
    }

    try {
      // Mark message as read locally
      setMessages(prevMessages => 
        prevMessages.map(msg => 
          (msg.messageId === messageId || msg.id === messageId) 
            ? { ...msg, isRead: true } 
            : msg
        )
      );
    } catch (error) {
      console.warn('Error marking message as read:', error);
    }
  }, []);

  const contextValue = {
    conversations,
    activeConversation,
    messages,
    isLoading,
    isLoadingMore,
    hasMoreMessages,
    isSending,
    unreadMessages,
    error,
    joinConversation,
    leaveConversation,
    loadMessages,
    loadMoreMessages,
    sendMessage,
    markAsRead
  };

  return (
    <MessageContext.Provider value={contextValue}>
      {children}
    </MessageContext.Provider>
  );
};

MessageProvider.propTypes = {
  children: PropTypes.node.isRequired
};

export default MessageContext; 
