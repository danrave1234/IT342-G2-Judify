# Judify Web Frontend Backend Configuration

This document explains how the Judify web frontend connects to the backend API.

## Backend Configuration

The web frontend is configured to connect to the following backend:

```
https://judify-795422705086.asia-east1.run.app
```

## API Endpoint Structure

All API endpoints are formatted as:
```
https://judify-795422705086.asia-east1.run.app/api/[endpoint]
```

For example:
- User authentication: `/api/users/authenticate`
- Get tutors: `/api/tutors/getAllProfiles`
- Messages: `/api/messages/conversation/{id}`

WebSocket connections use:
```
https://judify-795422705086.asia-east1.run.app/ws
```

**IMPORTANT NOTE: As of the latest update, the frontend website now uses polling instead of WebSockets for messaging functionality. The WebSocket information above is kept for reference purposes only.**

## How the Connection Works

The backend connection is configured in several places:

1. **Environment Variables**: The `.env.production` file contains the backend URL:
   ```
   VITE_API_URL=https://judify-795422705086.asia-east1.run.app
   VITE_WS_URL=https://judify-795422705086.asia-east1.run.app
   ```

2. **API Service**: The `src/services/api.js` file configures axios to use the backend URL:
   ```js
   const BACKEND_URL = import.meta.env.VITE_API_URL || 'https://judify-795422705086.asia-east1.run.app';
   ```
   The axios instance uses this as the baseURL, and all API calls include the `/api` prefix.

3. **Polling Service**: The `src/services/pollingService.js` file configures the polling mechanism for message updates:
   ```js
   this.defaultPollingInterval = 5000; // 5 seconds by default, matching mobile app
   ```
   Polling is used to check for new messages at regular intervals, replacing the previous WebSocket functionality.

4. **Vite Config**: For local development, `vite.config.js` proxies API requests:
   ```js
   server: {
     proxy: {
       '/api': {
         target: 'https://judify-795422705086.asia-east1.run.app',
         changeOrigin: true,
         secure: true,
         rewrite: (path) => path
       }
     }
   }
   ```

## Path Handling

- In production, all API calls from the frontend include the `/api` prefix.
- When deployed to production, axios makes requests directly to `https://judify-795422705086.asia-east1.run.app/api/...`
- In development mode, the frontend uses relative URLs (`/api/...`) that are proxied to the backend.

## How to Change the Backend URL

If you need to connect to a different backend, update the URL in these places:

1. Update `.env.production` with the new URL
2. Update the fallback URLs in `src/services/api.js`
3. Update the proxy configuration in `vite.config.js` for local development

## API Endpoints

All API endpoints in the application use the `/api` prefix.

When deployed to production, the frontend will make requests directly to the backend URL. In development mode, the frontend will use relative URLs that are proxied to the backend. 