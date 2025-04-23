import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { useSession } from '../../context/SessionContext';
import { SESSION_STATUS } from '../../types';
import { tutorProfileApi, tutorAvailabilityApi } from '../../api/api';
import API from '../../api/api';
import { toast } from 'react-toastify';

const BookSession = () => {
  const { tutorId } = useParams();
  const navigate = useNavigate();
  const { user } = useUser();
  const { createSession } = useSession();

  const [tutor, setTutor] = useState(null);
  const [tutorAvailability, setTutorAvailability] = useState({});
  const [loadingTutor, setLoadingTutor] = useState(true);
  const [loadingTimeSlots, setLoadingTimeSlots] = useState(false);
  const [selectedDate, setSelectedDate] = useState('');
  const [availableTimeSlots, setAvailableTimeSlots] = useState([]);
  const [selectedTimeSlot, setSelectedTimeSlot] = useState(null);
  const [profileIdToUse, setProfileIdToUse] = useState(null);
  const [sessionInfo, setSessionInfo] = useState({
    subject: '',
    notes: '',
    duration: 1,
    isOnline: true
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Try to get tutor info from localStorage as a fallback
  useEffect(() => {
    try {
      const storedTutorInfo = localStorage.getItem('lastViewedTutor');
      if (storedTutorInfo) {
        const parsedInfo = JSON.parse(storedTutorInfo);
        if (parsedInfo.id === tutorId) {
          console.log('Found stored tutor info:', parsedInfo);
          // We'll use this as a fallback if the API calls fail

          // If we're already in a loading state, show a message
          if (loadingTutor) {
            toast.info('Loading tutor information...', { autoClose: 2000 });
          }
        }
      }
    } catch (error) {
      console.error('Error reading from localStorage:', error);
    }
  }, [tutorId, loadingTutor]);

  // Fetch tutor data and availability
  useEffect(() => {
    const fetchTutorData = async () => {
      try {
        let tutorData;
        let profileIdToUse;

        console.log('Attempting to fetch tutor data with ID:', tutorId);

        // First try to fetch by profile ID
        try {
          const tutorResponse = await tutorProfileApi.getProfileById(tutorId);
          tutorData = tutorResponse.data;
          const profileId = tutorData.profileId;
          setProfileIdToUse(profileId);
          console.log('Successfully fetched tutor profile by profileId:', tutorId);
        } catch (error) {
          console.log('Error fetching by profileId:', error.message);

          // If that fails, try to fetch by user ID
          try {
            console.log('Profile not found with profileId, trying userId instead:', tutorId);
            const tutorResponse = await tutorProfileApi.getProfileByUserId(tutorId);
            tutorData = tutorResponse.data;
            const profileId = tutorData.profileId;
            setProfileIdToUse(profileId);
            console.log('Successfully fetched tutor profile by userId:', tutorId);
          } catch (userIdError) {
            console.log('Error fetching by userId:', userIdError.message);

            // If both methods fail, try using the TutorProfileContext's getTutorProfile method
            try {
              console.log('Trying to fetch using context method with ID:', tutorId);
              // This would require importing and using the TutorProfileContext
              // const { getTutorProfile } = useTutorProfile();
              // const result = await getTutorProfile(tutorId);
              // if (result.success) {
              //   tutorData = result.profile;
              //   profileIdToUse = tutorData.profileId;
              // } else {
              //   throw new Error(result.message);
              // }

              // Since we can't use hooks conditionally, we'll just throw an error
              throw new Error('Could not fetch tutor profile with ID: ' + tutorId);
            } catch (contextError) {
              console.error('All methods to fetch tutor profile failed:', contextError);
              throw new Error('Tutor profile could not be found');
            }
          }
        }

        if (!tutorData) {
          throw new Error('No tutor data was retrieved');
        }

        console.log('Retrieved tutor data:', tutorData);
        console.log('Using profileId for availability:', profileIdToUse);

        let availabilityData = [];
        try {
          // Fetch the tutor's availability
          const availabilityResponse = await tutorAvailabilityApi.getAvailabilities(profileIdToUse);
          availabilityData = availabilityResponse.data;
          console.log('Successfully fetched tutor availability:', availabilityData);
        } catch (availError) {
          console.error('Error fetching tutor availability:', availError);
          // Continue with empty availability rather than failing the whole process
          console.log('Continuing with empty availability');
        }

        // Process and group availability data by date
        const availabilityByDate = {};
        availabilityData.forEach(avail => {
          if (!availabilityByDate[avail.date]) {
            availabilityByDate[avail.date] = [];
          }
          availabilityByDate[avail.date].push({
            id: avail.availabilityId,
            startTime: avail.startTime,
            endTime: avail.endTime
          });
        });

        setTutor({
          id: tutorId,
          name: `${tutorData.firstName} ${tutorData.lastName}`,
          profilePicture: tutorData.profilePicture || 'https://via.placeholder.com/150',
          rate: tutorData.hourlyRate || 35,
          subjects: tutorData.subjects || ['General Tutoring']
        });

        setTutorAvailability(availabilityByDate);

        // Set default subject from tutor subjects if available
        if (tutorData.subjects && tutorData.subjects.length > 0) {
          setSessionInfo(prev => ({ ...prev, subject: tutorData.subjects[0] }));
        }
      } catch (error) {
        console.error('Error fetching tutor data:', error);

        // Try to use the stored tutor info as a fallback
        try {
          const storedTutorInfo = localStorage.getItem('lastViewedTutor');
          if (storedTutorInfo) {
            const parsedInfo = JSON.parse(storedTutorInfo);
            if (parsedInfo.id === tutorId) {
              console.log('Using stored tutor info as fallback:', parsedInfo);
              setTutor({
                id: tutorId,
                name: parsedInfo.name,
                profilePicture: parsedInfo.profilePicture,
                rate: 0, // We don't have this info
                subjects: parsedInfo.subjects || ['General Tutoring']
              });
              setTutorAvailability({});
              setError('Using limited tutor information. Some features may be unavailable. Try going back to the tutor list and selecting again.');
              setLoadingTutor(false);
              return;
            }
          }
        } catch (localStorageError) {
          console.error('Error using localStorage fallback:', localStorageError);
        }

        // Check if it's a 404 error (tutor not found)
        if (error.response && error.response.status === 404) {
          setError('Tutor not found. The tutor profile may have been removed or is unavailable.');
        } else {
          setError(`Failed to load tutor information: ${error.message || 'Unknown error'}. Please try again later.`);
        }
      } finally {
        setLoadingTutor(false);
      }
    };

    fetchTutorData();
  }, [tutorId]);

  // Generate available time slots when a date is selected
  useEffect(() => {
    if (!selectedDate || !tutorAvailability[selectedDate]) {
      setAvailableTimeSlots([]);
      return;
    }

    // Get the availability slots for the selected date
    const dateAvailability = tutorAvailability[selectedDate];
    
    if (!dateAvailability || dateAvailability.length === 0) {
      setAvailableTimeSlots([]);
      return;
    }

    setLoadingTimeSlots(true);

    // Process each availability slot to generate 30-minute sessions
    const processedTimeSlots = [];
    
    dateAvailability.forEach(slot => {
      // Convert time strings to Date objects for easier calculation
      const startTime = new Date(`${selectedDate}T${slot.startTime}`);
      const endTime = new Date(`${selectedDate}T${slot.endTime}`);
      
      // Adjust the end time based on session duration to ensure there's enough time
      const actualEndTime = new Date(endTime);
      actualEndTime.setMinutes(actualEndTime.getMinutes() - (sessionInfo.duration * 60));
      
      let currentTime = new Date(startTime);
      
      // Generate 30-minute slots
      while (currentTime <= actualEndTime) {
        const slotStart = new Date(currentTime);
        const slotEnd = new Date(currentTime);
        slotEnd.setMinutes(slotEnd.getMinutes() + (sessionInfo.duration * 60));
        
        processedTimeSlots.push({
          startTime: slotStart.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          endTime: slotEnd.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          value: slotStart.toISOString()
        });
        
        // Move to next 30-minute slot
        currentTime.setMinutes(currentTime.getMinutes() + 30);
      }
    });
    
    // Sort slots by time
    processedTimeSlots.sort((a, b) => {
      return new Date(a.value) - new Date(b.value);
    });
    
    // Remove duplicate slots
    const uniqueSlots = [];
    const seen = new Set();
    
    processedTimeSlots.forEach(slot => {
      if (!seen.has(slot.value)) {
        seen.add(slot.value);
        uniqueSlots.push(slot);
      }
    });
    
    setAvailableTimeSlots(uniqueSlots);
    setLoadingTimeSlots(false);
    setSelectedTimeSlot(null); // Reset selected time slot when date changes
  }, [selectedDate, tutorAvailability, sessionInfo.duration]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setSessionInfo(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleDateChange = (e) => {
    setSelectedDate(e.target.value);
  };

  const handleTimeSlotSelect = (timeSlot) => {
    setSelectedTimeSlot(timeSlot);
  };

  const calculateTotalPrice = () => {
    if (!tutor) return 0;
    return tutor.rate * sessionInfo.duration;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedTimeSlot) {
      setError('Please select a time slot.');
      return;
    }

    if (!sessionInfo.subject) {
      setError('Please select a subject.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      // Parse selected time slot
      const startTime = new Date(selectedTimeSlot.value);
      const endTime = new Date(startTime);
      endTime.setHours(endTime.getHours() + parseInt(sessionInfo.duration));

      const sessionData = {
        tutorId,
        studentId: user.userId,
        subject: sessionInfo.subject,
        notes: sessionInfo.notes,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        price: calculateTotalPrice(),
        status: SESSION_STATUS.SCHEDULED,
        locationData: sessionInfo.isOnline ? null : 'In person location',
        meetingLink: sessionInfo.isOnline ? 'https://meet.google.com/mock-link' : null
      };

      const result = await createSession(sessionData);

      if (result.success) {
        // Create event in Google Calendar
        try {
          await API.post('/calendar/create-event', {
            sessionId: result.session.sessionId
          });
          console.log('Google Calendar event created successfully');
        } catch (calendarError) {
          console.error('Error creating Google Calendar event:', calendarError);
          // Don't block the booking process if calendar event creation fails
          toast.warning('Session booked, but there was an issue syncing with Google Calendar');
        }

        // Store session data in localStorage for payment page
        localStorage.setItem('pendingSessionPayment', JSON.stringify({
          sessionId: result.session.sessionId,
          tutorId: tutorId,
          tutorName: tutor.name,
          subject: sessionInfo.subject,
          startTime: startTime.toISOString(),
          duration: sessionInfo.duration,
          price: calculateTotalPrice(),
          isOnline: sessionInfo.isOnline
        }));

        // Navigate to messages with session context after booking
        navigate('/student/messages', { 
          state: { 
            tutorId: tutorId, 
            sessionContext: {
              sessionDate: startTime.toLocaleDateString(),
              sessionTime: `${startTime.toLocaleTimeString()} - ${endTime.toLocaleTimeString()}`,
              subject: sessionInfo.subject
            }
          }
        });
      } else {
        setError(result.message || 'Failed to book session. Please try again.');
      }
    } catch (error) {
      console.error('Error booking session:', error);
      setError('An error occurred while booking the session. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingTutor) {
    return (
      <div className="flex items-center justify-center min-h-[80vh]">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!tutor) {
    return (
      <div className="text-center py-10">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">Tutor Not Found</h2>
        <p className="text-gray-600 dark:text-gray-400 mb-6">The tutor you&apos;re looking for doesn&apos;t exist or is no longer available.</p>
        <p className="text-gray-600 dark:text-gray-400 mb-6">Error details: {error}</p>
        <div className="flex justify-center space-x-4">
          <Link to="/student/find-tutors" className="btn-primary">Find Another Tutor</Link>
          <button 
            onClick={() => window.history.back()} 
            className="px-4 py-2 bg-gray-200 text-gray-800 rounded hover:bg-gray-300 transition-colors"
          >
            Go Back
          </button>
        </div>
      </div>
    );
  }

  if (tutorAvailability.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="mb-6">
          <Link to={`/tutors/${tutorId}`} className="text-primary-600 dark:text-primary-500 flex items-center">
            ← Back to tutor profile
          </Link>
        </div>

        <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
              Book a Session with {user.firstName} {user.lastName}
            </h1>
            <div className="flex items-center">
              <img 
                src={tutor.profilePicture} 
                alt={`${tutor.name}'s profile`}
                className="w-12 h-12 rounded-full object-cover mr-3"
              />
              <div>
                <p className="text-gray-900 dark:text-white font-medium">{user.username}</p>
                <p className="text-gray-600 dark:text-gray-400">${tutor.rate}/hour</p>
              </div>
            </div>
          </div>

          <div className="text-center py-8">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">No Availability</h2>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              This tutor hasn&apos;t set up their availability yet. Please check back later or contact them directly.
            </p>
            <Link to="/find-tutors" className="btn-primary">Find Another Tutor</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="mb-6">
        <Link to={`/tutors/${tutorId}`} className="text-primary-600 dark:text-primary-500 flex items-center">
          ← Back to tutor profile
        </Link>
      </div>

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
            Book a Session with {tutor.name}
          </h1>
          <div className="flex items-center">
            <img 
              src={tutor.profilePicture} 
              alt={`${tutor.name}'s profile`}
              className="w-12 h-12 rounded-full object-cover mr-3"
            />
            <div>
              <p className="text-gray-900 dark:text-white font-medium">{tutor.name}</p>
              <p className="text-gray-600 dark:text-gray-400">${tutor.rate}/hour</p>
            </div>
          </div>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 dark:bg-red-900/20 dark:border-red-800 dark:text-red-400 px-4 py-3 rounded mb-6">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="space-y-6">
            {/* Subject Selection */}
            <div>
              <label htmlFor="subject" className="block text-gray-700 dark:text-gray-300 mb-2">Subject</label>
              <select
                id="subject"
                name="subject"
                value={sessionInfo.subject}
                onChange={handleInputChange}
                className="input"
                required
              >
                <option value="" disabled>Select a subject</option>
                {tutor.subjects.map((subject, index) => (
                  <option key={index} value={subject}>{subject}</option>
                ))}
              </select>
            </div>

            {/* Session Type */}
            <div>
              <label className="block text-gray-700 dark:text-gray-300 mb-2">Session Type</label>
              <div className="flex items-center space-x-4">
                <label className="inline-flex items-center">
                  <input
                    type="checkbox"
                    name="isOnline"
                    checked={sessionInfo.isOnline}
                    onChange={handleInputChange}
                    className="form-checkbox h-5 w-5 text-primary-600"
                  />
                  <span className="ml-2 text-gray-700 dark:text-gray-300">Online Session</span>
                </label>
              </div>
            </div>

            {/* Duration */}
            <div>
              <label htmlFor="duration" className="block text-gray-700 dark:text-gray-300 mb-2">Duration (hours)</label>
              <select
                id="duration"
                name="duration"
                value={sessionInfo.duration}
                onChange={handleInputChange}
                className="input"
                required
              >
                <option value="1">1 hour</option>
                <option value="1.5">1.5 hours</option>
                <option value="2">2 hours</option>
                <option value="2.5">2.5 hours</option>
                <option value="3">3 hours</option>
              </select>
            </div>

            {/* Date Selection */}
            <div>
              <label htmlFor="date" className="block text-gray-700 dark:text-gray-300 mb-2">Date</label>
              <select
                id="date"
                name="date"
                value={selectedDate}
                onChange={handleDateChange}
                className="input"
                required
              >
                <option value="" disabled>Select a date</option>
                {Object.keys(tutorAvailability).length > 0 ? (
                  Object.keys(tutorAvailability)
                    .sort((a, b) => new Date(a) - new Date(b))
                    .map((date) => (
                      <option key={date} value={date}>
                        {new Date(date).toLocaleDateString('en-US', { 
                          weekday: 'long', 
                          year: 'numeric', 
                          month: 'long', 
                          day: 'numeric' 
                        })}
                      </option>
                    ))
                ) : (
                  <option value="" disabled>No available dates</option>
                )}
              </select>
            </div>

            {/* Time Slot Selection */}
            {selectedDate && (
              <div>
                <label className="block text-gray-700 dark:text-gray-300 mb-2">Available Time Slots</label>
                {loadingTimeSlots ? (
                  <div className="flex justify-center items-center h-20">
                    <div className="w-8 h-8 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
                    <p className="ml-3 text-gray-600 dark:text-gray-400">Checking availability...</p>
                  </div>
                ) : availableTimeSlots.length > 0 ? (
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
                    {availableTimeSlots.map((slot, index) => (
                      <button
                        key={index}
                        type="button"
                        onClick={() => handleTimeSlotSelect(slot)}
                        className={`py-2 px-3 rounded-lg border text-center transition-colors
                          ${selectedTimeSlot === slot 
                            ? 'border-primary-600 bg-primary-50 text-primary-700 dark:border-primary-500 dark:bg-primary-900/20 dark:text-primary-400' 
                            : 'border-gray-300 hover:border-primary-400 hover:bg-primary-50/50 dark:border-dark-600 dark:hover:border-primary-700 dark:hover:bg-primary-900/10'
                          }`}
                      >
                        {slot.startTime} - {slot.endTime}
                      </button>
                    ))}
                  </div>
                ) : (
                  <p className="text-yellow-600 dark:text-yellow-400">No available time slots for this date.</p>
                )}
              </div>
            )}

            {/* Notes */}
            <div>
              <label htmlFor="notes" className="block text-gray-700 dark:text-gray-300 mb-2">
                Notes (optional)
                <span className="text-gray-500 text-sm ml-1">- Share what you want to learn or ask</span>
              </label>
              <textarea
                id="notes"
                name="notes"
                value={sessionInfo.notes}
                onChange={handleInputChange}
                placeholder="I need help with calculus derivatives..."
                rows="3"
                className="input"
              ></textarea>
            </div>

            {/* Summary and Price */}
            <div className="bg-gray-50 dark:bg-dark-700 rounded-lg p-4">
              <h3 className="font-semibold text-gray-900 dark:text-white mb-3">Session Summary</h3>
              <div className="space-y-2 mb-4">
                {selectedTimeSlot && (
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Time:</span>
                    <span className="text-gray-900 dark:text-white">
                      {new Date(selectedDate).toLocaleDateString()} {selectedTimeSlot.startTime} - {selectedTimeSlot.endTime}
                    </span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Duration:</span>
                  <span className="text-gray-900 dark:text-white">{sessionInfo.duration} hours</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Type:</span>
                  <span className="text-gray-900 dark:text-white">{sessionInfo.isOnline ? 'Online' : 'In-person'}</span>
                </div>
                <div className="flex justify-between font-semibold">
                  <span className="text-gray-700 dark:text-gray-300">Total Price:</span>
                  <span className="text-gray-900 dark:text-white">${calculateTotalPrice()}</span>
                </div>
              </div>

              <div className="flex flex-col sm:flex-row gap-3">
                <button
                  type="submit"
                  disabled={submitting || !selectedTimeSlot}
                  className="btn-primary flex-1"
                >
                  {submitting ? 'Booking...' : 'Book Session'}
                </button>
                
                <button
                  type="button"
                  onClick={() => navigate('/student/messages', { 
                    state: { 
                      userId: tutorId,
                      sessionContext: null
                    }
                  })}
                  className="btn bg-blue-600 hover:bg-blue-700 text-white flex-1 flex items-center justify-center"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clipRule="evenodd" />
                  </svg>
                  Chat with Tutor
                </button>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BookSession; 
