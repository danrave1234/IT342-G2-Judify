/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        'primary': {
          50: '#EBF5FF',
          100: '#DBEAFE',
          200: '#BFDBFE', 
          300: '#93C5FD',
          400: '#60A5FA',
          500: '#3B82F6',  // Main primary color
          600: '#2563EB',  // Buttons and highlights
          700: '#1D4ED8',
          800: '#1E40AF',
          900: '#1E3A8A',
        },
        'dark': {
          900: '#121827', // Darkest background
          800: '#1F2937', // Dark background
          700: '#374151', // Lighter dark background
          600: '#4B5563', // Border color dark mode
          500: '#6B7280', // Secondary text dark mode
        },
        'light': {
          900: '#F9FAFB', // Lightest background
          800: '#F3F4F6', // Light background
          700: '#E5E7EB', // Border color light mode
          600: '#D1D5DB', // Divider light mode
          500: '#9CA3AF', // Secondary text light mode
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'Arial', 'sans-serif'],
      },
      borderRadius: {
        'xl': '0.75rem',
        '2xl': '1rem',
      },
      boxShadow: {
        'soft': '0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02)',
        'card': '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
      }
    },
  },
  plugins: [],
} 