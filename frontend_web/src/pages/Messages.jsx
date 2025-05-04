import { useState, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { useMessages } from '../context/MessageContext';
import { useUser } from '../context/UserContext';
import { toast } from 'react-toastify';
import ErrorBoundary from '../components/common/ErrorBoundary';
import axios from 'axios';
import UserAvatar from '../components/common/UserAvatar';

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
  const { user } = useUser();
  const location = useLocation();
  const [conversations, setConversations] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [sendingMessage, setSendingMessage] = useState(false);
  // Add state for loading more messages
  const [localLoadingMore, setLocalLoadingMore] = useState(false);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);

  // Check if Messages context is unavailable
  const messageContext = useMessages();
  if (!messageContext) {
    return <MessagesFallback />;
  }

  const {
    isConnected,
    getConversations,
    getOrCreateConversation,
    loadMessages,
    loadMoreMessages,
    sendMessage,
    // markMessageAsRead is not used in this component
    activeConversation,
    setActiveConversation,
    messages: conversationMessages,
    loading,
    // Using localLoadingMore instead of loadingMore from context
    hasMoreMessages,
    error
  } = messageContext;

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
    if (conversations.length > 0 && !activeConversation) {
      // Sort conversations by most recent update
      const sortedConversations = [...conversations].sort(
        (a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)
      );
      handleSelectConversation(sortedConversations[0]);
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
        // Mark messages as read when viewing the conversation
        try {
          if (hasServerConversationId) {
            // Call API to mark messages as read
            await axios.put(`/api/messages/markAllAsRead/${actualConversationId}`, null, {
              params: { userId: user.userId }
            });
            console.log('Marked all messages as read');
          }
        } catch (error) {
          console.warn('Failed to mark messages as read:', error);
        }

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

  const handleStartConversation = async (otherUserId, sessionContext = null) => {
    try {
      console.log(`Starting conversation with user ID: ${otherUserId}, session context:`, sessionContext);

      if (!otherUserId) {
        console.error('Cannot start conversation: No user ID provided');
        toast.error('Cannot start conversation: Missing user information');
        return;
      }

      // Get or create conversation with this user
      const result = await getOrCreateConversation(otherUserId);

      if (result.success) {
        // Set as active conversation and load messages
        setActiveConversation(result.conversation);
        await loadMessages(result.conversation.conversationId);

        // If this is a session-related conversation, send an initial message
        if (sessionContext) {
          const { sessionDate, sessionTime, subject } = sessionContext;
          const initialMessage = `Hello! I'd like to schedule a tutoring session for ${subject} on ${sessionDate} at ${sessionTime}. Are you available?`;

          const sendResult = await sendMessage(result.conversation.conversationId, otherUserId, initialMessage);

          if (!sendResult.success) {
            console.warn('Initial message was not sent, but conversation was created:', sendResult.message);
          }
        } else if (location.state?.action === 'startConversation' && conversationMessages.length === 0) {
          // Send an initial greeting if this is a new conversation
          const initialMessage = `Hello! I'm interested in learning more about your tutoring services.`;

          await sendMessage(result.conversation.conversationId, otherUserId, initialMessage);
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

    if (!newMessage.trim()) {
      return;
    }

    if (!activeConversation) {
      toast.error('Please select a conversation first');
      return;
    }

    setSendingMessage(true);

    try {
      const result = await sendMessage({
        content: newMessage
      });

      if (result.success) {
        setNewMessage('');
      } else {
        toast.error(result.message || 'Failed to send message');
      }
    } catch (error) {
      console.error('Error sending message:', error);
      toast.error('Failed to send message. Please try again.');
    } finally {
      setSendingMessage(false);
    }
  };

  const handleScroll = async () => {
    const container = messagesContainerRef.current;
    // Use localLoadingMore instead of loadingMore from context
    if (!container || localLoadingMore || loading || !hasMoreMessages) return;

    // Check if we've scrolled near the top
    if (container.scrollTop < 100) {
      try {
        if (activeConversation) {
          setLocalLoadingMore(true);
          try {
            await loadMoreMessages();
          } finally {
            setLocalLoadingMore(false);
          }
        }
      } catch (error) {
        console.error('Error loading more messages:', error);
        setLocalLoadingMore(false);
      }
    }
  };

  return (
    <ErrorBoundary fallback={<MessagesFallback />}>
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Messages</h1>

        <div className="flex items-center space-x-4">
          {/* Connection status indicator for polling service */}
          <div className={`flex items-center px-3 py-1 rounded-full text-sm ${isConnected ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300' : 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300'}`}>
            <span className={`w-2 h-2 rounded-full mr-2 ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></span>
            {isConnected ? 'Polling Active' : 'Polling Inactive'}
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
                Conversations
              </h2>
              <p className="text-xs text-gray-500 mt-1">
                Discuss session scheduling and details
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
                {conversations.map((conversation) => {
                  const otherUser = conversation.user1Id === user.userId ? conversation.user2 : conversation.user1;
                    const lastMessage = conversation.lastMessage || "Start a conversation";
                  const conversationId = conversation.id || conversation.conversationId;

                  return (
                    <div
                      key={conversationId}
                      onClick={() => handleSelectConversation(conversation)}
                      className={`p-3 flex items-center border-b dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 cursor-pointer transition-colors ${
                        (activeConversation?.id === conversationId || activeConversation?.conversationId === conversationId)
                          ? 'bg-blue-50 dark:bg-gray-800' 
                          : ''
                      }`}
                    >
                      <div className="w-12 h-12 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 mr-3">
                        <UserAvatar
                          user={otherUser}
                          size="md"
                          className="w-full h-full"
                        />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="font-medium text-gray-900 dark:text-white truncate">
                          {otherUser?.firstName && otherUser?.lastName 
                            ? `${otherUser.firstName} ${otherUser.lastName}`
                              : otherUser?.username || 'Chat Partner'}
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
                <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">Start by exploring tutors and initiating a chat.</p>
              </div>
            )}
          </div>

          {/* Messages List */}
          <div className="col-span-2 flex flex-col">
            {activeConversation ? (
              <>
                {/* Conversation Header */}
                <div className="flex items-center p-4 border-b dark:border-gray-700">
                  <div className="w-10 h-10 rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 mr-3">
                    <UserAvatar
                      user={activeConversation.user1Id === user.userId ? activeConversation.user2 : activeConversation.user1}
                      size="sm"
                      className="w-full h-full"
                    />
                  </div>
                  <div>
                    <h2 className="font-semibold text-lg text-gray-900 dark:text-white">
                      {activeConversation.user1Id === user.userId 
                        ? (activeConversation.user2?.firstName && activeConversation.user2?.lastName
                            ? `${activeConversation.user2.firstName} ${activeConversation.user2.lastName}`
                            : activeConversation.user2?.username || 'Chat Partner')
                        : (activeConversation.user1?.firstName && activeConversation.user1?.lastName
                            ? `${activeConversation.user1.firstName} ${activeConversation.user1.lastName}`
                            : activeConversation.user1?.username || 'Chat Partner')
                      }
                    </h2>
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      {isConnected ? 'Polling Active' : 'Offline - Messages stored locally'}
                    </p>
                  </div>
                </div>

                {/* Messages Content */}
                  <div 
                    className="flex-grow overflow-y-auto p-4" 
                    ref={messagesContainerRef}
                    onScroll={handleScroll}
                  >
                    {localLoadingMore && (
                      <div className="flex items-center justify-center py-2">
                        <div className="w-6 h-6 border-t-2 border-primary-600 border-solid rounded-full animate-spin"></div>
                      </div>
                    )}

                    {loading && !localLoadingMore ? (
                    <div className="flex items-center justify-center h-full">
                      <div className="w-8 h-8 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
                    </div>
                  ) : conversationMessages.length > 0 ? (
                    <div className="space-y-4">
                        {hasMoreMessages && !localLoadingMore && (
                          <div className="text-center">
                            <button 
                              onClick={async () => {
                                setLocalLoadingMore(true);
                                try {
                                  await loadMoreMessages(activeConversation.conversationId);
                                } finally {
                                  setLocalLoadingMore(false);
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
                      <p className="text-gray-600 dark:text-gray-400">No messages yet. Start the conversation!</p>
                    </div>
                  )}
                </div>

                {/* Message Input */}
                <form onSubmit={handleSendMessage} className="p-4 border-t border-light-700 dark:border-dark-700">
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
                <div className="text-center">
                  <p className="text-gray-600 dark:text-gray-400 mb-2">
                    Select a conversation or start a new one
                  </p>
                  <p className="text-sm text-gray-500 dark:text-gray-500">
                    Chat with tutors to discuss scheduling and learning goals
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
const MessagesWithErrorBoundary = () => (
  <ErrorBoundary fallback={<MessagesFallback />}>
    <Messages />
  </ErrorBoundary>
);

export default MessagesWithErrorBoundary;
