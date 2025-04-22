import { useState } from 'react';
import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';
import { FaStar, FaRegStar, FaStarHalfAlt, FaMapMarkerAlt, FaVideo, FaUserFriends, FaSpinner } from 'react-icons/fa';
import { toast } from 'react-toastify';
import defaultProfilePic from '../../assets/default-profile.png';

const TutorList = ({ 
  tutors, 
  loading, 
  error, 
  searchQuery, 
  setSearchQuery 
}) => {
  const navigate = useNavigate();
  
  // Function to handle navigation to tutor profile
  const handleViewProfileClick = (tutorId) => {
    console.log("Navigating to tutor profile:", tutorId);
    if (!tutorId) {
      toast.error("Tutor ID is missing. Cannot view profile.");
      return;
    }
    navigate(`/tutor/${tutorId}`);
  };

  // Render stars based on rating
  const renderRatingStars = (rating) => {
    const stars = [];
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 >= 0.5;
    
    for (let i = 1; i <= 5; i++) {
      if (i <= fullStars) {
        stars.push(<FaStar key={i} className="text-yellow-400" />);
      } else if (i === fullStars + 1 && hasHalfStar) {
        stars.push(<FaStarHalfAlt key={i} className="text-yellow-400" />);
      } else {
        stars.push(<FaRegStar key={i} className="text-yellow-400" />);
      }
    }
    
    return (
      <div className="flex">
        {stars}
        <span className="ml-1 text-sm text-gray-600 dark:text-gray-400">({rating})</span>
      </div>
    );
  };

  // Loading state
  if (loading) {
    return (
      <div className="w-full h-64 flex items-center justify-center">
        <FaSpinner className="animate-spin text-blue-600 text-3xl" />
        <span className="ml-2 text-gray-700 dark:text-gray-300">Loading tutors...</span>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="w-full bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700 p-4 rounded-lg">
        <p className="text-red-700 dark:text-red-400">{error}</p>
        <p className="text-sm text-red-600 dark:text-red-400 mt-2">
          Please try again later or contact support if the problem persists.
        </p>
      </div>
    );
  }

  // No results state
  if (tutors.length === 0) {
    return (
      <div className="w-full bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-6 rounded-lg text-center">
        <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-2">No tutors found</h3>
        <p className="text-gray-600 dark:text-gray-400">
          {searchQuery 
            ? `No results for "${searchQuery}". Try different keywords or filters.` 
            : "No tutors match your current filter criteria. Try adjusting your filters."}
        </p>
      </div>
    );
  }

  return (
    <div className="w-full">
      {/* Header with search */}
      <div className="flex flex-col sm:flex-row justify-between items-center mb-6">
        <div className="relative w-full sm:w-96 mb-4 sm:mb-0">
          <input
            type="text"
            placeholder="Search tutors..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full px-4 py-2 pl-10 border border-gray-300 dark:border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-800 dark:text-white"
          />
          <svg
            className="absolute left-3 top-2.5 h-5 w-5 text-gray-400 dark:text-gray-500"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            ></path>
          </svg>
        </div>
      </div>

      {/* Results count */}
      <p className="text-gray-600 dark:text-gray-400 mb-4">
        Showing {tutors.length} tutor{tutors.length !== 1 ? 's' : ''}
      </p>

      {/* Grid View */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {tutors.map((tutor) => (
          <div
            key={tutor.profileId || tutor.id}
            className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow"
          >
            <div className="p-5">
              <div className="flex items-center mb-4">
                <div className="h-16 w-16 rounded-full overflow-hidden mr-4 flex-shrink-0 bg-gray-200 dark:bg-gray-700">
                  <img
                    src={tutor.user?.profileImage || tutor.profilePicture || defaultProfilePic}
                    alt={`${tutor.user?.firstName || tutor.firstName} ${tutor.user?.lastName || tutor.lastName}`}
                    className="h-full w-full object-cover"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = defaultProfilePic;
                    }}
                  />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-800 dark:text-white">
                    {tutor.user?.firstName || tutor.firstName} {tutor.user?.lastName || tutor.lastName}
                  </h3>
                  {renderRatingStars(tutor.avgRating || tutor.rating || 0)}
                </div>
              </div>
              
              <div className="mb-3">
                <div className="flex items-center mb-1 text-gray-600 dark:text-gray-400">
                  <FaMapMarkerAlt className="mr-1" />
                  <span>{tutor.location || tutor.city || 'Location not specified'}{tutor.state ? `, ${tutor.state}` : ''}</span>
                </div>
                <div className="flex flex-wrap gap-2 mb-2">
                  {tutor.isOnline || tutor.offersOnline ? (
                    <span className="bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200 text-xs px-2 py-1 rounded-full flex items-center">
                      <FaVideo className="mr-1" /> Online
                    </span>
                  ) : null}
                  {tutor.isInPerson || tutor.offersInPerson ? (
                    <span className="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200 text-xs px-2 py-1 rounded-full flex items-center">
                      <FaUserFriends className="mr-1" /> In-Person
                    </span>
                  ) : null}
                </div>
              </div>
              
              <div className="mb-4">
                <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Subjects:</h4>
                <div className="flex flex-wrap gap-1">
                  {(tutor.subjects || tutor.subjectEntities || []).slice(0, 3).map((subject, index) => (
                    <span
                      key={index}
                      className="bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 text-xs px-2 py-1 rounded"
                    >
                      {typeof subject === 'string' ? subject : subject.name}
                    </span>
                  ))}
                  {(tutor.subjects || tutor.subjectEntities || []).length > 3 && (
                    <span className="text-gray-500 dark:text-gray-400 text-xs px-1">
                      +{(tutor.subjects || tutor.subjectEntities || []).length - 3} more
                    </span>
                  )}
                </div>
              </div>
              
              <div className="flex items-center justify-between mt-3">
                <span className="text-xl font-bold text-gray-900 dark:text-white">
                  ${tutor.hourlyRate || 25}/hr
                </span>
                <button
                  onClick={() => handleViewProfileClick(tutor.profileId || tutor.id)}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors text-sm"
                >
                  View Profile
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

TutorList.propTypes = {
  tutors: PropTypes.array.isRequired,
  loading: PropTypes.bool,
  error: PropTypes.string,
  searchQuery: PropTypes.string,
  setSearchQuery: PropTypes.func.isRequired
};

TutorList.defaultProps = {
  tutors: [],
  loading: false,
  error: '',
  searchQuery: ''
};

export default TutorList; 