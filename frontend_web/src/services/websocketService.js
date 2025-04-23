import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.isConnected = false;
    this.subscriptions = new Map();
    this.messageCallbacks = new Map();
    this.readReceiptCallbacks = new Map();
  }

  connect(userId, token, onConnected, onError) {
    // Disconnect if already connected
    if (this.isConnected) {
      this.disconnect();
    }

    // Create a new STOMP client
    this.client = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_API_BASE_URL + '/ws' || 'http://localhost:8080/ws'),
      connectHeaders: {
        'X-Authorization': token,
      },
      debug: function(str) {
        if (import.meta.env.MODE === 'development') {
          console.log('STOMP: ' + str);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Handle connection
    this.client.onConnect = frame => {
      this.isConnected = true;
      console.log('Connected to WebSocket');

      // Subscribe to user-specific destinations
      this.subscribeToUserQueue(userId, '/queue/messages', message => {
        // Process the message
        const receivedMessage = JSON.parse(message.body);
        
        // Notify conversation subscribers
        if (receivedMessage.conversationId) {
          const callback = this.messageCallbacks.get(`conversation.${receivedMessage.conversationId}`);
          if (callback) {
            callback(receivedMessage);
          }
        }
      });

      this.subscribeToUserQueue(userId, '/queue/errors', message => {
        console.error('WebSocket error:', JSON.parse(message.body));
      });

      this.subscribeToUserQueue(userId, '/queue/read-receipts', message => {
        const readReceipt = JSON.parse(message.body);
        const callback = this.readReceiptCallbacks.get(`message.${readReceipt.messageId}`);
        if (callback) {
          callback(readReceipt);
        }
      });

      // Callback that we're connected
      if (onConnected) {
        onConnected();
      }
    };

    // Handle errors
    this.client.onStompError = frame => {
      console.error('STOMP error:', frame.headers.message);
      if (onError) {
        onError(frame.headers.message);
      }
    };

    // Activate the client
    this.client.activate();
  }

  disconnect() {
    if (this.client && this.isConnected) {
      // Clear all subscriptions
      this.subscriptions.forEach((subscription) => {
        if (subscription && subscription.unsubscribe) {
          subscription.unsubscribe();
        }
      });
      this.subscriptions.clear();
      this.messageCallbacks.clear();
      this.readReceiptCallbacks.clear();

      // Disconnect the client
      this.client.deactivate();
      this.isConnected = false;
      console.log('Disconnected from WebSocket');
    }
  }

  subscribeToUserQueue(userId, destination, callback) {
    if (!this.isConnected || !this.client) {
      console.error('Not connected to WebSocket');
      return null;
    }

    const userDestination = `/user/${userId}${destination}`;
    const subscription = this.client.subscribe(userDestination, callback);
    this.subscriptions.set(userDestination, subscription);
    return subscription;
  }

  subscribeToConversation(conversationId, callback) {
    if (!this.isConnected || !this.client) {
      console.error('Not connected to WebSocket');
      return null;
    }

    const destination = `/topic/conversation.${conversationId}`;
    const subscription = this.client.subscribe(destination, message => {
      const receivedMessage = JSON.parse(message.body);
      callback(receivedMessage);
    });

    this.subscriptions.set(destination, subscription);
    this.messageCallbacks.set(`conversation.${conversationId}`, callback);
    return subscription;
  }

  unsubscribeFromConversation(conversationId) {
    const destination = `/topic/conversation.${conversationId}`;
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      this.messageCallbacks.delete(`conversation.${conversationId}`);
    }
  }

  sendMessage(message) {
    if (!this.isConnected || !this.client) {
      console.error('Not connected to WebSocket');
      return false;
    }

    this.client.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify(message)
    });

    return true;
  }

  joinConversation(conversationId, userId, username) {
    if (!this.isConnected || !this.client) {
      console.error('Not connected to WebSocket');
      return false;
    }

    const message = {
      conversationId: conversationId,
      senderId: userId,
      senderName: username,
      type: 'JOIN'
    };

    this.client.publish({
      destination: '/app/chat.join',
      body: JSON.stringify(message)
    });

    return true;
  }

  leaveConversation(conversationId, userId, username) {
    if (!this.isConnected || !this.client) {
      console.error('Not connected to WebSocket');
      return false;
    }

    const message = {
      conversationId: conversationId,
      senderId: userId,
      senderName: username,
      type: 'LEAVE'
    };

    this.client.publish({
      destination: '/app/chat.leave',
      body: JSON.stringify(message)
    });

    return true;
  }

  markMessageAsRead(messageId, senderId, conversationId) {
    if (!this.isConnected || !this.client) {
      console.error('Not connected to WebSocket');
      return false;
    }

    const message = {
      messageId: messageId,
      senderId: senderId,
      conversationId: conversationId,
    };

    this.client.publish({
      destination: '/app/chat.markAsRead',
      body: JSON.stringify(message)
    });

    return true;
  }

  onMessageRead(messageId, callback) {
    this.readReceiptCallbacks.set(`message.${messageId}`, callback);
  }

  isActive() {
    return this.isConnected && this.client !== null;
  }
}

// Create a singleton instance
const webSocketService = new WebSocketService();
export default webSocketService; 