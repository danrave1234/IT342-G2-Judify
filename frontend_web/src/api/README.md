# API Module Structure

This directory contains the API client and service modules for the Judify application.

## Overview

The API structure is organized into dedicated modules for each service or resource type:

- `api.js` - Configures the base axios instance with auth interceptors and common settings
- `authApi.js` - Authentication-related endpoints (login, register, etc.)
- `userApi.js` - User-related endpoints (profiles, settings, etc.)
- `tutorApi.js` - Tutor profile management
- `studentApi.js` - Student profile management
- `sessionApi.js` - Tutoring session management
- `messageApi.js` - Messaging functionality
- `conversationApi.js` - Conversation management
- `reviewApi.js` - Review system
- `availabilityApi.js` - Tutor availability and calendar integration
- `paymentApi.js` - Payment transaction endpoints
- `notificationApi.js` - Notification system
- `index.js` - Central export point for all API modules

## Usage

### Recommended Import Pattern

For all new code, import API modules directly from the main API export:

```javascript
import { api, messageApi, userApi } from '../api';

// Then use the APIs
messageApi.getMessages(conversationId);
userApi.getCurrentUser();
```

### Legacy Import Support

For compatibility with existing code, you can also import from `api/api.js`:

```javascript
import { messageApi } from '../api/api';
import API from '../api/api'; // Default export for legacy code
```

## Implementation Notes

1. All API modules use a consistent error handling and retry pattern
2. Multiple endpoint formats are tried to ensure compatibility with the backend
3. Each API function validates its required parameters before making requests
4. Debug logging is included for all API calls

## Resilient Design

This API implementation includes fallback mechanisms:

- Multiple endpoint patterns are tried in sequence for critical operations
- Automatic retries with exponential backoff for transient errors
- Graceful degradation when endpoints are unavailable

## Contributing

When adding new API endpoints:

1. Add them to the appropriate module based on resource type
2. Use the established patterns for error handling and fallbacks
3. Make sure to update the `index.js` exports if creating new modules 