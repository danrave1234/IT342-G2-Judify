import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaMapMarkerAlt, FaSave } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';
import axios from 'axios';

const Profile = () => {
  const { user } = useUser();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [interests, setInterests] = useState([]);
  const [newInterest, setNewInterest] = useState('');
  const { register, handleSubmit, setValue, formState: { errors } } = useForm();

  useEffect(() => {
    const fetchStudentProfile = async () => {
      try {
        const token = localStorage.getItem('judify_token');
        const config = {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        };

        const res = await axios.get(`/api/students/profile/${user.id}`, config);
        
        // Set form values
        setValue('bio', res.data.bio);
        setValue('gradeLevel', res.data.gradeLevel);
        setValue('school', res.data.school);
        setValue('location.city', res.data.location?.city);
        setValue('location.state', res.data.location?.state);
        setValue('location.country', res.data.location?.country);
        setValue('location.latitude', res.data.location?.latitude);
        setValue('location.longitude', res.data.location?.longitude);
        
        // Set interests
        if (res.data.interests) {
          setInterests(res.data.interests);
        }
      } catch (error) {
        toast.error('Failed to load profile data');
      }
    };

    if (user) {
      fetchStudentProfile();
    }
  }, [user, setValue]);

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      const profileData = {
        ...data,
        interests,
      };

      const token = localStorage.getItem('judify_token');
      const config = {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      };

      await axios.put(`/api/students/profile/${user.id}`, profileData, config);
      toast.success('Profile updated successfully');
    } catch (error) {
      toast.error('Failed to update profile');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAddInterest = () => {
    if (newInterest.trim() === '') {
      toast.error('Please enter an interest');
      return;
    }

    setInterests([...interests, newInterest]);
    setNewInterest('');
  };

  const handleRemoveInterest = (index) => {
    const updatedInterests = [...interests];
    updatedInterests.splice(index, 1);
    setInterests(updatedInterests);
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

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white shadow rounded-lg p-6 mb-6">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Student Profile</h1>
        
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Information */}
          <div>
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Basic Information</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label htmlFor="gradeLevel" className="block text-sm font-medium text-gray-700">Grade Level</label>
                <input
                  id="gradeLevel"
                  type="text"
                  placeholder="e.g. 10th Grade, Freshman, etc."
                  className={`mt-1 block w-full px-3 py-2 border ${
                    errors.gradeLevel ? 'border-red-500' : 'border-gray-300'
                  } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
                  {...register('gradeLevel', { required: 'Grade level is required' })}
                />
                {errors.gradeLevel && (
                  <p className="mt-1 text-sm text-red-600">{errors.gradeLevel.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="school" className="block text-sm font-medium text-gray-700">School</label>
                <input
                  id="school"
                  type="text"
                  placeholder="e.g. Washington High School"
                  className={`mt-1 block w-full px-3 py-2 border ${
                    errors.school ? 'border-red-500' : 'border-gray-300'
                  } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
                  {...register('school', { required: 'School is required' })}
                />
                {errors.school && (
                  <p className="mt-1 text-sm text-red-600">{errors.school.message}</p>
                )}
              </div>
            </div>
          </div>
          
          {/* Bio */}
          <div>
            <label htmlFor="bio" className="block text-sm font-medium text-gray-700">About Me</label>
            <textarea
              id="bio"
              rows="4"
              placeholder="Tell tutors about yourself, your learning style, and what you're looking for in a tutor."
              className={`mt-1 block w-full px-3 py-2 border ${
                errors.bio ? 'border-red-500' : 'border-gray-300'
              } rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500`}
              {...register('bio')}
            ></textarea>
          </div>
          
          {/* Interests */}
          <div>
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Academic Interests</h2>
            
            <div className="mb-4">
              <div className="flex flex-wrap gap-2 mb-3">
                {interests.map((interest, index) => (
                  <div 
                    key={index} 
                    className="flex items-center bg-blue-100 text-blue-800 px-3 py-1 rounded-full"
                  >
                    <span>{interest}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveInterest(index)}
                      className="ml-2 text-blue-600 hover:text-blue-800"
                    >
                      &times;
                    </button>
                  </div>
                ))}
              </div>
              
              <div className="flex gap-3">
                <input
                  type="text"
                  placeholder="Add an interest (e.g. Mathematics, History)"
                  value={newInterest}
                  onChange={(e) => setNewInterest(e.target.value)}
                  className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
                <button
                  type="button"
                  onClick={handleAddInterest}
                  className="px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 whitespace-nowrap"
                >
                  Add
                </button>
              </div>
            </div>
          </div>
          
          {/* Location */}
          <div>
            <h2 className="text-lg font-semibold text-gray-700 mb-4">Location</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div>
                <label htmlFor="location.city" className="block text-sm font-medium text-gray-700">City</label>
                <input
                  id="location.city"
                  type="text"
                  placeholder="e.g. New York"
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  {...register('location.city', { required: 'City is required' })}
                />
                {errors.location?.city && (
                  <p className="mt-1 text-sm text-red-600">{errors.location.city.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="location.state" className="block text-sm font-medium text-gray-700">State/Province</label>
                <input
                  id="location.state"
                  type="text"
                  placeholder="e.g. NY"
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  {...register('location.state', { required: 'State is required' })}
                />
                {errors.location?.state && (
                  <p className="mt-1 text-sm text-red-600">{errors.location.state.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="location.country" className="block text-sm font-medium text-gray-700">Country</label>
                <input
                  id="location.country"
                  type="text"
                  placeholder="e.g. USA"
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  {...register('location.country', { required: 'Country is required' })}
                />
                {errors.location?.country && (
                  <p className="mt-1 text-sm text-red-600">{errors.location.country.message}</p>
                )}
              </div>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-2">
              <div>
                <label htmlFor="location.latitude" className="block text-sm font-medium text-gray-700">Latitude</label>
                <input
                  id="location.latitude"
                  type="number"
                  step="any"
                  placeholder="e.g. 40.7128"
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  {...register('location.latitude', { required: 'Latitude is required' })}
                />
                {errors.location?.latitude && (
                  <p className="mt-1 text-sm text-red-600">{errors.location.latitude.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="location.longitude" className="block text-sm font-medium text-gray-700">Longitude</label>
                <input
                  id="location.longitude"
                  type="number"
                  step="any"
                  placeholder="e.g. -74.0060"
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  {...register('location.longitude', { required: 'Longitude is required' })}
                />
                {errors.location?.longitude && (
                  <p className="mt-1 text-sm text-red-600">{errors.location.longitude.message}</p>
                )}
              </div>
            </div>
            
            <button
              type="button"
              onClick={handleGetCurrentLocation}
              className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md shadow-sm text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              <FaMapMarkerAlt className="mr-2 text-blue-600" /> Get Current Location
            </button>
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