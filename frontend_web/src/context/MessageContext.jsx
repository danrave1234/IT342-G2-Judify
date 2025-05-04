import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useUser } from './UserContext';
import PollingService from '../services/pollingService';
import axios from 'axios';
import PropTypes from 'prop-types';

const MessageContext = createContext(null);

export const useMessages = () => useContext(MessageContext);

export const MessageProvider = ({ children }) => {
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

  // Initialize connection with the polling service when user is available
  useEffect(() => {
    if (!user) return;

    // Connect to the polling service
    PollingService.connect(
      user.userId,
      user.token || localStorage.getItem('auth_token'),
      // On connected callback
      () => {
        console.log('Polling service connected');
        setIsConnected(true);
        // Load conversations if any
        loadConversationsFromStorage();
      },
      // On error callback
      (errorMessage) => {
        console.error('Polling service connection error:', errorMessage);
        setError('Failed to connect to messaging service');
        // Still try to load data from localStorage
        loadConversationsFromStorage();
      }
    );

    // Clean up on unmount
    return () => {
      PollingService.disconnect();
      setIsConnected(false);
    };
  }, [user]);

  // Load conversations from localStorage
  const loadConversationsFromStorage = useCallback(async () => {
    if (!user) return { success: false, message: 'No user logged in' };

    try {
      console.log('Loading conversations from localStorage');
      const storageKey = `judify_conversations_${user.userId}`;
      const storedConversations = localStorage.getItem(storageKey);

      if (storedConversations) {
        const parsedConversations = JSON.parse(storedConversations);
        console.log(`Loaded ${parsedConversations.length} conversations from localStorage`);

        setConversations(parsedConversations);
        return { 
          success: true, 
          conversations: parsedConversations,
          message: 'Loaded conversations from localStorage'
        };
      } else {
        console.log('No conversations found in localStorage');
        return { 
          success: true, 
          conversations: [],
          message: 'No conversations found in localStorage'
        };
      }
    } catch (error) {
      console.error('Error loading conversations from localStorage:', error);
      return { 
        success: false, 
        conversations: [],
        message: 'Error loading conversations from localStorage'
      };
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

  // Load conversations from the server
  const getConversations = useCallback(async () => {
    if (!user) return { success: false, message: 'No user logged in' };

    setLoading(true);
    setError(null);

    try {
      console.log('Fetching conversations from server for user:', user.userId);

      // Try multiple endpoints to get conversations - needed for different user roles
      const endpoints = [
        `/api/conversations/user/${user.userId}`,
        `/api/conversations/findByUser/${user.userId}`,
        `/api/conversations/findByTutor/${user.userId}`,
        `/api/conversations/findByStudent/${user.userId}`,
        `/api/conversations` // Fallback to get all conversations and filter client-side
      ];

      let serverConversations = [];
      let serverSuccess = false;

      // Try endpoints until one works
      for (const endpoint of endpoints) {
        try {
          console.log(`Trying to fetch conversations from endpoint: ${endpoint}`);
          const response = await axios.get(endpoint);

          // Handle different response formats
          if (response?.data) {
            if (Array.isArray(response.data)) {
              // Direct array of conversations
              serverConversations = response.data;

              // If using the fallback all-conversations endpoint, filter to only include this user's conversations
              if (endpoint === '/api/conversations') {
                console.log('Using fallback endpoint, filtering conversations for current user');
                serverConversations = serverConversations.filter(conv => 
                  (conv.student && conv.student.userId === user.userId) || 
                  (conv.tutor && conv.tutor.userId === user.userId)
                );
              }

              serverSuccess = true;
              console.log(`Successfully fetched ${serverConversations.length} conversations from ${endpoint}`);
              break;
            } else if (response.data.content && Array.isArray(response.data.content)) {
              // Paginated response
              serverConversations = response.data.content;
              serverSuccess = true;
              console.log(`Successfully fetched ${serverConversations.length} conversations from ${endpoint} (paginated)`);
              break;
            } else if (response.data.conversations && Array.isArray(response.data.conversations)) {
              // Wrapped in conversations field
              serverConversations = response.data.conversations;
              serverSuccess = true;
              console.log(`Successfully fetched ${serverConversations.length} conversations from ${endpoint} (wrapped)`);
              break;
            }
          }
        } catch (err) {
          console.warn(`Endpoint ${endpoint} failed:`, err.message);
          // Continue to next endpoint
        }
      }

      if (serverSuccess && serverConversations.length > 0) {
        // Map server conversations to include client-friendly IDs and properties
        const processedConversations = serverConversations.map(conv => {
          // Handle different conversation formats
          let studentUser, tutorUser, otherUser, otherUserId;

          // Check if conversation has student/tutor fields
          if (conv.student && conv.tutor) {
            studentUser = conv.student;
            tutorUser = conv.tutor;

            // Determine who the other user is based on current user role
            if (user.userId === studentUser.userId) {
              otherUser = tutorUser;
              otherUserId = tutorUser.userId;
            } else {
              otherUser = studentUser;
              otherUserId = studentUser.userId;
            }
          } 
          // Handle different conversation formats (user1/user2 format)
          else if (conv.user1 && conv.user2) {
            if (conv.user1.userId === user.userId) {
              otherUser = conv.user2;
              otherUserId = conv.user2.userId;
            } else {
              otherUser = conv.user1;
              otherUserId = conv.user1.userId;
            }
          }
          // Handle minimal format with just IDs
          else {
            // Find the other user ID
            if (conv.user1Id === user.userId) {
              otherUserId = conv.user2Id;
            } else if (conv.user2Id === user.userId) {
              otherUserId = conv.user1Id;
            } else if (conv.studentId === user.userId) {
              otherUserId = conv.tutorId;
            } else if (conv.tutorId === user.userId) {
              otherUserId = conv.studentId;
            }

            // Create minimal other user object
            otherUser = { userId: otherUserId };
          }

          // Basic conversation object with fallbacks for missing fields
          return {
            // Keep both client-side ID and server ID for reference
            id: conv.conversationId || conv.id,
            conversationId: conv.conversationId || conv.id,
            serverConversationId: conv.conversationId || conv.id,

            // Add user references - ensure we have both IDs
            user1Id: user.userId,
            user2Id: otherUserId,
            user1: user,
            user2: otherUser,

            // Add student/tutor references if available
            student: conv.student || (user.role === 'STUDENT' ? user : otherUser),
            tutor: conv.tutor || (user.role === 'TUTOR' ? user : otherUser),

            // Extra data for display
            lastMessage: conv.lastMessage || "Start a conversation",
            updatedAt: conv.updatedAt || new Date().toISOString(),
            unreadCount: conv.unreadCount || 0
          };
        });

        console.log(`Processed ${processedConversations.length} conversations`);

        // Update state with fetched conversations
        setConversations(processedConversations);

        // Save to localStorage for offline access
        saveConversationsToStorage(processedConversations);

        setLoading(false);
        return { 
          success: true, 
          conversations: processedConversations,
          message: 'Conversations loaded successfully'
        };
      } else {
        // No conversations or failed to fetch
        console.log('No conversations found or failed to fetch from server, using localStorage');

        // Load from localStorage as fallback
        const storageResult = await loadConversationsFromStorage();

        setLoading(false);
        return { 
          success: storageResult.success, 
          conversations: storageResult.conversations || [],
          message: serverSuccess ? 'No conversations found on server' : 'Failed to fetch from server, using localStorage'
        };
      }
    } catch (error) {
      console.error('Error fetching conversations:', error);

      // Load from localStorage as fallback
      const storageResult = await loadConversationsFromStorage();

      setLoading(false);
      setError('Failed to fetch conversations from server');

      return { 
        success: storageResult.success, 
        conversations: storageResult.conversations || [],
        message: 'Error fetching conversations from server, using localStorage'
      };
    }
  }, [user, loadConversationsFromStorage, saveConversationsToStorage]);

  // Get or create a conversation with another user
  const getOrCreateConversation = useCallback(async (otherUserId) => {
    if (!user) return { success: false, message: 'No user logged in' };

    try {
      console.log(`Getting or creating conversation with user ${otherUserId}`);

      // First try to find an existing conversation with this user
      const existingConversation = conversations.find(conv => 
        (conv.user1Id === user.userId && conv.user2Id === Number(otherUserId)) || 
        (conv.user1Id === Number(otherUserId) && conv.user2Id === user.userId) ||
        (conv.student?.userId === user.userId && conv.tutor?.userId === Number(otherUserId)) ||
        (conv.student?.userId === Number(otherUserId) && conv.tutor?.userId === user.userId)
      );

      if (existingConversation) {
        console.log('Found existing conversation:', existingConversation);
        return { 
          success: true, 
          conversation: existingConversation,
          message: 'Found existing conversation'
        };
      }

      // If no existing conversation, create one
      console.log('No existing conversation found, creating new one');

      // Multiple endpoints to try for conversation creation
      const endpoints = [
        '/api/conversations',
        '/api/conversations/create'
      ];

      for (const endpoint of endpoints) {
        try {
          console.log(`Trying to create conversation with endpoint: ${endpoint}`);

          // Determine conversation payload based on user roles
          let payload;
          if (user.role === 'STUDENT') {
            payload = {
              studentId: user.userId,
              tutorId: Number(otherUserId)
            };
          } else if (user.role === 'TUTOR') {
            payload = {
              studentId: Number(otherUserId),
              tutorId: user.userId
            };
          } else {
            // Fallback if role is not determined or is something else
            payload = {
              user1Id: user.userId,
              user2Id: Number(otherUserId)
            };
          }

          const response = await axios.post(endpoint, payload);

          if (response?.data) {
            console.log('New conversation created:', response.data);

            // Create conversation object from response
            const newConversation = {
              // Keep both client-side ID and server ID for reference
              id: response.data.conversationId || response.data.id,
              conversationId: response.data.conversationId || response.data.id,
              serverConversationId: response.data.conversationId || response.data.id,

              // Add user references
              user1Id: user.userId,
              user2Id: Number(otherUserId),
              user1: user,
              user2: { userId: Number(otherUserId) },

              // Add student/tutor references if available
              student: user.role === 'STUDENT' ? user : { userId: Number(otherUserId) },
              tutor: user.role === 'TUTOR' ? user : { userId: Number(otherUserId) },

              // Extra data for display
              lastMessage: "",
              updatedAt: new Date().toISOString(),
              unreadCount: 0
            };

            // Add to conversations list
            const updatedConversations = [...conversations, newConversation];
            setConversations(updatedConversations);

            // Save to localStorage
            saveConversationsToStorage(updatedConversations);

            return { 
              success: true, 
              conversation: newConversation,
              message: 'New conversation created'
            };
          }
        } catch (err) {
          console.warn(`Failed to create conversation with endpoint ${endpoint}:`, err.message);
          // Try the next endpoint
        }
      }

      throw new Error('All conversation creation endpoints failed');
    } catch (error) {
      console.error('Error getting or creating conversation:', error);
      return { 
        success: false, 
        message: `Failed to create conversation: ${error.message}`
      };
    }
  }, [user, conversations, saveConversationsToStorage]);

  // Set active conversation and subscribe to messages
  useEffect(() => {
    if (!activeConversation) return;

    const conversationId = activeConversation.conversationId || activeConversation.id;
    console.log(`Subscribing to messages for conversation ${conversationId}`);

    // Set up subscription to receive new messages
    const subscription = PollingService.subscribeToConversation(
      conversationId, 
      (message) => {
        console.log('New message received:', message);

        // Add new message to state
        setMessages((prevMessages) => {
          // Check if message is already in the list
          const messageExists = prevMessages.some(m => 
            m.messageId === message.messageId || 
            m.id === message.messageId ||
            m.messageId === message.id ||
            m.id === message.id
          );

          if (messageExists) {
            return prevMessages;
          }

          // Add new message and sort by timestamp
          const updatedMessages = [...prevMessages, message].sort((a, b) => 
            new Date(b.timestamp || b.createdAt) - new Date(a.timestamp || a.createdAt)
          );

          return updatedMessages;
        });

        // Update conversation last message
        setConversations((prevConversations) => {
          const updatedConversations = prevConversations.map(conv => {
            if (conv.conversationId === conversationId || conv.id === conversationId) {
              return {
                ...conv,
                lastMessage: message.content,
                updatedAt: message.timestamp || message.createdAt || new Date().toISOString()
              };
            }
            return conv;
          });

          // Save updated conversations to localStorage
          saveConversationsToStorage(updatedConversations);

          return updatedConversations;
        });
      }
    );

    // Join the conversation
    PollingService.joinConversation(conversationId);

    return () => {
      // Unsubscribe when the active conversation changes
      if (subscription) {
        subscription.unsubscribe();
      }

      // Leave the conversation
      PollingService.leaveConversation(conversationId);
    };
  }, [activeConversation, saveConversationsToStorage]);

  // Load messages for a conversation
  const loadMessages = useCallback(async (conversationId) => {
    if (!conversationId) {
      console.error('No conversation ID provided');
      return { success: false, message: 'No conversation ID provided' };
    }

    setLoading(true);
    setCurrentPage(0);
    setMessages([]);
    setError(null);

    try {
      console.log(`Loading messages for conversation ${conversationId}`);
      const result = await PollingService.loadConversationMessages(conversationId, 0, messagePageSize);

      setLoading(false);

      if (result.success) {
        setMessages(result.messages);
        setHasMoreMessages(result.hasMore);
        console.log(`Loaded ${result.messages.length} messages`);
        return { 
          success: true, 
          messages: result.messages,
          hasMore: result.hasMore
        };
      } else {
        setError('Failed to load messages');
        console.error('Failed to load messages:', result.message);
        return { 
          success: false, 
          message: result.message
        };
      }
    } catch (error) {
      setLoading(false);
      setError('Error loading messages');
      console.error('Error loading messages:', error);
      return { 
        success: false, 
        message: error.message
      };
    }
  }, [messagePageSize]);

  // Load more messages for the current conversation
  const loadMoreMessages = useCallback(async () => {
    if (!activeConversation) {
      console.error('No active conversation');
      return { success: false, message: 'No active conversation' };
    }

    // Use ref to prevent duplicate calls while loading
    if (isLoadingMoreRef.current || loadingMore || !hasMoreMessages) {
      console.log('Not loading more messages: already loading or no more messages');
      return { success: false, message: 'Already loading or no more messages' };
    }

    const conversationId = activeConversation.conversationId || activeConversation.id;
    const nextPage = currentPage + 1;

    isLoadingMoreRef.current = true;
    setLoadingMore(true);

    try {
      console.log(`Loading more messages for conversation ${conversationId}, page ${nextPage}`);
      const result = await PollingService.loadMoreMessages(conversationId, nextPage, messagePageSize);

      setLoadingMore(false);
      isLoadingMoreRef.current = false;

      if (result.success) {
        setCurrentPage(nextPage);

        // Append new messages to existing messages, ensuring no duplicates
        setMessages((prevMessages) => {
          const existingMessageIds = new Set(prevMessages.map(m => m.messageId || m.id));
          const newMessages = result.messages.filter(m => 
            !existingMessageIds.has(m.messageId) && !existingMessageIds.has(m.id)
          );

          const updatedMessages = [...prevMessages, ...newMessages].sort((a, b) => 
            new Date(b.timestamp || b.createdAt) - new Date(a.timestamp || a.createdAt)
          );

          return updatedMessages;
        });

        setHasMoreMessages(result.hasMore);

        console.log(`Loaded ${result.messages.length} more messages`);
        return { 
          success: true, 
          messages: result.messages,
          hasMore: result.hasMore
        };
      } else {
        console.error('Failed to load more messages:', result.message);
        return { 
          success: false, 
          message: result.message
        };
      }
    } catch (error) {
      setLoadingMore(false);
      isLoadingMoreRef.current = false;
      console.error('Error loading more messages:', error);
      return { 
        success: false, 
        message: error.message
      };
    }
  }, [activeConversation, currentPage, hasMoreMessages, loadingMore, messagePageSize]);

  // Send a message in the current conversation
  const sendMessage = useCallback(async (message) => {
    if (!user) {
      console.error('No user logged in');
      return { success: false, message: 'No user logged in' };
    }

    if (!activeConversation) {
      console.error('No active conversation');
      return { success: false, message: 'No active conversation' };
    }

    const conversationId = activeConversation.conversationId || activeConversation.id;

    try {
      console.log(`Sending message in conversation ${conversationId}:`, message);

      // Determine recipient ID
      const recipientId = activeConversation.user1Id === user.userId ? 
        activeConversation.user2Id : activeConversation.user1Id;

      const messageData = {
        ...message,
        conversationId,
        senderId: user.userId,
        receiverId: recipientId,
        timestamp: new Date().toISOString()
      };

      // Send the message
      const result = await PollingService.sendMessage(messageData);

      if (result.success) {
        console.log('Message sent successfully:', result.message);

        // Update conversation with last message
        setConversations((prevConversations) => {
          const updatedConversations = prevConversations.map(conv => {
            if (conv.conversationId === conversationId || conv.id === conversationId) {
              return {
                ...conv,
                lastMessage: messageData.content,
                updatedAt: new Date().toISOString()
              };
            }
            return conv;
          });

          // Sort conversations by most recent update
          updatedConversations.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));

          // Save updated conversations to localStorage
          saveConversationsToStorage(updatedConversations);

          return updatedConversations;
        });

        return { 
          success: true, 
          message: result.message
        };
      } else {
        console.error('Failed to send message:', result.message);
        return { 
          success: false, 
          message: result.message
        };
      }
    } catch (error) {
      console.error('Error sending message:', error);
      return { 
        success: false, 
        message: error.message
      };
    }
  }, [user, activeConversation, saveConversationsToStorage]);

  // Mark a message as read
  const markMessageAsRead = useCallback(async (messageId, conversationId) => {
    if (!user) {
      console.error('No user logged in');
      return { success: false, message: 'No user logged in' };
    }

    try {
      console.log(`Marking message ${messageId} as read`);

      // Use polling service to mark message as read
      // Only pass messageId as that's all the polling service needs now
      const result = await PollingService.markMessageAsRead(messageId);

      // Update unread count in conversations
      if (result.success) {
        setConversations((prevConversations) => {
          const updatedConversations = prevConversations.map(conv => {
            if (conv.conversationId === conversationId || conv.id === conversationId) {
              return {
                ...conv,
                unreadCount: Math.max(0, (conv.unreadCount || 0) - 1)
              };
            }
            return conv;
          });

          // Save updated conversations to localStorage
          saveConversationsToStorage(updatedConversations);

          return updatedConversations;
        });
      }

      return result;
    } catch (error) {
      console.error('Error marking message as read:', error);
      return { 
        success: false, 
        message: error.message
      };
    }
  }, [user, saveConversationsToStorage]);

  // The context value
  const contextValue = {
    // Connection state
    isConnected,

    // Conversations
    conversations,
    getConversations,
    getOrCreateConversation,

    // Active conversation
    activeConversation,
    setActiveConversation,

    // Messages
    messages,
    loadMessages,
    loadMoreMessages,
    sendMessage,
    markMessageAsRead,

    // Loading state
    loading,
    loadingMore,
    hasMoreMessages,

    // Error state
    error
  };

  return (
    <MessageContext.Provider
      value={contextValue}
    >
      {children}
    </MessageContext.Provider>
  );
};

MessageProvider.propTypes = {
  children: PropTypes.node.isRequired
};

export default MessageProvider; 
