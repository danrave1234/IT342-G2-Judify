import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useUser } from '../../context/UserContext';

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
      // This would be replaced with actual API call
      // Mock data for demonstration
      setTimeout(() => {
        const mockAvailabilities = [
          { id: '1', dayOfWeek: 'Monday', startTime: '09:00', endTime: '17:00', recurring: true },
          { id: '2', dayOfWeek: 'Wednesday', startTime: '13:00', endTime: '19:00', recurring: true },
          { id: '3', dayOfWeek: 'Friday', startTime: '10:00', endTime: '15:00', recurring: true },
        ];
        setAvailabilities(mockAvailabilities);
        setLoading(false);
      }, 500);
    } catch (err) {
      setError('Failed to fetch availabilities. Please try again.');
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
      // This would be replaced with actual API call
      // Mock successful response
      setTimeout(() => {
        const newId = Math.floor(Math.random() * 1000).toString();
        const addedAvailability = {
          id: newId,
          ...newAvailability
        };
        
        setAvailabilities(prev => [...prev, addedAvailability]);
        setNewAvailability({
          dayOfWeek: 'Monday',
          startTime: '09:00',
          endTime: '17:00',
          recurring: true
        });
        setShowAddForm(false);
        setSaving(false);
      }, 500);
    } catch (err) {
      setError('Failed to add availability. Please try again.');
      setSaving(false);
    }
  };
  
  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this availability?')) {
      try {
        // This would be replaced with actual API call
        // Mock successful deletion
        setTimeout(() => {
          setAvailabilities(prev => prev.filter(a => a.id !== id));
        }, 300);
      } catch (err) {
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
          {!showAddForm && (
            <button 
              onClick={() => setShowAddForm(true)}
              className="btn-primary"
            >
              Add Availability
            </button>
          )}
        </div>
        
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
            <p className="text-gray-600 dark:text-gray-400 mb-4">You haven't set any availability yet. Add your first availability slot to start receiving bookings.</p>
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
            <span>Block off times in advance when you know you'll be unavailable to prevent scheduling conflicts.</span>
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