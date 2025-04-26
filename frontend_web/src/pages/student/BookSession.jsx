import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useSession } from '../../context/SessionContext';
import { SESSION_STATUS } from '../../types';

const BookSession = () => {
  const { tutorId } = useParams();
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const { createSession, loading } = useSession();
  
  const [tutor, setTutor] = useState(null);
  const [tutorAvailability, setTutorAvailability] = useState([]);
  const [loadingTutor, setLoadingTutor] = useState(true);
  const [selectedDate, setSelectedDate] = useState('');
  const [availableTimeSlots, setAvailableTimeSlots] = useState([]);
  const [selectedTimeSlot, setSelectedTimeSlot] = useState(null);
  const [sessionInfo, setSessionInfo] = useState({
    subject: '',
    notes: '',
    duration: 1,
    isOnline: true
  });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Fetch tutor data and availability
  useEffect(() => {
    const fetchTutorData = async () => {
      try {
        // This would be replaced with actual API calls
        // Simulate API response
        const tutorData = {
          id: tutorId,
          name: 'John Doe',
          profilePicture: 'https://via.placeholder.com/150',
          rate: 35,
          subjects: ['Mathematics', 'Physics', 'Computer Science']
        };
        
        const availabilityData = [
          {
            id: '1',
            date: '2023-12-01',
            startTime: '09:00',
            endTime: '17:00'
          },
          {
            id: '2',
            date: '2023-12-02',
            startTime: '10:00',
            endTime: '15:00'
          },
          {
            id: '3',
            date: '2023-12-03',
            startTime: '13:00',
            endTime: '18:00'
          }
        ];
        
        setTutor(tutorData);
        setTutorAvailability(availabilityData);
        
        // Set default subject from tutor subjects if available
        if (tutorData.subjects && tutorData.subjects.length > 0) {
          setSessionInfo(prev => ({ ...prev, subject: tutorData.subjects[0] }));
        }
      } catch (error) {
        console.error('Error fetching tutor data:', error);
        setError('Failed to load tutor information. Please try again later.');
      } finally {
        setLoadingTutor(false);
      }
    };
    
    fetchTutorData();
  }, [tutorId]);

  // Generate available time slots when a date is selected
  useEffect(() => {
    if (!selectedDate) {
      setAvailableTimeSlots([]);
      return;
    }
    
    // Find the availability for the selected date
    const availability = tutorAvailability.find(a => a.date === selectedDate);
    
    if (!availability) {
      setAvailableTimeSlots([]);
      return;
    }
    
    // Generate time slots in 30-minute increments
    const slots = [];
    const startTime = new Date(`${selectedDate}T${availability.startTime}`);
    const endTime = new Date(`${selectedDate}T${availability.endTime}`);
    
    // Adjust end time based on session duration
    const actualEndTime = new Date(endTime);
    actualEndTime.setHours(actualEndTime.getHours() - sessionInfo.duration);
    
    let currentTime = new Date(startTime);
    while (currentTime <= actualEndTime) {
      const slotStartTime = new Date(currentTime);
      const slotEndTime = new Date(currentTime);
      slotEndTime.setHours(slotEndTime.getHours() + sessionInfo.duration);
      
      slots.push({
        startTime: slotStartTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        endTime: slotEndTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        value: slotStartTime.toISOString()
      });
      
      // Move to next 30-minute slot
      currentTime.setMinutes(currentTime.getMinutes() + 30);
    }
    
    setAvailableTimeSlots(slots);
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
        studentId: currentUser.userId,
        subject: sessionInfo.subject,
        notes: sessionInfo.notes,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        price: calculateTotalPrice(),
        status: SESSION_STATUS.SCHEDULED,
        locationData: sessionInfo.isOnline ? null : 'In person location',
        meetingLink: sessionInfo.isOnline ? null : null
      };
      
      const result = await createSession(sessionData);
      
      if (result.success) {
        navigate(`/student/sessions/${result.session.sessionId}`);
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
        <p className="text-gray-600 dark:text-gray-400 mb-6">The tutor you're looking for doesn't exist or is no longer available.</p>
        <Link to="/find-tutors" className="btn-primary">Find Another Tutor</Link>
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
                {tutorAvailability.map((availability) => (
                  <option key={availability.id} value={availability.date}>
                    {new Date(availability.date).toLocaleDateString('en-US', { 
                      weekday: 'long', 
                      year: 'numeric', 
                      month: 'long', 
                      day: 'numeric' 
                    })}
                  </option>
                ))}
              </select>
            </div>
            
            {/* Time Slot Selection */}
            {selectedDate && (
              <div>
                <label className="block text-gray-700 dark:text-gray-300 mb-2">Available Time Slots</label>
                {availableTimeSlots.length > 0 ? (
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
              
              <button
                type="submit"
                disabled={submitting || !selectedTimeSlot}
                className="btn-primary w-full"
              >
                {submitting ? 'Booking...' : 'Book Session'}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BookSession; 