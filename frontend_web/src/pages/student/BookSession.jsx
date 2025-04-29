import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { useSession } from '../../context/SessionContext';
import { SESSION_STATUS } from '../../types';
import { tutorProfileApi } from '../../api/api';
import API from '../../api/api'; // Import the default export
import { toast } from 'react-toastify';
import { format, startOfDay, addMonths, parseISO } from 'date-fns';

// Import the DatePicker component
import DatePicker from '../../components/common/DatePicker';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const BookSession = () => {
  const { tutorId: tutorIdParam } = useParams(); // Renamed to avoid conflict
  const navigate = useNavigate();
  const { user } = useUser(); // Use UserContext
  const { createSession } = useSession();

  const [tutor, setTutor] = useState(null);
  const [rawTutorAvailability, setRawTutorAvailability] = useState([]);
  const [availableDates, setAvailableDates] = useState([]); // Initialize as empty array
  const [loadingTutor, setLoadingTutor] = useState(true);
  const [loadingTimeSlots, setLoadingTimeSlots] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);
  const [availableTimeSlots, setAvailableTimeSlots] = useState([]);
  const [selectedTimeSlot, setSelectedTimeSlot] = useState(null);
  const [sessionInfo, setSessionInfo] = useState({
    subject: '',
    notes: '',
    duration: 1,
    isOnline: true,
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [maxSelectableDate, setMaxSelectableDate] = useState(addMonths(new Date(), 1)); // Max 1 month ahead

  // Fetch tutor data and availability
  // Use useCallback to memoize the function if needed, though not strictly necessary here
  const fetchTutorData = useCallback(async () => {
    setLoadingTutor(true);
    setError('');
    setAvailableDates([]); // Reset dates on re-fetch

    const currentTutorId = tutorIdParam; // Use the param directly

    try {
      let tutorData;
      console.log('BookSession.jsx: Attempting to fetch tutor data with ID:', currentTutorId);

      // Use tutorProfileApi for profile fetching
      try {
        const tutorResponse = await tutorProfileApi.getProfileById(currentTutorId);
        tutorData = tutorResponse.data;
        console.log('Successfully fetched tutor profile by profileId:', currentTutorId);
      } catch (error) {
        console.warn(`Error fetching by profileId (${currentTutorId}), trying userId:`, error.message);
        try {
          const tutorResponse = await tutorProfileApi.getProfileByUserId(currentTutorId);
          tutorData = tutorResponse.data;
          console.log('Successfully fetched tutor profile by userId:', currentTutorId);
        } catch (userIdError) {
          console.error(`Error fetching tutor profile by userId (${currentTutorId}):`, userIdError);
          throw new Error('Tutor profile not found for ID: ' + currentTutorId);
        }
      }

      if (!tutorData || !tutorData.userId) {
        throw new Error('Incomplete tutor data retrieved. Missing user ID.');
      }
      console.log('Retrieved tutor data:', tutorData);

      // --- FIX: Store both userId and profileId in tutor state ---
      setTutor({
        profileId: tutorData.profileId || currentTutorId, // Ensure profileId is stored
        userId: tutorData.userId, // Keep userId for availability fetching etc.
        name: `${tutorData.firstName} ${tutorData.lastName}`,
        profilePicture: tutorData.profilePicture || 'https://upload.wikimedia.org/wikipedia/commons/7/7c/Profile_avatar_placeholder_large.png?20150327203541',
        rate: tutorData.hourlyRate || 35,
        subjects: tutorData.subjects || ['General Tutoring'],
      }); // --- END FIX ---

      // Fetch availability using the tutor's userId
      const tutorUserId = tutorData.userId;
      let availabilityData = [];
      try {
        console.log('Fetching tutor availability with userId:', tutorUserId);
        // Use the default exported API instance for availability
        const availabilityResponse = await API.get(`/tutor-availability/findByTutor/${tutorUserId}`);
        if (availabilityResponse.data && Array.isArray(availabilityResponse.data)) {
          availabilityData = availabilityResponse.data;
          console.log('Successfully fetched tutor availability with userId:', availabilityData);
          setRawTutorAvailability(availabilityData); // Store raw data
        } else {
          console.warn('No availability data returned or invalid format');
          setRawTutorAvailability([]); // Ensure it's an empty array if no data
        }
      } catch (availError) {
        console.error('Error fetching tutor availability:', availError);
        setError('Could not load tutor availability.');
        setRawTutorAvailability([]);
      }

      // Process availability into Date objects for DatePicker
      const processedDates = new Set();
      const today = startOfDay(new Date());
      const maxDate = addMonths(today, 1); // Limit to 1 month
      setMaxSelectableDate(maxDate); // Set max date for DatePicker

      availabilityData.forEach(avail => {
        const dayMap = {
          'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3, 'THURSDAY': 4,
          'FRIDAY': 5, 'SATURDAY': 6, 'SUNDAY': 0,
        };
        // Ensure dayOfWeek is treated as a string and handle case-insensitivity
        const dayNum = dayMap[String(avail.dayOfWeek).toUpperCase()];

        if (dayNum !== undefined) {
          for (let i = 0; i < 31; i++) { // Check next 31 days
            const date = new Date(today);
            date.setDate(today.getDate() + i);
            const dateAtStart = startOfDay(date);

            if (dateAtStart.getDay() === dayNum && dateAtStart <= maxDate) {
              // Basic check for time format validity (HH:MM)
              const timeRegex = /^([01]\d|2[0-3]):([0-5]\d)$/;
              if (avail.startTime && avail.endTime && timeRegex.test(avail.startTime) && timeRegex.test(avail.endTime)) {
                const startTimeParts = avail.startTime.split(':').map(Number);
                const endTimeParts = avail.endTime.split(':').map(Number);
                // Check if start time is before end time
                if (startTimeParts[0] < endTimeParts[0] || (startTimeParts[0] === endTimeParts[0] && startTimeParts[1] < endTimeParts[1])) {
                  processedDates.add(dateAtStart.getTime());
                } else {
                  console.warn(`Invalid time range (start >= end) for ${avail.dayOfWeek}: ${avail.startTime}-${avail.endTime}`);
                }
              } else {
                console.warn(`Invalid or missing time format for ${avail.dayOfWeek}: Start='${avail.startTime}', End='${avail.endTime}'`);
              }
            }
          }
        } else {
          console.warn(`Unknown day of week encountered in availability: ${avail.dayOfWeek}`);
        }
      });

      const uniqueDateObjects = Array.from(processedDates).map(ts => new Date(ts)).sort((a, b) => a - b);
      console.log('Processed available Date objects:', uniqueDateObjects);
      setAvailableDates(uniqueDateObjects); // Set the state for DatePicker

      // Set default subject
      if (tutorData.subjects && tutorData.subjects.length > 0) {
        setSessionInfo(prev => ({ ...prev, subject: tutorData.subjects[0] }));
      }

    } catch (err) {
      console.error('Error fetching tutor data or availability:', err);
      setError(`Failed to load tutor information: ${err.message}. Please try again.`);
      setAvailableDates([]); // Ensure availableDates is empty on error
    } finally {
      setLoadingTutor(false);
    }
  }, [tutorIdParam]); // Dependency array

  useEffect(() => {
    fetchTutorData();
  }, [fetchTutorData]); // Run fetchTutorData when the component mounts or tutorIdParam changes

  // --- Generate available time slots useEffect (Keep as is) ---
  useEffect(() => {
    if (!selectedDate || !rawTutorAvailability.length) {
      setAvailableTimeSlots([]);
      return;
    }

    setLoadingTimeSlots(true);
    const selectedDayOfWeek = format(selectedDate, 'EEEE'); // Get full day name (e.g., 'Monday')
    const dateStr = format(selectedDate, 'yyyy-MM-dd');

    const slotsForDay = rawTutorAvailability.filter(
        avail => String(avail.dayOfWeek).toUpperCase() === selectedDayOfWeek.toUpperCase()
    );

    const processedTimeSlots = [];
    const now = new Date();
    const isToday = startOfDay(selectedDate).getTime() === startOfDay(now).getTime();

    slotsForDay.forEach(slot => {
      try {
        // Ensure times are valid before creating Date objects
        if (slot.startTime && slot.endTime) {
          const startTime = new Date(`${dateStr}T${slot.startTime}`);
          const endTime = new Date(`${dateStr}T${slot.endTime}`);

          // Check if Date objects are valid
          if (!(startTime instanceof Date && !isNaN(startTime)) || !(endTime instanceof Date && !isNaN(endTime))) {
            console.warn(`Invalid time format in availability slot:`, slot);
            return; // Skip this slot
          }

          let currentTime = new Date(startTime);

          while (currentTime < endTime) {
            const slotEnd = new Date(currentTime);
            slotEnd.setMinutes(slotEnd.getMinutes() + (sessionInfo.duration * 60));

            // Ensure the slot ends within the tutor's availability window and is in the future if today
            if (slotEnd <= endTime && (!isToday || currentTime > now)) {
              processedTimeSlots.push({
                startTime: format(currentTime, 'p'), // e.g., "10:00 AM"
                endTime: format(slotEnd, 'p'), // e.g., "11:00 AM"
                value: currentTime.toISOString() // Store ISO string for submission
              });
            }
            // Move to next 30-minute interval
            currentTime.setMinutes(currentTime.getMinutes() + 30);
          }
        } else {
          console.warn(`Missing startTime or endTime in availability slot:`, slot);
        }
      } catch (error) {
        console.error('Error processing time slot:', error, slot);
      }
    });

    // Sort slots chronologically
    processedTimeSlots.sort((a, b) => new Date(a.value) - new Date(b.value));

    const uniqueSlots = Array.from(new Map(processedTimeSlots.map(item => [item.value, item])).values());

    console.log(`Generated ${uniqueSlots.length} unique time slots for ${format(selectedDate, 'yyyy-MM-dd')}`);
    setAvailableTimeSlots(uniqueSlots);
    setLoadingTimeSlots(false);
    setSelectedTimeSlot(null); // Reset selected time slot when date changes
  }, [selectedDate, rawTutorAvailability, sessionInfo.duration]);


  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setSessionInfo(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleDateChange = (date) => {
    // Log the selected date from the DatePicker component
    console.log('DatePicker returned:', date);
    // Ensure we are setting a Date object or null
    if (date instanceof Date && !isNaN(date)) {
      setSelectedDate(startOfDay(date)); // Store Date object, normalized
      setSelectedTimeSlot(null); // Reset time slot when date changes
    } else {
      setSelectedDate(null); // Set to null if invalid date received
      setSelectedTimeSlot(null);
      setAvailableTimeSlots([]); // Clear time slots if date is invalid/cleared
    }
  };

  // --- handleTimeSlotSelect (Keep as is) ---
  const handleTimeSlotSelect = (timeSlot) => {
    setSelectedTimeSlot(timeSlot);
  };

  // --- calculateTotalPrice (Keep as is) ---
  const calculateTotalPrice = () => {
    if (!tutor) return 0;
    return tutor.rate * sessionInfo.duration;
  };

  // --- handleSubmit ---
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedTimeSlot) {
      setError('Please select a time slot.');
      toast.error('Please select a time slot.');
      return;
    }
    if (!sessionInfo.subject) {
      setError('Please select a subject.');
      toast.error('Please select a subject.');
      return;
    }
    if (!user || !user.userId) {
      setError('User not found. Please log in again.');
      toast.error('User not found. Please log in again.');
      return;
    }
    // Ensure tutor ID is available
    if (!tutor || !tutor.profileId) { // Check for profileId now
      setError('Tutor information is missing.');
      toast.error('Tutor information is missing.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const startTime = new Date(selectedTimeSlot.value);
      const endTime = new Date(startTime);
      endTime.setMinutes(endTime.getMinutes() + sessionInfo.duration * 60);

      // --- FIX: Use profileId from tutor state for submission ---
      const sessionData = {
        studentId: user.userId,
        subject: sessionInfo.subject,
        notes: sessionInfo.notes,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        price: calculateTotalPrice(),
        status: SESSION_STATUS.SCHEDULED, // Or maybe PENDING if negotiation is needed
        locationData: sessionInfo.isOnline ? null : 'In person - TBD',
        // Include acceptance status
        studentAccepted: true,
        tutorAccepted: false,
        tutorId: tutor.profileId, // Use profileId here
      }; // --- END FIX ---

      console.log("Submitting session data:", sessionData);

      const result = await createSession(sessionData);

      if (result.success && result.session) {
        toast.success('Session booked successfully! Proceeding to payment.');
        // Store necessary info for payment page
        localStorage.setItem('pendingSessionPayment', JSON.stringify({
          sessionId: result.session.sessionId,
          tutorId: tutor.profileId, // Use profileId here too
          tutorName: tutor.name,
          subject: sessionInfo.subject,
          startTime: startTime.toISOString(),
          duration: sessionInfo.duration,
          price: calculateTotalPrice(),
          isOnline: sessionInfo.isOnline
        }));
        navigate('/student/payments?tab=payment'); // Redirect to payment page
      } else {
        setError(result.message || 'Failed to book session. Please try again.');
        toast.error(result.message || 'Failed to book session.');
      }
    } catch (err) {
      console.error('Error booking session:', err);
      const errorMsg = err.response?.data?.message || err.message || 'An unknown error occurred';
      setError(`An error occurred: ${errorMsg}. Please try again.`);
      toast.error(`Booking error: ${errorMsg}`);
    } finally {
      setSubmitting(false);
    }
  };

  // --- Render Loading State (Keep as is) ---
  if (loadingTutor) {
    return (
        <div className="flex items-center justify-center min-h-[80vh]">
          <LoadingSpinner size="xl" />
        </div>
    );
  }

  // --- Render Tutor Not Found State (Keep as is) ---
  if (!tutor) {
    return (
        <div className="text-center py-10">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">Tutor Not Found</h2>
          <p className="text-gray-600 dark:text-gray-400 mb-6">{error || 'The tutor profile may be unavailable.'}</p>
          <Link to="/student/find-tutors" className="btn-primary">Find Another Tutor</Link>
        </div>
    );
  }

  // --- Main Return JSX ---
  return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Back Link */}
        <div className="mb-6">
          {/* Use tutor.profileId if available, otherwise tutor.id */}
          <Link to={`/tutors/${tutor.profileId || tutor.userId}`} className="text-primary-600 dark:text-primary-500 flex items-center hover:underline">
            ← Back to tutor profile
          </Link>
        </div>

        <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700">
          {/* Tutor Info Header */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
              Book a Session with {tutor.name}
            </h1>
            <div className="flex items-center">
              <img
                  src={tutor.profilePicture}
                  alt={`${tutor.name}'s profile`}
                  className="w-12 h-12 rounded-full object-cover mr-3"
                  onError={(e) => { e.target.src = 'https://via.placeholder.com/150'; }} // Fallback image
              />
              <div>
                <p className="text-gray-900 dark:text-white font-medium">{tutor.name}</p>
                <p className="text-gray-600 dark:text-gray-400">${tutor.rate}/hour</p>
              </div>
            </div>
          </div>

          {/* Error Display */}
          {error && (
              <div className="bg-red-100 border border-red-400 text-red-700 dark:bg-red-900/20 dark:border-red-800 dark:text-red-400 px-4 py-3 rounded mb-6">
                {error}
              </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="space-y-6">
              {/* Subject Selection */}
              <div>
                <label htmlFor="subject" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Subject</label>
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
              {/* ... (keep as is) ... */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Session Type</label>
                <div className="flex items-center space-x-4">
                  <label className="inline-flex items-center">
                    <input
                        type="checkbox"
                        name="isOnline"
                        checked={sessionInfo.isOnline}
                        onChange={handleInputChange}
                        className="form-checkbox h-5 w-5 text-primary-600"
                    />
                    <span className="ml-2 text-sm text-gray-700 dark:text-gray-300">Online Session</span>
                  </label>
                </div>
              </div>

              {/* Duration */}
              {/* ... (keep as is) ... */}
              <div>
                <label htmlFor="duration" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Duration (hours)</label>
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
                </select>
              </div>


              {/* DatePicker component */}
              <div>
                <label htmlFor="date" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Date</label>
                <DatePicker
                    selectedDate={selectedDate}
                    onSelectDate={handleDateChange}
                    availableDates={availableDates} // Pass the processed available dates
                    placeholderText="Select an available date"
                    minDate={startOfDay(new Date())} // Ensure minDate is start of day
                    maxDate={maxSelectableDate} // Use the calculated max date
                />
                {availableDates.length === 0 && !loadingTutor && (
                    <div className="mt-2 p-3 bg-yellow-50 dark:bg-yellow-900/10 border border-yellow-200 dark:border-yellow-800/30 rounded-lg">
                      <p className="text-yellow-600 dark:text-yellow-400 text-sm">
                        <strong>No available dates found.</strong> This tutor may not have set their availability for the next month.
                      </p>
                      <p className="text-xs text-yellow-500 dark:text-yellow-500 mt-1">
                        Please check back later or contact the tutor directly via chat.
                      </p>
                    </div>
                )}
              </div>

              {/* Time Slot Selection */}
              {/* ... (keep as is) ... */}
              {selectedDate && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Available Time Slots</label>
                    {loadingTimeSlots ? (
                        <div className="flex justify-center items-center h-20">
                          <LoadingSpinner size="md" />
                          <p className="ml-3 text-sm text-gray-600 dark:text-gray-400">Loading time slots...</p>
                        </div>
                    ) : availableTimeSlots.length > 0 ? (
                        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
                          {availableTimeSlots.map((slot, index) => (
                              <button
                                  key={index}
                                  type="button"
                                  onClick={() => handleTimeSlotSelect(slot)}
                                  className={`py-2 px-3 rounded-lg border text-sm text-center transition-colors
                          ${selectedTimeSlot?.value === slot.value
                                      ? 'border-primary-600 bg-primary-50 text-primary-700 dark:border-primary-500 dark:bg-primary-900/20 dark:text-primary-400 font-semibold'
                                      : 'border-gray-300 hover:border-primary-400 hover:bg-primary-50/50 dark:border-dark-600 dark:hover:border-primary-700 dark:hover:bg-primary-900/10 text-gray-700 dark:text-gray-300'
                                  }`}
                              >
                                {slot.startTime}
                              </button>
                          ))}
                        </div>
                    ) : (
                        <div className="text-center py-4 border border-yellow-200 bg-yellow-50 dark:bg-yellow-900/10 dark:border-yellow-800/30 rounded-lg">
                          <p className="text-yellow-600 dark:text-yellow-400 text-sm">No available time slots for this date.</p>
                          <p className="text-xs text-yellow-500 dark:text-yellow-500 mt-1">Try selecting a different date or duration.</p>
                        </div>
                    )}
                  </div>
              )}

              {/* Notes */}
              {/* ... (keep as is) ... */}
              <div>
                <label htmlFor="notes" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Notes (optional)
                  <span className="text-gray-500 text-xs ml-1">- Share what you want to learn or ask</span>
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
              {/* ... (keep as is) ... */}
              <div className="bg-gray-50 dark:bg-dark-700 rounded-lg p-4">
                <h3 className="font-semibold text-gray-900 dark:text-white mb-3">Session Summary</h3>
                <div className="space-y-2 mb-4 text-sm">
                  {selectedTimeSlot && selectedDate && ( // Add selectedDate check
                      <div className="flex justify-between">
                        <span className="text-gray-600 dark:text-gray-400">Time:</span>
                        <span className="text-gray-900 dark:text-white font-medium">
                          {format(selectedDate, 'MMM d, yyyy')} {selectedTimeSlot.startTime}
                        </span>
                      </div>
                  )}
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Duration:</span>
                    <span className="text-gray-900 dark:text-white font-medium">{sessionInfo.duration} hours</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600 dark:text-gray-400">Type:</span>
                    <span className="text-gray-900 dark:text-white font-medium">{sessionInfo.isOnline ? 'Online' : 'In-person'}</span>
                  </div>
                  <div className="border-t border-gray-200 dark:border-dark-600 pt-2 mt-2"></div>
                  <div className="flex justify-between font-semibold">
                    <span className="text-gray-700 dark:text-gray-300">Total Price:</span>
                    <span className="text-gray-900 dark:text-white">${calculateTotalPrice().toFixed(2)}</span> {/* Ensure 2 decimal places */}
                  </div>
                </div>

                <div className="flex flex-col sm:flex-row gap-3">
                  <button
                      type="submit"
                      disabled={submitting || !selectedTimeSlot || loadingTimeSlots}
                      className="btn-primary flex-1 disabled:opacity-50"
                  >
                    {submitting ? 'Booking...' : 'Proceed to Payment'}
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