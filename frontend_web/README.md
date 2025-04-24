# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react/README.md) uses [Babel](https://babeljs.io/) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## Environment Variables Setup

To set up environment variables for OAuth and other configurations:

1. Create a `.env.local` file in the root of the frontend_web directory with the following content:

```
# OAuth Credentials
VITE_OAUTH_CLIENT_ID=your_oauth_client_id_here
VITE_OAUTH_CLIENT_SECRET=your_oauth_client_secret_here

# API URLs
VITE_API_URL=http://localhost:8080/api
VITE_AUTH_URL=http://localhost:8080/auth

# Environment
NODE_ENV=development
```

2. Replace the placeholder values with your actual OAuth credentials.

3. The `.env.local` file is automatically ignored by Git to protect sensitive information.
