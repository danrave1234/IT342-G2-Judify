import { useState, useEffect, useRef } from 'react';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { useWebSocket } from '../../context/WebSocketContext';
import { toast } from 'react-toastify';
import ErrorBoundary from '../../components/common/ErrorBoundary';
import axios from 'axios';

const TutorMessagesFallback = () => {
  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Messages</h1>
      </div>
      
      <div className="bg-white dark:bg-dark-800 shadow-card rounded-xl overflow-hidden border border-light-700 dark:border-dark-700 p-8 text-center">
        <div className="mb-4">
          <svg className="w-16 h-16 text-yellow-500 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
          </svg>
        </div>
        <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">Messaging Service Unavailable</h3>
        <p className="text-gray-600 dark:text-gray-400 mb-4">
          We're having trouble connecting to the messaging service. This might be due to a temporary issue.
        </p>
        <button
          onClick={() => window.location.reload()}
          className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded"
        >
          Try Again
        </button>
      </div>
    </div>
  );
};

const TutorMessages = () => {
  const { user } = useUser();
  const webSocketContext = useWebSocket();
  const navigate = useNavigate();
  const location = useLocation();

  // State
  const [conversations, setConversations] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [sendingMessage, setSendingMessage] = useState(false);
  const [error, setError] = useState('');
  const [loadingMore, setLoadingMore] = useState(false);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const [fetchingInterval, setFetchingInterval] = useState(null);

  // Check if WebSocket context is unavailable
  if (!webSocketContext) {
    return <TutorMessagesFallback />;
  }

  const {
    isConnected,
    getConversations,
    getOrCreateConversation,
    loadMessages,
    loadMoreMessages,
    sendMessage,
    markMessageAsRead,
    activeConversation,
    setActiveConversation,
    messages: conversationMessages,
    loading,
    hasMoreMessages
  } = webSocketContext;

  // Load conversations when component mounts
  useEffect(() => {
    console.log('Loading tutor conversations...');
    
    // Clear any existing intervals when component mounts
    if (fetchingInterval) {
      clearInterval(fetchingInterval);
    }

    const loadInitialConversations = async () => {
      try {
        console.log('Trying to fetch tutor conversations directly from API...');
        
        // Try multiple direct API endpoints for tutor conversations in order
        const endpoints = [
          // First try the standard endpoints
          `/api/conversations`,
          // Then try more specific endpoints if available
          `/api/conversations/findByUserRole/${user.userId}/TUTOR`,
          `/api/conversations/user/${user.userId}`
        ];
        
        let foundConversations = false;
        
        for (const endpoint of endpoints) {
          try {
            console.log(`Trying direct endpoint: ${endpoint}`);
            const response = await axios.get(endpoint);
            
            if (response.data) {
              let conversationsData = response.data;
              
              // Handle different response formats
              if (!Array.isArray(conversationsData) && conversationsData.content) {
                conversationsData = conversationsData.content;
              }
              
              if (Array.isArray(conversationsData)) {
                // Filter to include only conversations where this tutor is involved
                const filteredConversations = conversationsData.filter(conv => {
                  // Check several possible structures
                  return (conv.tutor && conv.tutor.userId === user.userId) || 
                         (conv.tutorId === user.userId) ||
                         (conv.user1Id === user.userId) ||
                         (conv.user2Id === user.userId);
                });
                
                if (filteredConversations.length > 0) {
                  console.log(`Found ${filteredConversations.length} tutor conversations via ${endpoint}`);
                  
                  // Map to the expected format
                  const formattedConversations = filteredConversations.map(conv => {
                    let studentId, studentUser;
                    
                    // Handle different API formats
                    if (conv.student) {
                      // Direct student/tutor format
                      studentId = conv.student.userId;
                      studentUser = conv.student;
                    } else if (conv.studentId) {
                      // ID-only format
                      studentId = conv.studentId;
                      studentUser = { userId: conv.studentId };
                    } else if (conv.user1Id && conv.user1Id !== user.userId) {
                      // Generic user format where user1 is the student
                      studentId = conv.user1Id;
                      studentUser = conv.user1 || { userId: conv.user1Id };
                    } else if (conv.user2Id && conv.user2Id !== user.userId) {
                      // Generic user format where user2 is the student
                      studentId = conv.user2Id;
                      studentUser = conv.user2 || { userId: conv.user2Id };
                    } else {
                      // Default case if we can't determine student
                      studentId = 0;
                      studentUser = { userId: 0 };
                    }
                    
                    return {
                      id: conv.conversationId || conv.id,
                      conversationId: conv.conversationId || conv.id,
                      serverConversationId: conv.conversationId || conv.id,
                      user1Id: user.userId,
                      user2Id: studentId,
                      user1: user,
                      user2: studentUser,
                      student: studentUser,
                      tutor: user,
                      lastMessage: conv.lastMessage || "Start a conversation",
                      updatedAt: conv.updatedAt || new Date().toISOString(),
                      unreadCount: conv.unreadCount || 0
                    };
                  });
                  
                  setConversations(formattedConversations);
                  foundConversations = true;
                  break;
                }
              }
            }
          } catch (error) {
            console.warn(`Endpoint ${endpoint} failed:`, error.message);
            // Continue to the next endpoint
          }
        }
        
        // If we didn't find any conversations via direct API calls, try using the context
        if (!foundConversations) {
          console.log('No conversations found via direct API, trying context...');
          const result = await getConversations();
          
          if (result.success && result.conversations.length > 0) {
            console.log(`Found ${result.conversations.length} conversations via context`);
            setConversations(result.conversations);
          } else {
            console.log('No conversations found via any method');
          }
        }
      } catch (error) {
        console.error('Error loading initial tutor conversations:', error);
      }
    };

    loadInitialConversations();

    // Set up periodic fetching of new conversations
    // This is especially important for tutors to see when students initiate new conversations
    const interval = setInterval(async () => {
      console.log('Checking for new tutor conversations...');
      loadInitialConversations();
    }, 15000); // Check every 15 seconds

    setFetchingInterval(interval);

    // Clean up interval on component unmount
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, []);
  
  // Handle pre-selected user from navigation (e.g., from student profile)
  useEffect(() => {
    if (!location.state?.studentId) return;

    const fetchConversations = async () => {
      console.log('Fetching existing tutor conversations...');
      const result = await getConversations();
      if (result.success) {
        setConversations(result.conversations || []);
        return result.conversations || [];
      }
      return [];
    };

    // Check if we need to start a conversation based on navigation state
    const handleNavigationState = async (conversations) => {
      if (location.state?.studentId) {
        const studentId = location.state.studentId;
        console.log('Navigation requested conversation with student ID:', studentId);

        // Check if conversation already exists
        const existingConversation = conversations.find(conv =>
          (conv.user1Id === user.userId && conv.user2Id === Number(studentId)) ||
          (conv.user1Id === Number(studentId) && conv.user2Id === user.userId)
        );

        if (existingConversation) {
          console.log('Found existing conversation, selecting it:', existingConversation.conversationId);
          handleSelectConversation(existingConversation);
        } else if (location.state?.action === 'startConversation') {
          console.log('Starting new conversation with student:', studentId);
          await handleStartConversation(studentId, location.state?.sessionContext);
        }
      }
    };

    fetchConversations().then(handleNavigationState);
  }, [location.state?.studentId, location.state?.action]);

  // Check if we need to pre-select a conversation after loading
  useEffect(() => {
    // If we have conversations but no active conversation, select the first one
    if (conversations.length > 0 && !activeConversation) {
      // Sort conversations by most recent update
      const sortedConversations = [...conversations].sort(
        (a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)
      );
      
      // For tutors, highlight conversations with unread messages first
      const unreadConversations = sortedConversations.filter(conv => conv.unreadCount && conv.unreadCount > 0);
      
      if (unreadConversations.length > 0) {
        handleSelectConversation(unreadConversations[0]);
      } else {
        handleSelectConversation(sortedConversations[0]);
      }
    }
  }, [conversations, activeConversation]);

  useEffect(() => {
    // Scroll to bottom when messages change
    setTimeout(() => {
      if (messagesEndRef.current) {
        messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
      }
    }, 100);
  }, [conversationMessages]);

  const handleSelectConversation = async (conversation) => {
    try {
      if (!conversation) {
        console.error('Cannot select conversation: No conversation provided');
        return;
      }
      
      const conversationId = conversation.conversationId || conversation.id;
      if (!conversationId) {
        console.error('Cannot select conversation: Missing conversation ID');
        return;
      }
      
      // Ensure the conversation has consistent ID properties before setting it active
      const updatedConversation = {
        ...conversation,
        id: conversationId,
        conversationId: conversationId
      };
      
      // Set active conversation - this will handle unsubscribing from the previous one
      setActiveConversation(updatedConversation);
      
      // Check if we have a server conversation ID (numeric database ID)
      const hasServerConversationId = conversation.serverConversationId &&
                                     !isNaN(conversation.serverConversationId) &&
                                     !isNaN(parseInt(conversation.serverConversationId));

      // Use server conversation ID if available for loading messages
      const actualConversationId = hasServerConversationId ?
                                  conversation.serverConversationId :
                                  conversationId;

      // Load messages for the selected conversation
      console.log(`Loading messages for conversation: ${conversationId} (server ID: ${actualConversationId})`);
      const result = await loadMessages(actualConversationId);

      if (!result || !result.success) {
        console.error('Failed to load messages:', result?.message || 'Unknown error');
        // We'll still keep the conversation selected, just show empty state
      } else {
        // Update unread count on this conversation
        setConversations(prevConversations => 
          prevConversations.map(conv => {
            if ((conv.id === conversationId || conv.conversationId === conversationId)) {
              return { ...conv, unreadCount: 0 };
            }
            return conv;
          })
        );
      }
    } catch (error) {
      console.error('Error selecting conversation:', error);
      toast.error('Could not load conversation messages');
    }
  };

  const handleStartConversation = async (studentId, sessionContext = null) => {
    try {
      console.log(`Starting conversation with student ID: ${studentId}, session context:`, sessionContext);
      
      if (!studentId) {
        console.error('Cannot start conversation: No student ID provided');
        toast.error('Cannot start conversation: Missing student information');
        return;
      }
      
      // Get or create conversation with this student
      const result = await getOrCreateConversation(studentId);
      
      if (result.success) {
        // Set as active conversation and load messages
        setActiveConversation(result.conversation);
        await loadMessages(result.conversation.conversationId);
        
        // If this is a session-related conversation, send an initial message
        if (sessionContext) {
          const { sessionDate, sessionTime, subject } = sessionContext;
          const initialMessage = `Hi! I'm your tutor for the ${subject} session on ${sessionDate} at ${sessionTime}. Is there anything specific you'd like to discuss before our session?`;
          
          const sendResult = await sendMessage(result.conversation.conversationId, studentId, initialMessage);
          
          if (!sendResult.success) {
            console.warn('Initial message was not sent, but conversation was created:', sendResult.message);
          }
        } else if (location.state?.action === 'startConversation' && conversationMessages.length === 0) {
          // Send an initial greeting if this is a new conversation
          const initialMessage = `Hello, I'm your tutor. How can I help you with your studies?`;
          
          await sendMessage(result.conversation.conversationId, studentId, initialMessage);
        }
        
        // Refresh conversations list to include the new one
        const convResult = await getConversations();
        if (convResult.success) {
          setConversations(convResult.conversations);
        }

        return result.conversation;
      } else {
        console.error('Failed to start conversation:', result.message);
        toast.error('Failed to start conversation. Please try again.');
        return null;
      }
    } catch (error) {
      console.error('Error starting conversation:', error);
      toast.error('Failed to start conversation');
      return null;
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    
    if (!newMessage.trim() || !activeConversation) return;
    
    setSendingMessage(true);
    setError('');
    
    try {
      // Get the ID of the student (receiver)
      const receiverId = activeConversation.user1Id === user.userId 
        ? activeConversation.user2Id 
        : activeConversation.user1Id;
      
      // Get the actual conversation ID
      const conversationId = activeConversation.serverConversationId || activeConversation.conversationId || activeConversation.id;
      
      console.log(`Sending message to student ${receiverId} in conversation ${conversationId}`);
      
      const result = await sendMessage(conversationId, receiverId, newMessage.trim());
      
      if (result.success) {
        setNewMessage('');
        // If this was stored locally only, show a small indicator
        if (result.delivery === 'local') {
          toast.info('Message saved locally', { 
            autoClose: 2000, 
            hideProgressBar: true,
            position: 'bottom-right'
          });
        }
        
        // Ensure we scroll to the bottom after sending
        setTimeout(() => {
          if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
          }
        }, 100);
      } else {
        // Don't show duplicate message error to the user
        if (result.message !== 'Duplicate message detected') {
          setError(result.message || 'Failed to send message. Please try again.');
          toast.error('Failed to send message');
        }
      }
    } catch (err) {
      console.error('Error in handleSendMessage:', err);
      setError('Failed to send message. Please try again.');
      toast.error('Failed to send message');
    } finally {
      setSendingMessage(false);
    }
  };

  // Handle loading more messages when scrolling to top
  const handleScroll = async () => {
    const container = messagesContainerRef.current;
    if (!container || loadingMore || loading || !hasMoreMessages) return;

    // Check if we've scrolled near the top
    if (container.scrollTop < 100) {
      setLoadingMore(true);
      try {
        if (activeConversation) {
          const result = await loadMoreMessages(activeConversation.conversationId);
          if (!result || !result.success) {
            console.error('Failed to load more messages:', result?.message || 'Unknown error');
          }
        }
      } catch (error) {
        console.error('Error loading more messages:', error);
      } finally {
        setLoadingMore(false);
      }
    }
  };

  const handleViewStudentProfile = (studentId) => {
    if (!studentId) return;
    navigate(`/tutor/student-profile/${studentId}`);
  };

  return (
    <ErrorBoundary fallback={<TutorMessagesFallback />}>
      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Student Messages</h1>
          
          <div className="flex items-center space-x-4">
            {/* Connection status indicator for WebSocket */}
            <div className={`flex items-center px-3 py-1 rounded-full text-sm ${isConnected ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300' : 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300'}`}>
              <span className={`w-2 h-2 rounded-full mr-2 ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></span>
              {isConnected ? 'Connected' : 'Disconnected'}
            </div>
            
            <div className="text-sm text-gray-600 dark:text-gray-400">
              <span className="italic">Messages are stored locally</span>
            </div>
          </div>
        </div>
        
        <div className="bg-white dark:bg-dark-800 shadow-card rounded-xl overflow-hidden border border-light-700 dark:border-dark-700">
          <div className="grid grid-cols-1 md:grid-cols-3 h-[70vh]">
            {/* Conversations List */}
            <div className="border-r border-light-700 dark:border-dark-700 overflow-y-auto">
              <div className="p-4 border-b border-light-700 dark:border-dark-700">
                <h2 className="font-semibold text-gray-900 dark:text-white">
                  Student Conversations
                </h2>
                <p className="text-xs text-gray-500 mt-1">
                  Messages from students about sessions and questions
                </p>
              </div>
              
              {loading && conversations.length === 0 ? (
                <div className="flex items-center justify-center h-32">
                  <div className="w-8 h-8 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
                </div>
              ) : error ? (
                <div className="p-4 text-center text-red-600 dark:text-red-400">
                  <p className="mb-2">Error: {error}</p>
                  <button 
                    onClick={() => getConversations()}
                    className="text-primary-600 dark:text-primary-500 hover:underline"
                  >
                    Try Again
                  </button>
                </div>
              ) : conversations.length > 0 ? (
                <ul>
                  {conversations
                    .sort((a, b) => {
                      // First sort by unread (unread first)
                      if ((a.unreadCount || 0) > 0 && (b.unreadCount || 0) === 0) return -1;
                      if ((a.unreadCount || 0) === 0 && (b.unreadCount || 0) > 0) return 1;
                      // Then by update time (newest first)
                      return new Date(b.updatedAt) - new Date(a.updatedAt);
                    })
                    .map((conversation) => {
                    const otherUser = conversation.user1Id === user.userId ? conversation.user2 : conversation.user1;
                    const lastMessage = conversation.lastMessage || "Start a conversation";
                    const conversationId = conversation.id || conversation.conversationId;
                    const hasUnread = conversation.unreadCount && conversation.unreadCount > 0;
                    
                    return (
                      <div
                        key={conversationId}
                        onClick={() => handleSelectConversation(conversation)}
                        className={`p-3 flex items-center border-b dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 cursor-pointer transition-colors ${
                          (activeConversation?.id === conversationId || activeConversation?.conversationId === conversationId)
                            ? 'bg-blue-50 dark:bg-gray-800' 
                            : hasUnread ? 'bg-yellow-50 dark:bg-yellow-900/10' : ''
                        }`}
                      >
                        <div className="w-12 h-12 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 mr-3">
                          {otherUser?.profileImage ? (
                            <img
                              src={otherUser.profileImage}
                              alt={`${otherUser?.firstName || otherUser?.username || 'Student'}'s profile`}
                              className="w-full h-full object-cover"
                            />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-gray-500 dark:text-gray-400 text-lg">
                              {otherUser?.firstName?.charAt(0) || otherUser?.username?.charAt(0) || 'S'}
                            </div>
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <h3 className="font-medium text-gray-900 dark:text-white truncate">
                            {otherUser?.firstName && otherUser?.lastName 
                              ? `${otherUser.firstName} ${otherUser.lastName}`
                              : otherUser?.username || 'Student'}
                          </h3>
                          <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                            {lastMessage}
                          </p>
                        </div>
                        {conversation.unreadCount > 0 && (
                          <div className="ml-2 bg-blue-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs">
                            {conversation.unreadCount}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </ul>
              ) : (
                <div className="p-6 text-center">
                  <p className="text-gray-500 dark:text-gray-400">No conversations yet.</p>
                  <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">
                    Students will appear here when they initiate a conversation with you.
                  </p>
                </div>
              )}
            </div>
            
            {/* Messages List */}
            <div className="col-span-2 flex flex-col">
              {activeConversation ? (
                <>
                  {/* Conversation Header */}
                  <div className="flex items-center justify-between p-4 border-b dark:border-gray-700">
                    <div className="flex items-center">
                      <div className="w-10 h-10 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 mr-3">
                        {activeConversation.user1Id === user.userId 
                          ? (activeConversation.user2?.profileImage ? (
                              <img 
                                src={activeConversation.user2.profileImage} 
                                alt={`${activeConversation.user2?.firstName || activeConversation.user2?.username || 'Student'}'s profile`}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-gray-500 dark:text-gray-400 text-lg">
                                {activeConversation.user2?.firstName?.charAt(0) || activeConversation.user2?.username?.charAt(0) || 'S'}
                              </div>
                            ))
                          : (activeConversation.user1?.profileImage ? (
                              <img 
                                src={activeConversation.user1.profileImage} 
                                alt={`${activeConversation.user1?.firstName || activeConversation.user1?.username || 'Student'}'s profile`}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-gray-500 dark:text-gray-400 text-lg">
                                {activeConversation.user1?.firstName?.charAt(0) || activeConversation.user1?.username?.charAt(0) || 'S'}
                              </div>
                            ))
                        }
                      </div>
                      <div>
                        <h2 className="font-semibold text-lg text-gray-900 dark:text-white">
                          {activeConversation.user1Id === user.userId 
                            ? (activeConversation.user2?.firstName && activeConversation.user2?.lastName
                                ? `${activeConversation.user2.firstName} ${activeConversation.user2.lastName}`
                                : activeConversation.user2?.username || 'Student')
                            : (activeConversation.user1?.firstName && activeConversation.user1?.lastName
                                ? `${activeConversation.user1.firstName} ${activeConversation.user1.lastName}`
                                : activeConversation.user1?.username || 'Student')
                          }
                        </h2>
                        <p className="text-sm text-gray-600 dark:text-gray-400">
                          {isConnected ? 'Connected' : 'Offline - Messages stored locally'}
                        </p>
                      </div>
                    </div>

                    {/* View Profile button */}
                    <button
                      onClick={() => handleViewStudentProfile(
                        activeConversation.user1Id === user.userId 
                          ? activeConversation.user2Id 
                          : activeConversation.user1Id
                      )}
                      className="px-3 py-1 bg-primary-100 text-primary-700 hover:bg-primary-200 
                        dark:bg-primary-900/20 dark:text-primary-400 dark:hover:bg-primary-900/30 
                        rounded-md text-sm transition-colors"
                    >
                      View Profile
                    </button>
                  </div>
                  
                  {/* Messages Content */}
                  <div 
                    className="flex-grow overflow-y-auto p-4" 
                    ref={messagesContainerRef}
                    onScroll={handleScroll}
                  >
                    {loadingMore && (
                      <div className="flex items-center justify-center py-2">
                        <div className="w-6 h-6 border-t-2 border-primary-600 border-solid rounded-full animate-spin"></div>
                      </div>
                    )}
                    
                    {loading && !loadingMore ? (
                      <div className="flex items-center justify-center h-full">
                        <div className="w-8 h-8 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
                      </div>
                    ) : conversationMessages.length > 0 ? (
                      <div className="space-y-4">
                        {hasMoreMessages && !loadingMore && (
                          <div className="text-center">
                            <button 
                              onClick={async () => {
                                setLoadingMore(true);
                                try {
                                  await loadMoreMessages(activeConversation.conversationId);
                                } finally {
                                  setLoadingMore(false);
                                }
                              }}
                              className="text-sm text-primary-600 hover:text-primary-700 dark:text-primary-400 hover:underline"
                            >
                              Load more messages
                            </button>
                          </div>
                        )}
                        
                        {conversationMessages.map((msg) => {
                          const isMine = msg.senderId === user.userId;
                          const messageTime = msg.timestamp 
                            ? new Date(msg.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})
                            : '';
                          
                          return (
                            <div 
                              key={msg.messageId}
                              className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}
                            >
                              <div 
                                className={`max-w-[80%] p-3 rounded-lg ${
                                  isMine 
                                    ? 'bg-primary-500 text-white rounded-tr-none' 
                                    : 'bg-gray-100 dark:bg-dark-700 text-gray-900 dark:text-white rounded-tl-none'
                                }`}
                              >
                                <p>{msg.content}</p>
                                <span className={`text-xs block mt-1 ${
                                  isMine ? 'text-primary-100' : 'text-gray-500 dark:text-gray-400'
                                }`}>
                                  {messageTime}
                                </span>
                              </div>
                            </div>
                          );
                        })}
                        <div ref={messagesEndRef} />
                      </div>
                    ) : (
                      <div className="flex items-center justify-center h-full">
                        <div className="text-center">
                          <p className="text-gray-600 dark:text-gray-400">No messages yet.</p>
                          <p className="text-sm text-gray-500 dark:text-gray-500 mt-1">
                            Send a message to begin helping this student.
                          </p>
                        </div>
                      </div>
                    )}
                  </div>
                  
                  {/* Message Input */}
                  <form onSubmit={handleSendMessage} className="p-4 border-t border-light-700 dark:border-dark-700">
                    {error && (
                      <div className="mb-2 text-sm text-red-600 dark:text-red-400">
                        {error}
                      </div>
                    )}
                    <div className="flex items-center">
                      <input 
                        type="text"
                        placeholder="Type your message..."
                        value={newMessage}
                        onChange={(e) => setNewMessage(e.target.value)}
                        className="flex-grow mr-4 px-4 py-2 rounded-full bg-gray-100 dark:bg-dark-700 text-gray-900 dark:text-white"
                        disabled={sendingMessage}
                      />
                      <button 
                        type="submit"
                        disabled={!newMessage.trim() || sendingMessage}
                        className="px-4 py-2 rounded-full bg-primary-600 text-white disabled:opacity-50"
                      >
                        {sendingMessage ? 'Sending...' : 'Send'}
                      </button>
                    </div>
                  </form>
                </>
              ) : (
                <div className="flex items-center justify-center h-full">
                  <div className="text-center max-w-md p-6">
                    <div className="mb-4">
                      <svg className="w-16 h-16 text-primary-500 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"></path>
                      </svg>
                    </div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                      Your Student Messages
                    </h3>
                    <p className="text-gray-600 dark:text-gray-400 mb-4">
                      Select a conversation to view messages from your students. New messages will appear automatically.
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-500">
                      Students can initiate conversations about scheduling and learning goals. Respond promptly to build strong relationships.
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </ErrorBoundary>
  );
};

// Wrap the component with ErrorBoundary for the whole page
const TutorMessagesWithErrorBoundary = () => (
  <ErrorBoundary fallback={<TutorMessagesFallback />}>
    <TutorMessages />
  </ErrorBoundary>
);

export default TutorMessagesWithErrorBoundary; 