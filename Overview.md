# Judify Real-Time Messaging System Overview

## Introduction

The Judify platform integrates a real-time messaging system that facilitates communication between students and tutors. This system is designed specifically to support session scheduling and coordination, with messages stored locally rather than in the database to ensure privacy and reduce server load.

## Technical Architecture

### WebSocket Implementation

- **Technology**: The system uses WebSockets (via the `WebSocketContext`) to provide real-time messaging capabilities.
- **Connection Endpoint**: Currently connects to an echo WebSocket server for demonstration, but can be updated to use a production WebSocket endpoint.
- **State Management**: Manages connection status, conversations, and messages through React state and context.

### Data Storage

- **LocalStorage Persistence**: All messages and conversations are stored in the browser's localStorage rather than in a database.
- **Storage Format**:
  - Conversations: Stored under key `judify_conversations_${userId}`
  - Messages: Stored under key `judify_messages_${conversationId}`
- **Data Structure**:
  - Conversations include metadata about the participants and the most recent message
  - Messages include sender, receiver, content, timestamp, and read status

## User Experience

### Interface

- The Messages component displays:
  - A list of conversations on the left
  - A conversation view on the right with message history
  - A message input area at the bottom
  - Connection status indicator showing online/offline status

### Session Scheduling Integration

- When a student books a session with a tutor, the system:
  1. Creates the session in the backend
  2. Automatically navigates to the Messages page
  3. Initiates a conversation with the tutor if one doesn't exist
  4. Sends an initial message containing session details (date, time, subject)

### Real-Time Features

- Messages appear instantly after sending
- Connection status is shown prominently
- Input is disabled when disconnected to prevent message loss

## Security and Privacy

- Messages are stored only in the local browser, not transmitted to a central database
- Each user only sees their own conversations
- WebSocket connections are established only when the user is logged in

## Limitations

- Messages are device-specific (not synced across devices)
- Message history is lost if localStorage is cleared
- Offline users will not receive messages until they reconnect

## Future Enhancements

- Implement message delivery confirmations
- Add support for image and file attachments
- Develop message notifications when the app is not in focus
- Add end-to-end encryption for additional privacy

## Implementation Details

The core components of the messaging system include:

1. **WebSocketContext.jsx**: Provides the context for WebSocket connections and message handling
2. **Messages.jsx**: The main user interface component for the messaging system
3. **BookSession.jsx**: Integrates with the messaging system to create conversations upon session booking
4. **SessionContext.jsx**: Returns tutor information when sessions are created to facilitate conversation creation

This architecture prioritizes user privacy by keeping conversations local while maintaining a seamless, real-time communication experience between students and tutors. 