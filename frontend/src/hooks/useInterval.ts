import { useEffect, useRef } from 'react';

/**
 * Custom hook for running a callback at a specified interval
 * Useful for polling API endpoints for real-time updates
 * 
 * @param callback - Function to execute at each interval
 * @param delay - Interval delay in milliseconds (null to pause)
 */
export function useInterval(callback: () => void, delay: number | null) {
  const savedCallback = useRef<() => void>();

  // Remember the latest callback
  useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  // Set up the interval
  useEffect(() => {
    function tick() {
      if (savedCallback.current) {
        savedCallback.current();
      }
    }

    if (delay !== null) {
      const id = setInterval(tick, delay);
      return () => clearInterval(id);
    }
  }, [delay]);
}
