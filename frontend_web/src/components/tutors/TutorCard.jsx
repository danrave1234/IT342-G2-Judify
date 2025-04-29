import React, { useMemo } from 'react';
import { FaStar, FaMapMarkerAlt, FaVideo, FaUserFriends, FaDollarSign } from 'react-icons/fa';
import { motion } from 'framer-motion';
import defaultAvatar from '../../assets/images/default-avatar.png';

const TutorCard = ({ tutor, viewMode, onViewProfile }) => {
  // Extract data with fallbacks for different API response formats
  const tutorId = tutor.profileId || tutor.id;
  const profileImage = tutor.user?.profileImage || tutor.profilePicture || defaultAvatar;
  const fullName = tutor.user?.fullName || tutor.fullName || tutor.username || 'Unnamed Tutor';
  const location = tutor.city || tutor.location || 'Location not specified';
  const state = tutor.state || '';
  const hourlyRate = tutor.hourlyRate || 0;
  const rating = tutor.rating || 0;
  const offersOnline = tutor.offersOnline || tutor.onlineAvailability || false;
  const offersInPerson = tutor.offersInPerson || tutor.inPersonAvailability || false;
  
  // Format subjects (handle both array of strings and array of objects)
  const subjects = useMemo(() => {
    if (!tutor.subjects && !tutor.subjectEntities) return [];
    
    if (tutor.subjectEntities) {
      return tutor.subjectEntities.map(s => s.name || s);
    }
    
    return tutor.subjects || [];
  }, [tutor]);

  // For grid view
  if (viewMode === 'grid') {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="bg-white dark:bg-dark-800 rounded-xl overflow-hidden shadow-md hover:shadow-lg transition-shadow border border-gray-100 dark:border-gray-800"
      >
        {/* Profile header */}
        <div className="bg-gradient-to-r from-primary-600 to-primary-500 h-24 relative">
          <div className="absolute -bottom-10 left-4 h-20 w-20 rounded-full border-4 border-white dark:border-dark-800 overflow-hidden">
            <img 
              src={profileImage} 
              alt={fullName}
              className="h-full w-full object-cover"
              onError={(e) => { e.target.src = defaultAvatar; }}
            />
          </div>
        </div>
        
        {/* Content */}
        <div className="pt-12 p-4">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-1 line-clamp-1">{fullName}</h3>
          
          {location && (
            <div className="flex items-center text-gray-600 dark:text-gray-400 text-sm mb-2">
              <FaMapMarkerAlt className="mr-1 text-gray-400" />
              <span>{location}{state ? `, ${state}` : ''}</span>
            </div>
          )}
          
          {/* Rating */}
          <div className="flex items-center mb-3">
            <div className="flex">
              {[1, 2, 3, 4, 5].map((star) => (
                <FaStar
                  key={star}
                  className={`${
                    star <= rating ? 'text-yellow-400' : 'text-gray-300 dark:text-gray-600'
                  } text-sm`}
                />
              ))}
            </div>
            <span className="ml-2 text-sm text-gray-600 dark:text-gray-400">
              {rating.toFixed(1)}
            </span>
          </div>
          
          {/* Price */}
          <div className="flex items-center text-gray-800 dark:text-gray-200 font-semibold mb-3">
            <FaDollarSign className="text-primary-600 dark:text-primary-400 mr-1" />
            <span>${hourlyRate.toFixed(2)}/hr</span>
          </div>
          
          {/* Subjects */}
          <div className="mb-4">
            <div className="flex flex-wrap gap-1">
              {subjects.slice(0, 3).map((subject, idx) => (
                <span
                  key={idx}
                  className="inline-block bg-gray-100 dark:bg-dark-700 text-gray-800 dark:text-gray-300 text-xs px-2 py-1 rounded-full"
                >
                  {subject}
                </span>
              ))}
              {subjects.length > 3 && (
                <span className="inline-block text-gray-500 dark:text-gray-400 text-xs px-1">
                  +{subjects.length - 3} more
                </span>
              )}
            </div>
          </div>
          
          {/* Session Type */}
          <div className="flex flex-wrap gap-2 text-sm mb-4">
            {offersOnline && (
              <div className="flex items-center text-gray-700 dark:text-gray-300">
                <FaVideo className="mr-1 text-primary-600 dark:text-primary-400" />
                <span>Online</span>
              </div>
            )}
            {offersInPerson && (
              <div className="flex items-center text-gray-700 dark:text-gray-300">
                <FaUserFriends className="mr-1 text-primary-600 dark:text-primary-400" />
                <span>In-Person</span>
              </div>
            )}
          </div>
          
          {/* View Profile Button */}
          <button
            onClick={() => onViewProfile(tutorId)}
            className="w-full py-2 px-4 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-lg transition duration-300 shadow-sm"
          >
            View Profile
          </button>
        </div>
      </motion.div>
    );
  }
  
  // For list view
  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3 }}
      className="bg-white dark:bg-dark-800 rounded-xl overflow-hidden shadow-md hover:shadow-lg transition-shadow border border-gray-100 dark:border-gray-800"
    >
      <div className="flex flex-col md:flex-row">
        {/* Avatar */}
        <div className="md:w-1/6 p-4 flex items-center justify-center">
          <div className="h-20 w-20 rounded-full overflow-hidden border-2 border-gray-100 dark:border-gray-800">
            <img 
              src={profileImage} 
              alt={fullName}
              className="h-full w-full object-cover"
              onError={(e) => { e.target.src = defaultAvatar; }}
            />
          </div>
        </div>
        
        {/* Content */}
        <div className="md:w-3/6 p-4 border-t md:border-t-0 md:border-l border-gray-100 dark:border-gray-800">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-1">{fullName}</h3>
          
          {location && (
            <div className="flex items-center text-gray-600 dark:text-gray-400 text-sm mb-2">
              <FaMapMarkerAlt className="mr-1 text-gray-400" />
              <span>{location}{state ? `, ${state}` : ''}</span>
            </div>
          )}
          
          {/* Rating */}
          <div className="flex items-center mb-3">
            <div className="flex">
              {[1, 2, 3, 4, 5].map((star) => (
                <FaStar
                  key={star}
                  className={`${
                    star <= rating ? 'text-yellow-400' : 'text-gray-300 dark:text-gray-600'
                  } text-sm`}
                />
              ))}
            </div>
            <span className="ml-2 text-sm text-gray-600 dark:text-gray-400">
              {rating.toFixed(1)}
            </span>
          </div>
          
          {/* Subjects */}
          <div>
            <div className="flex flex-wrap gap-1">
              {subjects.slice(0, 3).map((subject, idx) => (
                <span
                  key={idx}
                  className="inline-block bg-gray-100 dark:bg-dark-700 text-gray-800 dark:text-gray-300 text-xs px-2 py-1 rounded-full"
                >
                  {subject}
                </span>
              ))}
              {subjects.length > 3 && (
                <span className="inline-block text-gray-500 dark:text-gray-400 text-xs px-1">
                  +{subjects.length - 3} more
                </span>
              )}
            </div>
          </div>
        </div>
        
        {/* Right Side - Price and buttons */}
        <div className="md:w-2/6 p-4 bg-gray-50 dark:bg-dark-700 flex flex-col justify-between">
          <div>
            {/* Price */}
            <div className="flex items-center justify-center md:justify-start text-gray-800 dark:text-gray-200 font-semibold mb-3">
              <FaDollarSign className="text-primary-600 dark:text-primary-400 mr-1" />
              <span>${hourlyRate.toFixed(2)}/hr</span>
            </div>
            
            {/* Session Type */}
            <div className="flex flex-wrap justify-center md:justify-start gap-2 text-sm mb-4">
              {offersOnline && (
                <div className="flex items-center text-gray-700 dark:text-gray-300">
                  <FaVideo className="mr-1 text-primary-600 dark:text-primary-400" />
                  <span>Online</span>
                </div>
              )}
              {offersInPerson && (
                <div className="flex items-center text-gray-700 dark:text-gray-300">
                  <FaUserFriends className="mr-1 text-primary-600 dark:text-primary-400" />
                  <span>In-Person</span>
                </div>
              )}
            </div>
          </div>
          
          {/* View Profile Button */}
          <button
            onClick={() => onViewProfile(tutorId)}
            className="w-full py-2 px-4 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-lg transition duration-300 shadow-sm"
          >
            View Profile
          </button>
        </div>
      </div>
    </motion.div>
  );
};

export default TutorCard; 