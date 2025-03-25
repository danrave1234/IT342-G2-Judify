import { useState, useEffect } from 'react';
import { FaSun, FaMoon } from 'react-icons/fa';

const DarkModeToggle = () => {
  const [darkMode, setDarkMode] = useState(
    localStorage.getItem('darkMode') === 'true' || 
    window.matchMedia('(prefers-color-scheme: dark)').matches
  );

  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('darkMode', 'true');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('darkMode', 'false');
    }
  }, [darkMode]);

  const toggleDarkMode = () => {
    setDarkMode(!darkMode);
  };

  return (
    <div className="flex items-center">
      <span className="mr-2 text-gray-500 dark:text-gray-400">
        {darkMode ? 
          <FaMoon className="h-4 w-4" /> :
          <FaSun className="h-4 w-4" />
        }
      </span>
      <div 
        className={`dark-mode-toggle ${darkMode ? 'active' : ''}`}
        onClick={toggleDarkMode}
        role="checkbox"
        aria-checked={darkMode}
        tabIndex={0}
      />
    </div>
  );
};

export default DarkModeToggle; 