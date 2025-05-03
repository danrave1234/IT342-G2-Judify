import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useUser } from './UserContext';
import WebSocketService from '../services/websocketService';
import axios from 'axios';

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
          console.log('WebSocket service connected');
      setIsConnected(true);
        },
        // On error callback
        (errorMessage) => {
          console.error('WebSocket service connection error:', errorMessage);
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
        
        // Cache the processed conversations to localStorage for offline use
        saveConversationsToStorage(processedConversations);
        
        return { 
          success: true, 
          conversations: processedConversations,
          message: 'Loaded conversations from server'
        };
      }
      
      // Fall back to localStorage if server fetching failed
      return await loadConversationsFromStorage();
      
    } catch (error) {
      console.error('Error fetching conversations:', error);
      setError('Failed to load conversations');
      
      // Try to load from localStorage as fallback
      return await loadConversationsFromStorage();
    } finally {
      setLoading(false);
    }
  }, [user, saveConversationsToStorage, loadConversationsFromStorage]);

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
    
    const activeConvId = activeConversation.conversationId || activeConversation.id;
    const messageConvId = message.conversationId;
    
    // Only process messages for the active conversation
    if (messageConvId != activeConvId && messageConvId != activeConversation.serverConversationId) {
      console.log(`Ignoring message for non-active conversation: ${messageConvId} (active: ${activeConvId})`);
      
      // Update unread count for non-active conversations
      if (message.senderId !== user.userId) {
        setConversations(prevConversations => {
          return prevConversations.map(conv => {
            const convId = conv.conversationId || conv.id;
            const serverConvId = conv.serverConversationId;
            
            if (convId == messageConvId || serverConvId == messageConvId) {
              return {
                ...conv,
                lastMessage: message.content,
                updatedAt: new Date().toISOString(),
                unreadCount: (conv.unreadCount || 0) + 1
              };
            }
            return conv;
          });
        });
      }
      
      return;
    }
    
    // For the active conversation, add the message to the messages list
    setMessages(prevMessages => {
      // Avoid duplicate messages
      if (prevMessages.some(msg => msg.messageId === message.messageId)) {
        return prevMessages;
      }
      
      const newMessages = [...prevMessages, message];
      
      // Sort by timestamp, oldest first (ascending)
      newMessages.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
      
      // Only save the latest messages to storage (latest 50 messages to save space)
      saveMessagesToStorage(activeConversation.conversationId || activeConversation.id, 
                           newMessages.slice(0, 50));
      return newMessages;
    });
    
    // Update last message in conversations list
    setConversations(prevConversations => {
      const updatedConversations = prevConversations.map(conv => {
        const convId = conv.conversationId || conv.id;
        const serverConvId = conv.serverConversationId;
        
        if (convId == activeConvId || convId == messageConvId || 
            serverConvId == activeConvId || serverConvId == messageConvId) {
          return {
            ...conv,
            lastMessage: message.content,
            updatedAt: new Date().toISOString()
          };
        }
        return conv;
      });
      
      // Sort conversations by most recent message
      updatedConversations.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
      
      saveConversationsToStorage(updatedConversations);
      return updatedConversations;
    });
    
    // Auto-mark received messages as read if they're from someone else
    if (message.senderId !== user.userId && !message.isRead && isConnected) {
      WebSocketService.markMessageAsRead(message.messageId, message.senderId, messageConvId);
    }
  }, [activeConversation, saveMessagesToStorage, saveConversationsToStorage, user, isConnected]);

  // Load messages for a conversation with pagination
  const loadMessages = useCallback(async (conversationId) => {
    if (!user || !conversationId) return { success: false };
    
    setLoading(true);
    setCurrentPage(0); // Reset to first page when loading a new conversation
    
    try {
      // Find the conversation object to check for serverConversationId
      const conversation = conversations.find(conv => 
        conv.id === conversationId || conv.conversationId === conversationId
      );
      
      // Use server conversation ID if available (numeric database ID)
      const hasServerConversationId = conversation?.serverConversationId && 
                                      !isNaN(conversation.serverConversationId) && 
                                      !isNaN(parseInt(conversation.serverConversationId));
      
      const actualConversationId = hasServerConversationId ? 
                                   conversation.serverConversationId : 
                                   conversationId;
      
      console.log(`Loading messages for conversation ${conversationId} (server ID: ${actualConversationId})`);
      
      // Subscribe to this conversation for real-time updates FIRST to catch any messages
      // that might arrive during the loading process
      if (isConnected) {
        const subscription = WebSocketService.subscribeToConversation(actualConversationId, (message) => {
          console.log(`Received message in conversation ${actualConversationId}:`, message);
          handleNewMessage(message);
        });
        
        if (!subscription) {
          console.warn(`Failed to subscribe to conversation ${actualConversationId}`);
        } else {
          console.log(`Successfully subscribed to conversation ${actualConversationId} for real-time updates`);
        }
      } else {
        console.warn(`Not connected to WebSocket, cannot subscribe to conversation ${actualConversationId}`);
      }
      
      // Now that we're subscribed, load existing messages from the server with pagination
      const result = await WebSocketService.loadConversationMessages(actualConversationId, 0, messagePageSize);
      
      if (result.success) {
        // Update state
        setMessages(result.messages);
        setHasMoreMessages(result.hasMore);
        
        // Also save to localStorage as a backup (only the first page)
        saveMessagesToStorage(actualConversationId, result.messages);
        
        // Mark messages as read when opening the conversation
        if (isConnected && result.messages.length > 0) {
          result.messages.forEach(msg => {
            // Only mark other people's messages as read
            if (msg.senderId !== user.userId && !msg.isRead) {
              WebSocketService.markMessageAsRead(msg.messageId, msg.senderId, actualConversationId);
            }
          });
        }
        
        return { success: true, messages: result.messages, hasMore: result.hasMore };
      } else {
        // If server fetch fails, try localStorage backup
        const storageKey = `judify_messages_${actualConversationId}`;
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
        // Find the conversation object to check for serverConversationId
        const conversation = conversations.find(conv => 
          conv.id === conversationId || conv.conversationId === conversationId
        );
        
        // Use server conversation ID if available
        const hasServerConversationId = conversation?.serverConversationId && 
                                       !isNaN(conversation.serverConversationId) && 
                                       !isNaN(parseInt(conversation.serverConversationId));
        
        const actualConversationId = hasServerConversationId ? 
                                    conversation.serverConversationId : 
                                    conversationId;
        
        const storageKey = `judify_messages_${actualConversationId}`;
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
  }, [user, isConnected, handleNewMessage, saveMessagesToStorage, messagePageSize, conversations]);

  // Load more messages (older messages) for pagination
  const loadMoreMessages = useCallback(async () => {
    if (!user || !activeConversation || !hasMoreMessages || isLoadingMoreRef.current) {
      return { success: false };
    }
    
    const conversationId = activeConversation.conversationId || activeConversation.id;
    
    // Check if we have a valid server conversation ID (numeric database ID)
    const hasServerConversationId = activeConversation.serverConversationId && 
                                   !isNaN(activeConversation.serverConversationId) && 
                                   !isNaN(parseInt(activeConversation.serverConversationId));
    
    // Use server conversation ID if available
    const actualConversationId = hasServerConversationId ? 
                                activeConversation.serverConversationId : 
                                conversationId;
    
    console.log(`Loading more messages for conversation ${conversationId} (server ID: ${actualConversationId})`);
    
    const nextPage = currentPage + 1;
    
    isLoadingMoreRef.current = true;
    setLoadingMore(true);
    
    try {
      // Load next page of messages using our improved service
      const result = await WebSocketService.loadMoreMessages(actualConversationId, nextPage, messagePageSize);
      
      if (result.success && result.messages.length > 0) {
        // Append older messages to the existing messages
        setMessages(prevMessages => {
          const combinedMessages = [...prevMessages, ...result.messages];
          
          // Remove duplicates based on messageId
          const uniqueMessages = Array.from(
            new Map(combinedMessages.map(msg => [msg.messageId, msg])).values()
          );
          
          // Sort by timestamp, oldest first (ascending)
          uniqueMessages.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
          
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

  // Set active conversation and handle switching between conversations
  const setActiveConversationWithCleanup = useCallback(async (conversation) => {
    if (!conversation) return;
    
    // Ensure we're not setting the same conversation repeatedly
    if (activeConversation?.id === conversation.id || 
        activeConversation?.conversationId === conversation.conversationId) {
      return;
    }
    
    // Clean up the old subscription if necessary
    if (activeConversation) {
      const oldConversationId = activeConversation.conversationId || activeConversation.id;
      if (oldConversationId) {
        console.log(`Cleaning up old conversation: ${oldConversationId}`);
        WebSocketService.leaveConversation(oldConversationId);
      }
    }
    
    // Set the new active conversation
    setActiveConversation(conversation);
    
    // Clear any existing messages when switching conversations
    setMessages([]);
    
    console.log(`Set new active conversation: ${conversation.conversationId || conversation.id}`);
  }, [activeConversation]);

  // Get or create a conversation with another user
  const getOrCreateConversation = useCallback(async (otherUserId) => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    try {
      console.log(`Getting or creating conversation with user ${otherUserId}`);
      
      // Check if we already have a conversation with this user
      const existingConversation = conversations.find(conv => 
        (conv.user1Id === user.userId && conv.user2Id === Number(otherUserId)) ||
        (conv.user1Id === Number(otherUserId) && conv.user2Id === user.userId)
      );
      
      if (existingConversation) {
        console.log('Found existing conversation:', existingConversation);
        return { success: true, conversation: existingConversation };
      }
      
      console.log('No existing conversation found, creating new one');
      
      // Try to create a conversation using different endpoints based on user roles
      let response;
      let success = false;
      
      const endpoints = [
        {
          method: 'post',
          url: '/api/conversations', 
          data: { 
            firstUserId: Number(user.userId),
            secondUserId: Number(otherUserId)
          }
        },
        {
          method: 'post',
          url: `/api/conversations/create/${user.userId}/${otherUserId}`,
          data: {}
        }
      ];
      
      // For student-tutor conversations
      if (user.role === 'STUDENT') {
        endpoints.push({
          method: 'post',
          url: `/api/conversations/student-tutor/${user.userId}/${otherUserId}`,
          data: {}
        });
      } else if (user.role === 'TUTOR') {
        endpoints.push({
          method: 'post',
          url: `/api/conversations/student-tutor/${otherUserId}/${user.userId}`,
          data: {}
        });
      }
      
      // Try each endpoint until one works
      for (const endpoint of endpoints) {
        try {
          console.log(`Trying to create conversation with endpoint: ${endpoint.url}`);
          if (endpoint.method === 'post') {
            response = await axios.post(endpoint.url, endpoint.data);
          } else {
            response = await axios.get(endpoint.url, { params: endpoint.data });
          }
          
          if (response?.data) {
            success = true;
            console.log('Successfully created conversation:', response.data);
            break;
          }
        } catch (err) {
          console.warn(`Endpoint ${endpoint.url} failed:`, err.message);
          // Continue to next endpoint
        }
      }
      
      if (success && response?.data) {
        // Create a client-friendly conversation object
        const newConversation = {
          id: response.data.conversationId,
          conversationId: response.data.conversationId,
          serverConversationId: response.data.conversationId,
          user1Id: Number(user.userId),
          user2Id: Number(otherUserId),
          user1: user,
          user2: { userId: Number(otherUserId) }, // Basic user info until we get more
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          lastMessage: null
        };
        
        // Update conversations list
        setConversations(prev => {
          const updated = [...prev, newConversation];
          saveConversationsToStorage(updated);
          return updated;
        });
        
        return { success: true, conversation: newConversation };
      }
      
      // If server creation failed, create a local conversation
      console.log('Server conversation creation failed, creating local conversation');
      
      // Generate a unique client-side ID for this conversation
      const timestamp = Date.now();
      const randomId = Math.random().toString(36).substring(2, 10);
      const clientConversationId = `conv_${timestamp}_${randomId}`;
      
      // Create a new conversation object
      const localConversation = {
        id: clientConversationId,
        conversationId: clientConversationId,
        user1Id: Number(user.userId),
        user2Id: Number(otherUserId),
        user1: user,
        user2: { userId: Number(otherUserId) },
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        lastMessage: null
        };
        
        // Add to conversations list
      setConversations(prev => {
        const updated = [...prev, localConversation];
        saveConversationsToStorage(updated);
        return updated;
      });
      
      return { success: true, conversation: localConversation };
      
    } catch (error) {
      console.error('Error creating conversation:', error);
      return { success: false, message: 'Failed to create conversation: ' + error.message };
    }
  }, [user, conversations, saveConversationsToStorage]);

  // Send a message
  const sendMessage = useCallback(async (conversationId, receiverId, content) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!conversationId) return { success: false, message: 'No conversation specified' };
    if (!content.trim()) return { success: false, message: 'Message cannot be empty' };
    
    try {
      // Check if the conversation ID is client-generated (contains non-numeric characters)
      const isClientGenerated = conversationId && /[^0-9]/.test(conversationId.toString());
      
      // For client-generated IDs, keep as string; for server IDs, convert to number
      const actualConversationId = isClientGenerated 
        ? conversationId 
        : (Number(conversationId) || conversationId);
      
      // Always ensure receiverId is a number
      const numericReceiverId = Number(receiverId);
      const numericSenderId = Number(user.userId);
      
      console.log(`Sending message to ${numericReceiverId} in conversation ${actualConversationId}`);
      
      // Find the conversation object to check for serverConversationId
      const conversation = conversations.find(conv => 
        conv.id === conversationId || conv.conversationId === conversationId
      );
      
      // Generate truly unique ID with high entropy
      const timestamp = Date.now();
      const randomId = Math.random().toString(36).substring(2, 10) + 
                       Math.random().toString(36).substring(2, 10);
      const uniqueId = `${timestamp}_${randomId}`;
      
      // Create the message object
      const newMessage = {
        messageId: `msg_${uniqueId}`,
        conversationId: actualConversationId,
        senderId: numericSenderId,
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
      
      console.log('Sending message with data:', newMessage);
      
      // For server-based conversations (numeric IDs), try REST API
      if (!isClientGenerated && !isNaN(Number(actualConversationId))) {
        try {
          const response = await axios.post('/api/messages', {
            conversationId: Number(actualConversationId),
            senderId: numericSenderId,
            receiverId: numericReceiverId,
            content: content
          });
          
          console.log('Message sent via REST API:', response.data);
          
          // Use the server-generated message data if available
          if (response.data && response.data.messageId) {
            // Update the message with server data but keep our timestamp if server doesn't provide one
            const serverMessage = {
              ...response.data,
              timestamp: response.data.createdAt || response.data.timestamp || newMessage.timestamp
            };
            
            handleNewMessage(serverMessage);
            
            return { 
              success: true, 
              message: serverMessage,
              delivery: 'server'
            };
          }
        } catch (restError) {
          console.warn('REST API message sending failed, falling back to WebSocket:', restError);
          // Fall back to WebSocket
        }
      }
      
      // For client-generated conversation IDs or if REST API failed
      // Attempt to use WebSocket - the WebSocketService will handle this with localStorage if needed
      try {
      const success = await WebSocketService.sendMessage(newMessage);
      
      if (success) {
        // Always handle the message locally for client-side rendering
        handleNewMessage(newMessage);
        
        return { 
          success: true, 
          message: newMessage,
            delivery: isConnected ? 'websocket' : 'local'
        };
      } else {
          // If sending failed entirely, still handle locally for UI consistency
      handleNewMessage(newMessage);
          
          // Store in localStorage directly as a last resort
          const storageKey = `judify_messages_${actualConversationId}`;
          try {
            const existingMessagesStr = localStorage.getItem(storageKey);
            const existingMessages = existingMessagesStr ? JSON.parse(existingMessagesStr) : [];
            localStorage.setItem(storageKey, JSON.stringify([...existingMessages, newMessage]));
          } catch (e) {
            console.error('Failed to save message to localStorage:', e);
          }
          
          return { 
            success: true, 
            message: newMessage,
            delivery: 'local'
          };
        }
      } catch (error) {
        console.error('WebSocket message sending failed:', error);
        
        // Last resort: Store in localStorage directly
        handleNewMessage(newMessage);
        
        const storageKey = `judify_messages_${actualConversationId}`;
        try {
          const existingMessagesStr = localStorage.getItem(storageKey);
          const existingMessages = existingMessagesStr ? JSON.parse(existingMessagesStr) : [];
          localStorage.setItem(storageKey, JSON.stringify([...existingMessages, newMessage]));
        } catch (e) {
          console.error('Failed to save message to localStorage:', e);
        }
      
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
  }, [user, isConnected, activeConversation, messages, handleNewMessage, conversations]);

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
        setActiveConversation: setActiveConversationWithCleanup,
        getConversations,
        getOrCreateConversation,
        loadMessages,
        loadMoreMessages,
        sendMessage,
        markMessageAsRead,
        joinConversation: (conversationId) => WebSocketService.joinConversation(conversationId),
        leaveConversation: (conversationId) => WebSocketService.leaveConversation(conversationId)
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
}; 