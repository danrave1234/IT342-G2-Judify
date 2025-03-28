import { useState, useEffect, useRef } from 'react';
import { useLocation, Link } from 'react-router-dom';
import { useUser } from '../context/UserContext';
import { useMessage } from '../context/MessageContext';

const Messages = () => {
  const { user } = useUser();
  const { 
    getConversations, 
    getOrCreateConversation, 
    loadMessages, 
    sendMessage, 
    activeConversation, 
    setActiveConversation,
    messages: conversationMessages,
    loading
  } = useMessage();
  const location = useLocation();
  
  const [conversations, setConversations] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [sendingMessage, setSendingMessage] = useState(false);
  const [error, setError] = useState('');
  
  const messagesEndRef = useRef(null);
  
  // Handle pre-selected user from navigation (e.g., from tutor profile or session)
  useEffect(() => {
    const fetchConversations = async () => {
      const result = await getConversations();
      if (result.success) {
        setConversations(result.conversations);
      }
    };

    fetchConversations();

    // Handle navigation from other pages (e.g., tutor/student sending a message)
    if (location.state?.tutorId || location.state?.studentId) {
      const otherUserId = location.state?.tutorId || location.state?.studentId;
      handleStartConversation(otherUserId);
    }
  }, []);

  useEffect(() => {
    // Scroll to bottom when messages change
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [conversationMessages]);

  const handleSelectConversation = async (conversation) => {
    setActiveConversation(conversation);
    await loadMessages(conversation.conversationId);
  };

  const handleStartConversation = async (otherUserId) => {
    const result = await getOrCreateConversation(otherUserId);
    if (result.success) {
      await loadMessages(result.conversation.conversationId);
      
      // Refresh conversations list to include the new one
      const convResult = await getConversations();
      if (convResult.success) {
        setConversations(convResult.conversations);
      }
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    
    if (!newMessage.trim() || !activeConversation) return;
    
    setSendingMessage(true);
    
    try {
      const receiverId = activeConversation.user1Id === user.userId 
        ? activeConversation.user2Id 
        : activeConversation.user1Id;
      
      await sendMessage(activeConversation.conversationId, receiverId, newMessage.trim());
      setNewMessage('');
    } catch (err) {
      setError('Failed to send message. Please try again.');
    } finally {
      setSendingMessage(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">Messages</h1>
      
      <div className="bg-white dark:bg-dark-800 shadow-card rounded-xl overflow-hidden border border-light-700 dark:border-dark-700">
        <div className="grid grid-cols-1 md:grid-cols-3 h-[70vh]">
          {/* Conversations List */}
          <div className="border-r border-light-700 dark:border-dark-700 overflow-y-auto">
            <div className="p-4 border-b border-light-700 dark:border-dark-700">
              <h2 className="font-semibold text-gray-900 dark:text-white">Conversations</h2>
            </div>
            
            {loading && conversations.length === 0 ? (
              <div className="flex items-center justify-center h-32">
                <div className="w-8 h-8 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
              </div>
            ) : conversations.length > 0 ? (
              <ul>
                {conversations.map((conv) => {
                  const isActive = activeConversation?.conversationId === conv.conversationId;
                  const otherUser = conv.user1Id === user.userId ? conv.user2 : conv.user1;
                  
                  return (
                    <li 
                      key={conv.conversationId}
                      onClick={() => handleSelectConversation(conv)}
                      className={`p-4 cursor-pointer border-b border-light-700 dark:border-dark-700 
                        ${isActive ? 'bg-primary-50 dark:bg-primary-900/20' : 'hover:bg-gray-50 dark:hover:bg-dark-700'}`}
                    >
                      <div className="flex items-center">
                        <div className="w-10 h-10 rounded-full bg-gray-200 dark:bg-dark-700 mr-3"></div>
                        <div>
                          <h3 className="font-medium text-gray-900 dark:text-white">
                            {otherUser?.username || 'User'}
                          </h3>
                          <p className="text-sm text-gray-600 dark:text-gray-400 truncate">
                            {conv.lastMessage || 'Start a conversation'}
                          </p>
                        </div>
                      </div>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <div className="p-4 text-center text-gray-600 dark:text-gray-400">
                No conversations yet
              </div>
            )}
          </div>
          
          {/* Message Area */}
          <div className="col-span-2 flex flex-col h-full">
            {activeConversation ? (
              <>
                {/* Header */}
                <div className="p-4 border-b border-light-700 dark:border-dark-700 flex items-center">
                  <div className="w-8 h-8 rounded-full bg-gray-200 dark:bg-dark-700 mr-3"></div>
                  <h3 className="font-medium text-gray-900 dark:text-white">
                    {activeConversation.user1Id === user.userId 
                      ? activeConversation.user2?.username || 'User' 
                      : activeConversation.user1?.username || 'User'}
                  </h3>
                </div>
                
                {/* Messages */}
                <div className="flex-grow overflow-y-auto p-4">
                  {loading ? (
                    <div className="flex items-center justify-center h-full">
                      <div className="w-8 h-8 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
                    </div>
                  ) : conversationMessages.length > 0 ? (
                    <div className="space-y-4">
                      {conversationMessages.map((msg) => {
                        const isMine = msg.senderId === user.userId;
                        
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
                                {new Date(msg.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                              </span>
                            </div>
                          </div>
                        );
                      })}
                      <div ref={messagesEndRef} />
                    </div>
                  ) : (
                    <div className="flex items-center justify-center h-full">
                      <p className="text-gray-600 dark:text-gray-400">
                        No messages yet. Start the conversation!
                      </p>
                    </div>
                  )}
                </div>
                
                {/* Input Area */}
                <div className="p-4 border-t border-light-700 dark:border-dark-700">
                  {error && (
                    <div className="mb-2 text-sm text-red-600 dark:text-red-400">
                      {error}
                    </div>
                  )}
                  <form onSubmit={handleSendMessage} className="flex gap-2">
                    <input
                      type="text"
                      value={newMessage}
                      onChange={(e) => setNewMessage(e.target.value)}
                      placeholder="Type a message..."
                      className="input flex-grow"
                      disabled={sendingMessage}
                    />
                    <button
                      type="submit"
                      disabled={!newMessage.trim() || sendingMessage}
                      className="btn-primary whitespace-nowrap"
                    >
                      {sendingMessage ? 'Sending...' : 'Send'}
                    </button>
                  </form>
                </div>
              </>
            ) : (
              <div className="flex items-center justify-center h-full">
                <p className="text-gray-600 dark:text-gray-400">
                  Select a conversation to start messaging
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Messages; 