import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useUser } from './UserContext';

const WebSocketContext = createContext(null);

export const useWebSocket = () => useContext(WebSocketContext);

export const WebSocketProvider = ({ children }) => {
  const { user } = useUser();
  const [socket, setSocket] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [conversations, setConversations] = useState([]);
  const [activeConversation, setActiveConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Initialize WebSocket connection
  useEffect(() => {
    if (!user) return;

    // For demonstration, we'll use a free echo WebSocket server
    // In production, you would use your own WebSocket server endpoint
    const wsUrl = 'wss://echo.websocket.org';
    const newSocket = new WebSocket(wsUrl);

    newSocket.onopen = () => {
      console.log('WebSocket connected');
      setIsConnected(true);
      // Load conversations from localStorage when socket connects
      loadConversationsFromStorage();
    };

    newSocket.onmessage = (event) => {
      // Handle incoming messages
      const receivedMessage = JSON.parse(event.data);
      if (receivedMessage.type === 'chat_message' && activeConversation?.conversationId === receivedMessage.conversationId) {
        handleNewMessage(receivedMessage);
      }
    };

    newSocket.onclose = () => {
      console.log('WebSocket disconnected');
      setIsConnected(false);
    };

    newSocket.onerror = (error) => {
      console.error('WebSocket error:', error);
      setError('Failed to connect to chat service');
    };

    setSocket(newSocket);

    // Clean up on unmount
    return () => {
      if (newSocket) {
        newSocket.close();
      }
    };
  }, [user]);

  // Load conversations from localStorage
  const loadConversationsFromStorage = useCallback(() => {
    if (!user) return;
    
    try {
      const storedConversations = localStorage.getItem(`judify_conversations_${user.userId}`);
      if (storedConversations) {
        setConversations(JSON.parse(storedConversations));
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

  // Load messages for a conversation from localStorage
  const loadMessages = useCallback((conversationId) => {
    if (!user || !conversationId) return { success: false };
    
    setLoading(true);
    
    try {
      const storageKey = `judify_messages_${conversationId}`;
      const storedMessages = localStorage.getItem(storageKey);
      const messagesArray = storedMessages ? JSON.parse(storedMessages) : [];
      setMessages(messagesArray);
      
      return { success: true, messages: messagesArray };
    } catch (error) {
      console.error('Error loading messages from localStorage:', error);
      setError('Failed to load messages');
      return { success: false, message: 'Failed to load messages' };
    } finally {
      setLoading(false);
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
      const newMessages = [...prevMessages, message];
      saveMessagesToStorage(activeConversation.conversationId || activeConversation.id, newMessages);
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
  }, [user, conversations, saveConversationsToStorage, loadMessages]);

  // Send a message
  const sendMessage = useCallback((conversationId, receiverId, content) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!conversationId) return { success: false, message: 'No conversation specified' };
    if (!content.trim()) return { success: false, message: 'Message cannot be empty' };
    
    try {
      // Ensure receiverId is treated as a number
      const numericReceiverId = Number(receiverId);
      
      console.log(`Sending message to conversation ${conversationId}, receiver: ${numericReceiverId}`);
      
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
        createdAt: new Date().toISOString(),
        isRead: false,
        type: 'chat_message'
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
            (Date.now() - new Date(msg.createdAt).getTime()) < 5000 // Within last 5 seconds
          );
          
          if (isDuplicate) {
            console.warn('Prevented duplicate message send');
            return { success: false, message: 'Duplicate message detected' };
          }
        }
      }
      
      // Try to send the message through WebSocket if connected
      let socketSent = false;
      if (isConnected && socket && socket.readyState === WebSocket.OPEN) {
        try {
          console.log('WebSocket connected, sending message through socket');
          socket.send(JSON.stringify(newMessage));
          socketSent = true;
        } catch (socketError) {
          console.error('Error sending via WebSocket, falling back to local storage:', socketError);
        }
      } else {
        console.log('WebSocket not connected, saving message locally only');
      }
      
      // Always handle the message locally to ensure it's displayed and saved
      handleNewMessage(newMessage);
      
      return { 
        success: true, 
        message: newMessage,
        delivery: socketSent ? 'socket' : 'local'
      };
    } catch (error) {
      console.error('Error sending message:', error);
      setError('Failed to send message');
      return { success: false, message: 'Failed to send message' };
    }
  }, [user, socket, isConnected, activeConversation, messages, handleNewMessage]);

  return (
    <WebSocketContext.Provider
      value={{
        isConnected,
        loading,
        error,
        conversations,
        messages,
        activeConversation,
        setActiveConversation,
        getConversations,
        getOrCreateConversation,
        loadMessages,
        sendMessage
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
}; 