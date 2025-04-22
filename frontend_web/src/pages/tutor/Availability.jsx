import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { toast } from 'react-toastify';
import axios from 'axios';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { FaCalendarAlt, FaSync, FaExclamationTriangle } from 'react-icons/fa';

const Availability = () => {
  const { user } = useUser();
  const [availabilities, setAvailabilities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [newAvailability, setNewAvailability] = useState({
    dayOfWeek: 'Monday',
    startTime: '09:00',
    endTime: '17:00',
    recurring: true
  });

  // Google Calendar integration
  const [calendarConnected, setCalendarConnected] = useState(false);
  const [calendarEvents, setCalendarEvents] = useState([]);
  const [loadingCalendar, setLoadingCalendar] = useState(false);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [showCalendarView, setShowCalendarView] = useState(false);
  const [conflicts, setConflicts] = useState([]);

  const daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

  useEffect(() => {
    fetchAvailabilities();
    checkCalendarConnection();
  }, []);

  // Check if the user has connected their Google Calendar
  const checkCalendarConnection = async () => {
    try {
      const response = await axios.get(`/api/calendar/check-connection?userId=${user.userId}`);
      setCalendarConnected(response.data.connected);
    } catch (err) {
      console.error('Error checking calendar connection:', err);
      setCalendarConnected(false);
    }
  };

  // Connect to Google Calendar
  const connectToGoogleCalendar = async () => {
    try {
      const response = await axios.get(`/api/calendar/connect?userId=${user.userId}`);
      if (response.data.authUrl) {
        // Open the Google authorization URL in a new window
        window.open(response.data.authUrl, '_blank');
        toast.info('Please complete the Google Calendar authorization in the new window');
      }
    } catch (err) {
      console.error('Error connecting to Google Calendar:', err);
      toast.error('Failed to connect to Google Calendar');
    }
  };

  // Fetch calendar events for a specific date
  const fetchCalendarEvents = async (date) => {
    if (!calendarConnected) return;

    setLoadingCalendar(true);
    try {
      const formattedDate = date.toISOString().split('T')[0]; // Format as YYYY-MM-DD
      const response = await axios.get(`/api/calendar/events?userId=${user.userId}&date=${formattedDate}`);
      setCalendarEvents(response.data);

      // Check for conflicts with availability
      checkForConflicts(date, response.data);
    } catch (err) {
      console.error('Error fetching calendar events:', err);
      toast.error('Failed to fetch calendar events');
    } finally {
      setLoadingCalendar(false);
    }
  };

  // Check for conflicts between availability and calendar events
  const checkForConflicts = (date, events) => {
    const dayOfWeek = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'][date.getDay()];
    const dayAvailabilities = availabilities.filter(a => a.dayOfWeek === dayOfWeek);

    const newConflicts = [];

    dayAvailabilities.forEach(availability => {
      events.forEach(event => {
        const eventStart = new Date(event.start);
        const eventEnd = new Date(event.end);

        const availStart = new Date(date);
        const [startHour, startMinute] = availability.startTime.split(':').map(Number);
        availStart.setHours(startHour, startMinute, 0, 0);

        const availEnd = new Date(date);
        const [endHour, endMinute] = availability.endTime.split(':').map(Number);
        availEnd.setHours(endHour, endMinute, 0, 0);

        // Check if there's an overlap
        if ((eventStart >= availStart && eventStart < availEnd) ||
            (eventEnd > availStart && eventEnd <= availEnd) ||
            (eventStart <= availStart && eventEnd >= availEnd)) {
          newConflicts.push({
            availability,
            event
          });
        }
      });
    });

    setConflicts(newConflicts);
  };

  // Handle date selection in calendar view
  const handleDateChange = (date) => {
    setSelectedDate(date);
    fetchCalendarEvents(date);
  };

  const fetchAvailabilities = async () => {
    setLoading(true);
    try {
      // Fetch tutor availabilities from the API
      const response = await fetch(`/tutor-availability/tutor/${user.userId}`);

      if (!response.ok) {
        throw new Error('Failed to fetch availabilities');
      }

      const data = await response.json();
      setAvailabilities(data);
    } catch (err) {
      console.error('Error fetching availabilities:', err);
      setError('Failed to fetch availabilities. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setNewAvailability(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);

    // Validate time range
    if (newAvailability.startTime >= newAvailability.endTime) {
      setError('End time must be after start time');
      setSaving(false);
      return;
    }

    try {
      // Create availability data with tutor ID
      const availabilityData = {
        ...newAvailability,
        tutor: {
          userId: user.userId
        }
      };

      // Send POST request to create new availability
      const response = await fetch('/tutor-availability', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(availabilityData)
      });

      if (!response.ok) {
        throw new Error('Failed to create availability');
      }

      // Get the created availability from response
      const createdAvailability = await response.json();

      // Add the new availability to the list
      setAvailabilities(prev => [...prev, createdAvailability]);

      // Reset form
      setNewAvailability({
        dayOfWeek: 'Monday',
        startTime: '09:00',
        endTime: '17:00',
        recurring: true
      });

      setShowAddForm(false);
    } catch (error) {
      console.error('Error creating availability:', error);
      setError('Failed to add availability. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this availability?')) {
      try {
        // Send DELETE request to delete availability
        const response = await fetch(`/tutor-availability/${id}`, {
          method: 'DELETE'
        });

        if (!response.ok) {
          throw new Error('Failed to delete availability');
        }

        // Remove the deleted availability from the list
        setAvailabilities(prev => prev.filter(a => a.availabilityId !== id));
      } catch (error) {
        console.error('Error deleting availability:', error);
        setError('Failed to delete availability. Please try again.');
      }
    }
  };

  // Helper function to format time for display
  const formatTime = (time) => {
    const [hours, minutes] = time.split(':');
    const period = hours >= 12 ? 'PM' : 'AM';
    const formattedHours = hours % 12 || 12;
    return `${formattedHours}:${minutes} ${period}`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[80vh]">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Your Availability</h1>
        <p className="text-gray-600 dark:text-gray-400">
          Set your weekly availability for tutoring sessions. Students will only be able to book during these times.
        </p>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 dark:bg-red-900/20 dark:border-red-800 dark:text-red-400 px-4 py-3 rounded mb-6">
          {error}
        </div>
      )}

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700 mb-6">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Weekly Schedule</h2>
          <div className="flex space-x-2">
            <button 
              onClick={() => setShowCalendarView(!showCalendarView)}
              className="flex items-center px-4 py-2 bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition-colors"
            >
              <FaCalendarAlt className="mr-2" />
              {showCalendarView ? 'Hide Calendar' : 'Show Calendar'}
            </button>
            {!showAddForm && (
              <button 
                onClick={() => setShowAddForm(true)}
                className="btn-primary"
              >
                Add Availability
              </button>
            )}
          </div>
        </div>

        {/* Google Calendar Integration */}
        <div className="mb-6 p-4 bg-gray-50 dark:bg-dark-700 rounded-lg">
          <div className="flex justify-between items-center">
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-1">Google Calendar Integration</h3>
              <p className="text-gray-600 dark:text-gray-400 text-sm">
                Sync your availability with Google Calendar to avoid scheduling conflicts.
              </p>
            </div>
            <button 
              onClick={connectToGoogleCalendar}
              className={`flex items-center px-4 py-2 rounded-lg transition-colors ${
                calendarConnected 
                  ? 'bg-green-100 text-green-700 hover:bg-green-200 dark:bg-green-900/20 dark:text-green-400' 
                  : 'bg-blue-100 text-blue-700 hover:bg-blue-200 dark:bg-blue-900/20 dark:text-blue-400'
              }`}
            >
              <FaSync className="mr-2" />
              {calendarConnected ? 'Refresh Calendar' : 'Connect Calendar'}
            </button>
          </div>

          {conflicts.length > 0 && (
            <div className="mt-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg dark:bg-yellow-900/10 dark:border-yellow-800">
              <div className="flex items-start">
                <FaExclamationTriangle className="text-yellow-500 mt-0.5 mr-2" />
                <div>
                  <p className="font-medium text-yellow-700 dark:text-yellow-400">
                    {conflicts.length} scheduling {conflicts.length === 1 ? 'conflict' : 'conflicts'} detected
                  </p>
                  <p className="text-sm text-yellow-600 dark:text-yellow-500">
                    Some of your availability windows conflict with events in your Google Calendar.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Calendar View */}
        {showCalendarView && (
          <div className="mb-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Calendar View</h3>
              <div className="flex items-center">
                {loadingCalendar && (
                  <div className="mr-2 w-4 h-4 border-t-2 border-blue-500 border-solid rounded-full animate-spin"></div>
                )}
                <DatePicker
                  selected={selectedDate}
                  onChange={handleDateChange}
                  className="px-3 py-2 border border-gray-300 rounded-md"
                  dateFormat="MMMM d, yyyy"
                />
              </div>
            </div>

            <div className="bg-white dark:bg-dark-900 border border-gray-200 dark:border-dark-700 rounded-lg p-4">
              {calendarEvents.length > 0 ? (
                <div className="space-y-2">
                  <h4 className="font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Events on {selectedDate.toLocaleDateString()}
                  </h4>
                  {calendarEvents.map((event, index) => (
                    <div key={index} className="p-3 bg-gray-50 dark:bg-dark-800 rounded border border-gray-200 dark:border-dark-700">
                      <p className="font-medium">{event.title}</p>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        {new Date(event.start).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - 
                        {new Date(event.end).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-center text-gray-500 dark:text-gray-400 py-4">
                  No events scheduled for this day
                </p>
              )}
            </div>
          </div>
        )}

        {showAddForm && (
          <div className="bg-gray-50 dark:bg-dark-700 rounded-lg p-4 mb-6">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Add New Availability</h3>
            <form onSubmit={handleSubmit}>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label htmlFor="dayOfWeek" className="block text-gray-700 dark:text-gray-300 mb-2">Day of Week</label>
                  <select
                    id="dayOfWeek"
                    name="dayOfWeek"
                    value={newAvailability.dayOfWeek}
                    onChange={handleInputChange}
                    className="input"
                    required
                  >
                    {daysOfWeek.map(day => (
                      <option key={day} value={day}>{day}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-gray-700 dark:text-gray-300 mb-2">Recurring</label>
                  <div className="flex items-center">
                    <input
                      type="checkbox"
                      id="recurring"
                      name="recurring"
                      checked={newAvailability.recurring}
                      onChange={handleInputChange}
                      className="form-checkbox h-5 w-5 text-primary-600"
                    />
                    <label htmlFor="recurring" className="ml-2 text-gray-700 dark:text-gray-300">
                      Repeat weekly
                    </label>
                  </div>
                </div>
                <div>
                  <label htmlFor="startTime" className="block text-gray-700 dark:text-gray-300 mb-2">Start Time</label>
                  <input
                    type="time"
                    id="startTime"
                    name="startTime"
                    value={newAvailability.startTime}
                    onChange={handleInputChange}
                    className="input"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="endTime" className="block text-gray-700 dark:text-gray-300 mb-2">End Time</label>
                  <input
                    type="time"
                    id="endTime"
                    name="endTime"
                    value={newAvailability.endTime}
                    onChange={handleInputChange}
                    className="input"
                    required
                  />
                </div>
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowAddForm(false)}
                  className="bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-dark-700 dark:text-gray-300 dark:hover:bg-dark-600 px-4 py-2 rounded-lg transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="btn-primary"
                >
                  {saving ? 'Saving...' : 'Save Availability'}
                </button>
              </div>
            </form>
          </div>
        )}

        {availabilities.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
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
                  <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Recurring
                  </th>
                  <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-dark-800 divide-y divide-gray-200 dark:divide-gray-700">
                {availabilities.map((availability) => (
                  <tr key={availability.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                      {availability.dayOfWeek}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                      {formatTime(availability.startTime)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                      {formatTime(availability.endTime)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                      {availability.recurring ? 'Yes' : 'No'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => handleDelete(availability.id)}
                        className="text-red-600 dark:text-red-400 hover:text-red-900 dark:hover:text-red-300"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-8">
            <p className="text-gray-600 dark:text-gray-400 mb-4">You haven&apos;t set any availability yet. Add your first availability slot to start receiving bookings.</p>
            {!showAddForm && (
              <button 
                onClick={() => setShowAddForm(true)}
                className="btn-primary"
              >
                Add Availability
              </button>
            )}
          </div>
        )}
      </div>

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Tips for Setting Availability</h2>
        <ul className="space-y-2 text-gray-700 dark:text-gray-300">
          <li className="flex items-start">
            <span className="inline-flex items-center justify-center h-6 w-6 rounded-full bg-primary-100 text-primary-800 dark:bg-primary-900/30 dark:text-primary-300 mr-2">1</span>
            <span>Set consistent hours each week to attract regular students.</span>
          </li>
          <li className="flex items-start">
            <span className="inline-flex items-center justify-center h-6 w-6 rounded-full bg-primary-100 text-primary-800 dark:bg-primary-900/30 dark:text-primary-300 mr-2">2</span>
            <span>Include evening and weekend slots if possible, as these are popular with working students.</span>
          </li>
          <li className="flex items-start">
            <span className="inline-flex items-center justify-center h-6 w-6 rounded-full bg-primary-100 text-primary-800 dark:bg-primary-900/30 dark:text-primary-300 mr-2">3</span>
            <span>Block off times in advance when you know you&apos;ll be unavailable to prevent scheduling conflicts.</span>
          </li>
          <li className="flex items-start">
            <span className="inline-flex items-center justify-center h-6 w-6 rounded-full bg-primary-100 text-primary-800 dark:bg-primary-900/30 dark:text-primary-300 mr-2">4</span>
            <span>Consider adding buffer time between sessions (e.g., set 1:30-3:00 and 3:30-5:00 instead of back-to-back slots).</span>
          </li>
        </ul>

        <div className="mt-6">
          <Link to="/tutor/dashboard" className="text-primary-600 dark:text-primary-500 hover:underline">
            ← Back to Dashboard
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Availability; 
