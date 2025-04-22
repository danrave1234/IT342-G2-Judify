import { useState, useEffect } from 'react';
import { useSession } from '../../context/SessionContext';
import { Link } from 'react-router-dom';
import { FaCalendarAlt, FaClock, FaPlus, FaTrash, FaGoogle } from 'react-icons/fa';
import { tutorAvailabilityApi } from '../../api/api';
import { toast } from 'react-toastify';
import { SESSION_STATUS } from '../../types';

const Sessions = () => {
  // Sessions state
  const { getTutorSessions, loading: sessionsLoading } = useSession();
  const [sessions, setSessions] = useState([]);
  const [activeTab, setActiveTab] = useState('upcoming');
  const [mainTab, setMainTab] = useState('sessions');

  // Availability state
  const [availabilities, setAvailabilities] = useState([]);
  const [availabilityForm, setAvailabilityForm] = useState({
    dayOfWeek: 'Monday',
    startTime: '09:00',
    endTime: '17:00'
  });
  const [isConnectedToCalendar, setIsConnectedToCalendar] = useState(false);
  const [calendarLoading, setCalendarLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (mainTab === 'sessions') {
      fetchSessions();
    } else {
      fetchAvailabilities();
      checkCalendarConnection();
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
  const checkCalendarConnection = async () => {
    setCalendarLoading(true);
    try {
      const response = await tutorAvailabilityApi.checkCalendarConnection();
      setIsConnectedToCalendar(response.data.isConnected);
    } catch (error) {
      console.error('Error checking calendar connection:', error);
      setIsConnectedToCalendar(false);
    } finally {
      setCalendarLoading(false);
    }
  };

  const connectToGoogleCalendar = async () => {
    try {
      const response = await tutorAvailabilityApi.getCalendarAuthUrl();
      window.location.href = response.data.authUrl;
    } catch (error) {
      console.error('Error getting auth URL:', error);
      toast.error('Failed to connect to Google Calendar. Please try again.');
    }
  };

  const fetchAvailabilities = async () => {
    setLoading(true);
    try {
      const response = await tutorAvailabilityApi.getAvailabilities();
      setAvailabilities(response.data || []);
    } catch (error) {
      console.error('Error fetching availabilities:', error);
      toast.error('Failed to load availability data');
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

    try {
      await tutorAvailabilityApi.createAvailability(availabilityForm);
      toast.success('Availability added successfully');
      
      // Reset form
      setAvailabilityForm({
        dayOfWeek: 'Monday',
        startTime: '09:00',
        endTime: '17:00'
      });
      
      // Refresh availabilities
      fetchAvailabilities();
    } catch (error) {
      console.error('Error creating availability:', error);
      toast.error('Failed to add availability. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this availability?')) {
      try {
        await tutorAvailabilityApi.deleteAvailability(id);
        toast.success('Availability deleted successfully');
        fetchAvailabilities();
      } catch (error) {
        console.error('Error deleting availability:', error);
        toast.error('Failed to delete availability. Please try again.');
      }
    }
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

      {sessionsLoading ? (
        <div className="flex justify-center items-center h-64">
          <div className="w-12 h-12 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
        </div>
      ) : (
        <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md overflow-hidden">
          <div className="p-6 border-b border-gray-200 dark:border-dark-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              {activeTab === 'upcoming' ? 'Upcoming Sessions' : 'Past Sessions'}
            </h2>
            <p className="text-gray-500 dark:text-gray-400 mt-2">
              {activeTab === 'upcoming' 
                ? 'Sessions that are scheduled or confirmed'
                : 'Your completed and cancelled sessions'}
            </p>
          </div>
          
          {(activeTab === 'upcoming' ? upcomingSessions : pastSessions).length > 0 ? (
            <div className="divide-y divide-gray-200 dark:divide-dark-700">
              {(activeTab === 'upcoming' ? upcomingSessions : pastSessions).map(session => {
                const { date, time } = formatSessionTime(session.startTime, session.endTime);
                return (
                  <div key={session.sessionId} className="p-6 hover:bg-gray-50 dark:hover:bg-dark-700">
                    <div className="flex flex-col md:flex-row md:items-center md:justify-between">
                      <div className="mb-4 md:mb-0">
                        <div className="flex items-center mb-2">
                          <img 
                            src={session.studentProfilePicture || "https://via.placeholder.com/40"} 
                            alt={`${session.studentName}'s profile`} 
                            className="w-10 h-10 rounded-full mr-3 object-cover"
                          />
                          <div>
                            <h3 className="font-medium text-gray-900 dark:text-white">{session.studentName || "Student"}</h3>
                            <p className="text-sm text-gray-500 dark:text-gray-400">{session.subject}</p>
                          </div>
                        </div>
                        
                        <div className="space-y-1 text-sm">
                          <p className="flex items-center text-gray-600 dark:text-gray-400">
                            <FaCalendarAlt className="mr-2" /> {date}
                          </p>
                          <p className="flex items-center text-gray-600 dark:text-gray-400">
                            <FaClock className="mr-2" /> {time}
                          </p>
                        </div>
                      </div>
                      
                      <div className="flex flex-col items-end">
                        <div className="mb-2">
                          <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusClass(session.status)}`}>
                            {session.status}
                          </span>
                        </div>
                        
                        <div className="text-lg font-bold text-gray-900 dark:text-white mb-2">
                          ${session.price}
                        </div>
                        
                        <div className="flex space-x-2">
                          <Link
                            to={`/tutor/sessions/${session.sessionId}`}
                            className="text-primary-600 dark:text-primary-500 hover:text-primary-700 dark:hover:text-primary-400 text-sm font-medium"
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
          ) : (
            <div className="p-6 text-center">
              <p className="text-gray-500 dark:text-gray-400">
                {activeTab === 'upcoming' 
                  ? 'No upcoming sessions found.'
                  : 'No past sessions found.'}
              </p>
            </div>
          )}
        </div>
      )}
    </>
  );

  // Render Availability Content
  const renderAvailabilityContent = () => (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {/* Google Calendar Integration */}
      <div className="lg:col-span-1">
        <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Google Calendar
          </h2>
          
          {calendarLoading ? (
            <div className="flex items-center justify-center py-4">
              <div className="w-8 h-8 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
            </div>
          ) : isConnectedToCalendar ? (
            <div>
              <div className="bg-green-100 dark:bg-green-900/20 text-green-800 dark:text-green-300 p-4 rounded-lg mb-4">
                <p className="font-medium">Connected to Google Calendar</p>
                <p className="text-sm mt-1">Your availability and sessions will sync automatically.</p>
              </div>
              <button 
                onClick={connectToGoogleCalendar}
                className="w-full flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none"
              >
                <FaGoogle className="mr-2" /> Reconnect Calendar
              </button>
            </div>
          ) : (
            <div>
              <div className="bg-yellow-100 dark:bg-yellow-900/20 text-yellow-800 dark:text-yellow-300 p-4 rounded-lg mb-4">
                <p className="font-medium">Not connected to Google Calendar</p>
                <p className="text-sm mt-1">Connect your calendar to keep your schedule in sync.</p>
              </div>
              <button 
                onClick={connectToGoogleCalendar}
                className="w-full flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none"
              >
                <FaGoogle className="mr-2" /> Connect Calendar
              </button>
            </div>
          )}
        </div>

        {/* Add Availability Form */}
        <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Add Availability
          </h2>
          
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
                className="mt-1 block w-full py-2 px-3 border border-gray-300 dark:border-dark-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm dark:bg-dark-700 dark:text-white"
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
                className="mt-1 block w-full py-2 px-3 border border-gray-300 dark:border-dark-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm dark:bg-dark-700 dark:text-white"
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
                className="mt-1 block w-full py-2 px-3 border border-gray-300 dark:border-dark-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm dark:bg-dark-700 dark:text-white"
                required
              />
            </div>
            
            <div className="pt-4">
              <button
                type="submit"
                disabled={submitting}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? 'Adding...' : 'Add Availability'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Current Availability Table */}
      <div className="lg:col-span-2">
        <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            Current Availability
          </h2>
          
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="w-10 h-10 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
            </div>
          ) : availabilities.length > 0 ? (
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
                  {availabilities.map((avail) => (
                    <tr key={avail.availabilityId} className="hover:bg-gray-50 dark:hover:bg-dark-700">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">
                        {avail.dayOfWeek}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                        {formatTime(avail.startTime)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                        {formatTime(avail.endTime)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button
                          onClick={() => handleDelete(avail.availabilityId)}
                          className="text-red-600 hover:text-red-900 dark:text-red-500 dark:hover:text-red-400"
                        >
                          <FaTrash />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="text-center py-8">
              <FaCalendarAlt className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-600 mb-4" />
              <p className="text-gray-500 dark:text-gray-400 mb-4">You haven't set up any availability yet.</p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Add your available times using the form to let students know when they can book sessions with you.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-white">Tutoring Management</h1>
      
      {/* Main Tabs */}
      {renderMainTabs()}
      
      {/* Content based on active main tab */}
      {mainTab === 'sessions' ? renderSessionsContent() : renderAvailabilityContent()}
    </div>
  );
};

export default Sessions; 