# Mobile and Web Integration Documentation

This document provides a comprehensive overview of the integration between the mobile and web frontends of the Judify tutoring platform. It details the shared API endpoints, data models, and implementation approaches to ensure consistency across platforms.

## API Endpoints and Implementation

### Authentication

| Feature | Web Implementation | Mobile Implementation | API Endpoint |
|---------|-------------------|----------------------|--------------|
| Login | Uses UserContext with axios for API calls | Uses NetworkUtils.authenticateUser | `/api/users/authenticate` |
| Registration | Uses UserContext with axios for API calls | Uses NetworkUtils.registerUser | `/api/users/addUser` |
| Google OAuth | Redirects to `/oauth2/authorization/google` | Not implemented | `/oauth2/authorization/google` |
| Token Storage | Stores in localStorage | Stores in app preferences | N/A |

**Integration Notes:**
- Mobile app explicitly maps between "LEARNER" (frontend) and "STUDENT" (backend) roles
- Web frontend doesn't have explicit role mapping
- Web supports Google OAuth while mobile doesn't

### Tutor Profiles

| Feature | Web Implementation | Mobile Implementation | API Endpoint |
|---------|-------------------|----------------------|--------------|
| Get Tutor Profile | Uses tutorProfileApi.getProfileById or getProfileByUserId | Uses NetworkUtils.getTutorProfile or findTutorByUserId | `/api/tutors/{id}` or `/api/tutors/user/{userId}` |
| Get All Tutors | Uses API.get | Uses NetworkUtils.getAllTutorProfiles | `/api/tutors` |
| Search Tutors | Uses API.get with query params | Uses NetworkUtils.findTutorsByExpertise | `/api/tutors/search` |
| Get Tutor Subjects | Uses API.get | Uses NetworkUtils.getSubjectsByTutorProfileId | `/api/tutor-subjects/findByTutor/{tutorId}` |

**Integration Notes:**
- Both platforms fetch tutor profiles and subjects from the same endpoints
- Mobile has additional methods for random tutors and subject-specific tutors

### Session Booking

| Feature | Web Implementation | Mobile Implementation | API Endpoint |
|---------|-------------------|----------------------|--------------|
| Get Tutor Availability | Uses API.get | Uses NetworkUtils.getTutorAvailability | `/api/tutor-availability/findByTutor/{tutorId}` |
| Get Availability by Day | Uses filtering in frontend | Uses NetworkUtils.getTutorAvailabilityByDay | `/api/tutor-availability/findByTutorAndDay/{tutorId}/{dayOfWeek}` |
| Create Session | Uses SessionContext.createSession | Uses NetworkUtils.createTutoringSession | `/api/tutoring-sessions` |
| Get User Sessions | Uses SessionContext.getSessions | Uses NetworkUtils.getTutorSessions or getLearnerSessions | `/api/tutoring-sessions/tutor/{tutorId}` or `/api/tutoring-sessions/learner/{learnerId}` |

**Integration Notes:**
- Both platforms implement similar booking flows
- Both handle time slot generation based on tutor availability
- Both support online and in-person sessions

### Messaging

| Feature | Web Implementation | Mobile Implementation | API Endpoint |
|---------|-------------------|----------------------|--------------|
| Get Conversations | Uses MessageContext with multiple fallback endpoints | Uses NetworkUtils.getConversationsForUser with fallbacks | `/api/conversations/user/{userId}`, `/api/conversations/findByUser/{userId}`, etc. |
| Create Conversation | Uses MessageContext.getOrCreateConversation | Uses NetworkUtils.createConversation | `/api/conversations` or `/api/conversations/createConversation` |
| Get Messages | Uses PollingService.loadConversationMessages | Uses NetworkUtils.getMessages | `/api/messages/conversation/{conversationId}` |
| Send Message | Uses PollingService.sendMessage | Uses NetworkUtils.sendMessage | `/api/messages/sendMessage` |
| Mark Message as Read | Uses PollingService.markMessageAsRead | Uses NetworkUtils.markMessageAsRead | `/api/messages/markAsRead/{messageId}` |

**Integration Notes:**
- Web uses polling for real-time updates while mobile uses manual refreshing
- Both platforms try multiple endpoints for conversation retrieval
- Both handle message formatting and timestamp conversion

## Data Models

### User

| Field | Web | Mobile | Notes |
|-------|-----|--------|-------|
| userId | ✓ | ✓ | Primary identifier |
| email | ✓ | ✓ | Used for login |
| firstName | ✓ | ✓ | |
| lastName | ✓ | ✓ | |
| role | ✓ | ✓ | Mobile maps STUDENT ↔ LEARNER |
| token | ✓ | ✓ | Authentication token |

### Tutor Profile

| Field | Web | Mobile | Notes |
|-------|-----|--------|-------|
| profileId/id | ✓ | ✓ | Primary identifier |
| userId | ✓ | ✓ | Reference to user |
| name | ✓ | ✓ | |
| bio | ✓ | ✓ | |
| subjects | ✓ | ✓ | |
| hourlyRate | ✓ | ✓ | |
| rating | ✓ | ✓ | |

### Tutoring Session

| Field | Web | Mobile | Notes |
|-------|-----|--------|-------|
| sessionId/id | ✓ | ✓ | Primary identifier |
| tutorId | ✓ | ✓ | |
| learnerId/studentId | ✓ | ✓ | Web uses learnerId, Mobile uses both |
| startTime | ✓ | ✓ | ISO format |
| endTime | ✓ | ✓ | ISO format |
| subject | ✓ | ✓ | |
| sessionType | ✓ | ✓ | Online or In-Person |
| status | ✓ | ✓ | |
| notes | ✓ | ✓ | Optional |

### Conversation

| Field | Web | Mobile | Notes |
|-------|-----|--------|-------|
| conversationId/id | ✓ | ✓ | Primary identifier |
| studentId | ✓ | ✓ | |
| tutorId | ✓ | ✓ | |
| lastMessage | ✓ | ✓ | |
| updatedAt | ✓ | ✓ | |
| unreadCount | ✓ | ✓ | |

### Message

| Field | Web | Mobile | Notes |
|-------|-----|--------|-------|
| messageId/id | ✓ | ✓ | Primary identifier |
| conversationId | ✓ | ✓ | |
| senderId | ✓ | ✓ | |
| content | ✓ | ✓ | |
| timestamp | ✓ | ✓ | |
| isRead | ✓ | ✓ | |

## Implementation Differences

### API Base URL

- **Web**: 
  ```javascript
  const BACKEND_URL = 'http://localhost:8080';
  ```

- **Mobile**:
  ```kotlin
  private const val DEPLOYED_BACKEND_URL = "http://192.168.1.4:8080"
  private val BASE_URL: String = "$DEPLOYED_BACKEND_URL/api"
  ```

### Authentication Flow

- **Web**:
  1. User enters credentials
  2. UserContext.login calls API
  3. Token stored in localStorage
  4. User redirected to appropriate dashboard

- **Mobile**:
  1. User enters credentials
  2. NetworkUtils.authenticateUser calls API
  3. Token stored in preferences
  4. User navigated to appropriate dashboard

### Real-time Messaging

- **Web**:
  - Uses PollingService for real-time updates
  - Subscribes to conversations
  - Updates UI when new messages arrive

- **Mobile**:
  - Uses manual refreshing
  - No subscription mechanism
  - Updates UI on user action or app refresh

## Integration Testing Checklist

To ensure proper integration between mobile and web platforms, test the following scenarios:

1. **User Authentication**
   - [ ] Create a user account on web, verify login works on mobile
   - [ ] Update user profile on mobile, verify changes appear on web

2. **Tutor Profiles**
   - [ ] Create a tutor profile on web, verify it appears in mobile search
   - [ ] Update tutor availability on mobile, verify it affects booking on web

3. **Session Booking**
   - [ ] Book a session on web, verify it appears in mobile dashboard
   - [ ] Cancel a session on mobile, verify it updates on web

4. **Messaging**
   - [ ] Start a conversation on web, verify it appears on mobile
   - [ ] Send messages on mobile, verify they appear on web
   - [ ] Mark messages as read on web, verify read status on mobile

## Recommendations for Improved Integration

1. **Standardize API Base URLs**
   - Use environment variables for configuration
   - Ensure both platforms point to the same backend in each environment

2. **Unify Role Terminology**
   - Standardize on either "STUDENT" or "LEARNER" across backend and both frontends
   - Update role mapping in both platforms

3. **Implement Consistent Real-time Updates**
   - Add WebSocket support to both platforms
   - Ensure notifications work consistently

4. **Create Shared API Client Library**
   - Consider extracting common API calls into a shared specification
   - Generate client libraries for both platforms

5. **Implement Comprehensive Integration Tests**
   - Create automated tests that verify cross-platform scenarios
   - Include these tests in CI/CD pipeline