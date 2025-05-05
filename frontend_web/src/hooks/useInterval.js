import { useEffect, useRef } from 'react';

/**
 * Custom hook for setting up an interval
 * Based on Dan Abramov's implementation: https://overreacted.io/making-setinterval-declarative-with-react-hooks/
 * 
 * @param {Function} callback - Function to call on each interval
 * @param {number} delay - Interval delay in milliseconds, null to pause
 */
function useInterval(callback, delay) {
  const savedCallback = useRef();

  // Remember the latest callback
  useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  // Set up the interval
  useEffect(() => {
    function tick() {
      savedCallback.current();
    }
    
    if (delay !== null) {
      const id = setInterval(tick, delay);
      return () => clearInterval(id);
    }
  }, [delay]);
}

export default useInterval; 