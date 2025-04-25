import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useUser } from './UserContext';
import WebSocketService from '../services/WebSocketService';

const WebSocketContext = createContext(null);

export const useWebSocket = () => useContext(WebSocketContext);

export const WebSocketProvider = ({ children }) => {
  const { user } = useUser();
  const [isConnected, setIsConnected] = useState(false);
  const [conversations, setConversations] = useState([]);
  const [activeConversation, setActiveConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMoreMessages, setHasMoreMessages] = useState(false);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const messagePageSize = 20; // Number of messages to load per page
  
  // Ref to track if we're already loading more messages
  const isLoadingMoreRef = useRef(false);

  // Load conversations from localStorage on component mount
  useEffect(() => {
    if (user) {
      loadConversationsFromStorage();
    }
  }, [user]);

  // Initialize message service connection
  useEffect(() => {
    if (!user) return;

    try {
      // Connect to the message service
      WebSocketService.connect(
        user.userId,
        user.token || localStorage.getItem('auth_token'),
        // On connected callback
        () => {
          console.log('Message service connected');
          setIsConnected(true);
        },
        // On error callback
        (errorMessage) => {
          console.error('Message service connection error:', errorMessage);
          setError('Failed to connect to messaging service');
          // Still try to load data from localStorage
          loadConversationsFromStorage();
        }
      );
    } catch (error) {
      console.error('Error setting up message service:', error);
      setError('Failed to initialize messaging service');
      // Still try to load data from localStorage
      loadConversationsFromStorage();
    }

    // Clean up on unmount
    return () => {
      WebSocketService.disconnect();
      setIsConnected(false);
    };
  }, [user]);

  // Load conversations from localStorage
  const loadConversationsFromStorage = useCallback(() => {
    if (!user) return;
    
    try {
      const storedConversations = localStorage.getItem(`judify_conversations_${user.userId}`);
      if (storedConversations) {
        const parsedConversations = JSON.parse(storedConversations);
        setConversations(parsedConversations);
        console.log('Loaded conversations from localStorage:', parsedConversations.length);
      }
    } catch (error) {
      console.error('Error loading conversations from localStorage:', error);
    }
  }, [user]);

  // Save conversations to localStorage
  const saveConversationsToStorage = useCallback((updatedConversations) => {
    if (!user) return;
    
    try {
      localStorage.setItem(`judify_conversations_${user.userId}`, JSON.stringify(updatedConversations));
    } catch (error) {
      console.error('Error saving conversations to localStorage:', error);
    }
  }, [user]);

  // Save messages for a conversation to localStorage
  const saveMessagesToStorage = useCallback((conversationId, messagesArray) => {
    if (!user || !conversationId) return;
    
    try {
      const storageKey = `judify_messages_${conversationId}`;
      localStorage.setItem(storageKey, JSON.stringify(messagesArray));
    } catch (error) {
      console.error('Error saving messages to localStorage:', error);
    }
  }, [user]);

  // Handle a new message (received or sent)
  const handleNewMessage = useCallback((message) => {
    if (!activeConversation) return;
    
    setMessages(prevMessages => {
      // Avoid duplicate messages
      if (prevMessages.some(msg => msg.messageId === message.messageId)) {
        return prevMessages;
      }
      
      const newMessages = [message, ...prevMessages];
      
      // Sort by timestamp, newest first
      newMessages.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
      
      // Only save the latest messages to storage (latest 50 messages to save space)
      saveMessagesToStorage(activeConversation.conversationId || activeConversation.id, 
                           newMessages.slice(0, 50));
      return newMessages;
    });
    
    // Update last message in conversations list
    setConversations(prevConversations => {
      const updatedConversations = prevConversations.map(conv => {
        const convId = conv.conversationId || conv.id;
        const activeConvId = activeConversation.conversationId || activeConversation.id;
        
        if (convId === activeConvId) {
          return {
            ...conv,
            lastMessage: message.content,
            updatedAt: new Date().toISOString()
          };
        }
        return conv;
      });
      
      saveConversationsToStorage(updatedConversations);
      return updatedConversations;
    });
  }, [activeConversation, saveMessagesToStorage, saveConversationsToStorage]);

  // Load messages for a conversation with pagination
  const loadMessages = useCallback(async (conversationId) => {
    if (!user || !conversationId) return { success: false };
    
    setLoading(true);
    setCurrentPage(0); // Reset to first page when loading a new conversation
    
    try {
      // Subscribe to this conversation for real-time updates
      if (isConnected) {
        const subscription = WebSocketService.subscribeToConversation(conversationId, (message) => {
          handleNewMessage(message);
        });
        
        if (!subscription) {
          console.warn(`Failed to subscribe to conversation ${conversationId}`);
        }
      }
      
      // Load existing messages from the server with pagination
      const result = await WebSocketService.loadConversationMessages(conversationId, 0, messagePageSize);
      
      if (result.success) {
        // Update state
        setMessages(result.messages);
        setHasMoreMessages(result.hasMore);
        
        // Also save to localStorage as a backup (only the first page)
        saveMessagesToStorage(conversationId, result.messages);
        
        return { success: true, messages: result.messages, hasMore: result.hasMore };
      } else {
        // If server fetch fails, try localStorage backup
        const storageKey = `judify_messages_${conversationId}`;
        const storedMessages = localStorage.getItem(storageKey);
        
        if (storedMessages) {
          try {
            const parsedMessages = JSON.parse(storedMessages);
            setMessages(parsedMessages);
            setHasMoreMessages(false); // No more messages from localStorage
            return { success: true, messages: parsedMessages, hasMore: false };
          } catch (parseError) {
            console.error('Error parsing stored messages:', parseError);
          }
        }
        
        // If all fails, return empty array
        setMessages([]);
        setHasMoreMessages(false);
        return { success: false, message: 'Failed to load messages', messages: [], hasMore: false };
      }
    } catch (error) {
      console.error('Error loading messages:', error);
      setError('Failed to load messages');
      
      // Try localStorage backup
      try {
        const storageKey = `judify_messages_${conversationId}`;
        const storedMessages = localStorage.getItem(storageKey);
        if (storedMessages) {
          const parsedMessages = JSON.parse(storedMessages);
          setMessages(parsedMessages);
          setHasMoreMessages(false);
          return { success: true, messages: parsedMessages, hasMore: false };
        }
      } catch (e) {
        console.error('Error loading messages from localStorage:', e);
      }
      
      setMessages([]);
      setHasMoreMessages(false);
      return { success: false, message: 'Failed to load messages', messages: [], hasMore: false };
    } finally {
      setLoading(false);
    }
  }, [user, isConnected, handleNewMessage, saveMessagesToStorage, messagePageSize]);

  // Load more messages (older messages) for pagination
  const loadMoreMessages = useCallback(async () => {
    if (!user || !activeConversation || !hasMoreMessages || isLoadingMoreRef.current) {
      return { success: false };
    }
    
    const conversationId = activeConversation.conversationId || activeConversation.id;
    const nextPage = currentPage + 1;
    
    isLoadingMoreRef.current = true;
    setLoadingMore(true);
    
    try {
      // Load next page of messages
      const result = await WebSocketService.loadMoreMessages(conversationId, nextPage, messagePageSize);
      
      if (result.success && result.messages.length > 0) {
        // Append older messages to the existing messages (at the end, since newer messages are at the top)
        setMessages(prevMessages => {
          const combinedMessages = [...prevMessages, ...result.messages];
          
          // Remove duplicates based on messageId
          const uniqueMessages = Array.from(
            new Map(combinedMessages.map(msg => [msg.messageId, msg])).values()
          );
          
          // Sort by timestamp, newest first
          uniqueMessages.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
          
          return uniqueMessages;
        });
        
        setCurrentPage(nextPage);
        setHasMoreMessages(result.hasMore);
        
        return { success: true, hasMore: result.hasMore };
      } else {
        // No more messages or failed to load
        setHasMoreMessages(false);
        return { success: false };
      }
    } catch (error) {
      console.error('Error loading more messages:', error);
      setError('Failed to load more messages');
      return { success: false };
    } finally {
      setLoadingMore(false);
      isLoadingMoreRef.current = false;
    }
  }, [user, activeConversation, hasMoreMessages, currentPage, messagePageSize]);

  // Get all conversations
  const getConversations = useCallback(() => {
    if (!user) return { success: false, message: 'No user logged in', conversations: [] };
    
    loadConversationsFromStorage();
    return { success: true, conversations };
  }, [user, conversations, loadConversationsFromStorage]);

  // Get or create a conversation with another user
  const getOrCreateConversation = useCallback((otherUserId) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!otherUserId) return { success: false, message: 'No recipient specified' };
    
    setLoading(true);
    
    try {
      console.log(`Creating/finding conversation between ${user.userId} and ${otherUserId}`);
      
      // Ensure otherUserId is treated as a number for comparison
      const numericOtherId = Number(otherUserId);
      
      // Check if conversation already exists
      let conversation = conversations.find(conv => 
        (conv.user1Id === user.userId && conv.user2Id === numericOtherId) || 
        (conv.user1Id === numericOtherId && conv.user2Id === user.userId)
      );
      
      // If no conversation found, create a new one
      if (!conversation) {
        // Generate a unique conversation ID
        const newConversationId = `conv_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        
        // Try to get tutor data from localStorage
        let otherUserData = null;
        try {
          // Check if we have tutor info stored
          const storedTutorInfo = localStorage.getItem('lastViewedTutor');
          if (storedTutorInfo) {
            const parsedInfo = JSON.parse(storedTutorInfo);
            
            // Check if this is the tutor we're looking for (profileId, id, or userId could match)
            if (parsedInfo.id == otherUserId || 
                parsedInfo.profileId == otherUserId || 
                parsedInfo.userId == otherUserId) {
              
              console.log('Found stored tutor info that matches requested ID:', parsedInfo);
              
              // Create user object with all available data
              otherUserData = {
                userId: numericOtherId,
                username: parsedInfo.username || `Tutor ${otherUserId}`,
                firstName: parsedInfo.firstName || 'Tutor',
                lastName: parsedInfo.lastName || `${otherUserId}`,
                profileImage: parsedInfo.profilePicture || `https://ui-avatars.com/api/?name=Tutor+${otherUserId}&background=random`
              };
            }
          }
        } catch (e) {
          console.warn('Error accessing stored tutor data:', e);
        }
        
        // Fallback to default data if no stored tutor info is found
        if (!otherUserData) {
          console.log('No stored tutor data found, creating placeholder data');
          otherUserData = {
            userId: numericOtherId,
            username: `Tutor ${otherUserId}`,
            firstName: `Tutor`,
            lastName: `${otherUserId}`,
            profileImage: `https://ui-avatars.com/api/?name=Tutor+${otherUserId}&background=random`
          };
        }
        
        console.log('Creating new conversation with data:', {
          currentUser: user,
          otherUser: otherUserData
        });
        
        // Create full conversation object
        conversation = {
          id: newConversationId,
          conversationId: newConversationId,
          user1Id: user.userId,
          user2Id: numericOtherId,
          user1: { 
            userId: user.userId, 
            username: user.username || user.firstName || 'You',
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            profileImage: user.profileImage || user.profilePicture || ''
          },
          user2: otherUserData,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          lastMessage: ''
        };
        
        // Add to conversations list
        const updatedConversations = [...conversations, conversation];
        setConversations(updatedConversations);
        saveConversationsToStorage(updatedConversations);
        
        console.log('New conversation created with ID:', newConversationId);
      } else {
        // Ensure the conversation has both id and conversationId for consistency
        if (!conversation.id && conversation.conversationId) {
          conversation.id = conversation.conversationId;
        } else if (!conversation.conversationId && conversation.id) {
          conversation.conversationId = conversation.id;
        }
        console.log('Found existing conversation:', conversation.conversationId);
      }
      
      // Set as active conversation and load messages
      setActiveConversation(conversation);
      loadMessages(conversation.conversationId || conversation.id);
      
      return { success: true, conversation };
    } catch (error) {
      console.error('Error creating conversation:', error);
      setError('Failed to create conversation');
      return { success: false, message: 'Failed to create conversation' };
    } finally {
      setLoading(false);
    }
  }, [user, conversations, saveConversationsToStorage, loadMessages, isConnected]);

  // Send a message
  const sendMessage = useCallback(async (conversationId, receiverId, content) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!conversationId) return { success: false, message: 'No conversation specified' };
    if (!content.trim()) return { success: false, message: 'Message cannot be empty' };
    
    try {
      // Ensure receiverId is treated as a number
      const numericReceiverId = Number(receiverId);
      
      console.log(`Sending message to ${numericReceiverId} in conversation ${conversationId}`);
      
      // Generate truly unique ID with high entropy
      const timestamp = Date.now();
      const randomId = Math.random().toString(36).substring(2, 10) + 
                       Math.random().toString(36).substring(2, 10);
      const uniqueId = `${timestamp}_${randomId}`;
      
      // Create the message object
      const newMessage = {
        messageId: `msg_${uniqueId}`,
        conversationId,
        senderId: user.userId,
        receiverId: numericReceiverId,
        content,
        timestamp: new Date().toISOString(),
        isRead: false,
        type: 'CHAT'
      };
      
      // Check if the same message was already sent (prevents duplicates)
      if (activeConversation) {
        const activeConvId = activeConversation.conversationId || activeConversation.id;
        const isActiveConversation = activeConvId === conversationId;
        
        if (isActiveConversation) {
          const existingMessages = messages || [];
          const isDuplicate = existingMessages.some(msg => 
            msg.senderId === user.userId && 
            msg.content === content && 
            (Date.now() - new Date(msg.timestamp).getTime()) < 5000 // Within last 5 seconds
          );
          
          if (isDuplicate) {
            console.warn('Prevented duplicate message send');
            return { success: false, message: 'Duplicate message detected' };
          }
        }
      }
      
      // Send the message using our service
      const success = await WebSocketService.sendMessage(newMessage);
      
      if (success) {
        return { 
          success: true, 
          message: newMessage,
          delivery: isConnected ? 'server' : 'local'
        };
      } else {
        // If sending via service failed, handle the message locally
        handleNewMessage(newMessage);
        
        return { 
          success: true, 
          message: newMessage,
          delivery: 'local'
        };
      }
    } catch (error) {
      console.error('Error sending message:', error);
      setError('Failed to send message');
      return { success: false, message: 'Failed to send message' };
    }
  }, [user, isConnected, activeConversation, messages, handleNewMessage]);

  // Mark a message as read
  const markMessageAsRead = useCallback(async (messageId, senderId, conversationId) => {
    if (!isConnected) return false;
    
    try {
      return await WebSocketService.markMessageAsRead(messageId, senderId, conversationId);
    } catch (error) {
      console.error('Error marking message as read:', error);
      return false;
    }
  }, [isConnected]);

  return (
    <WebSocketContext.Provider
      value={{
        isConnected,
        loading,
        loadingMore,
        hasMoreMessages,
        error,
        conversations,
        messages,
        activeConversation,
        setActiveConversation,
        getConversations,
        getOrCreateConversation,
        loadMessages,
        loadMoreMessages,
        sendMessage,
        markMessageAsRead
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
}; 