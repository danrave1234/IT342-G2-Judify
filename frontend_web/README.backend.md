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

3. **WebSocket Service**: The `src/services/websocketService.js` file configures WebSocket connections:
   ```js
   const BACKEND_URL = import.meta.env.VITE_API_URL || 'https://judify-795422705086.asia-east1.run.app';
   const WS_URL = import.meta.env.VITE_WS_URL || BACKEND_URL;
   ```
   WebSocket connections are made to `${WS_URL}/ws`.

4. **Vite Config**: For local development, `vite.config.js` proxies API and WebSocket requests:
   ```js
   server: {
     proxy: {
       '/api': {
         target: 'https://judify-795422705086.asia-east1.run.app',
         changeOrigin: true,
         secure: true,
         rewrite: (path) => path
       },
       '/ws': {
         target: 'https://judify-795422705086.asia-east1.run.app',
         ws: true,
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
2. Update the fallback URLs in `src/services/api.js` and `src/services/websocketService.js`
3. Update the proxy configuration in `vite.config.js` for local development

## API Endpoints

All API endpoints in the application use the `/api` prefix. The WebSocket endpoints use the `/ws` prefix.

When deployed to production, the frontend will make requests directly to the backend URL. In development mode, the frontend will use relative URLs that are proxied to the backend. 