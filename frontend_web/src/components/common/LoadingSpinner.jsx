import React from 'react';

/**
 * A custom loading spinner component that doesn't depend on flowbite-react
 */
const LoadingSpinner = ({ size = 'md', className = '' }) => {
  // Size classes
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12',
    xl: 'w-16 h-16',
  };

  // Get the size class or default to medium
  const sizeClass = sizeClasses[size] || sizeClasses.md;

  return (
    <div className={`flex justify-center items-center ${className}`}>
      <div className={`${sizeClass} border-4 border-gray-200 border-t-primary-600 rounded-full animate-spin`}></div>
    </div>
  );
};

export default LoadingSpinner; 