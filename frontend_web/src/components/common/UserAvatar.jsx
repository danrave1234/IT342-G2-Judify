import PropTypes from 'prop-types';

/**
 * User Avatar component for displaying user profile images with fallback
 */
const UserAvatar = ({ user, size = 'md', className = '' }) => {
  // Size classes
  const sizeClasses = {
    sm: 'w-8 h-8',
    md: 'w-10 h-10',
    lg: 'w-12 h-12',
    xl: 'w-16 h-16'
  };
  
  // Extract user information
  const { profileImage, firstName, lastName, username } = user || {};
  
  // Get first letter of name or username for fallback avatar
  const getInitial = () => {
    if (firstName) return firstName.charAt(0).toUpperCase();
    if (username) return username.charAt(0).toUpperCase();
    return '?';
  };
  
  // Base classes for the avatar
  const baseClasses = `${sizeClasses[size] || sizeClasses.md} rounded-full overflow-hidden bg-gray-200 dark:bg-gray-700 flex-shrink-0 ${className}`;
  
  return (
    <div className={baseClasses}>
      {profileImage ? (
        <img
          src={profileImage}
          alt={`${firstName || username || 'User'}'s profile`}
          className="w-full h-full object-cover"
        />
      ) : (
        <div className="w-full h-full flex items-center justify-center text-gray-500 dark:text-gray-400 font-medium">
          {getInitial()}
        </div>
      )}
    </div>
  );
};

UserAvatar.propTypes = {
  user: PropTypes.shape({
    profileImage: PropTypes.string,
    firstName: PropTypes.string,
    lastName: PropTypes.string,
    username: PropTypes.string
  }),
  size: PropTypes.oneOf(['sm', 'md', 'lg', 'xl']),
  className: PropTypes.string
};

export default UserAvatar; 