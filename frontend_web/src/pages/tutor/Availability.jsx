import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useUser } from '../../context/UserContext';
import { toast } from 'react-toastify';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { FaCalendarAlt, FaSync, FaExclamationTriangle, FaArrowRight } from 'react-icons/fa';
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

  const daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

  useEffect(() => {
    fetchAvailabilities();
  }, []);

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

      {/* New Timeline View Notice */}
      <div className="bg-blue-100 dark:bg-blue-900/20 border border-blue-400 dark:border-blue-800 text-blue-700 dark:text-blue-400 px-4 py-3 rounded mb-6">
        <div className="flex items-center">
          <FaCalendarAlt className="text-xl mr-2" />
          <div>
            <p className="font-medium">New Visual Schedule Available!</p>
            <p className="text-sm mt-1">
              Check out the new visual timeline of your weekly availability in the Sessions page.
            </p>
            <Link 
              to="/tutor/sessions" 
              className="inline-flex items-center mt-2 text-sm font-medium text-blue-700 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300"
            >
              Go to Sessions <FaArrowRight className="ml-1" />
            </Link>
          </div>
        </div>
      </div>

      <div className="bg-white dark:bg-dark-800 rounded-xl shadow-card p-6 border border-light-700 dark:border-dark-700 mb-6">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Weekly Schedule</h2>
          <div className="flex space-x-2">
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

        {/* Availability Form */}
        {showAddForm && (
          <div className="bg-gray-50 dark:bg-dark-700 p-4 rounded-lg mb-6">
            <form onSubmit={handleSubmit}>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label htmlFor="dayOfWeek" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Day of Week
                  </label>
                  <select
                    id="dayOfWeek"
                    name="dayOfWeek"
                    value={newAvailability.dayOfWeek}
                    onChange={handleInputChange}
                    className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 dark:bg-dark-700 dark:border-dark-600 dark:text-white"
                  >
                    {daysOfWeek.map(day => (
                      <option key={day} value={day}>{day}</option>
                    ))}
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
                    value={newAvailability.startTime}
                    onChange={handleInputChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 dark:bg-dark-700 dark:border-dark-600 dark:text-white"
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
                    value={newAvailability.endTime}
                    onChange={handleInputChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500 dark:bg-dark-700 dark:border-dark-600 dark:text-white"
                  />
                  </div>
                </div>
              </div>
              <div className="flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setShowAddForm(false)}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 dark:bg-dark-700 dark:text-gray-300 dark:border-dark-600 dark:hover:bg-dark-600"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-medium text-white bg-primary-600 border border-transparent rounded-md shadow-sm hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                >
                  Add
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Current Availability */}
          <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-dark-700">
            <thead className="bg-gray-50 dark:bg-dark-800">
                <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Day
                  </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Start Time
                  </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    End Time
                  </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
            <tbody className="bg-white dark:bg-dark-800 divide-y divide-gray-200 dark:divide-dark-700">
              {availabilities.length > 0 ? (
                availabilities.map(avail => (
                  <tr key={avail.id}>
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
                        onClick={() => handleDelete(avail.id)}
                        className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" className="px-6 py-4 text-center text-sm text-gray-500 dark:text-gray-400">
                    No availability slots added yet.
                  </td>
                </tr>
              )}
              </tbody>
            </table>
        </div>
      </div>
    </div>
  );
};

export default Availability; 
