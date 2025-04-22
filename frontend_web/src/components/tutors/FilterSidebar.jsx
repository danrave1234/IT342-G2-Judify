import { useState } from 'react';
import PropTypes from 'prop-types';
import { FaFilter, FaStar, FaChalkboardTeacher, FaVideo, FaUserFriends } from 'react-icons/fa';

const FilterSidebar = ({ 
  subjects, 
  selectedSubject, 
  onSubjectChange, 
  rateRange, 
  onRateChange, 
  minRating, 
  onRatingChange, 
  sessionType, 
  onSessionTypeChange, 
  applyFilters, 
  resetFilters 
}) => {
  // Local state to track the input fields before submitting
  const [tempRateMin, setTempRateMin] = useState(rateRange.min);
  const [tempRateMax, setTempRateMax] = useState(rateRange.max);

  // Handle applying the rate range
  const handleRateApply = () => {
    onRateChange({ min: tempRateMin, max: tempRateMax });
  };

  // Handle rating selection (1-5 stars)
  const handleRatingClick = (rating) => {
    onRatingChange(rating === minRating ? 0 : rating);
  };

  return (
    <div className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow-md">
      <div className="mb-5">
        <div className="flex items-center mb-3">
          <FaFilter className="mr-2 text-blue-600 dark:text-blue-400" />
          <h3 className="text-lg font-semibold text-gray-800 dark:text-white">Filters</h3>
        </div>
        <button 
          onClick={resetFilters}
          className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
        >
          Reset All Filters
        </button>
      </div>

      {/* Subject Selection */}
      <div className="mb-6">
        <div className="flex items-center mb-3">
          <FaChalkboardTeacher className="mr-2 text-blue-600 dark:text-blue-400" />
          <h3 className="font-medium text-gray-800 dark:text-white">Subject</h3>
        </div>
        <select
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
          value={selectedSubject}
          onChange={(e) => onSubjectChange(e.target.value)}
        >
          <option value="">All Subjects</option>
          {subjects.map((subject) => (
            <option key={subject} value={subject}>
              {subject}
            </option>
          ))}
        </select>
      </div>

      {/* Price Range */}
      <div className="mb-6">
        <div className="flex items-center mb-3">
          <span className="mr-2 text-blue-600 dark:text-blue-400">$</span>
          <h3 className="font-medium text-gray-800 dark:text-white">Hourly Rate</h3>
        </div>
        <div className="flex items-center space-x-2 mb-3">
          <div className="relative flex-1">
            <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-gray-500 dark:text-gray-400">$</span>
            <input
              type="number"
              placeholder="Min"
              className="w-full pl-7 pr-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
              value={tempRateMin}
              onChange={(e) => setTempRateMin(e.target.value)}
            />
          </div>
          <span className="text-gray-500 dark:text-gray-400">-</span>
          <div className="relative flex-1">
            <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-gray-500 dark:text-gray-400">$</span>
            <input
              type="number"
              placeholder="Max"
              className="w-full pl-7 pr-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
              value={tempRateMax}
              onChange={(e) => setTempRateMax(e.target.value)}
            />
          </div>
        </div>
        <button
          onClick={handleRateApply}
          className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-blue-600 dark:hover:bg-blue-700"
        >
          Apply
        </button>
      </div>

      {/* Tutor Rating */}
      <div className="mb-6">
        <div className="flex items-center mb-3">
          <FaStar className="mr-2 text-blue-600 dark:text-blue-400" />
          <h3 className="font-medium text-gray-800 dark:text-white">Tutor Rating</h3>
        </div>
        <div className="flex items-center space-x-1">
          {[5, 4, 3, 2, 1].map((rating) => (
            <button
              key={rating}
              onClick={() => handleRatingClick(rating)}
              className={`flex items-center px-3 py-2 border ${
                minRating >= rating
                  ? 'bg-blue-50 border-blue-500 text-blue-700 dark:bg-blue-900 dark:border-blue-400 dark:text-blue-200'
                  : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700'
              } rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500`}
            >
              {rating}+ <FaStar className="ml-1 text-yellow-400" />
            </button>
          ))}
        </div>
      </div>

      {/* Session Type */}
      <div className="mb-6">
        <div className="flex items-center mb-3">
          <FaVideo className="mr-2 text-blue-600 dark:text-blue-400" />
          <h3 className="font-medium text-gray-800 dark:text-white">Session Type</h3>
        </div>
        <div className="flex flex-col space-y-2">
          <button
            onClick={() => onSessionTypeChange('all')}
            className={`flex items-center px-3 py-2 border ${
              sessionType === 'all'
                ? 'bg-blue-50 border-blue-500 text-blue-700 dark:bg-blue-900 dark:border-blue-400 dark:text-blue-200'
                : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700'
            } rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500`}
          >
            <FaUserFriends className="mr-2" /> All Sessions
          </button>
          
          <button
            onClick={() => onSessionTypeChange('online')}
            className={`flex items-center px-3 py-2 border ${
              sessionType === 'online'
                ? 'bg-blue-50 border-blue-500 text-blue-700 dark:bg-blue-900 dark:border-blue-400 dark:text-blue-200'
                : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700'
            } rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500`}
          >
            <FaVideo className="mr-2" /> Online Only
          </button>
          
          <button
            onClick={() => onSessionTypeChange('inPerson')}
            className={`flex items-center px-3 py-2 border ${
              sessionType === 'inPerson'
                ? 'bg-blue-50 border-blue-500 text-blue-700 dark:bg-blue-900 dark:border-blue-400 dark:text-blue-200'
                : 'bg-white border-gray-300 text-gray-700 hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700'
            } rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500`}
          >
            <FaUserFriends className="mr-2" /> In-Person Only
          </button>
        </div>
      </div>

      {/* Apply Filters Button */}
      <button
        onClick={applyFilters}
        className="w-full bg-blue-600 text-white py-3 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-blue-600 dark:hover:bg-blue-700"
      >
        Apply Filters
      </button>
    </div>
  );
};

FilterSidebar.propTypes = {
  subjects: PropTypes.array.isRequired,
  selectedSubject: PropTypes.string,
  onSubjectChange: PropTypes.func.isRequired,
  rateRange: PropTypes.shape({
    min: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    max: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
  }),
  onRateChange: PropTypes.func.isRequired,
  minRating: PropTypes.number,
  onRatingChange: PropTypes.func.isRequired,
  sessionType: PropTypes.string,
  onSessionTypeChange: PropTypes.func.isRequired,
  applyFilters: PropTypes.func.isRequired,
  resetFilters: PropTypes.func.isRequired
};

FilterSidebar.defaultProps = {
  subjects: [],
  selectedSubject: '',
  rateRange: { min: '', max: '' },
  minRating: 0,
  sessionType: 'all'
};

export default FilterSidebar; 