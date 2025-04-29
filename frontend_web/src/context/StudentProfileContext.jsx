import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { studentProfileApi } from '../api/api';
import { useUser } from './UserContext';
import { toast } from 'react-toastify';

const StudentProfileContext = createContext(null);

export const useStudentProfile = () => useContext(StudentProfileContext);

export const StudentProfileProvider = ({ children }) => {
  const { user } = useUser();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [profileExists, setProfileExists] = useState(false);
  const [lastRefresh, setLastRefresh] = useState(0);

  // Create a memoized loadProfile function that doesn't change on re-renders
  const loadProfile = useCallback(async (forceRefresh = false) => {
    if (!user || !user.userId) {
      console.warn("Cannot load profile: No user ID available");
      return { success: false, message: 'User ID is required', profile: null };
    }

    // Don't refresh if we recently loaded the profile (within last 30 seconds),
    // unless forceRefresh is true
    const now = Date.now();
    if (!forceRefresh && now - lastRefresh < 30000 && profile) {
      console.log('Using cached profile data');
      return { success: true, profile };
    }

    setLoading(true);
    setError(null);

    try {
      console.log(`Loading student profile for user ID: ${user.userId}`);
      const response = await studentProfileApi.getProfileByUserId(user.userId);
      const profileData = response.data;

      console.log('Profile data loaded successfully:', profileData);

      setProfile(profileData);
      setProfileExists(true);
      setLastRefresh(now);

      return { success: true, profile: profileData };
    } catch (err) {
      console.error('Error loading student profile:', err);

      if (err.response?.status === 404) {
        // Profile doesn't exist yet - this is normal for new users
        console.log('No existing profile found - profile needs to be created');
        setProfileExists(false);
        setProfile(null);
        return { success: false, message: 'Profile not found', profile: null };
      }

      const message = err.response?.data?.message || 'Failed to load profile';
      setError(message);

      // Don't show toast for 404 errors (expected for new users)
      if (err.response?.status !== 404) {
        toast.error(`Failed to load profile: ${message}`);
      }

      return { success: false, message, profile: null };
    } finally {
      setLoading(false);
    }
  }, [user, profile, lastRefresh]);

  // Load profile when user changes
  useEffect(() => {
    if (user && user.userId) {
      loadProfile();
    } else {
      // Reset profile state when user is not logged in
      setProfile(null);
      setProfileExists(false);
      setError(null);
    }
  }, [user, loadProfile]);

  const updateProfile = async (profileData) => {
    if (!user || !user.userId) {
      const errorMsg = 'Cannot update profile: No user ID available';
      console.error(errorMsg);
      toast.error(errorMsg);
      return { success: false, message: errorMsg };
    }

    setLoading(true);
    setError(null);

    try {
      // Ensure userId is included in the profile data
      const completeProfileData = {
        ...profileData,
        userId: user.userId
      };

      console.log(`Updating student profile for user ID: ${user.userId}`);

      let response;
      try {
        if (profileExists) {
          console.log('Updating existing profile...');
          response = await studentProfileApi.updateProfile(user.userId, completeProfileData);
        } else {
          console.log('Creating new profile...');
          response = await studentProfileApi.createProfile(completeProfileData);
          setProfileExists(true);
        }
      } catch (err) {
        // If update fails but we're not sure if profile exists, try to create
        if (!profileExists || err.response?.status === 404) {
          console.log('Update failed, trying to create new profile instead...');
          response = await studentProfileApi.createProfile(completeProfileData);
          setProfileExists(true);
        } else {
          throw err;
        }
      }

      const updatedProfile = response.data;
      setProfile(updatedProfile);
      setLastRefresh(Date.now());

      // Also update the user's profile picture in localStorage if it's included in the profile data
      if (profileData.profilePicture) {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
          const userData = JSON.parse(storedUser);
          userData.profileImage = profileData.profilePicture;
          localStorage.setItem('user', JSON.stringify(userData));
        }
      }

      return { success: true, profile: updatedProfile };
    } catch (err) {
      console.error('Error updating student profile:', err);

      const message = err.response?.data?.message || 'Failed to update profile';
      setError(message);
      toast.error(`Failed to update profile: ${message}`);

      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const refreshProfile = async () => {
    setLoading(true);
    try {
      await loadProfile(true);
      return { success: true };
    } catch (error) {
      console.error('Error refreshing profile:', error);
      return { success: false, message: 'Failed to refresh profile' };
    } finally {
      setLoading(false);
    }
  };

  return (
    <StudentProfileContext.Provider
      value={{
        profile,
        loading,
        error,
        profileExists,
        loadProfile,
        updateProfile,
        refreshProfile
      }}
    >
      {children}
    </StudentProfileContext.Provider>
  );
}; 
