import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { UserProvider } from './context/UserContext'

// Configure axios defaults
import axios from 'axios'
axios.defaults.baseURL = 'http://localhost:8080'  // Update with your backend URL

// This provides a mock for the missing tailwindcss/version.js that flowbite-react tries to import
window.tailwindVersion = { version: '3.3.5' }

// Fix for flowbite-react's attempt to import tailwindcss/version.js
if (import.meta.glob) {
  const modules = import.meta.glob('/node_modules/tailwindcss/version.js', { eager: true })
  if (!modules['/node_modules/tailwindcss/version.js']) {
    import.meta.glob = new Proxy(import.meta.glob, {
      apply(target, thisArg, argArray) {
        const result = Reflect.apply(target, thisArg, argArray)
        if (argArray[0] === 'tailwindcss/version.js') {
          return { 
            'tailwindcss/version.js': () => Promise.resolve({ default: window.tailwindVersion }) 
          }
        }
        return result
      }
    })
  }
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <UserProvider>
      <App />
    </UserProvider>
  </React.StrictMode>,
)
