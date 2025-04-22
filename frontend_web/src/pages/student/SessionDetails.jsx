import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useSession } from '../../context/SessionContext';
import { reviewApi } from '../../api/api';
import { SESSION_STATUS } from '../../types';

const SessionDetails = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { getSessionById, updateSessionStatus, loading } = useSession();
  const [session, setSession] = useState(null);
  const [review, setReview] = useState(null);
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewForm, setReviewForm] = useState({
    rating: 5,
    comment: '',
  });

  useEffect(() => {
    fetchSessionDetails();
  }, [sessionId]);

  const fetchSessionDetails = async () => {
    const result = await getSessionById(sessionId);
    if (result.success) {
      setSession(result.session);

      // Check if session has a review
      try {
        const reviewResponse = await reviewApi.getReviews({ sessionId });
        if (reviewResponse.data && reviewResponse.data.content?.length > 0) {
          setReview(reviewResponse.data.content[0]);
        }
      } catch (error) {
        console.error('Error fetching review:', error);
      }
    } else {
      // Handle error or redirect
      navigate('/student/sessions');
    }
  };

  const handleCancelSession = async () => {
    if (window.confirm('Are you sure you want to cancel this session?')) {
      const result = await updateSessionStatus(sessionId, SESSION_STATUS.CANCELLED);
      if (result.success) {
        setSession(result.session);
      }
    }
  };

  const handleReviewChange = (e) => {
    const { name, value } = e.target;
    setReviewForm(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    setSubmittingReview(true);

    try {
      const reviewData = {
        sessionId: session.sessionId,
        tutorId: session.tutorId,
        studentId: session.studentId,
        rating: parseInt(reviewForm.rating),
        comment: reviewForm.comment
      };

      const response = await reviewApi.createReview(reviewData);
      setReview(response.data);
      setReviewForm({ rating: 5, comment: '' });
    } catch (error) {
      console.error('Error submitting review:', error);
    } finally {
      setSubmittingReview(false);
    }
  };

  if (loading || !session) {
    return (
      <div className="flex items-center justify-center min-h-[80vh]">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  const isUpcoming = session.status === SESSION_STATUS.SCHEDULED;
  const isPast = [SESSION_STATUS.COMPLETED, SESSION_STATUS.CANCELLED].includes(session.status);
  const canSubmitReview = session.status === SESSION_STATUS.COMPLETED && !review;
  const canCancel = session.status === SESSION_STATUS.SCHEDULED;

  const sessionDate = new Date(session.startTime);
  const endTime = new Date(session.endTime);
  const durationHours = (endTime - sessionDate) / (1000 * 60 * 60);

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="mb-6">
        <Link to="/student/sessions" className="text-primary-600 dark:text-primary-500 flex items-center">
          ← Back to sessions
        </Link>
      </div>

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700 mb-8">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            {session.subject} Session
          </h1>
          <div className="mt-2 sm:mt-0">
            <span className={`inline-flex px-3 py-1 rounded-full text-sm font-medium
              ${session.status === SESSION_STATUS.SCHEDULED ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-300' :
                session.status === SESSION_STATUS.COMPLETED ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300' :
                session.status === SESSION_STATUS.CANCELLED ? 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300' :
                'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-300'
              }`}
            >
              {session.status}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Session Details</h2>
            <div className="space-y-3">
              <div>
                <span className="text-gray-500 dark:text-gray-400">Date:</span>
                <p className="text-gray-900 dark:text-white">{sessionDate.toLocaleDateString()}</p>
              </div>
              <div>
                <span className="text-gray-500 dark:text-gray-400">Time:</span>
                <p className="text-gray-900 dark:text-white">
                  {sessionDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - 
                  {endTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </p>
              </div>
              <div>
                <span className="text-gray-500 dark:text-gray-400">Duration:</span>
                <p className="text-gray-900 dark:text-white">{durationHours} hours</p>
              </div>
              <div>
                <span className="text-gray-500 dark:text-gray-400">Price:</span>
                <p className="text-gray-900 dark:text-white">${session.price}</p>
              </div>
              {session.locationData && (
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Location:</span>
                  <p className="text-gray-900 dark:text-white">{session.locationData}</p>
                </div>
              )}
              {session.meetingLink && (
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Meeting Link:</span>
                  <p className="text-gray-900 dark:text-white">
                    <a 
                      href={session.meetingLink} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="text-primary-600 dark:text-primary-500 hover:underline"
                    >
                      {session.meetingLink}
                    </a>
                  </p>
                </div>
              )}
            </div>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Tutor Information</h2>
            <div className="flex items-center mb-4">
              <img 
                src="https://via.placeholder.com/60" 
                alt="Tutor profile"
                className="h-16 w-16 rounded-full object-cover mr-4"
              />
              <div>
                <h3 className="text-lg font-medium text-gray-900 dark:text-white">Tutor Name</h3>
                {session.tutorId ? (
                  <Link 
                    to={`/tutors/${session.tutorId}`}
                    className="text-primary-600 dark:text-primary-500 text-sm"
                  >
                    View Profile
                  </Link>
                ) : (
                  <span className="text-gray-400 dark:text-gray-600 text-sm">
                    Profile not available
                  </span>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <div>
                <span className="text-gray-500 dark:text-gray-400">Subject:</span>
                <p className="text-gray-900 dark:text-white">{session.subject}</p>
              </div>
              {session.notes && (
                <div>
                  <span className="text-gray-500 dark:text-gray-400">Session Notes:</span>
                  <p className="text-gray-900 dark:text-white">{session.notes}</p>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="mt-8 flex flex-wrap gap-3">
          {canCancel && (
            <button
              onClick={handleCancelSession}
              className="bg-red-100 text-red-700 hover:bg-red-200 dark:bg-red-900/20 dark:text-red-400 dark:hover:bg-red-900/30 px-4 py-2 rounded-lg transition-colors"
            >
              Cancel Session
            </button>
          )}
          {isUpcoming && session.meetingLink && (
            <a
              href={session.meetingLink}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary"
            >
              Join Meeting
            </a>
          )}
          {!isPast && session.tutorId && (
            <Link
              to={`/student/messages`}
              state={{ tutorId: session.tutorId }}
              className="bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-dark-700 dark:text-gray-300 dark:hover:bg-dark-600 px-4 py-2 rounded-lg transition-colors"
            >
              Message Tutor
            </Link>
          )}
        </div>
      </div>

      {/* Review Section */}
      {canSubmitReview && (
        <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Leave a Review</h2>
          <form onSubmit={handleSubmitReview}>
            <div className="mb-4">
              <label className="block text-gray-700 dark:text-gray-300 mb-2">Rating</label>
              <div className="flex gap-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setReviewForm(prev => ({ ...prev, rating: star }))}
                    className="text-3xl focus:outline-none"
                  >
                    <span className={star <= reviewForm.rating ? 'text-yellow-500' : 'text-gray-300 dark:text-gray-600'}>
                      ★
                    </span>
                  </button>
                ))}
              </div>
            </div>
            <div className="mb-4">
              <label htmlFor="comment" className="block text-gray-700 dark:text-gray-300 mb-2">Comment</label>
              <textarea
                id="comment"
                name="comment"
                rows="4"
                value={reviewForm.comment}
                onChange={handleReviewChange}
                placeholder="Share your experience with this tutor..."
                className="input"
                required
              ></textarea>
            </div>
            <button
              type="submit"
              disabled={submittingReview}
              className="btn-primary"
            >
              {submittingReview ? 'Submitting...' : 'Submit Review'}
            </button>
          </form>
        </div>
      )}

      {/* Existing Review */}
      {review && (
        <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Your Review</h2>
          <div>
            <div className="flex mb-2">
              {Array.from({ length: 5 }).map((_, i) => (
                <span 
                  key={i} 
                  className={`text-2xl ${i < review.rating ? 'text-yellow-500' : 'text-gray-300 dark:text-gray-600'}`}
                >
                  ★
                </span>
              ))}
            </div>
            <p className="text-gray-700 dark:text-gray-300">{review.comment}</p>
            <p className="text-sm text-gray-500 mt-2">
              Submitted on {new Date(review.createdAt).toLocaleDateString()}
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default SessionDetails; 
