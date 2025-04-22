import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { FaArrowLeft, FaStar } from 'react-icons/fa';
import { toast } from 'react-toastify';
import { reviewApi } from '../../api/api';

const SessionReview = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { getSessionById } = useSession();
  
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    fetchSessionData();
  }, [sessionId]);

  const fetchSessionData = async () => {
    if (!sessionId) return;
    
    setLoading(true);
    try {
      const result = await getSessionById(sessionId);
      if (result.success) {
        setSession(result.session);
        
        // Check if session already has a review
        if (result.session.hasReview) {
          toast.info('You have already reviewed this session');
          navigate(`/student/sessions/${sessionId}`);
          return;
        }
      } else {
        toast.error('Failed to load session details');
        navigate('/student/sessions');
      }
    } catch (error) {
      console.error('Error fetching session:', error);
      toast.error('An error occurred while loading the session');
      navigate('/student/sessions');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    
    if (rating === 0) {
      setError('Please select a rating');
      return;
    }
    
    if (comment.trim().length < 10) {
      setError('Please provide a comment (minimum 10 characters)');
      return;
    }
    
    setError('');
    setSubmitting(true);
    
    try {
      await reviewApi.createReview({
        sessionId: sessionId,
        tutorId: session.tutorId,
        studentId: session.studentId,
        rating: rating,
        comment: comment
      });
      
      toast.success('Review submitted successfully');
      navigate(`/student/sessions/${sessionId}`);
    } catch (error) {
      console.error('Error submitting review:', error);
      toast.error('Failed to submit review. Please try again.');
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[70vh]">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!session) {
    return (
      <div className="container mx-auto py-8 px-4">
        <div className="text-center">
          <h1 className="text-2xl font-bold mb-4">Session Not Found</h1>
          <p className="mb-6">The session you're looking for doesn't exist or has been removed.</p>
          <Link to="/student/sessions" className="btn-primary">
            Back to Sessions
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="mb-6">
        <Link to={`/student/sessions/${sessionId}`} className="text-primary-600 dark:text-primary-500 flex items-center">
          <FaArrowLeft className="mr-2" /> Back to Session Details
        </Link>
      </div>

      <div className="max-w-2xl mx-auto bg-white dark:bg-dark-800 rounded-lg shadow-md overflow-hidden">
        <div className="p-6 border-b border-gray-200 dark:border-dark-700">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Review Your Session
          </h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">
            Share your experience with {session.tutorName} for the {session.subject} session.
          </p>
        </div>

        <div className="p-6">
          {/* Session Summary */}
          <div className="mb-8 bg-gray-50 dark:bg-dark-700 p-4 rounded-lg">
            <div className="flex items-center mb-4">
              <img 
                src={session.tutorProfilePicture || "https://via.placeholder.com/50"} 
                alt={`${session.tutorName}'s profile`} 
                className="w-12 h-12 rounded-full mr-3 object-cover"
              />
              <div>
                <h3 className="font-medium text-gray-900 dark:text-white">{session.tutorName}</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  {new Date(session.startTime).toLocaleDateString()} · {session.subject}
                </p>
              </div>
            </div>
          </div>

          {error && (
            <div className="bg-red-100 border border-red-400 text-red-700 dark:bg-red-900/20 dark:border-red-800 dark:text-red-400 px-4 py-3 rounded mb-6">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmitReview}>
            {/* Rating Stars */}
            <div className="mb-6">
              <label className="block text-gray-700 dark:text-gray-300 mb-2">
                How would you rate your session?
              </label>
              <div className="flex">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    className="text-3xl focus:outline-none"
                    onClick={() => setRating(star)}
                    onMouseEnter={() => setHoverRating(star)}
                    onMouseLeave={() => setHoverRating(0)}
                  >
                    <FaStar
                      className={`
                        ${(hoverRating || rating) >= star
                          ? 'text-yellow-500'
                          : 'text-gray-300 dark:text-gray-600'}
                        transition-colors
                      `}
                    />
                  </button>
                ))}
              </div>
              <div className="text-sm text-gray-600 dark:text-gray-400 mt-2">
                {rating === 1 && 'Poor'}
                {rating === 2 && 'Below Average'}
                {rating === 3 && 'Average'}
                {rating === 4 && 'Good'}
                {rating === 5 && 'Excellent'}
              </div>
            </div>

            {/* Review Comment */}
            <div className="mb-6">
              <label htmlFor="comment" className="block text-gray-700 dark:text-gray-300 mb-2">
                Write your review
              </label>
              <textarea
                id="comment"
                rows="5"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                className="w-full rounded-lg border border-gray-300 dark:border-dark-600 p-3 focus:outline-none focus:ring-2 focus:ring-primary-500 dark:bg-dark-700 dark:text-white"
                placeholder="Share details about your experience, what went well, and what could be improved..."
                required
              ></textarea>
            </div>

            {/* Submit Button */}
            <div className="flex justify-end">
              <button
                type="submit"
                disabled={submitting || rating === 0}
                className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? (
                  <span className="flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Submitting...
                  </span>
                ) : (
                  'Submit Review'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default SessionReview; 