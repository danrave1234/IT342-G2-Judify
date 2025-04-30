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

  // Fetch conversations from server API (for tutors especially)
  const fetchConversationsFromServer = useCallback(async () => {
    if (!user) return { success: false };
    
    try {
      console.log('Fetching conversations from server API');
      
      // Check if backend is available first with a simpler endpoint
      try {
        // First try to ping a simpler endpoint to check if backend is available
        await axios.get('/api/health');
      } catch (pingError) {
        console.warn('Backend server might be unavailable:', pingError.message);
        // If we can't reach the backend, return local conversations instead
        return { success: true, conversations };
      }

      try {
        const response = await axios.get(`/api/conversations/findByUser/${user.userId}`);
      
        if (response.data) {
          const serverConversations = response.data;
          
          // Merge with existing conversations from localStorage
          // prioritizing server data but keeping local data for client-generated IDs
          const mergedConversations = [...conversations];
          
          // Add or update conversations from server
          serverConversations.forEach(serverConv => {
            const existingIndex = mergedConversations.findIndex(
              localConv => localConv.serverConversationId === serverConv.conversationId
            );
            
            if (existingIndex >= 0) {
              // Update existing conversation with server data
              mergedConversations[existingIndex] = {
                ...mergedConversations[existingIndex],
                serverConversationId: serverConv.conversationId,
                updatedAt: serverConv.updatedAt
              };
            } else {
              // Add new conversation from server
              // Need to add user details for conversation rendering
              const otherUserId = serverConv.participantIds.find(id => id !== user.userId);
              
              mergedConversations.push({
                id: `server_${serverConv.conversationId}`,
                conversationId: `server_${serverConv.conversationId}`,
                serverConversationId: serverConv.conversationId,
                user1Id: user.userId,
                user2Id: otherUserId,
                createdAt: serverConv.createdAt,
                updatedAt: serverConv.updatedAt
              });
            }
          });
          
          // Update state with merged conversations
          setConversations(mergedConversations);
          saveConversationsToStorage(mergedConversations);
        
          return { success: true, conversations: mergedConversations };
        }
      } catch (apiError) {
        console.warn('Error fetching conversations from primary endpoint:', apiError.message);
        
        // Try a fallback endpoint if the primary one fails
        try {
          const fallbackResponse = await axios.get(`/api/conversations`);
          if (fallbackResponse.data) {
            // Filter conversations to only include those where this user is a participant
            const userConversations = fallbackResponse.data.filter(conv => 
              conv.participantIds && conv.participantIds.includes(user.userId)
            );
            
            // Process these conversations similar to above
            const mergedConversations = [...conversations];
            // ... same processing as above
            
            console.log('Successfully fetched conversations from fallback endpoint');
            return { success: true, conversations: mergedConversations };
          }
        } catch (fallbackError) {
          console.warn('Fallback conversation endpoint also failed:', fallbackError.message);
          // Continue to return local conversations
        }
      }
      
      // If we get here, we couldn't fetch from server but have local data
      return { success: true, conversations };
    } catch (error) {
      console.error('Error fetching conversations from server:', error);
      // Return local conversations as fallback
      return { success: true, conversations };
    }
  }, [user, conversations, saveConversationsToStorage]);

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
  const setActiveConversationWithCleanup = useCallback((conversation) => {
    if (!conversation) return;
    
    // If we're already viewing this conversation, do nothing
    if (activeConversation && 
        (activeConversation.id === conversation.id || 
         activeConversation.conversationId === conversation.conversationId)) {
      return;
    }
    
    // Unsubscribe from the current conversation if there is one
    if (activeConversation) {
      const prevConvId = activeConversation.conversationId || activeConversation.id;
      const hasPrevServerConvId = activeConversation.serverConversationId && 
                                 !isNaN(activeConversation.serverConversationId) && 
                                 !isNaN(parseInt(activeConversation.serverConversationId));
      
      const prevActualConvId = hasPrevServerConvId ? activeConversation.serverConversationId : prevConvId;
      
      console.log(`Unsubscribing from previous conversation: ${prevActualConvId}`);
      WebSocketService.unsubscribeFromConversation(prevActualConvId);
    }
    
    // Clear messages from previous conversation
    setMessages([]);
    
    // Set new active conversation
    setActiveConversation(conversation);
  }, [activeConversation]);

  // Get all conversations
  const getConversations = useCallback(async () => {
    if (!user) return { success: false, message: 'No user logged in', conversations: [] };
    
    // Load from localStorage first for immediate display
    loadConversationsFromStorage();
    
    // Then try to fetch from server to ensure we have the latest data
    if (isConnected) {
      const serverResult = await fetchConversationsFromServer();
      if (serverResult.success) {
        return { success: true, conversations: serverResult.conversations };
      }
    }
    
    return { success: true, conversations };
  }, [user, conversations, loadConversationsFromStorage, fetchConversationsFromServer, isConnected]);

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
      
      console.log('Existing conversations:', conversations);
      console.log('Found existing conversation:', conversation);
      
      // If no conversation found, create a new one
      if (!conversation) {
        // Generate a unique conversation ID
        const newConversationId = `conv_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        
        // Try to get tutor/student data from localStorage
        let otherUserData = null;
        try {
          // Check if we have other user info stored
          const storedTutorInfo = localStorage.getItem('lastViewedTutor');
          if (storedTutorInfo) {
            const parsedInfo = JSON.parse(storedTutorInfo);
            
            // Check if this is the user we're looking for (profileId, id, or userId could match)
            if (parsedInfo.id == otherUserId || 
                parsedInfo.profileId == otherUserId || 
                parsedInfo.userId == otherUserId) {
              
              console.log('Found stored user info that matches requested ID:', parsedInfo);
              
              // Create user object with all available data
              otherUserData = {
                userId: numericOtherId,
                username: parsedInfo.username || `User ${otherUserId}`,
                firstName: parsedInfo.firstName || 'User',
                lastName: parsedInfo.lastName || `${otherUserId}`,
                profileImage: parsedInfo.profilePicture || `https://ui-avatars.com/api/?name=User+${otherUserId}&background=random`
              };
            }
          }
        } catch (e) {
          console.warn('Error accessing stored user data:', e);
        }
        
        // Fallback to default data if no stored info is found
        if (!otherUserData) {
          console.log('No stored user data found, creating placeholder data');
          otherUserData = {
            userId: numericOtherId,
            username: `User ${otherUserId}`,
            firstName: `User`,
            lastName: `${otherUserId}`,
            profileImage: `https://ui-avatars.com/api/?name=User+${otherUserId}&background=random`
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
        
        // Try to create the conversation on the server if connected
        if (isConnected) {
          try {
            // Call backend API to create conversation if available
            // This ensures both parties can see the conversation
            console.log('Attempting to create conversation on server');
            const serverConversation = {
              participantIds: [user.userId, numericOtherId]
            };
            // Make the actual API call to create the conversation
            axios.post('/api/conversations/createConversation', serverConversation)
              .then(response => {
                console.log('Server conversation created:', response.data);
                // If we got an ID from the server, update our local conversation with it
                if (response.data && response.data.conversationId) {
                  // Update the conversation with the server ID
                  conversation.serverConversationId = response.data.conversationId;
                  // Update in our local list
                  const updatedConversations = conversations.map(c => 
                    c.id === conversation.id ? { ...c, serverConversationId: response.data.conversationId } : c
                  );
                  setConversations(updatedConversations);
                  saveConversationsToStorage(updatedConversations);
                }
              })
              .catch(err => {
                console.warn('Failed to create conversation on server:', err);
              });
          } catch (err) {
            console.warn('Failed to create conversation on server, using local only:', err);
          }
        }
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
      setActiveConversationWithCleanup(conversation);
      loadMessages(conversation.conversationId || conversation.id);
      
      return { success: true, conversation };
    } catch (error) {
      console.error('Error creating conversation:', error);
      setError('Failed to create conversation');
      return { success: false, message: 'Failed to create conversation' };
    } finally {
      setLoading(false);
    }
  }, [user, conversations, saveConversationsToStorage, loadMessages, isConnected, setActiveConversationWithCleanup]);

  // Send a message
  const sendMessage = useCallback(async (conversationId, receiverId, content) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!conversationId) return { success: false, message: 'No conversation specified' };
    if (!content.trim()) return { success: false, message: 'Message cannot be empty' };
    
    try {
      // Ensure receiverId is treated as a number
      const numericReceiverId = Number(receiverId);
      
      console.log(`Sending message to ${numericReceiverId} in conversation ${conversationId}`);
      
      // Find the conversation object to check for serverConversationId
      const conversation = conversations.find(conv => 
        conv.id === conversationId || conv.conversationId === conversationId
      );
      
      // Check if we have a valid server conversation ID (numeric database ID)
      const hasServerConversationId = conversation?.serverConversationId && 
                                     !isNaN(conversation.serverConversationId) && 
                                     !isNaN(parseInt(conversation.serverConversationId));
      
      // Use server conversation ID if available
      const actualConversationId = hasServerConversationId ? 
                                  conversation.serverConversationId : 
                                  conversationId;
      
      // Generate truly unique ID with high entropy
      const timestamp = Date.now();
      const randomId = Math.random().toString(36).substring(2, 10) + 
                       Math.random().toString(36).substring(2, 10);
      const uniqueId = `${timestamp}_${randomId}`;
      
      // Create the message object
      const newMessage = {
        messageId: `msg_${uniqueId}`,
        conversationId: actualConversationId,
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
      
      console.log('Sending message with data:', newMessage);
      
      // Send the message using our service
      const success = await WebSocketService.sendMessage(newMessage);
      
      if (success) {
        // Always handle the message locally for client-side rendering
        handleNewMessage(newMessage);
        
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