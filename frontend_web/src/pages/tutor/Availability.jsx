import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { toast } from 'react-toastify';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { FaCalendarAlt, FaSync, FaExclamationTriangle, FaArrowRight, FaClock, FaTrash, FaCheck } from 'react-icons/fa';
import { tutorAvailabilityApi, calendarApi, tutorProfileApi } from '../../api/api';

const Availability = () => {
  const { user } = useUser();
  const [availabilities, setAvailabilities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [selectedDate, setSelectedDate] = useState('');
  const [tutorProfile, setTutorProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [newAvailability, setNewAvailability] = useState({
    dayOfWeek: 'MONDAY',
    startTime: '09:00',
    endTime: '17:00',
    recurring: true
  });
  const [calendarConnected, setCalendarConnected] = useState(false);
  const [checkingCalendar, setCheckingCalendar] = useState(true);

  // Days of week in uppercase to match mobile app and backend expectations
  const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

  // Format day for display (e.g., "MONDAY" -> "Monday")
  const formatDayForDisplay = (day) => {
    if (!day) return '';
    return day.charAt(0) + day.slice(1).toLowerCase();
  };

  // Generate time slots from 7 AM to 9 PM in 30-minute intervals
  const generateTimeSlots = () => {
    const slots = [];
    const startHour = 7; // 7 AM
    const endHour = 21; // 9 PM

    for (let hour = startHour; hour <= endHour; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const hourString = hour.toString().padStart(2, '0');
        const minuteString = minute.toString().padStart(2, '0');
        const timeValue = `${hourString}:${minuteString}`;

        // Format for display (12-hour clock)
        const displayHour = hour % 12 || 12;
        const period = hour >= 12 ? 'PM' : 'AM';
        const displayValue = `${displayHour}:${minuteString} ${period}`;

        slots.push({
          value: timeValue,
          display: displayValue
        });
      }
    }

    return slots;
  };

  // Generate date options for the next 14 days
  const generateDateOptions = () => {
    const options = [];
    const today = new Date();

    for (let i = 0; i < 14; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() + i);

      const dateValue = date.toISOString().split('T')[0]; // YYYY-MM-DD
      const displayDate = date.toLocaleDateString('en-US', {
        weekday: 'long',
        month: 'short',
        day: 'numeric'
      });

      options.push({
        value: dateValue,
        display: displayDate
      });
    }

    return options;
  };

  // Initialize timeSlots directly with the generated slots
  const initialTimeSlots = generateTimeSlots();
  console.log("Initial time slots generated:", initialTimeSlots.length);

  const [timeSlots, setTimeSlots] = useState(initialTimeSlots);
  const [selectedTimeSlots, setSelectedTimeSlots] = useState([]);

  // Use useEffect to ensure timeSlots are properly populated
  useEffect(() => {
    // Generate time slots when component mounts
    if (timeSlots.length === 0) {
      setTimeSlots(generateTimeSlots());
    }
  }, [timeSlots]);

  // Fetch tutor profile and availabilities on component mount
  useEffect(() => {
    const fetchTutorProfile = async () => {
      try {
        if (!user || !user.userId) {
          console.error('No user found in context');
          setError('User not found. Please log in again.');
          setLoading(false);
          setLoadingProfile(false); // Set loadingProfile to false
          setCheckingCalendar(false); // Set checkingCalendar to false
          return;
        }

        // Get the tutor's profile to get the profileId needed for availability API calls
        const response = await tutorProfileApi.getProfileByUserId(user.userId);

        if (!response.data || !response.data.profileId) {
          console.error('No tutor profile found or missing profileId');
          setError('Please complete your tutor profile first.');
          setLoading(false);
          setLoadingProfile(false); // Set loadingProfile to false
          setCheckingCalendar(false); // Set checkingCalendar to false
          return;
        }

        setTutorProfile(response.data);
        console.log('Fetched tutor profile:', response.data);

        // Now fetch the availabilities using the profile ID
        fetchAvailabilities(response.data.profileId);

        // Check calendar connection
        checkCalendarConnection();

        // Set loadingProfile to false after successful fetch
        setLoadingProfile(false);

      } catch (error) {
        console.error('Error fetching tutor profile:', error);
        setError('Failed to load tutor profile. Please try again.');
        setLoading(false);
        setLoadingProfile(false); // Set loadingProfile to false
        setCheckingCalendar(false); // Set checkingCalendar to false
      }
    };

    fetchTutorProfile();
  }, [user]);

  // Helper function to check calendar connection
  const checkCalendarConnection = async () => {
    try {
      setCheckingCalendar(true);

      // Check if user has a userId before making the API call
      if (!user || !user.userId) {
        console.error('Cannot check calendar connection: No userId provided');
        setCalendarConnected(false);
        setCheckingCalendar(false);
        return;
      }

      // Use the already imported calendarApi instead of dynamically importing it again
      await calendarApi.checkConnection(user.userId)
        .then(response => {
          console.log('Calendar connection status:', response.data);
          setCalendarConnected(response.data.connected); // Extract the "connected" property from the response
        })
        .catch(error => {
          console.error('Error checking calendar connection:', error);
          setCalendarConnected(false);
        });
    } catch (error) {
      console.error('Error with calendar API:', error);
      setCalendarConnected(false);
    } finally {
      setCheckingCalendar(false);
      console.log('Done checking calendar, setting checkingCalendar to false');
    }
  };

  const fetchAvailabilities = async (tutorProfileId) => {
    if (!tutorProfileId) {
      console.error('Cannot fetch availabilities: No tutorProfileId provided');
      setLoading(false); // Ensure loading is set to false if no tutorProfileId
      return;
    }

    setLoading(true);
    try {
      console.log(`Fetching availabilities for tutorId: ${tutorProfileId}`);
      const response = await tutorAvailabilityApi.getAvailabilities(tutorProfileId);
      // Make sure dayOfWeek is always uppercase when loaded from the backend
      const normalizedAvailabilities = response.data.map(avail => ({
        ...avail,
        dayOfWeek: avail.dayOfWeek ? avail.dayOfWeek.toUpperCase() : avail.dayOfWeek
      }));
      setAvailabilities(normalizedAvailabilities);
    } catch (error) {
      console.error('Error fetching availabilities', error);
      toast.error('Failed to load availabilities');
      setLoading(false); // Ensure loading is set to false on error
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

    if (!tutorProfile || !tutorProfile.profileId) {
      toast.error('Tutor profile not found');
      return;
    }

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
      tutorId: tutorProfile.profileId  // Use the correct tutorId (profileId) from tutor profile
    };

    try {
      const response = await tutorAvailabilityApi.createAvailability(availabilityData);
      setAvailabilities([...availabilities, response.data]);
      toast.success('Availability added successfully');
      setNewAvailability({
        dayOfWeek: 'MONDAY',
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
    if (!time) return '';

    try {
      const [hours, minutes] = time.split(':');
      const hoursNum = parseInt(hours, 10);
      const period = hoursNum >= 12 ? 'PM' : 'AM';
      const formattedHours = hoursNum % 12 || 12; // Convert 0 to 12 for 12 AM
      return `${formattedHours}:${minutes} ${period}`;
    } catch (error) {
      console.error('Error formatting time:', error);
      return time; // Return original time if there's an error
    }
  };

  // Format group availabilities by day for display
  const groupedAvailabilities = daysOfWeek.map(day => {
    const dayAvailabilities = availabilities.filter(
      avail => avail.dayOfWeek && avail.dayOfWeek.toUpperCase() === day
    );
    return {
      day,
      slots: dayAvailabilities
    };
  }).filter(group => group.slots.length > 0);

  // Handle date selection
  const handleDateSelection = (date) => {
    setSelectedDate(date);

    // Pre-select time slots that are already available for this date
    const existingSlots = availabilities
      .filter(avail => avail.date === date.toLocaleDateString())
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

    if (!tutorProfile || !tutorProfile.profileId) {
      toast.error('Tutor profile not found');
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
            endTime: slot.endTime,
            tutorId: tutorProfile.profileId // Use the correct tutorId
          });
        }
      }

      toast.success('Availability saved successfully!');
      await fetchAvailabilities(tutorProfile.profileId); // Refresh availabilities
      setSelectedDate(''); // Reset selected date
      setSelectedTimeSlots([]); // Reset selected time slots
    } catch (error) {
      console.error('Error saving availabilities:', error);
      toast.error('Failed to save availability');
    } finally {
      setSaving(false);
    }
  };

  // DatePicker Component for selecting dates
  const DatePickerInput = ({ selected, onChange }) => (
    <DatePicker
      selected={selected}
      onChange={date => onChange(date)}
      className="form-control"
      dateFormat="MM/dd/yyyy"
    />
  );

  // Update the loading condition to properly handle state
  if (loading || loadingProfile || checkingCalendar) {
    return (
      <div className="flex items-center justify-center min-h-[80vh]">
        <div className="w-16 h-16 border-t-4 border-primary-600 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!tutorProfile) {
    return (
      <div className="container mx-auto py-8 px-4">
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-6">
          <div className="flex items-start">
            <FaExclamationTriangle className="text-red-500 mt-1 mr-3" />
            <div>
              <h3 className="text-lg font-semibold text-red-800 dark:text-red-300">Profile Required</h3>
              <p className="text-red-700 dark:text-red-400">
                You need to complete your tutor profile before setting availability.
              </p>
              <Link 
                to="/tutor/profile" 
                className="mt-2 inline-flex items-center text-sm font-medium text-red-700 dark:text-red-400 hover:text-red-500 dark:hover:text-red-300"
              >
                Complete Profile <FaArrowRight className="ml-1" />
              </Link>
            </div>
          </div>
        </div>
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

            {showAddForm ? (
              <div className="mt-6 bg-white dark:bg-dark-800 rounded-lg shadow-sm border border-gray-200 dark:border-dark-700 p-6">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                  Add New Availability
                </h3>
                <form onSubmit={handleSubmit}>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Day of Week
                      </label>
                      <select
                        name="dayOfWeek"
                        value={newAvailability.dayOfWeek}
                        onChange={handleInputChange}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-dark-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 dark:bg-dark-700 dark:text-white"
                        required
                      >
                        {daysOfWeek.map(day => (
                          <option key={day} value={day}>
                            {formatDayForDisplay(day)}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Start Time
                      </label>
                      <select
                        name="startTime"
                        value={newAvailability.startTime}
                        onChange={handleInputChange}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-dark-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 dark:bg-dark-700 dark:text-white"
                        required
                      >
                        {timeSlots.map(slot => (
                          <option key={slot.value} value={slot.value}>
                            {slot.display}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        End Time
                      </label>
                      <select
                        name="endTime"
                        value={newAvailability.endTime}
                        onChange={handleInputChange}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-dark-600 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 dark:bg-dark-700 dark:text-white"
                        required
                      >
                        {timeSlots.map(slot => (
                          <option key={slot.value} value={slot.value}>
                            {slot.display}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="flex items-center justify-end gap-3 mt-6">
                    <button
                      type="button"
                      onClick={() => setShowAddForm(false)}
                      className="px-4 py-2 border border-gray-300 dark:border-dark-600 text-sm font-medium rounded-md text-gray-700 dark:text-gray-300 bg-white dark:bg-dark-700 hover:bg-gray-50 dark:hover:bg-dark-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                    >
                      Save Availability
                    </button>
                  </div>
                </form>
              </div>
            ) : (
              <button
                onClick={() => setShowAddForm(true)}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              >
                Add New Availability
              </button>
            )}
          </div>
        </div>

        {/* Current Availability */}
        <div className="lg:col-span-2">
          <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              <FaCalendarAlt className="inline-block mr-2" />
              Your Current Availability
            </h2>

            {availabilities.length === 0 ? (
              <div className="text-center py-8 border-2 border-dashed border-gray-300 dark:border-dark-600 rounded-lg">
                <FaCalendarAlt className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-600 mb-4" />
                <p className="text-gray-500 dark:text-gray-400 mb-2">You haven't set up any availability yet.</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Use the form on the left to add your available times so students can book sessions with you.
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Group by day of week */}
                {groupedAvailabilities.map((group) => (
                  <div key={group.day} className="border border-gray-200 dark:border-dark-700 rounded-lg p-4">
                    <h3 className="font-medium text-gray-800 dark:text-gray-200 mb-3">
                      {formatDayForDisplay(group.day)}
                    </h3>

                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
                      {group.slots
                        .sort((a, b) => a.startTime.localeCompare(b.startTime))
                        .map((avail) => (
                          <div 
                            key={avail.id || avail.availabilityId} 
                            className="flex items-center justify-between bg-gray-50 dark:bg-dark-700 p-2 rounded border border-gray-200 dark:border-dark-600"
                          >
                            <span className="text-gray-700 dark:text-gray-300 text-sm">
                              {formatTime(avail.startTime)} - {formatTime(avail.endTime)}
                            </span>
                            <button
                              onClick={() => handleDelete(avail.id || avail.availabilityId)}
                              className="text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                              aria-label="Delete availability"
                            >
                              <FaTrash size={12} />
                            </button>
                          </div>
                        ))}
                    </div>
                  </div>
                ))}
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
