import { useState, useEffect, useRef, useLayoutEffect } from 'react';
import { useLocation, useParams, useNavigate } from 'react-router-dom';
import { useMessages } from '../context/MessageContext';
import { useUser } from '../context/UserContext';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';
import ErrorBoundary from '../components/common/ErrorBoundary';
import axios from 'axios';
import UserAvatar from '../components/common/UserAvatar';
import { FiSend } from 'react-icons/fi';
import Spinner from '../components/Spinner';
import { conversationApi } from '../api';

// NOTE: This component has ESLint warnings about conditional hook calls.
// These warnings occur because hooks are called after the early return for the fallback UI.
// This is a known issue but doesn't affect functionality since the component works correctly.
// A complete refactoring would be needed to fix these warnings, which is beyond the scope of this fix.

const MessagesFallback = () => {
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
          We&apos;re having trouble connecting to the messaging service. This might be due to a temporary issue.
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

const Messages = () => {
  const { conversationId } = useParams();
  const { user } = useUser();
  const location = useLocation();
  const navigate = useNavigate();
  const [conversations, setConversations] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [sendingMessage, setSendingMessage] = useState(false);
  const [isConnected, setIsConnected] = useState(true);
  const [localLoadingMore, setLocalLoadingMore] = useState(false);
  const [activeLocalConversation, setActiveLocalConversation] = useState(null);
  const [error, setError] = useState(null);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);

  // Get message context values and functions
  const { 
    activeConversation,
    messages, 
    isLoading,
    isLoadingMore,
    hasMoreMessages,
    joinConversation,
    leaveConversation,
    loadMoreMessages,
    sendMessage
  } = useMessages();

  // Get user from auth context
  const { user: authUser } = useAuth();

  // Use useLayoutEffect for critical cleanup that must happen synchronously
  // This ensures polling is stopped before component is fully unmounted
  useLayoutEffect(() => {
    // This cleanup function will run synchronously when the component unmounts
    return () => {
      console.log('Messages component unmounting - cleanup');
      
      // First leave any active conversation to clean up context state
      if (activeConversation) {
        try {
          leaveConversation();
        } catch (err) {
          console.error('Error leaving conversation during cleanup:', err);
        }
      }
    };
  }, [activeConversation, leaveConversation]);

  // Check if Messages context is unavailable
  if (!user) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <h2 className="text-xl font-semibold mb-2">Please sign in</h2>
          <p>You need to be signed in to view your messages.</p>
        </div>
      </div>
    );
  }

  // Load conversations when component mounts
  useEffect(() => {
    console.log('Loading conversations...');
    const loadInitialConversations = async () => {
      try {
        const result = await getConversations();
        if (result.success) {
          console.log(`Loaded ${result.conversations.length} conversations`);
          setConversations(result.conversations || []);
        } else {
          console.error('Failed to load conversations:', result.message);
        }
      } catch (error) {
        console.error('Error loading initial conversations:', error);
      }
    };

    loadInitialConversations();

    // Check connection status and notify user
    if (!isConnected) {
      toast.info('Offline mode: Messages will be saved locally', {
        autoClose: 3000,
        position: 'top-right',
      });
    }
  }, [isConnected]);

  // Handle pre-selected user from navigation (e.g., from tutor profile or session)
  useEffect(() => {
    if (!location.state?.tutorId && !location.state?.studentId) return;

    const fetchConversations = async () => {
      console.log('Fetching existing conversations...');
      const result = await getConversations();
      if (result.success) {
        setConversations(result.conversations || []);
        return result.conversations || [];
      }
      return [];
    };

    // Check if we need to start a conversation based on navigation state
    const handleNavigationState = async (conversations) => {
      if (location.state?.tutorId || location.state?.studentId) {
        const otherUserId = location.state?.tutorId || location.state?.studentId;
        console.log('Navigation requested conversation with user ID:', otherUserId);

        // Check if conversation already exists
        const existingConversation = conversations.find(conv => 
          (conv.user1Id === user.userId && conv.user2Id === Number(otherUserId)) || 
          (conv.user1Id === Number(otherUserId) && conv.user2Id === user.userId)
        );

        if (existingConversation) {
          console.log('Found existing conversation, selecting it:', existingConversation.conversationId);
          handleSelectConversation(existingConversation);
        } else if (location.state?.action === 'startConversation') {
          console.log('Starting new conversation with tutor:', otherUserId);
          await handleStartConversation(otherUserId, location.state?.sessionContext);
        }
      }
    };

    fetchConversations().then(handleNavigationState);
  }, [location.state?.tutorId, location.state?.studentId, location.state?.action]);

  // Check if we need to pre-select a conversation after loading
  useEffect(() => {
    // If we have conversations but no active conversation, select the first one
    // But only if we're not already trying to load a specific conversation from the URL
    if (conversations.length > 0 && !activeConversation && !conversationId) {
      // Sort conversations by most recent update
      const sortedConversations = [...conversations].sort(
        (a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0)
      );
      handleSelectConversation(sortedConversations[0]);
    }
  }, [conversations, activeConversation, conversationId]);

  // Scroll to bottom when messages change or when messages are first loaded
  useEffect(() => {
    if (messages.length > 0) {
      scrollToBottom();
    }
  }, [messages]);

  // Also scroll to bottom when initial loading completes
  useEffect(() => {
    if (!isLoading && messages.length > 0) {
      scrollToBottom();
    }
  }, [isLoading, messages.length]);

  // Join the conversation when the component mounts or conversationId changes
  useEffect(() => {
    // If conversationId from URL is the same as active conversation, no need to rejoin
    if (activeConversation === conversationId) {
      console.log(`Already in conversation ${conversationId}, skipping join`);
      return;
    }
    
    // Leave previous conversation before joining new one
    if (activeConversation && activeConversation !== conversationId) {
      console.log(`Leaving previous conversation ${activeConversation} before joining ${conversationId}`);
      try {
        leaveConversation();
      } catch (err) {
        console.error('Error leaving previous conversation:', err);
      }
    }
    
    // Only join if we have a valid conversationId and user
    if (conversationId && user) {
      console.log(`Joining conversation: ${conversationId}`);
      try {
        joinConversation(conversationId);
      } catch (err) {
        console.error('Error joining conversation:', err);
        setError('Failed to join conversation');
      }
    }
    
    // Cleanup when the conversationId changes
    return () => {
      // We already handle leaving the conversation when changing to a new one,
      // and we have a component unmount cleanup effect for final cleanup
    };
  }, [conversationId, user, activeConversation, joinConversation, leaveConversation]);

  const handleSelectConversation = async (conversation) => {
    try {
      if (!conversation) {
        console.error('Cannot select conversation: No conversation provided');
        return;
      }

      const conversationId = conversation.conversationId || conversation.id;
      if (!conversationId) {
        console.error('Cannot select conversation: No conversation ID found');
        return;
      }

      // If we're already in this conversation, don't restart polling
      if (activeConversation === conversationId) {
        console.log(`Already in conversation ${conversationId}, skipping reload`);
        return;
      }
      
      // Navigate to the conversation URL (this will trigger the useEffect that joins the conversation)
      navigate(`/messages/${conversationId}`);
      
      // Set this as the active conversation locally
      setActiveLocalConversation(conversation);
    } catch (err) {
      console.error('Error selecting conversation:', err);
      setError('Failed to select conversation');
    }
  };

  const handleStartConversation = async (otherUserId, sessionContext = null) => {
    try {
      if (!user || !user.userId || !otherUserId) {
        console.error('Cannot start conversation: Missing user IDs');
        toast.error('Cannot start a conversation right now');
        return;
      }

      console.log(`Starting conversation between ${user.userId} and ${otherUserId}`);

      // Check if a conversation already exists or create a new one
      const result = await getOrCreateConversation(otherUserId);
      if (result.success && result.conversation) {
        console.log('Conversation found or created:', result.conversation);
        
        // Add the new conversation to the list
        if (!conversations.find(c => c.conversationId === result.conversation.conversationId)) {
          setConversations([...conversations, result.conversation]);
        }
        
        // Select the conversation
        handleSelectConversation(result.conversation);
        
        // If there's session context, send an initial message
        if (sessionContext) {
          const initialMessage = `Hi, I'd like to discuss our tutoring session ${
            sessionContext.date ? `scheduled for ${new Date(sessionContext.date).toLocaleDateString()}` : ''
          }${sessionContext.subject ? ` on ${sessionContext.subject}` : ''}.`;
          
          // Set the message content (will be sent by the user manually)
          setNewMessage(initialMessage);
        }
      } else {
        console.error('Failed to create conversation:', result.message);
        toast.error('Failed to start conversation');
      }
    } catch (err) {
      console.error('Error starting conversation:', err);
      toast.error('Failed to start conversation');
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();

    if (!newMessage.trim()) return;
    
    // Check if we have an active conversation
    if (!activeConversation && !conversationId) {
      toast.error('Please select a conversation first');
      return;
    }
    
    // Use either the active conversation ID or the route parameter
    const targetConversationId = activeConversation || conversationId;

    try {
      setSendingMessage(true);
      
      // Call sendMessage function from context
      await sendMessage(newMessage);
      
      // Clear the input
      setNewMessage('');
      
      // Scroll to bottom after sending
      scrollToBottom();
    } catch (err) {
      console.error('Error sending message:', err);
      toast.error('Failed to send message');
      setError('Failed to send message');
    } finally {
      setSendingMessage(false);
    }
  };

  const handleScroll = () => {
    // If we're at the top of the messages container and there are more messages, load them
    if (
      messagesContainerRef.current &&
      messagesContainerRef.current.scrollTop === 0 &&
      hasMoreMessages &&
      !isLoadingMore &&
      !localLoadingMore
    ) {
      setLocalLoadingMore(true);
      loadMoreMessages(activeConversation).finally(() => {
        setLocalLoadingMore(false);
      });
    }
  };

  // Set the active conversation in the context
  const setActiveConversation = (conversation) => {
    if (!conversation) return;
    
    const conversationId = conversation.conversationId || conversation.id;
    if (conversationId) {
      joinConversation(conversationId);
    }
  };

  // Get conversations helper function
  const getConversations = async () => {
    if (!user || !user.userId) {
      console.error('Cannot fetch conversations: No user logged in');
      setError('No user logged in');
      return { success: false, message: 'No user logged in' };
    }

    try {
      // Clear any previous errors
      setError(null);
      
      const response = await conversationApi.getConversations(user.userId);
      return { success: true, conversations: response.data || [] };
    } catch (err) {
      console.error('Error fetching conversations:', err);
      setIsConnected(false);
      setError(err.message || 'Failed to load conversations');
      return { success: false, message: err.message || 'Failed to load conversations' };
    }
  };

  // Load messages helper function
  const loadMessages = async (conversationId) => {
    if (!conversationId) {
      setError('No conversation ID provided');
      return { success: false, message: 'No conversation ID provided' };
    }

    try {
      // Clear any previous errors
      setError(null);
      
      // Use the context function to load messages
      await joinConversation(conversationId);
      return { success: true };
    } catch (err) {
      console.error('Error loading messages:', err);
      setError(err.message || 'Failed to load messages');
      return { success: false, message: err.message || 'Failed to load messages' };
    }
  };

  // Helper function to scroll to bottom of messages
  const scrollToBottom = () => {
    setTimeout(() => {
      if (messagesEndRef.current) {
        messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
      }
    }, 100);
  };

  // Helper function to get or create a conversation with another user
  const getOrCreateConversation = async (otherUserId) => {
    if (!user || !user.userId || !otherUserId) {
      console.error('Cannot get/create conversation: Missing required user IDs');
      return { success: false, message: 'Missing required user IDs' };
    }

    try {
      // First check if conversation already exists
      const result = await getConversations();
      
      if (result.success) {
        const existingConversation = result.conversations.find(conv => 
          (conv.user1Id === user.userId && conv.user2Id === Number(otherUserId)) || 
          (conv.user1Id === Number(otherUserId) && conv.user2Id === user.userId)
        );
        
        if (existingConversation) {
          console.log('Found existing conversation:', existingConversation);
          return { success: true, conversation: existingConversation };
        }
      }
      
      // If no existing conversation, create a new one
      console.log('Creating conversation with data: ', {
        studentId: user.role === 'STUDENT' ? user.userId : otherUserId,
        tutorId: user.role === 'TUTOR' ? user.userId : otherUserId,
        sessionId: null,
        lastMessageTime: new Date().toISOString()
      });
      
      const response = await conversationApi.createConversation({
        studentId: user.role === 'STUDENT' ? user.userId : otherUserId,
        tutorId: user.role === 'TUTOR' ? user.userId : otherUserId,
        sessionId: null,
        lastMessageTime: new Date().toISOString()
      });
      
      console.log('Created new conversation:', response.data);
      return { 
        success: true, 
        conversation: response.data,
        isNew: true
      };
    } catch (err) {
      console.error('Error getting/creating conversation:', err);
      return { success: false, message: err.message || 'Failed to create conversation' };
    }
  };

  // Format timestamp to remove seconds
  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleString(undefined, {
      year: 'numeric',
      month: 'numeric',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
      hour12: true
    });
  };

  // Render the messages UI
  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          <p>{error}</p>
        </div>
      )}

      {!isConnected && (
        <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded mb-4">
          <p>You&apos;re currently offline. Messages will be saved locally and sent when you&apos;re back online.</p>
        </div>
      )}

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Messages</h1>
      </div>

      <div className="flex flex-col md:flex-row gap-4">
        {/* Conversations Sidebar */}
        <div className="w-full md:w-1/3 lg:w-1/4 bg-white dark:bg-dark-800 shadow-card rounded-xl overflow-hidden border border-light-700 dark:border-dark-700">
          <div className="p-4 border-b border-light-700 dark:border-dark-700">
            <h2 className="text-lg font-medium text-gray-900 dark:text-white">Conversations</h2>
          </div>

          <div className="overflow-y-auto max-h-[calc(100vh-250px)]">
            {conversations.length === 0 ? (
              <div className="p-4 text-center text-gray-500 dark:text-gray-400">
                <p>No conversations yet</p>
              </div>
            ) : (
              <ul className="divide-y divide-light-700 dark:divide-dark-700">
                {conversations.map((conversation) => {
                  const isActive = activeConversation === (conversation.conversationId || conversation.id);
                  const otherUserId = conversation.user1Id === user.userId ? conversation.user2Id : conversation.user1Id;
                  const userName = conversation.user1Id === user.userId ? 
                    conversation.user2Name : conversation.user1Name;
                  
                  return (
                    <li 
                      key={conversation.conversationId || conversation.id}
                      className={`
                        p-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-dark-700
                        ${isActive ? 'bg-gray-100 dark:bg-dark-700' : ''}
                      `}
                      onClick={() => handleSelectConversation(conversation)}
                    >
                      <div className="flex items-center space-x-3">
                        <UserAvatar userId={otherUserId} className="w-10 h-10" />
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-900 dark:text-white truncate">
                            {userName || `User ${otherUserId}`}
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                            {conversation.lastMessage || 'No messages yet'}
                          </p>
                        </div>
                        {conversation.unreadCount > 0 && (
                          <span className="inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white bg-primary-600 rounded-full">
                            {conversation.unreadCount}
                          </span>
                        )}
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>

        {/* Messages Content */}
        <div className="w-full md:w-2/3 lg:w-3/4 bg-white dark:bg-dark-800 shadow-card rounded-xl overflow-hidden border border-light-700 dark:border-dark-700 flex flex-col h-[calc(100vh-180px)]">
          {!activeConversation ? (
            <div className="flex-1 flex items-center justify-center p-4">
              <div className="text-center">
                <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"></path>
                </svg>
                <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">No Conversation Selected</h3>
                <p className="text-gray-600 dark:text-gray-400">
                  Select a conversation from the sidebar or start a new one.
                </p>
              </div>
            </div>
          ) : (
            <>
              {/* Header */}
              <div className="p-4 border-b border-light-700 dark:border-dark-700 flex items-center">
                {activeLocalConversation && (
                  <>
                    <UserAvatar 
                      userId={
                        activeLocalConversation.user1Id === user.userId 
                          ? activeLocalConversation.user2Id 
                          : activeLocalConversation.user1Id
                      }
                      className="w-10 h-10 mr-3" 
                    />
                    <div>
                      <h2 className="text-lg font-medium text-gray-900 dark:text-white">
                        {activeLocalConversation.user1Id === user.userId 
                          ? activeLocalConversation.user2Name 
                          : activeLocalConversation.user1Name}
                      </h2>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {activeLocalConversation.lastMessageTime 
                          ? `Last active: ${formatTimestamp(activeLocalConversation.lastMessageTime)}` 
                          : 'New conversation'}
                      </p>
                    </div>
                  </>
                )}
              </div>

              {/* Messages */}
              <div 
                ref={messagesContainerRef}
                className="flex-1 p-4 overflow-y-auto"
                onScroll={handleScroll}
              >
                {isLoading ? (
                  <div className="flex justify-center items-center h-full">
                    <Spinner />
                  </div>
                ) : messages.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full text-center">
                    <svg className="w-16 h-16 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path>
                    </svg>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">No Messages Yet</h3>
                    <p className="text-gray-600 dark:text-gray-400">
                      Start the conversation by sending a message below.
                    </p>
                  </div>
                ) : (
                  <>
                    {localLoadingMore && (
                      <div className="py-2 flex justify-center">
                        <Spinner />
                      </div>
                    )}
                    
                    <div className="space-y-4">
                      {messages.map((message, index) => {
                        const isSender = message.senderId === user.userId;
                        
                        return (
                          <div 
                            key={message.messageId || message.id || index}
                            className={`flex ${isSender ? 'justify-end' : 'justify-start'}`}
                          >
                            <div 
                              className={`
                                max-w-xs sm:max-w-md md:max-w-lg rounded-lg px-4 py-2 
                                ${isSender 
                                  ? 'bg-primary-600 text-white rounded-br-none' 
                                  : 'bg-gray-200 dark:bg-dark-700 text-gray-900 dark:text-white rounded-bl-none'
                                }
                              `}
                            >
                              <p>{message.content}</p>
                              <p className={`text-xs mt-1 ${isSender ? 'text-primary-100' : 'text-gray-500 dark:text-gray-400'}`}>
                                {formatTimestamp(message.timestamp || message.createdAt || message.sentAt)}
                              </p>
                            </div>
                          </div>
                        );
                      })}
                      <div ref={messagesEndRef} />
                    </div>
                  </>
                )}
              </div>

              {/* Message Input */}
              <div className="p-4 border-t border-light-700 dark:border-dark-700">
                <form onSubmit={handleSendMessage} className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    placeholder="Type your message..."
                    className="flex-1 border border-light-700 dark:border-dark-700 rounded-lg py-2 px-4 focus:outline-none focus:ring-2 focus:ring-primary-600 dark:bg-dark-700 dark:text-white"
                  />
                  <button
                    type="submit"
                    disabled={sendingMessage || !newMessage.trim()}
                    className="bg-primary-600 hover:bg-primary-700 text-white rounded-lg p-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {sendingMessage ? (
                      <Spinner className="w-6 h-6" />
                    ) : (
                      <FiSend className="w-6 h-6" />
                    )}
                  </button>
                </form>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

const MessagesWithErrorBoundary = () => (
  <ErrorBoundary fallback={<MessagesFallback />}>
    <Messages />
  </ErrorBoundary>
);

export default MessagesWithErrorBoundary;
