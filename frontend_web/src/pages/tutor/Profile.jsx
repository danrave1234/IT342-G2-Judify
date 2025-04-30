import { useState, useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { FaMapMarkerAlt, FaSave, FaTrash, FaPlus } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';
import axios from 'axios';

const Profile = () => {
  const { user, updateProfile } = useUser();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [subjects, setSubjects] = useState([]);
  const [newSubject, setNewSubject] = useState('');
  const [newExpertiseLevel, setNewExpertiseLevel] = useState('BEGINNER');
  const [availabilities, setAvailabilities] = useState([]);
  const [newAvailability, setNewAvailability] = useState({
    dayOfWeek: 'MONDAY',
    startTime: new Date(new Date().setHours(9, 0, 0, 0)),
    endTime: new Date(new Date().setHours(17, 0, 0, 0)),
  });
  const { register, handleSubmit, setValue, control, formState: { errors } } = useForm();

  useEffect(() => {
    const fetchTutorProfile = async () => {
      try {
        const token = localStorage.getItem('judify_token');
        const config = {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        };

        const res = await axios.get(`/api/tutors/profile/${user.id}`, config);
        
        // Set form values
        setValue('bio', res.data.bio);
        setValue('title', res.data.title);
        setValue('education', res.data.education);
        setValue('experience', res.data.experience);
        setValue('hourlyRate', res.data.hourlyRate);
        setValue('isOnlineAvailable', res.data.isOnlineAvailable);
        setValue('isInPersonAvailable', res.data.isInPersonAvailable);
        setValue('location.city', res.data.location?.city);
        setValue('location.state', res.data.location?.state);
        setValue('location.country', res.data.location?.country);
        setValue('location.latitude', res.data.location?.latitude);
        setValue('location.longitude', res.data.location?.longitude);
        
        // Set subjects
        if (res.data.subjects) {
          setSubjects(res.data.subjects);
        }
        
        // Set availabilities
        if (res.data.availabilities) {
          setAvailabilities(res.data.availabilities.map(a => ({
            ...a,
            startTime: new Date(`2023-01-01T${a.startTime}`),
            endTime: new Date(`2023-01-01T${a.endTime}`),
          })));
        }
      } catch (error) {
        toast.error('Failed to load profile data');
      }
    };

    if (user) {
      fetchTutorProfile();
    }
  }, [user, setValue]);

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      const profileData = {
        ...data,
        subjects,
        availabilities: availabilities.map(a => ({
          ...a,
          startTime: a.startTime.toTimeString().split(' ')[0],
          endTime: a.endTime.toTimeString().split(' ')[0],
        })),
      };

      const token = localStorage.getItem('judify_token');
      const config = {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      };

      await axios.put(`/api/tutors/profile/${user.id}`, profileData, config);
      toast.success('Profile updated successfully');
    } catch (error) {
      toast.error('Failed to update profile');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAddSubject = () => {
    if (newSubject.trim() === '') {
      toast.error('Please enter a subject');
      return;
    }

    setSubjects([...subjects, { name: newSubject, expertiseLevel: newExpertiseLevel }]);
    setNewSubject('');
    setNewExpertiseLevel('BEGINNER');
  };

  const handleRemoveSubject = (index) => {
    const updatedSubjects = [...subjects];
    updatedSubjects.splice(index, 1);
    setSubjects(updatedSubjects);
  };

  const handleAddAvailability = () => {
    if (!newAvailability.dayOfWeek || !newAvailability.startTime || !newAvailability.endTime) {
      toast.error('Please fill in all availability fields');
      return;
    }

    if (newAvailability.startTime >= newAvailability.endTime) {
      toast.error('Start time must be before end time');
      return;
    }

    setAvailabilities([...availabilities, { ...newAvailability }]);
    setNewAvailability({
      dayOfWeek: 'MONDAY',
      startTime: new Date(new Date().setHours(9, 0, 0, 0)),
      endTime: new Date(new Date().setHours(17, 0, 0, 0)),
    });
  };

  const handleRemoveAvailability = (index) => {
    const updatedAvailabilities = [...availabilities];
    updatedAvailabilities.splice(index, 1);
    setAvailabilities(updatedAvailabilities);
  };

  const handleGetCurrentLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setValue('location.latitude', position.coords.latitude);
          setValue('location.longitude', position.coords.longitude);
          toast.success('Location updated');
        },
        (error) => {
          toast.error('Error getting location: ' + error.message);
        }
      );
    } else {
      toast.error('Geolocation is not supported by this browser');
    }
  };

  const dayOptions = [
    { value: 'MONDAY', label: 'Monday' },
    { value: 'TUESDAY', label: 'Tuesday' },
    { value: 'WEDNESDAY', label: 'Wednesday' },
    { value: 'THURSDAY', label: 'Thursday' },
    { value: 'FRIDAY', label: 'Friday' },
    { value: 'SATURDAY', label: 'Saturday' },
    { value: 'SUNDAY', label: 'Sunday' },
  ];

  const expertiseLevelOptions = [
    { value: 'BEGINNER', label: 'Beginner' },
    { value: 'INTERMEDIATE', label: 'Intermediate' },
    { value: 'ADVANCED', label: 'Advanced' },
    { value: 'EXPERT', label: 'Expert' },
  ];

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white shadow rounded-lg p-6 mb-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Profile Information</h1>
        
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Information */}
          <div>
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Basic Information</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label htmlFor="title" className="block text-sm font-medium text-gray-700">Professional Title</label>
                <input
                  id="title"
                  type="text"
                  placeholder="e.g. Mathematics Tutor"
                  className={`mt-1 block w-full px-3 py-2 border ${
                    errors.title ? 'border-red-500' : 'border-gray-300'
                  } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
                  {...register('title', { required: 'Title is required' })}
                />
                {errors.title && (
                  <p className="mt-1 text-sm text-red-600">{errors.title.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="hourlyRate" className="block text-sm font-medium text-gray-700">Hourly Rate ($)</label>
                <input
                  id="hourlyRate"
                  type="number"
                  min="1"
                  step="0.01"
                  placeholder="e.g. 25.00"
                  className={`mt-1 block w-full px-3 py-2 border ${
                    errors.hourlyRate ? 'border-red-500' : 'border-gray-300'
                  } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
                  {...register('hourlyRate', { 
                    required: 'Hourly rate is required',
                    min: { value: 1, message: 'Hourly rate must be at least 1' },
                  })}
                />
                {errors.hourlyRate && (
                  <p className="mt-1 text-sm text-red-600">{errors.hourlyRate.message}</p>
                )}
              </div>
            </div>
          </div>
          
          {/* Bio */}
          <div>
            <label htmlFor="bio" className="block text-sm font-medium text-gray-700">Bio</label>
            <textarea
              id="bio"
              rows="4"
              placeholder="Tell students about yourself, your teaching style, and experience."
              className={`mt-1 block w-full px-3 py-2 border ${
                errors.bio ? 'border-red-500' : 'border-gray-300'
              } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
              {...register('bio', { required: 'Bio is required' })}
            ></textarea>
            {errors.bio && (
              <p className="mt-1 text-sm text-red-600">{errors.bio.message}</p>
            )}
          </div>
          
          {/* Education & Experience */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label htmlFor="education" className="block text-sm font-medium text-gray-700">Education</label>
              <textarea
                id="education"
                rows="3"
                placeholder="e.g. Master's in Mathematics, Harvard University"
                className={`mt-1 block w-full px-3 py-2 border ${
                  errors.education ? 'border-red-500' : 'border-gray-300'
                } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
                {...register('education', { required: 'Education is required' })}
              ></textarea>
              {errors.education && (
                <p className="mt-1 text-sm text-red-600">{errors.education.message}</p>
              )}
            </div>
            <div>
              <label htmlFor="experience" className="block text-sm font-medium text-gray-700">Experience</label>
              <textarea
                id="experience"
                rows="3"
                placeholder="e.g. 5 years teaching mathematics at secondary level"
                className={`mt-1 block w-full px-3 py-2 border ${
                  errors.experience ? 'border-red-500' : 'border-gray-300'
                } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
                {...register('experience', { required: 'Experience is required' })}
              ></textarea>
              {errors.experience && (
                <p className="mt-1 text-sm text-red-600">{errors.experience.message}</p>
              )}
            </div>
          </div>
          
          {/* Subjects & Expertise */}
          <div>
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Subjects & Expertise</h2>
            
            <div className="mb-4">
              <div className="flex flex-wrap gap-2 mb-3">
                {subjects.map((subject, index) => (
                  <div 
                    key={index} 
                    className="flex items-center bg-blue-100 text-blue-800 px-3 py-1 rounded-full"
                  >
                    <span>{subject.name} ({subject.expertiseLevel.toLowerCase()})</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveSubject(index)}
                      className="ml-2 text-blue-600 hover:text-blue-800"
                    >
                      &times;
                    </button>
                  </div>
                ))}
              </div>
              
              <div className="flex flex-col sm:flex-row gap-3">
                <input
                  type="text"
                  placeholder="Add a subject (e.g. Algebra)"
                  value={newSubject}
                  onChange={(e) => setNewSubject(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
                <select
                  value={newExpertiseLevel}
                  onChange={(e) => setNewExpertiseLevel(e.target.value)}
                  className="block w-full sm:w-1/3 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  {expertiseLevelOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={handleAddSubject}
                  className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                >
                  <FaPlus className="mr-2" /> Add
                </button>
              </div>
              {subjects.length === 0 && (
                <p className="mt-1 text-sm text-red-600">Add at least one subject</p>
              )}
            </div>
          </div>
          
          {/* Availability */}
          <div>
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Availability</h2>
            
            <div className="mb-4">
              <div className="space-y-3 mb-4">
                {availabilities.map((availability, index) => (
                  <div 
                    key={index} 
                    className="flex flex-wrap items-center justify-between p-3 bg-gray-50 border border-gray-200 rounded-lg"
                  >
                    <div className="font-medium">
                      {dayOptions.find(d => d.value === availability.dayOfWeek)?.label}
                    </div>
                    <div>
                      {availability.startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - 
                      {availability.endTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </div>
                    <button
                      type="button"
                      onClick={() => handleRemoveAvailability(index)}
                      className="text-red-600 hover:text-red-800"
                    >
                      <FaTrash />
                    </button>
                  </div>
                ))}
              </div>
              
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-3">
                <select
                  value={newAvailability.dayOfWeek}
                  onChange={(e) => setNewAvailability({...newAvailability, dayOfWeek: e.target.value})}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  {dayOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                
                <div className="flex items-center">
                  <span className="mr-2 text-sm font-medium text-gray-700">From:</span>
                  <DatePicker
                    selected={newAvailability.startTime}
                    onChange={(time) => setNewAvailability({...newAvailability, startTime: time})}
                    showTimeSelect
                    showTimeSelectOnly
                    timeIntervals={15}
                    timeCaption="Time"
                    dateFormat="h:mm aa"
                    className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
                
                <div className="flex items-center">
                  <span className="mr-2 text-sm font-medium text-gray-700">To:</span>
                  <DatePicker
                    selected={newAvailability.endTime}
                    onChange={(time) => setNewAvailability({...newAvailability, endTime: time})}
                    showTimeSelect
                    showTimeSelectOnly
                    timeIntervals={15}
                    timeCaption="Time"
                    dateFormat="h:mm aa"
                    className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
              </div>
              
              <button
                type="button"
                onClick={handleAddAvailability}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                <FaPlus className="mr-2" /> Add Time Slot
              </button>
              
              {availabilities.length === 0 && (
                <p className="mt-1 text-sm text-red-600">Add at least one availability slot</p>
              )}
            </div>
          </div>
          
          {/* Session Type */}
          <div>
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Session Type</h2>
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="flex items-center">
                <input
                  id="isOnlineAvailable"
                  type="checkbox"
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  {...register('isOnlineAvailable')}
                />
                <label htmlFor="isOnlineAvailable" className="ml-2 block text-sm text-gray-700">
                  Available for online sessions
                </label>
              </div>
              <div className="flex items-center">
                <input
                  id="isInPersonAvailable"
                  type="checkbox"
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  {...register('isInPersonAvailable')}
                />
                <label htmlFor="isInPersonAvailable" className="ml-2 block text-sm text-gray-700">
                  Available for in-person sessions
                </label>
              </div>
            </div>
          </div>
          
          {/* Submit Button */}
          <div className="pt-4 border-t">
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center justify-center w-full sm:w-auto px-6 py-3 border border-transparent text-base font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Saving...
                </>
              ) : (
                <>
                  <FaSave className="mr-2" /> Save Profile
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Profile; 