import { createContext, useContext, useState, useEffect } from 'react';
import { tutorProfileApi } from '../api/api';
import { useUser } from './UserContext';
import { toast } from 'react-toastify';

const TutorProfileContext = createContext(null);

export const useTutorProfile = () => useContext(TutorProfileContext);

export const TutorProfileProvider = ({ children }) => {
  const { user } = useUser();
  const [tutorProfile, setTutorProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Load tutor profile if user is a tutor
  useEffect(() => {
    const loadTutorProfile = async () => {
      if (user && user.role === 'TUTOR') {
        setLoading(true);
        try {
          const response = await tutorProfileApi.getProfileByUserId(user.userId);
          setTutorProfile(response.data);
        } catch (err) {
          console.error('Error loading tutor profile:', err);
          setError(err.response?.data?.message || 'Failed to load tutor profile');
        } finally {
          setLoading(false);
        }
      }
    };

    loadTutorProfile();
  }, [user]);

  const createProfile = async (profileData) => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    setLoading(true);
    setError(null);
    
    try {
      // Add userId to profile data
      const data = { ...profileData, userId: user.userId };
      const response = await tutorProfileApi.createProfile(data);
      
      setTutorProfile(response.data);
      toast.success('Tutor profile created successfully');
      return { success: true, profile: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to create tutor profile';
      setError(message);
      toast.error(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const updateProfile = async (profileData) => {
    if (!tutorProfile) return { success: false, message: 'No tutor profile found' };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await tutorProfileApi.updateProfile(tutorProfile.profileId, profileData);
      
      setTutorProfile(response.data);
      toast.success('Tutor profile updated successfully');
      return { success: true, profile: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to update tutor profile';
      setError(message);
      toast.error(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const searchTutors = async (params) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await tutorProfileApi.searchProfiles(params);
      return { success: true, results: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to search tutors';
      setError(message);
      return { success: false, message, results: [] };
    } finally {
      setLoading(false);
    }
  };

  const getTutorProfile = async (profileId) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await tutorProfileApi.getProfileById(profileId);
      return { success: true, profile: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to load tutor profile';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  return (
    <TutorProfileContext.Provider
      value={{
        tutorProfile,
        loading,
        error,
        createProfile,
        updateProfile,
        searchTutors,
        getTutorProfile
      }}
    >
      {children}
    </TutorProfileContext.Provider>
  );
}; 