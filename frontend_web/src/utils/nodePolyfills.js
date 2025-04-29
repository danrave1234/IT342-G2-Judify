/**
 * Polyfills for Node.js globals that might be required by libraries like SockJS
 * This file provides browser-compatible versions of common Node.js globals
 */

// If window is available (browser environment)
if (typeof window !== 'undefined') {
  // Provide global for libraries that expect Node.js global
  if (typeof global === 'undefined') {
    window.global = window;
  }

  // Provide process.env for libraries that expect Node.js process
  if (typeof process === 'undefined') {
    window.process = { env: {} };
  } else if (typeof process.env === 'undefined') {
    process.env = {};
  }

  // Provide Buffer for libraries that expect Node.js Buffer
  if (typeof Buffer === 'undefined') {
    window.Buffer = {
      isBuffer: () => false,
      from: (data) => new Uint8Array(data),
    };
  }

  // Provide crypto for libraries that expect Node.js crypto
  if (typeof crypto === 'undefined' || !crypto.getRandomValues) {
    // Minimal implementation for libraries requiring randomness
    window.crypto = {
      getRandomValues: function(array) {
        for (let i = 0; i < array.length; i++) {
          array[i] = Math.floor(Math.random() * 256);
        }
        return array;
      }
    };
  }
}

export default {
  setup: () => {
    // This function is intentionally empty as the polyfills
    // are applied when the file is imported
    console.log('Node.js polyfills have been applied');
  }
}; 