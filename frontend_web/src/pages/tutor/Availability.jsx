import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { toast } from 'react-toastify';
import axios from 'axios';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { FaCalendarAlt, FaSync, FaExclamationTriangle, FaClock, FaCheck, FaTrash } from 'react-icons/fa';
import { useSelector } from 'react-redux';
import { tutorAvailabilityApi } from '../../api/api';

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

  // Generate the next 14 days for date selection
  const generateDateOptions = () => {
    const dates = [];
    const today = new Date();
    
    for (let i = 0; i < 14; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() + i);
      
      const formattedDate = date.toISOString().split('T')[0];
      const displayDate = date.toLocaleDateString('en-US', {
        weekday: 'long',
        month: 'long',
        day: 'numeric'
      });
      
      dates.push({ value: formattedDate, display: displayDate });
    }
    
    return dates;
  };

  // Generate time slots from 7 AM to 10 PM in 30-minute increments
  const generateTimeSlots = () => {
    const slots = [];
    const startHour = 7; // 7 AM
    const endHour = 22; // 10 PM
    
    for (let hour = startHour; hour < endHour; hour++) {
      for (let minute of [0, 30]) {
        const time = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
        const displayTime = new Date(`2000-01-01T${time}:00`).toLocaleTimeString([], {
          hour: '2-digit',
          minute: '2-digit'
        });
        
        slots.push({ value: time, display: displayTime });
      }
    }
    
    return slots;
  };

  useEffect(() => {
    fetchAvailabilities();
    checkCalendarConnection();
    setTimeSlots(generateTimeSlots());
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
      const response = await tutorAvailabilityApi.getAvailabilities(user.userId);
      setAvailabilities(response.data);
    } catch (error) {
      console.error('Error fetching availabilities', error);
      toast.error('Failed to load availabilities');
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
    
    if (!newAvailability.dayOfWeek || !newAvailability.startTime || !newAvailability.endTime) {
      toast.error('Please fill in all fields');
      return;
    }
    
    if (newAvailability.startTime >= newAvailability.endTime) {
      toast.error('End time must be after start time');
      return;
    }
    
    const availabilityData = {
      dayOfWeek: newAvailability.dayOfWeek,
      startTime: newAvailability.startTime,
      endTime: newAvailability.endTime,
      tutorId: user.userId
    };
    
    try {
      const response = await tutorAvailabilityApi.createAvailability(availabilityData);
      setAvailabilities([...availabilities, response.data]);
      toast.success('Availability added successfully');
      setNewAvailability({
        dayOfWeek: 'Monday',
        startTime: '09:00',
        endTime: '17:00',
        recurring: true
      });
      setShowAddForm(false);
    } catch (error) {
      console.error('Error adding availability', error);
      toast.error('Failed to add availability');
    }
  };

  const handleDelete = async (id) => {
    try {
      await tutorAvailabilityApi.deleteAvailability(id);
      setAvailabilities(availabilities.filter(avail => avail.id !== id));
      toast.success('Availability deleted successfully');
    } catch (error) {
      console.error('Error deleting availability', error);
      toast.error('Failed to delete availability');
    }
  };

  // Helper function to format time for display
  const formatTime = (time) => {
    const [hours, minutes] = time.split(':');
    const period = hours >= 12 ? 'PM' : 'AM';
    const formattedHours = hours % 12 || 12;
    return `${formattedHours}:${minutes} ${period}`;
  };

  const [timeSlots, setTimeSlots] = useState([]);
  const [selectedTimeSlots, setSelectedTimeSlots] = useState([]);

  // Handle date selection
  const handleDateSelection = (e) => {
    const date = e.target.value;
    setSelectedDate(date);
    
    // Pre-select time slots that are already available for this date
    const existingSlots = availabilities
      .filter(avail => avail.date === date)
      .map(avail => ({
        startTime: avail.startTime,
        endTime: avail.endTime,
        id: avail.availabilityId
      }));
    
    setSelectedTimeSlots(existingSlots);
  };

  // Toggle time slot selection
  const toggleTimeSlot = (startSlot) => {
    // Find the end time (30 minutes after start time)
    const startTime = startSlot.value;
    const startIndex = timeSlots.findIndex(slot => slot.value === startTime);
    
    if (startIndex === -1 || startIndex >= timeSlots.length - 1) return;
    
    const endTime = timeSlots[startIndex + 1].value;
    
    // Check if this slot is already selected
    const existingSlotIndex = selectedTimeSlots.findIndex(
      slot => slot.startTime === startTime && slot.endTime === endTime
    );
    
    if (existingSlotIndex !== -1) {
      // If already selected, remove it
      setSelectedTimeSlots(prev => prev.filter((_, index) => index !== existingSlotIndex));
    } else {
      // If not selected, add it
      setSelectedTimeSlots(prev => [...prev, { startTime, endTime }]);
    }
  };

  // Save availabilities for the selected date
  const saveAvailabilities = async () => {
    if (!selectedDate) {
      toast.error('Please select a date');
      return;
    }
    
    if (selectedTimeSlots.length === 0) {
      toast.error('Please select at least one time slot');
      return;
    }
    
    setSaving(true);
    
    try {
      // First, delete existing availabilities for this date
      const existingAvails = availabilities.filter(avail => avail.date === selectedDate);
      
      for (const avail of existingAvails) {
        await tutorAvailabilityApi.deleteAvailability(avail.availabilityId);
      }
      
      // Then, create new availabilities
      for (const slot of selectedTimeSlots) {
        if (!slot.id) { // Only create if it doesn't already exist
          await tutorAvailabilityApi.createAvailability({
            date: selectedDate,
            startTime: slot.startTime,
            endTime: slot.endTime
          });
        }
      }
      
      toast.success('Availability saved successfully!');
      await fetchAvailabilities(); // Refresh availabilities
      setSelectedDate(''); // Reset selected date
      setSelectedTimeSlots([]); // Reset selected time slots
    } catch (error) {
      console.error('Error saving availabilities:', error);
      toast.error('Failed to save availability');
    } finally {
      setSaving(false);
    }
  };

  // Group availabilities by date for display
  const groupedAvailabilities = availabilities.reduce((acc, avail) => {
    if (!acc[avail.date]) {
      acc[avail.date] = [];
    }
    acc[avail.date].push(avail);
    return acc;
  }, {});

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[80vh]">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-white">Manage Your Availability</h1>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Set Availability Panel */}
        <div className="lg:col-span-1">
          <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              <FaCalendarAlt className="inline-block mr-2" />
              Set Your Availability
            </h2>
            
            <div className="space-y-4">
              <div>
                <label htmlFor="date" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Select a Date
                </label>
                <select
                  id="date"
                  value={selectedDate}
                  onChange={handleDateSelection}
                  className="mt-1 block w-full py-2 px-3 border border-gray-300 dark:border-dark-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm dark:bg-dark-700 dark:text-white"
                >
                  <option value="">-- Select a date --</option>
                  {generateDateOptions().map((date) => (
                    <option key={date.value} value={date.value}>
                      {date.display}
                    </option>
                  ))}
                </select>
              </div>
              
              {selectedDate && (
                <>
                  <div>
                    <h3 className="text-md font-medium text-gray-700 dark:text-gray-300 mb-2">
                      <FaClock className="inline-block mr-2" />
                      Select Time Slots
                    </h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">
                      Click on a time to mark yourself as available for a 30-minute session starting at that time.
                    </p>
                    
                    <div className="grid grid-cols-3 gap-2 max-h-60 overflow-y-auto p-2 border border-gray-200 dark:border-dark-700 rounded">
                      {timeSlots.map((slot) => {
                        const isSelected = selectedTimeSlots.some(
                          selected => selected.startTime === slot.value
                        );
                        
                        return (
                          <button
                            key={slot.value}
                            type="button"
                            onClick={() => toggleTimeSlot(slot)}
                            className={`py-2 px-2 text-sm rounded-md text-center transition-colors ${
                              isSelected
                                ? 'bg-primary-100 text-primary-700 border border-primary-300 dark:bg-primary-900/30 dark:text-primary-300 dark:border-primary-700'
                                : 'bg-gray-50 text-gray-700 border border-gray-200 hover:bg-gray-100 dark:bg-dark-700 dark:text-gray-300 dark:border-dark-600 dark:hover:bg-dark-600'
                            }`}
                          >
                            {slot.display}
                            {isSelected && <FaCheck className="inline-block ml-1 text-xs" />}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                  
                  <div>
                    <button
                      type="button"
                      onClick={saveAvailabilities}
                      disabled={saving || selectedTimeSlots.length === 0}
                      className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {saving ? 'Saving...' : 'Save Availability'}
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Current Availability Calendar */}
        <div className="lg:col-span-2">
          <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              <FaCalendarAlt className="inline-block mr-2" />
              Your Current Availability
            </h2>
            
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <div className="w-10 h-10 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
              </div>
            ) : Object.keys(groupedAvailabilities).length > 0 ? (
              <div className="space-y-6">
                {Object.entries(groupedAvailabilities)
                  .sort(([dateA], [dateB]) => new Date(dateA) - new Date(dateB))
                  .map(([date, slots]) => (
                    <div key={date} className="border border-gray-200 dark:border-dark-700 rounded-lg p-4">
                      <h3 className="font-medium text-gray-800 dark:text-gray-200 mb-3">
                        {new Date(date).toLocaleDateString('en-US', {
                          weekday: 'long',
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        })}
                      </h3>
                      
                      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2 mt-2">
                        {slots
                          .sort((a, b) => {
                            const timeA = a.startTime.split(':').map(Number);
                            const timeB = b.startTime.split(':').map(Number);
                            return (timeA[0] * 60 + timeA[1]) - (timeB[0] * 60 + timeB[1]);
                          })
                          .map((slot) => (
                            <div
                              key={slot.availabilityId}
                              className="flex items-center justify-between bg-gray-50 dark:bg-dark-700 p-2 rounded border border-gray-200 dark:border-dark-600"
                            >
                              <span className="text-gray-700 dark:text-gray-300 text-sm">
                                {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
                              </span>
                              <button
                                onClick={() => handleDelete(slot.availabilityId)}
                                className="text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                              >
                                <FaTrash size={12} />
                              </button>
                            </div>
                          ))}
                      </div>
                    </div>
                  ))}
              </div>
            ) : (
              <div className="text-center py-8 border-2 border-dashed border-gray-300 dark:border-dark-600 rounded-lg">
                <FaCalendarAlt className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-600 mb-4" />
                <p className="text-gray-500 dark:text-gray-400 mb-2">You haven't set up any availability yet.</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Use the form on the left to add your available times so students can book sessions with you.
                </p>
              </div>
            )}
          </div>
        </div>
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
