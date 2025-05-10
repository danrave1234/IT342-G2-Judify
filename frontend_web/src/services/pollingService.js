import { messageApi, conversationApi } from '../api';

// Constants for polling
const POLLING_INTERVAL = 5000; // 5 seconds
const MAX_RETRIES = 3;
const BACKOFF_FACTOR = 1.5; // Exponential backoff factor

// Store active polling intervals for cleanup
const activePolling = new Map();

// Track if we're actively on the messages page to avoid unnecessary polling
let isMessagingActive = false;
let isShuttingDown = false;

/**
 * Message polling service that provides real-time updates for conversations
 * This implements a similar approach to the mobile app with multiple endpoint fallbacks
 */
export const messagePollingService = {
  /**
   * Start polling for new messages in a conversation
   * @param {number} conversationId - ID of the conversation to poll
   * @param {Function} onNewMessages - Callback function when new messages arrive
   * @param {number} initialTimestamp - Optional initial timestamp to fetch messages after
   * @param {Function} onError - Optional error callback
   * @returns {Function} Function to stop polling
   */
  startPolling: (conversationId, onNewMessages, initialTimestamp = null, onError = null) => {
    if (!conversationId) {
      console.error('Cannot start polling: conversationId is undefined');
      return () => {};
    }

    // If we're in the process of shutting down, don't start new polling
    if (isShuttingDown) {
      console.log('Not starting polling because app is shutting down');
      return () => {};
    }

    // Set messaging as active
    isMessagingActive = true;
    
    console.log(`Starting polling for conversation ${conversationId}`);

    // Stop any existing polling for this conversation
    messagePollingService.stopPolling(conversationId);

    // Set initial timestamp if not provided
    let lastTimestamp = initialTimestamp || new Date().toISOString();
    let retryCount = 0;
    let currentInterval = POLLING_INTERVAL;
    let skipNextPoll = false;

    // Define the polling function
    const poll = async () => {
      // If we're shutting down or not on the messages page, don't poll
      if (isShuttingDown || !isMessagingActive || skipNextPoll) {
        skipNextPoll = false;
        return;
      }

      console.log(`Polling for new messages since ${lastTimestamp} in conversation ${conversationId}`);

      try {
        // Fetch new messages
        const messages = await fetchNewMessages(conversationId, lastTimestamp);
        
        if (messages.length > 0) {
          console.log(`Received ${messages.length} new messages for conversation ${conversationId}`);
          
          // Update the timestamp to the latest message
          const latestMessage = messages.reduce((latest, current) => {
            const latestTime = new Date(latest.timestamp || latest.createdAt || latest.sentAt).getTime();
            const currentTime = new Date(current.timestamp || current.createdAt || current.sentAt).getTime();
            return currentTime > latestTime ? current : latest;
          }, messages[0]);
          
          lastTimestamp = latestMessage.timestamp || latestMessage.createdAt || latestMessage.sentAt;
          console.log(`Updated timestamp to ${lastTimestamp}`);
          
          // Reset retry count on successful fetch
          retryCount = 0;
          currentInterval = POLLING_INTERVAL;
          
          // Call the callback with new messages only if messaging is still active
          if (isMessagingActive && !isShuttingDown) {
            onNewMessages(messages);
          }
        } else {
          console.log(`No new messages found since ${lastTimestamp}`);
        }
      } catch (error) {
        // Only log the error if debugging is needed - reduce console spam
        if (retryCount === 0) {
          console.error(`Error polling for conversation ${conversationId}:`, error);
        }
        
        // Implement exponential backoff
        retryCount++;
        if (retryCount <= MAX_RETRIES) {
          currentInterval = POLLING_INTERVAL * Math.pow(BACKOFF_FACTOR, retryCount);
          console.log(`Retry ${retryCount}/${MAX_RETRIES} with interval ${currentInterval}ms`);
        } else {
          retryCount = 0; // Reset after max retries
          currentInterval = POLLING_INTERVAL;
          // Skip the next poll to give the system a break
          skipNextPoll = true;
          console.log(`Max retries reached, skipping next poll`);
        }
        
        // Call error callback if provided
        if (onError && isMessagingActive && !isShuttingDown) {
          onError(error);
        }
      }
    };

    // Start initial poll immediately for faster feedback
    poll();

    // Set up interval for repeated polling
    const intervalId = setInterval(poll, POLLING_INTERVAL);
    
    // Store the interval ID for cleanup
    activePolling.set(conversationId, {
      interval: intervalId,
      poll: poll  // Store the poll function for manual polling if needed
    });

    // Return function to stop polling
    return () => messagePollingService.stopPolling(conversationId);
  },

  /**
   * Stop polling for a specific conversation
   * @param {number} conversationId - ID of the conversation to stop polling
   */
  stopPolling: (conversationId) => {
    if (activePolling.has(conversationId)) {
      const polling = activePolling.get(conversationId);
      
      // Clear the interval
      if (polling.interval) clearInterval(polling.interval);
      
      activePolling.delete(conversationId);
      console.log(`Stopped polling for conversation ${conversationId}`);
    }
  },

  /**
   * Stop all active polling intervals
   */
  stopAllPolling: () => {
    isShuttingDown = true;
    
    if (activePolling.size > 0) {
      console.log(`Stopping all polling (${activePolling.size} active polls)`);
      
      activePolling.forEach((polling, conversationId) => {
        if (polling.interval) clearInterval(polling.interval);
        console.log(`Stopped polling for conversation ${conversationId}`);
      });
      
      activePolling.clear();
    }
    
    // Reset messaging state
    isMessagingActive = false;
    
    // Reset shutdown flag after a short delay to allow cleanup to complete
    setTimeout(() => {
      isShuttingDown = false;
    }, 1000);
  },

  /**
   * Manually trigger a poll for new messages
   * @param {number} conversationId - ID of the conversation to poll
   */
  forcePoll: (conversationId) => {
    if (activePolling.has(conversationId)) {
      const polling = activePolling.get(conversationId);
      if (polling.poll) {
        console.log(`Manually polling for conversation ${conversationId}`);
        polling.poll();
      }
    }
  },

  /**
   * Set the active state of messaging
   * @param {boolean} active - Whether messaging is active
   */
  setMessagingActive: (active) => {
    if (active === isMessagingActive) return; // No change
    
    isMessagingActive = active;
    console.log(`Messaging active state set to: ${active}`);
    
    // If messaging is becoming inactive, stop all polling
    if (!active) {
      messagePollingService.stopAllPolling();
    }
  },

  /**
   * Join a conversation to start receiving messages
   * @param {number} conversationId - ID of the conversation to join
   * @returns {Promise} Promise that resolves when joined
   */
  joinConversation: async (conversationId) => {
    if (!conversationId) {
      console.error('Cannot join conversation: conversationId is undefined');
      return Promise.reject(new Error('Conversation ID is required'));
    }

    // Set messaging as active
    isMessagingActive = true;
    console.log(`Joining conversation ${conversationId}`);

    try {
      // Try to get conversation details
      const response = await conversationApi.getConversation(conversationId);
      
      // Check if we got a valid response
      if (!response || !response.data) {
        console.warn(`No data returned for conversation ${conversationId}`);
        // Return a minimal conversation object instead of rejecting
        return { conversationId };
      }
      
      // If needed, update conversation names (make sure participants have names)
      try {
        await conversationApi.updateConversationNames(conversationId);
      } catch (nameError) {
        console.warn('Could not update conversation names, continuing anyway');
      }
      
      return response.data;
    } catch (error) {
      console.error(`Error joining conversation ${conversationId}:`, error);
      // Instead of propagating the error, return a minimal conversation object
      // This prevents the UI from breaking when auth token isn't available yet
      return { conversationId };
    }
  },

  /**
   * Load initial messages for a conversation
   * @param {number} conversationId - ID of the conversation
   * @param {Object} params - Optional parameters like page, size
   * @returns {Promise<Array>} Promise that resolves to array of messages
   */
  loadMessages: async (conversationId, params = { page: 0, size: 20 }) => {
    if (!conversationId) {
      console.error('Cannot load messages: conversationId is undefined');
      return Promise.reject(new Error('Conversation ID is required'));
    }

    console.log(`Loading messages for conversation ${conversationId}`);

    try {
      // Try to get messages using our updated APIs with fallbacks
      const response = await messageApi.getMessages(conversationId, params);
      return response.data || [];
    } catch (error) {
      console.error(`Error loading messages for conversation ${conversationId}:`, error);
      // Return empty array instead of rejecting to prevent UI crashes
      return [];
    }
  },

  /**
   * Send a message to a conversation
   * @param {Object} messageData - Message data to send
   * @returns {Promise} Promise that resolves when message is sent
   */
  sendMessage: async (messageData) => {
    if (!messageData.conversationId || !messageData.senderId || !messageData.content) {
      console.error('Cannot send message: missing required data');
      return Promise.reject(new Error('Missing required message data'));
    }

    console.log(`Sending message to conversation ${messageData.conversationId}`);

    try {
      const response = await messageApi.sendMessage(messageData);
      
      // After sending a message, immediately force a poll to get all messages including our own
      setTimeout(() => {
        messagePollingService.forcePoll(messageData.conversationId);
      }, 500);
      
      return response.data;
    } catch (error) {
      console.error(`Error sending message to conversation ${messageData.conversationId}:`, error);
      throw error;
    }
  },

  /**
   * Mark all messages in a conversation as read
   * @param {number} conversationId - ID of the conversation
   * @param {number} userId - ID of the current user
   * @returns {Promise} Promise that resolves when messages are marked as read
   */
  markAllAsRead: async (conversationId, userId) => {
    if (!conversationId || !userId) {
      console.warn('Cannot mark messages as read: missing conversationId or userId');
      return Promise.resolve(); // Don't reject, just ignore
    }

    try {
      await messageApi.markAllAsRead(conversationId, userId);
      // Removed verbose logging to reduce console spam
    } catch (error) {
      console.warn(`Could not mark all messages as read for conversation ${conversationId}`);
      // Don't throw, this is a non-critical operation
    }
  },
  
  /**
   * Mark a single message as read
   * @param {number} messageId - ID of the message to mark as read
   * @returns {Promise} Promise that resolves when message is marked as read
   */
  markAsRead: async (messageId) => {
    if (!messageId) {
      console.warn('Cannot mark message as read: missing messageId');
      return Promise.resolve(); // Don't reject, just ignore
    }
    
    try {
      await messageApi.markAsRead(messageId);
    } catch (error) {
      console.warn(`Could not mark message ${messageId} as read`);
      // Don't throw, this is a non-critical operation
    }
  }
};

/**
 * Helper function to fetch new messages using multiple endpoint patterns
 * @param {number} conversationId - ID of the conversation
 * @param {string} since - Timestamp to fetch messages after
 * @returns {Promise<Array>} Array of new messages
 */
async function fetchNewMessages(conversationId, since) {
  // Skip fetching if messaging has been deactivated or is shutting down
  if (!isMessagingActive || isShuttingDown) {
    return [];
  }
  
  try {
    // Use our updated messageApi getNewMessages function which handles filtering
    const response = await messageApi.getNewMessages(conversationId, since);
    return response.data || [];
  } catch (error) {
    console.error(`Error fetching new messages for conversation ${conversationId}:`, error);
    return []; // Return empty array to prevent UI disruption
  }
}

export default messagePollingService;
