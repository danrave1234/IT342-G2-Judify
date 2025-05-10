import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTutorProfile } from '../context/TutorProfileContext';

const FindTutors = () => {
  const { searchTutors, loading, error } = useTutorProfile();
  const [tutors, setTutors] = useState([]);
  const [searchParams, setSearchParams] = useState({
    subject: '',
    minRating: 0,
    maxPrice: '',
  });

  useEffect(() => {
    fetchTutors();
  }, []);

  const fetchTutors = async () => {
    const result = await searchTutors(searchParams);
    if (result.success) {
      setTutors(result.results);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchTutors();
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setSearchParams(prev => ({
      ...prev,
      [name]: value
    }));
  };

  return (
    <div className="page-container py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-8">Find Tutors</h1>

        {/* Search Filters */}
        <div className="card mb-8">
          <form onSubmit={handleSearch} className="p-4">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label htmlFor="subject" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Subject
                </label>
                <input
                  type="text"
                  id="subject"
                  name="subject"
                  className="input"
                  placeholder="Math, Science, etc."
                  value={searchParams.subject}
                  onChange={handleInputChange}
                />
              </div>

              <div>
                <label htmlFor="minRating" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Minimum Rating
                </label>
                <select
                  id="minRating"
                  name="minRating"
                  className="input"
                  value={searchParams.minRating}
                  onChange={handleInputChange}
                >
                  <option value="0">Any Rating</option>
                  <option value="3">3+ Stars</option>
                  <option value="4">4+ Stars</option>
                  <option value="4.5">4.5+ Stars</option>
                </select>
              </div>

              <div>
                <label htmlFor="maxPrice" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Maximum Price/Hour
                </label>
                <input
                  type="number"
                  id="maxPrice"
                  name="maxPrice"
                  className="input"
                  placeholder="Enter maximum price"
                  value={searchParams.maxPrice}
                  onChange={handleInputChange}
                />
              </div>
            </div>

            <div className="mt-4 flex justify-end">
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? 'Searching...' : 'Search Tutors'}
              </button>
            </div>
          </form>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
            {error}
          </div>
        )}

        {/* Results */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {tutors.length > 0 ? (
            tutors.map(tutor => (
              <div key={tutor.profileId} className="card p-6">
                <div className="flex items-start">
                  <img 
                    src={tutor.profilePicture || "https://via.placeholder.com/80"} 
                    alt={`${tutor.username}'s profile`}
                    className="w-16 h-16 rounded-full object-cover mr-4"
                  />
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                      {tutor.username}
                    </h3>
                    <div className="flex items-center text-sm text-gray-600 dark:text-gray-400">
                      <span className="text-yellow-500 mr-1">★</span> 
                      {tutor.rating || 'No ratings yet'}
                    </div>
                    <p className="text-primary-600 dark:text-primary-500 font-semibold mt-1">
                      ${tutor.hourlyRate}/hour
                    </p>
                  </div>
                </div>

                <div className="mt-4">
                  <h4 className="text-sm font-medium text-gray-900 dark:text-white">Subjects:</h4>
                  <div className="flex flex-wrap gap-2 mt-1">
                    {tutor.subjects?.map(subject => (
                      <span key={subject} className="text-xs bg-primary-100 dark:bg-primary-900/20 text-primary-800 dark:text-primary-300 px-2 py-1 rounded">
                        {subject}
                      </span>
                    ))}
                  </div>
                </div>

                  <button
                    className="mt-auto w-full bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-3 rounded transition duration-200"
                    onClick={(e) => {
                      e.stopPropagation();
                      window.location.href = `/student/book/${tutor.userId || tutor.user?.userId}`;
                    }}
                  >
                    Book Session
                  </button>
              </div>
            ))
          ) : (
            <div className="col-span-full text-center py-12">
              <p className="text-gray-600 dark:text-gray-400">
                {loading ? 'Loading tutors...' : 'No tutors found matching your criteria. Try adjusting your search.'}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default FindTutors; 
