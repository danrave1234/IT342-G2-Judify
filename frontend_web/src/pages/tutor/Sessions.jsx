import { useState, useEffect, useRef } from 'react';
import { useSession } from '../../context/SessionContext';
import { Link } from 'react-router-dom';
import { FaCalendarAlt, FaClock, FaPlus, FaTrash } from 'react-icons/fa';
import { tutorAvailabilityApi } from '../../api/api';
import { toast } from 'react-toastify';
import { SESSION_STATUS } from '../../types';
import Timetable from '../../components/Timetable';
import moment from 'moment';

// No need to import CSS here since it's imported in the Timeline component

const Sessions = () => {
  // Sessions state
  const { getTutorSessions, loading: sessionsLoading } = useSession();
  const [sessions, setSessions] = useState([]);
  const [activeTab, setActiveTab] = useState('upcoming');
  const [mainTab, setMainTab] = useState('availability');

  // Availability state
  const [availabilities, setAvailabilities] = useState([]);
  const [availabilityForm, setAvailabilityForm] = useState({
    dayOfWeek: 'Monday',
    startTime: '09:00',
    endTime: '17:00'
  });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  
  // Form state
  const [day, setDay] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  
  // Ref for scroll
  const sessionRef = useRef(null);

  useEffect(() => {
    if (mainTab === 'sessions') {
      fetchSessions();
    } else {
      fetchAvailabilities();
    }
  }, [mainTab]);

  // Sessions functions
  const fetchSessions = async () => {
    const result = await getTutorSessions();
    if (result.success) {
      setSessions(result.sessions);
    }
  };

  const upcomingSessions = sessions.filter(session => 
    session.status === SESSION_STATUS.SCHEDULED || 
    session.status === SESSION_STATUS.CONFIRMED
  );
  
  const pastSessions = sessions.filter(session => 
    session.status === SESSION_STATUS.COMPLETED || 
    session.status === SESSION_STATUS.CANCELLED
  );

  const formatSessionTime = (startTime, endTime) => {
    const start = new Date(startTime);
    const end = new Date(endTime);
    
    const date = start.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short', 
      day: 'numeric',
      year: 'numeric'
    });
    
    const startStr = start.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit'
    });
    
    const endStr = end.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit'
    });
    
    return { date, time: `${startStr} - ${endStr}` };
  };

  const getStatusClass = (status) => {
    switch(status) {
      case SESSION_STATUS.SCHEDULED: 
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-300';
      case SESSION_STATUS.CONFIRMED:
        return 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-300';
      case SESSION_STATUS.COMPLETED:
        return 'bg-purple-100 text-purple-800 dark:bg-purple-900/20 dark:text-purple-300';
      case SESSION_STATUS.CANCELLED:
        return 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-300';
      default:
        return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300';
    }
  };

  // Availability functions
  const fetchAvailabilities = async () => {
    setLoading(true);
    try {
      // Check if this is a new user by seeing when the account was created
      const userData = localStorage.getItem('user');
      if (!userData) {
        // No user data, return empty list
        setAvailabilities([]);
        return;
      }
      
      const user = JSON.parse(userData);
      const isNewAccount = user.createdAt ? 
        (new Date() - new Date(user.createdAt)) < 24 * 60 * 60 * 1000 : // Less than 24 hours old
        true; // If createdAt is missing, assume it's a new account
      
      // For new accounts, don't use mock data - show empty state
      if (isNewAccount) {
        console.log('New account detected, showing empty availability state');
        setAvailabilities([]);
        return;
      }
      
      // Use the API to get real availability data
      try {
        const response = await tutorAvailabilityApi.getAvailabilities(user.userId);
        if (response && response.data && response.data.length > 0) {
          setAvailabilities(response.data);
          return;
        }
      } catch (apiError) {
        console.error('API error fetching availabilities:', apiError);
        // Only use mock data for development and existing accounts
        if (!isNewAccount && process.env.NODE_ENV === 'development') {
          // Mock data for development
          const mockAvailabilities = [
            { availabilityId: 1, dayOfWeek: 'Monday', startTime: '09:00', endTime: '12:00' },
            { availabilityId: 2, dayOfWeek: 'Monday', startTime: '13:00', endTime: '17:00' },
            { availabilityId: 3, dayOfWeek: 'Wednesday', startTime: '10:00', endTime: '15:00' },
            { availabilityId: 4, dayOfWeek: 'Friday', startTime: '09:00', endTime: '17:00' }
          ];
          
          setAvailabilities(mockAvailabilities);
        } else {
          // For new accounts or production, show empty state
          setAvailabilities([]);
        }
      }
    } catch (error) {
      console.error('Error fetching availabilities:', error);
      toast.error('Failed to load availability data');
      setAvailabilities([]);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setAvailabilityForm(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);

    // Validate time range
    const start = availabilityForm.startTime;
    const end = availabilityForm.endTime;
    if (start >= end) {
      toast.error('End time must be after start time');
      setSubmitting(false);
      return;
    }

    // Check for conflicts in existing availabilities
    const hasConflict = checkForConflicts(availabilityForm);
    if (hasConflict) {
      toast.error('This time slot conflicts with an existing availability');
      setSubmitting(false);
      return;
    }

    try {
      // For development: Skip API call and use mock data
      // Get user ID from localStorage
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const userId = user.userId || 1; // Default to 1 for development
      
      // Add userId to availability data
      const availabilityWithUserId = {
        ...availabilityForm,
        userId
      };
      
      console.log('Submitting availability data:', availabilityWithUserId);
      
      // Skip API call for now due to CORS
      // const response = await tutorAvailabilityApi.createAvailability(availabilityWithUserId);
      // console.log('Availability creation response:', response);
      
      // Mock a successful response
      const mockId = Math.floor(Math.random() * 1000) + 5;
      const newAvailability = {
        availabilityId: mockId,
        ...availabilityForm
      };
      
      // Update state with new mock availability
      const updatedAvailabilities = [...availabilities, newAvailability];
      setAvailabilities(updatedAvailabilities);
      
      toast.success('Availability added successfully (Development Mode)');
      
      // Reset form
      setAvailabilityForm({
        dayOfWeek: 'Monday',
        startTime: '09:00',
        endTime: '17:00'
      });
    } catch (error) {
      console.error('Error creating availability:', error);
      
      // More detailed error messages
      if (error.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        console.error('Server response:', error.response.data);
        toast.error(`Failed to add availability: ${error.response.data.message || 'Server error'}`);
      } else if (error.request) {
        // The request was made but no response was received
        console.error('No response received:', error.request);
        toast.error('Failed to add availability: No response from server. Backend may be down.');
      } else {
        // Something happened in setting up the request
        toast.error(`Failed to add availability: ${error.message}`);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this availability?')) {
      try {
        // Skip API call for now
        // await tutorAvailabilityApi.deleteAvailability(id);
        
        // Remove from local state
        const updatedAvailabilities = availabilities.filter(a => a.availabilityId !== id);
        setAvailabilities(updatedAvailabilities);
        
        toast.success('Availability deleted successfully (Development Mode)');
      } catch (error) {
        console.error('Error deleting availability:', error);
        toast.error('Failed to delete availability. Please try again.');
      }
    }
  };

  // Function to check for conflicts in existing availabilities
  const checkForConflicts = (newAvailability) => {
    const { dayOfWeek, startTime, endTime } = newAvailability;
    
    // Convert time strings to minutes since midnight for easier comparison
    const newStart = timeToMinutes(startTime);
    const newEnd = timeToMinutes(endTime);
    
    // Check against all existing availabilities
    return availabilities.some(existing => {
      // Only check same day
      if (existing.dayOfWeek !== dayOfWeek) return false;
      
      const existingStart = timeToMinutes(existing.startTime);
      const existingEnd = timeToMinutes(existing.endTime);
      
      // Check for overlap
      // New start time falls within existing slot
      const startsWithinExisting = newStart >= existingStart && newStart < existingEnd;
      // New end time falls within existing slot
      const endsWithinExisting = newEnd > existingStart && newEnd <= existingEnd;
      // New slot completely contains existing slot
      const containsExisting = newStart <= existingStart && newEnd >= existingEnd;
      
      return startsWithinExisting || endsWithinExisting || containsExisting;
    });
  };
  
  // Helper function to convert time string (HH:MM) to minutes since midnight
  const timeToMinutes = (timeStr) => {
    const [hours, minutes] = timeStr.split(':').map(Number);
    return hours * 60 + minutes;
  };

  const formatTime = (time) => {
    try {
      const [hours, minutes] = time.split(':');
      return new Date(0, 0, 0, hours, minutes).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return time;
    }
  };

  // Render Main Tabs
  const renderMainTabs = () => (
    <div className="mb-6 border-b border-gray-200 dark:border-dark-700">
      <div className="flex space-x-8">
        <button
          className={`py-4 px-1 border-b-2 font-medium text-sm ${
            mainTab === 'sessions'
              ? 'border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500'
              : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
          }`}
          onClick={() => setMainTab('sessions')}
        >
          Sessions
        </button>
        <button
          className={`py-4 px-1 border-b-2 font-medium text-sm ${
            mainTab === 'availability'
              ? 'border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500'
              : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
          }`}
          onClick={() => setMainTab('availability')}
        >
          Availability
        </button>
      </div>
    </div>
  );

  // Render Sessions Content
  const renderSessionsContent = () => (
    <>
      {/* Sessions Tabs */}
      <div className="mb-6 border-b border-gray-200 dark:border-dark-700">
        <div className="flex space-x-8">
          <button
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'upcoming'
                ? 'border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
            }`}
            onClick={() => setActiveTab('upcoming')}
          >
            Upcoming Sessions
          </button>
          <button
            className={`py-4 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'past'
                ? 'border-primary-600 text-primary-600 dark:border-primary-500 dark:text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300'
            }`}
            onClick={() => setActiveTab('past')}
          >
            Past Sessions
          </button>
        </div>
      </div>

      {/* Session Lists */}
      {sessionsLoading ? (
        <div className="flex justify-center py-12">
          <div className="w-12 h-12 border-t-4 border-b-4 border-primary-600 rounded-full animate-spin"></div>
        </div>
      ) : activeTab === 'upcoming' ? (
        upcomingSessions.length === 0 ? (
          <div className="text-center py-12">
            <FaCalendarAlt className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-600" />
            <h3 className="mt-2 text-sm font-medium text-gray-900 dark:text-white">No upcoming sessions</h3>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              You don't have any upcoming tutoring sessions scheduled.
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {upcomingSessions.map(session => {
                const { date, time } = formatSessionTime(session.startTime, session.endTime);
                return (
                <div 
                  key={session.id} 
                  className="bg-white dark:bg-dark-800 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden"
                >
                  <div className="p-6">
                    <div className="flex items-center justify-between">
                      <h3 className="text-lg font-medium text-gray-900 dark:text-white">{session.subject || 'Tutoring Session'}</h3>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusClass(session.status)}`}>
                        {session.status}
                      </span>
                    </div>
                    <div className="mt-4 flex justify-between">
                      <div className="flex space-x-4">
                          <div>
                          <div className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Date</div>
                          <div className="mt-1 flex items-center text-sm text-gray-900 dark:text-gray-300">
                            <FaCalendarAlt className="mr-1.5 h-4 w-4 flex-shrink-0 text-gray-400 dark:text-gray-500" />
                            {date}
                          </div>
                        </div>
                        <div>
                          <div className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Time</div>
                          <div className="mt-1 flex items-center text-sm text-gray-900 dark:text-gray-300">
                            <FaClock className="mr-1.5 h-4 w-4 flex-shrink-0 text-gray-400 dark:text-gray-500" />
                            {time}
                          </div>
                        </div>
                      </div>
                      <div>
                        <Link 
                          to={`/tutor/sessions/${session.id}`}
                          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                        >
                          View Details
                        </Link>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )
      ) : (
        pastSessions.length === 0 ? (
          <div className="text-center py-12">
            <FaCalendarAlt className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-600" />
            <h3 className="mt-2 text-sm font-medium text-gray-900 dark:text-white">No past sessions</h3>
            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
              You don't have any completed or cancelled sessions yet.
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {pastSessions.map(session => {
              const { date, time } = formatSessionTime(session.startTime, session.endTime);
              return (
                <div 
                  key={session.id} 
                  className="bg-white dark:bg-dark-800 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700 overflow-hidden"
                >
                  <div className="p-6">
                    <div className="flex items-center justify-between">
                      <h3 className="text-lg font-medium text-gray-900 dark:text-white">{session.subject || 'Tutoring Session'}</h3>
                          <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusClass(session.status)}`}>
                            {session.status}
                          </span>
                    </div>
                    <div className="mt-4 flex justify-between">
                      <div className="flex space-x-4">
                        <div>
                          <div className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Date</div>
                          <div className="mt-1 flex items-center text-sm text-gray-900 dark:text-gray-300">
                            <FaCalendarAlt className="mr-1.5 h-4 w-4 flex-shrink-0 text-gray-400 dark:text-gray-500" />
                            {date}
                          </div>
                        </div>
                        <div>
                          <div className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Time</div>
                          <div className="mt-1 flex items-center text-sm text-gray-900 dark:text-gray-300">
                            <FaClock className="mr-1.5 h-4 w-4 flex-shrink-0 text-gray-400 dark:text-gray-500" />
                            {time}
                          </div>
                        </div>
                      </div>
                      <div>
                          <Link
                          to={`/tutor/sessions/${session.id}`}
                          className="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 dark:bg-dark-700 dark:text-gray-300 dark:border-dark-600 dark:hover:bg-dark-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                          >
                            View Details
                          </Link>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
        )
      )}
    </>
  );

  // Transform availabilities for Timetable component
  const formatAvailabilitiesForTimetable = () => {
    return availabilities.map(avail => ({
      day: avail.dayOfWeek,
      startTime: avail.startTime,
      endTime: avail.endTime
    }));
  };

  // Render Availability Content with only Timetable
  const renderAvailabilityContent = () => (
    <div className="space-y-8">
      <div className="bg-white dark:bg-dark-800 rounded-lg shadow border border-gray-200 dark:border-dark-700 overflow-hidden">
        <div className="p-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">Weekly Availability Schedule</h3>
            </div>
          
          {/* Timetable only */}
          <div className="mb-8 border border-gray-200 dark:border-dark-600 rounded-md overflow-hidden">
            {loading ? (
              <div className="flex items-center justify-center h-96">
                <div className="w-12 h-12 border-t-4 border-b-4 border-primary-600 rounded-full animate-spin"></div>
              </div>
            ) : (
              <div style={{ height: "500px", overflowY: "auto" }}>
                <Timetable 
                  availabilities={formatAvailabilitiesForTimetable()}
                />
              </div>
            )}
            </div>
        </div>
        </div>

        {/* Add Availability Form */}
      <div className="bg-white dark:bg-dark-800 rounded-lg shadow border border-gray-200 dark:border-dark-700 overflow-hidden">
        <div className="p-6">
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">Add New Availability</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="dayOfWeek" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Day of Week
              </label>
              <select
                id="dayOfWeek"
                name="dayOfWeek"
                value={availabilityForm.dayOfWeek}
                onChange={handleInputChange}
                className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 dark:bg-dark-700 dark:border-dark-600 dark:text-white"
                required
              >
                <option value="Monday">Monday</option>
                <option value="Tuesday">Tuesday</option>
                <option value="Wednesday">Wednesday</option>
                <option value="Thursday">Thursday</option>
                <option value="Friday">Friday</option>
                <option value="Saturday">Saturday</option>
                <option value="Sunday">Sunday</option>
              </select>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="startTime" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Start Time
              </label>
              <input
                type="time"
                id="startTime"
                name="startTime"
                value={availabilityForm.startTime}
                onChange={handleInputChange}
                  className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 dark:bg-dark-700 dark:border-dark-600 dark:text-white"
                required
              />
            </div>
            <div>
              <label htmlFor="endTime" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                End Time
              </label>
              <input
                type="time"
                id="endTime"
                name="endTime"
                value={availabilityForm.endTime}
                onChange={handleInputChange}
                  className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 dark:bg-dark-700 dark:border-dark-600 dark:text-white"
                required
              />
              </div>
            </div>
            
            <div className="flex justify-end">
              <button
                type="submit"
                disabled={submitting}
                className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? (
                  <>
                    <div className="w-4 h-4 mr-2 border-t-2 border-b-2 border-white rounded-full animate-spin"></div>
                    Saving...
                  </>
                ) : (
                  <>
                    <FaPlus className="mr-2 -ml-1 h-4 w-4" />
                    Add Availability
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Current Availability List */}
      <div className="bg-white dark:bg-dark-800 rounded-lg shadow border border-gray-200 dark:border-dark-700 overflow-hidden">
        <div className="p-6">
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">Current Availability</h3>
          
          {loading ? (
            <div className="flex justify-center py-8">
              <div className="w-12 h-12 border-t-4 border-b-4 border-primary-600 rounded-full animate-spin"></div>
            </div>
          ) : availabilities.length === 0 ? (
            <div className="text-center py-8">
              <FaCalendarAlt className="h-8 w-8 text-gray-400 dark:text-gray-600 mx-auto mb-2" />
              <p className="text-gray-500 dark:text-gray-400">No availability slots added yet</p>
              <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">Use the form above to add your availability</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 dark:divide-dark-700">
                <thead className="bg-gray-50 dark:bg-dark-700">
                  <tr>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                      Day
                    </th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                      Start Time
                    </th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                      End Time
                    </th>
                    <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-dark-800 divide-y divide-gray-200 dark:divide-dark-700">
                  {availabilities.map((availability) => (
                    <tr key={availability.availabilityId}>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">
                        {availability.dayOfWeek}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                        {formatTime(availability.startTime)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                        {formatTime(availability.endTime)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button
                          onClick={() => handleDelete(availability.availabilityId)}
                          className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300"
                        >
                          <FaTrash className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-8">Tutor Dashboard</h1>
      
      {/* Main Content */}
      <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md border border-gray-200 dark:border-dark-700 overflow-hidden">
      {renderMainTabs()}
        <div className="p-6">
      {mainTab === 'sessions' ? renderSessionsContent() : renderAvailabilityContent()}
        </div>
      </div>
    </div>
  );
};

export default Sessions; 