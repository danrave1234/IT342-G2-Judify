import React, { memo } from 'react';
import PropTypes from 'prop-types';
import { FaMapMarkerAlt, FaStar, FaVideo, FaUserFriends, FaDollarSign } from 'react-icons/fa';

const TutorGridCard = ({ tutor, onViewProfile }) => {
  // Get tutor data with fallbacks for different data structures
  const tutorName = `${tutor.firstName || tutor.user?.firstName || ''} ${tutor.lastName || tutor.user?.lastName || ''}`.trim();
  const tutorImage = tutor.profilePicture || tutor.user?.profilePicture;
  const tutorId = tutor.profileId || tutor.userId;
  const tutorSubjects = tutor.subjects || [];
  const tutorLocation = tutor.location?.city || tutor.city;
  const tutorState = tutor.location?.state || tutor.state;
  const tutorExpertise = tutor.expertise || tutor.username || tutor.user?.username;
  
  // Get initials for avatar fallback
  const getInitials = () => {
    const first = (tutor.firstName || tutor.user?.firstName || '?')[0];
    const last = (tutor.lastName || tutor.user?.lastName || '?')[0];
    return `${first}${last}`;
  };

  return (
    <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md overflow-hidden transition-all duration-300 hover:shadow-lg transform hover:-translate-y-1 border border-gray-100 dark:border-gray-800 h-full">
      <div className="p-5">
        {/* Profile header with image and basic info */}
        <div className="flex items-center mb-4">
          <div 
            className="w-16 h-16 rounded-full overflow-hidden mr-4 cursor-pointer"
            onClick={() => onViewProfile(tutorId)}
          >
            {tutorImage ? (
              <img 
                src={tutorImage} 
                alt={tutorName} 
                className="w-full h-full object-cover"
                loading="lazy"
              />
            ) : (
              <div className="w-full h-full bg-gradient-to-r from-primary-500 to-primary-700 flex items-center justify-center text-white">
                <span className="text-xl font-semibold">{getInitials()}</span>
              </div>
            )}
          </div>
          <div>
            <h3 
              className="text-lg font-semibold mb-1 hover:text-primary-600 dark:hover:text-primary-400 cursor-pointer transition-colors"
              onClick={() => onViewProfile(tutorId)}
            >
              {tutorName || 'Unnamed Tutor'}
            </h3>
            <div className="flex items-center">
              <FaStar className="text-yellow-400 mr-1" />
              <span className="font-medium">{tutor.rating || 'New'}</span>
              <span className="mx-1 text-gray-400">•</span>
              <span className="text-gray-600 dark:text-gray-400 text-sm">{tutor.totalReviews || 0} reviews</span>
            </div>
            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
              {tutorExpertise || 'Tutor'}
            </p>
          </div>
        </div>
        
        {/* Location */}
        <div className="flex items-center text-sm text-gray-500 dark:text-gray-400 mb-2">
          <FaMapMarkerAlt className="mr-1.5 text-gray-400" />
          {tutorLocation ? (
            <span>
              {tutorLocation}
              {tutorState ? `, ${tutorState}` : ''}
            </span>
          ) : (
            <span>Location not specified</span>
          )}
        </div>
        
        {/* Hourly rate */}
        <div className="flex items-center text-sm text-gray-500 dark:text-gray-400 mb-3">
          <FaDollarSign className="mr-1.5 text-gray-400" />
          <span className="font-medium text-gray-800 dark:text-gray-200">${tutor.hourlyRate || 0}/hour</span>
        </div>
        
        {/* Session types */}
        <div className="flex items-center mb-4 text-sm text-gray-500 dark:text-gray-400">
          {(tutor.isOnlineAvailable || tutor.onlineAvailable) && (
            <div className="flex items-center mr-3 bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 px-2 py-1 rounded-full">
              <FaVideo className="mr-1.5" />
              <span>Online</span>
            </div>
          )}
          {(tutor.isInPersonAvailable || tutor.inPersonAvailable) && (
            <div className="flex items-center bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 px-2 py-1 rounded-full">
              <FaUserFriends className="mr-1.5" />
              <span>In-person</span>
            </div>
          )}
        </div>
        
        {/* Subjects */}
        <div className="flex flex-wrap gap-1.5 mb-5">
          {tutorSubjects.slice(0, 3).map((subject, index) => (
            <span key={index} className="px-2.5 py-1 text-xs bg-blue-100 dark:bg-blue-900/50 text-blue-800 dark:text-blue-200 rounded-full">
              {typeof subject === 'string' ? subject : subject.subject || subject.name || 'Subject'}
            </span>
          ))}
          {tutorSubjects.length > 3 && (
            <span className="px-2.5 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-full">
              +{tutorSubjects.length - 3} more
            </span>
          )}
          {(!tutorSubjects || tutorSubjects.length === 0) && (
            <span className="px-2.5 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-full">
              No subjects listed
            </span>
          )}
        </div>
        
        {/* View profile button */}
        <button
          onClick={() => onViewProfile(tutorId)} 
          className="w-full bg-primary-600 hover:bg-primary-700 text-white font-semibold py-2.5 px-4 rounded-lg transition duration-200 shadow-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50"
        >
          View Profile
        </button>
      </div>
    </div>
  );
};

TutorGridCard.propTypes = {
  tutor: PropTypes.object.isRequired,
  onViewProfile: PropTypes.func.isRequired
};

export default memo(TutorGridCard); 