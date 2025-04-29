import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useTutorProfile } from '../context/TutorProfileContext';
import { reviewApi } from '../api/api';
import { useUser } from '../context/UserContext';

const TutorDetails = () => {
  const { tutorId } = useParams();
  const { getTutorProfile, loading } = useTutorProfile();
  const { user, isStudent } = useUser();
  const [tutor, setTutor] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchTutorProfile = async () => {
      const result = await getTutorProfile(tutorId);
      if (result.success) {
        const tutorData = result.profile;
        setTutor(tutorData);
        
        // Save tutor data to localStorage for messaging functionality
        try {
          localStorage.setItem('lastViewedTutor', JSON.stringify({
            ...tutorData,
            id: tutorId,
            profileId: tutorId,
            userId: tutorData.userId
          }));
        } catch (error) {
          console.error('Error saving tutor data to localStorage:', error);
        }
      }
    };

    const fetchReviews = async () => {
      setReviewsLoading(true);
      try {
        const response = await reviewApi.getTutorReviews(tutorId, { page: 0, size: 5 });
        setReviews(response.data.content || []);
      } catch (error) {
        console.error('Error fetching reviews:', error);
      } finally {
        setReviewsLoading(false);
      }
    };

    fetchTutorProfile();
    fetchReviews();
  }, [tutorId]);

  const handleChatWithTutor = (e) => {
    e.preventDefault();
    
    // Ensure tutor data is saved in localStorage before navigating
    if (tutor) {
      try {
        localStorage.setItem('lastViewedTutor', JSON.stringify({
          ...tutor,
          id: tutorId,
          profileId: tutorId,
          userId: tutor.userId
        }));
        
        // Navigate to messages with the tutor ID and set action to start conversation
        navigate('/student/messages', { 
          state: { 
            tutorId: tutor.userId || tutorId,
            action: 'startConversation'
          } 
        });
      } catch (error) {
        console.error('Error saving tutor data before chat:', error);
        // Navigate anyway even if localStorage fails
        navigate('/student/messages', { state: { tutorId: tutorId, action: 'startConversation' } });
      }
    } else {
      // If no tutor data, just navigate with the ID from URL
      navigate('/student/messages', { state: { tutorId: tutorId, action: 'startConversation' } });
    }
  };

  if (loading || !tutor) {
    return (
      <div className="page-container flex items-center justify-center min-h-screen">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="page-container py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <Link to="/find-tutors" className="text-primary-600 dark:text-primary-500 flex items-center">
            ← Back to tutors
          </Link>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column: Tutor Info */}
          <div className="lg:col-span-2">
            <div className="card p-6 mb-8">
              <div className="flex flex-col sm:flex-row items-center sm:items-start">
                <img 
                  src={tutor.profilePicture || "https://via.placeholder.com/150"} 
                  alt={`${tutor.username}'s profile`}
                  className="w-32 h-32 rounded-full object-cover mb-4 sm:mb-0 sm:mr-6"
                />
                <div>
                  <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{tutor.username}</h1>
                  <div className="flex items-center mt-1">
                    <span className="text-yellow-500 mr-1">★</span>
                    <span className="text-gray-700 dark:text-gray-300">{tutor.rating || 'No ratings yet'} ({tutor.totalReviews || 0} reviews)</span>
                  </div>
                  <p className="text-primary-600 dark:text-primary-500 font-semibold mt-2">
                    ${tutor.hourlyRate}/hour
                  </p>
                  <div className="mt-3">
                    <h3 className="text-sm font-medium text-gray-900 dark:text-white">Expertise:</h3>
                    <p className="text-gray-600 dark:text-gray-400">{tutor.expertise}</p>
                  </div>
                </div>
              </div>

              <div className="mt-6">
                <h3 className="text-lg font-medium text-gray-900 dark:text-white">About</h3>
                <p className="mt-2 text-gray-600 dark:text-gray-400">{tutor.bio}</p>
              </div>

              <div className="mt-6">
                <h3 className="text-lg font-medium text-gray-900 dark:text-white">Subjects</h3>
                <div className="flex flex-wrap gap-2 mt-2">
                  {tutor.subjects?.map(subject => (
                    <span 
                      key={subject} 
                      className="px-3 py-1 bg-primary-100 dark:bg-primary-900/20 text-primary-800 dark:text-primary-300 rounded-full text-sm"
                    >
                      {subject}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* Reviews Section */}
            <div className="card p-6">
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                Reviews ({tutor.totalReviews || 0})
              </h3>
              
              {reviewsLoading ? (
                <div className="text-center py-6">
                  <div className="w-10 h-10 border-t-4 border-primary-600 border-solid rounded-full animate-spin mx-auto"></div>
                </div>
              ) : reviews.length > 0 ? (
                <div className="space-y-6">
                  {reviews.map(review => (
                    <div key={review.reviewId} className="border-b border-gray-200 dark:border-dark-700 pb-6 last:border-0 last:pb-0">
                      <div className="flex justify-between items-start">
                        <div className="flex items-center">
                          <div className="flex items-center">
                            {Array.from({ length: 5 }).map((_, i) => (
                              <span 
                                key={i} 
                                className={`text-lg ${i < review.rating ? 'text-yellow-500' : 'text-gray-300 dark:text-gray-600'}`}
                              >
                                ★
                              </span>
                            ))}
                          </div>
                          <span className="ml-2 text-sm text-gray-600 dark:text-gray-400">
                            by {review.studentName}
                          </span>
                        </div>
                        <span className="text-xs text-gray-500">
                          {new Date(review.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                      <p className="mt-2 text-gray-600 dark:text-gray-400">
                        {review.comment}
                      </p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-gray-600 dark:text-gray-400 text-center py-6">
                  No reviews yet for this tutor.
                </p>
              )}
            </div>
          </div>

          {/* Right Column: Book Session */}
          <div className="lg:col-span-1">
            <div className="card p-6 sticky top-6">
              <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">
                Book a Session
              </h3>
              
              <div className="mb-6">
                <p className="text-gray-600 dark:text-gray-400 mb-2">
                  Hourly Rate:
                </p>
                <p className="text-2xl font-bold text-primary-600 dark:text-primary-500">
                  ${tutor.hourlyRate}
                </p>
              </div>
              
              {isStudent() ? (
                <Link 
                  to={`/student/book/${tutorId}`} 
                  className="btn-primary w-full text-center block mb-3"
                >
                  Book Now
                </Link>
              ) : !user ? (
                <div>
                  <Link 
                    to="/login" 
                    className="btn-primary w-full text-center block mb-2"
                  >
                    Log in to Book
                  </Link>
                  <p className="text-sm text-gray-600 dark:text-gray-400 text-center">
                    New to Judify?{' '}
                    <Link to="/register" className="text-primary-600 dark:text-primary-500">
                      Sign up
                    </Link>
                  </p>
                </div>
              ) : (
                <p className="text-center text-gray-600 dark:text-gray-400">
                  Tutors cannot book sessions with other tutors.
                </p>
              )}
              
              {isStudent() && (
                <button
                  onClick={handleChatWithTutor}
                  className="btn bg-blue-600 hover:bg-blue-700 text-white w-full text-center block flex items-center justify-center"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clipRule="evenodd" />
                  </svg>
                  Chat with Tutor
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TutorDetails; 