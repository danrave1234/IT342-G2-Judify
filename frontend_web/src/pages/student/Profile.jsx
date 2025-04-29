import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { FaMapMarkerAlt, FaSave, FaSync } from 'react-icons/fa';
import { useUser } from '../../context/UserContext';
import { useStudentProfile } from '../../context/StudentProfileContext';

const Profile = () => {
  const { user } = useUser();
  const { profile, loading, error, updateProfile, refreshProfile } = useStudentProfile();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [interests, setInterests] = useState([]);
  const [newInterest, setNewInterest] = useState('');
  const { register, handleSubmit, setValue, reset } = useForm();

  // Set form values when profile data loads
  useEffect(() => {
    if (profile) {
      // Reset form with profile data
      reset({
        bio: profile.bio || '',
        gradeLevel: profile.gradeLevel || '',
        school: profile.school || '',
        location: {
          city: profile.location?.city || '',
          state: profile.location?.state || '',
          country: profile.location?.country || '',
          latitude: profile.location?.latitude || '',
          longitude: profile.location?.longitude || ''
        }
      });
      
      // Set interests
      if (profile.interests && Array.isArray(profile.interests)) {
        setInterests(profile.interests);
      }
    }
  }, [profile, reset]);

  const onSubmit = async (data) => {
    if (!user || !user.userId) {
      toast.error('Unable to update profile - user information missing');
      return;
    }
    
    setIsSubmitting(true);
    
    try {
      const profileData = {
        ...data,
        interests,
        userId: user.userId
      };

      console.log('Submitting profile data:', profileData);
      
      const result = await updateProfile(profileData);
      
      if (result.success) {
        toast.success('Profile updated successfully');
      } else {
        toast.error(result.message || 'Failed to update profile');
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      toast.error('Failed to update profile');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRefresh = async () => {
    try {
      await refreshProfile();
      toast.success('Profile refreshed');
    } catch (error) {
      console.error('Error refreshing profile:', error);
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

  if (!user) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-white shadow rounded-lg p-6 mb-6">
          <h1 className="text-2xl font-bold text-gray-800 mb-6">Student Profile</h1>
          <p className="text-gray-700">Please log in to view and update your profile.</p>
        </div>
      </div>
    );
  }
  
  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-white shadow rounded-lg p-6 mb-6">
          <h1 className="text-2xl font-bold text-gray-800 mb-6">Student Profile</h1>
          <div className="flex justify-center items-center h-40">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            <p className="ml-2">Loading profile...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white shadow rounded-lg p-6 mb-6">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">Student Profile</h1>
          <button 
            type="button" 
            onClick={handleRefresh}
            className="flex items-center px-3 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200"
          >
            <FaSync className="mr-2" /> Refresh
          </button>
        </div>
        
        {error && (
          <div className="mb-6 p-4 bg-red-50 text-red-700 rounded-lg">
            <p className="font-medium">Error loading profile</p>
            <p className="text-sm">{error}</p>
            <p className="text-sm mt-2">
              User ID: {user?.userId || 'Missing'}
            </p>
          </div>
        )}
        
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Bio section */}
          <div>
            <label htmlFor="bio" className="block text-sm font-medium text-gray-700 mb-1">Bio</label>
            <textarea
              id="bio"
              rows="4"
              {...register('bio')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Tell us about yourself..."
            ></textarea>
          </div>

          {/* Education section */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label htmlFor="gradeLevel" className="block text-sm font-medium text-gray-700 mb-1">Grade Level</label>
              <input
                type="text"
                id="gradeLevel"
                {...register('gradeLevel')}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                placeholder="e.g., Freshman, Sophomore, etc."
              />
            </div>
            <div>
              <label htmlFor="school" className="block text-sm font-medium text-gray-700 mb-1">School</label>
              <input
                type="text"
                id="school"
                {...register('school')}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                placeholder="Your school or university name"
              />
            </div>
          </div>

          {/* Location section */}
          <div>
            <div className="flex justify-between items-center mb-2">
              <h3 className="text-lg font-medium text-gray-800">Location</h3>
              <button
                type="button"
                onClick={handleGetCurrentLocation}
                className="flex items-center text-sm text-blue-600 hover:text-blue-800"
              >
                <FaMapMarkerAlt className="mr-1" /> Use current location
              </button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div>
                <label htmlFor="location.city" className="block text-sm font-medium text-gray-700 mb-1">City</label>
                <input
                  type="text"
                  id="location.city"
                  {...register('location.city')}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <div>
                <label htmlFor="location.state" className="block text-sm font-medium text-gray-700 mb-1">State/Province</label>
                <input
                  type="text"
                  id="location.state"
                  {...register('location.state')}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <div>
                <label htmlFor="location.country" className="block text-sm font-medium text-gray-700 mb-1">Country</label>
                <input
                  type="text"
                  id="location.country"
                  {...register('location.country')}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="location.latitude" className="block text-sm font-medium text-gray-700 mb-1">Latitude</label>
                <input
                  type="text"
                  id="location.latitude"
                  {...register('location.latitude')}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  readOnly
                />
              </div>
              <div>
                <label htmlFor="location.longitude" className="block text-sm font-medium text-gray-700 mb-1">Longitude</label>
                <input
                  type="text"
                  id="location.longitude"
                  {...register('location.longitude')}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  readOnly
                />
              </div>
            </div>
          </div>

          {/* Interests section */}
          <div>
            <h3 className="text-lg font-medium text-gray-800 mb-2">Interests</h3>
            <div className="flex mb-2">
              <input
                type="text"
                value={newInterest}
                onChange={(e) => setNewInterest(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-l-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                placeholder="Add an interest (e.g., Programming, Music, etc.)"
              />
              <button
                type="button"
                onClick={handleAddInterest}
                className="px-4 py-2 bg-blue-600 text-white rounded-r-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                Add
              </button>
            </div>
            <div className="flex flex-wrap gap-2 mt-2">
              {interests.map((interest, index) => (
                <div key={index} className="flex items-center bg-blue-100 text-blue-800 rounded-full px-3 py-1">
                  <span>{interest}</span>
                  <button
                    type="button"
                    onClick={() => handleRemoveInterest(index)}
                    className="ml-2 text-blue-600 hover:text-blue-800 focus:outline-none"
                  >
                    &times;
                  </button>
                </div>
              ))}
              {interests.length === 0 && (
                <p className="text-gray-500 text-sm">No interests added yet.</p>
              )}
            </div>
          </div>

          {/* Submit button */}
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <div className="animate-spin h-4 w-4 mr-2 border-2 border-white border-t-transparent rounded-full"></div>
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