import React, { memo } from 'react';
import PropTypes from 'prop-types';
import { FaMapMarkerAlt, FaStar, FaVideo, FaUserFriends, FaDollarSign } from 'react-icons/fa';

const TutorListCard = ({ tutor, onViewProfile }) => {
  // Get tutor data with fallbacks for different data structures
  const tutorName = `${tutor.firstName || tutor.user?.firstName || ''} ${tutor.lastName || tutor.user?.lastName || ''}`.trim();
  const tutorImage = tutor.profilePicture || tutor.user?.profilePicture;
  const tutorId = tutor.profileId || tutor.userId;
  const tutorSubjects = tutor.subjects || [];
  const tutorLocation = tutor.location?.city || tutor.city;
  const tutorState = tutor.location?.state || tutor.state;
  const tutorExpertise = tutor.expertise || tutor.username || tutor.user?.username;
  const tutorBio = tutor.bio || tutor.biography || '';
  
  // Get initials for avatar fallback
  const getInitials = () => {
    const first = (tutor.firstName || tutor.user?.firstName || '?')[0];
    const last = (tutor.lastName || tutor.user?.lastName || '?')[0];
    return `${first}${last}`;
  };

  return (
    <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-5 border border-gray-100 dark:border-gray-800 transition-all duration-300 hover:shadow-lg">
      <div className="flex flex-col sm:flex-row">
        {/* Profile Image */}
        <div 
          className="w-24 h-24 rounded-full overflow-hidden mb-4 sm:mb-0 sm:mr-5 cursor-pointer flex-shrink-0 mx-auto sm:mx-0"
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
              <span className="text-2xl font-semibold">{getInitials()}</span>
            </div>
          )}
        </div>
        
        {/* Tutor Information */}
        <div className="flex-1">
          <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start">
            {/* Left column - Name, expertise, rating */}
            <div>
              <h3 
                className="text-xl font-semibold hover:text-primary-600 dark:hover:text-primary-400 cursor-pointer transition-colors text-center sm:text-left"
                onClick={() => onViewProfile(tutorId)}
              >
                {tutorName || 'Unnamed Tutor'}
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-2 text-center sm:text-left">
                {tutorExpertise || 'Tutor'}
              </p>
              
              <div className="flex items-center mb-3 justify-center sm:justify-start">
                <div className="flex items-center">
                  <FaStar className="text-yellow-400 mr-1" />
                  <span className="font-medium">{tutor.rating || 'New'}</span>
                </div>
                <span className="mx-1.5 text-gray-400">•</span>
                <span className="text-gray-600 dark:text-gray-400 text-sm">{tutor.totalReviews || 0} reviews</span>
              </div>
            </div>
            
            {/* Right column - Price, location */}
            <div className="mt-2 sm:mt-0 sm:text-right">
              <p className="text-xl font-semibold text-gray-800 dark:text-white text-center sm:text-right">
                ${tutor.hourlyRate || 0}/hour
              </p>
              
              <div className="flex items-center text-sm text-gray-500 dark:text-gray-400 mt-1.5 justify-center sm:justify-end">
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
            </div>
          </div>
          
          {/* Subjects */}
          <div className="flex flex-wrap gap-1.5 my-3 justify-center sm:justify-start">
            {tutorSubjects.map((subject, index) => (
              <span key={index} className="px-2.5 py-1 text-xs bg-blue-100 dark:bg-blue-900/50 text-blue-800 dark:text-blue-200 rounded-full">
                {typeof subject === 'string' ? subject : subject.subject || subject.name || 'Subject'}
              </span>
            ))}
            {(!tutorSubjects || tutorSubjects.length === 0) && (
              <span className="px-2.5 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-full">
                No subjects listed
              </span>
            )}
          </div>
          
          {/* Bio */}
          <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-2 mb-4 mt-3">
            {tutorBio || 'No bio available'}
          </p>
          
          {/* Session types and view profile button */}
          <div className="flex items-center justify-between mt-3 flex-col sm:flex-row gap-3 sm:gap-0">
            <div className="flex items-center text-sm text-gray-500 dark:text-gray-400">
              {(tutor.isOnlineAvailable || tutor.onlineAvailable) && (
                <div className="flex items-center mr-3 bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 px-2.5 py-1.5 rounded-full">
                  <FaVideo className="mr-1.5" />
                  <span>Online</span>
                </div>
              )}
              {(tutor.isInPersonAvailable || tutor.inPersonAvailable) && (
                <div className="flex items-center bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 px-2.5 py-1.5 rounded-full">
                  <FaUserFriends className="mr-1.5" />
                  <span>In-person</span>
                </div>
              )}
            </div>
            
            <button
              onClick={() => onViewProfile(tutorId)} 
              className="bg-primary-600 hover:bg-primary-700 text-white font-semibold py-2 px-5 rounded-lg transition duration-200 shadow-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50 w-full sm:w-auto"
            >
              View Profile
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

TutorListCard.propTypes = {
  tutor: PropTypes.object.isRequired,
  onViewProfile: PropTypes.func.isRequired
};

export default memo(TutorListCard); 