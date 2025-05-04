import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { tutorProfileApi, userApi } from '../api/api';
import { useUser } from './UserContext';
import { toast } from 'react-toastify';

const TutorProfileContext = createContext(null);

export const useTutorProfile = () => useContext(TutorProfileContext);

export const TutorProfileProvider = ({ children }) => {
  const { user } = useUser();
  const [tutorProfile, setTutorProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [profileExists, setProfileExists] = useState(true);
  const [profileLoaded, setProfileLoaded] = useState(false);
  const [lastRefresh, setLastRefresh] = useState(0);

  const loadTutorProfile = useCallback(async (forceRefresh = false) => {
    if (!user || !user.userId || user.role !== 'TUTOR') {
      return { success: false, message: 'Not a tutor user' };
    }

    const now = Date.now();
    if (!forceRefresh && now - lastRefresh < 30000 && tutorProfile) {
      console.log('Using cached tutor profile data');
      return { success: true, profile: tutorProfile };
    }

    setLoading(true);
    try {
      console.log(`Loading tutor profile for user ID: ${user.userId}`);
      const response = await tutorProfileApi.getProfileByUserId(user.userId);
      setTutorProfile(response.data);
      setProfileExists(true);
      setLastRefresh(now);
      setProfileLoaded(true);
      return { success: true, profile: response.data };
    } catch (err) {
      console.error('Error loading tutor profile:', err);

      if (err.response?.status === 404) {
        setProfileExists(false);
        setError('No tutor profile found. Please create one.');
      } else {
        setError(err.response?.data?.message || 'Failed to load tutor profile');
      }
      
      setProfileLoaded(true);
      return { success: false, message: 'Failed to load tutor profile' };
    } finally {
      setLoading(false);
    }
  }, [user, lastRefresh]);

  useEffect(() => {
    if (user && user.role === 'TUTOR' && !profileLoaded) {
      loadTutorProfile();
    } else if (!user) {
      setTutorProfile(null);
      setProfileExists(true);
      setError(null);
      setProfileLoaded(false);
    }
  }, [user, loadTutorProfile, profileLoaded]);

  const createProfile = async (profileData) => {
    if (!user) return { success: false, message: 'No user logged in' };

    setLoading(true);
    setError(null);

    try {
      const data = { ...profileData, userId: user.userId };

      const response = await tutorProfileApi.createProfile(data);

      setTutorProfile(response.data);
      setProfileExists(true);
      setProfileLoaded(true);
      setLastRefresh(Date.now());
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
    if (!profileExists) {
      return createProfile(profileData);
    }

    if (!tutorProfile) {
      return { success: false, message: 'No tutor profile found' };
    }

    setLoading(true);
    setError(null);

    try {
      const response = await tutorProfileApi.updateProfile(tutorProfile.profileId, profileData);

      setTutorProfile(response.data);
      setLastRefresh(Date.now());

      if (profileData.profilePicture) {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
          const user = JSON.parse(storedUser);
          user.profileImage = profileData.profilePicture;
          localStorage.setItem('user', JSON.stringify(user));
        }
      }

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

  const searchTutors = async (params = {}) => {
    setLoading(true);
    setError(null);

    try {
      console.log('Searching tutors with params:', params);

      const { subject, query, minRating, minRate, maxRate, ...otherParams } = params;

      const searchQuery = query || '';
      const response = await userApi.searchUsers(searchQuery, 'TUTOR');

      if (response?.data) {
        console.log('Found users with TUTOR role:', response.data.length);

        const tutorUsers = response.data;

        const tutorProfiles = [];

        for (const user of tutorUsers) {
          try {
            const profileResponse = await tutorProfileApi.getProfileByUserId(user.userId);
            if (profileResponse?.data) {
              const combinedTutor = {
                ...profileResponse.data,
                user,
                firstName: user.firstName,
                lastName: user.lastName,
                username: user.username,
                email: user.email,
                profilePicture: user.profilePicture,
              };
              tutorProfiles.push(combinedTutor);
            } else {
              tutorProfiles.push({
                profileId: null,
                userId: user.userId,
                user,
                firstName: user.firstName,
                lastName: user.lastName,
                username: user.username,
                email: user.email,
                profilePicture: user.profilePicture,
                subjects: [],
                hourlyRate: 0,
                rating: 0,
                totalReviews: 0,
              });
            }
          } catch (err) {
            console.warn(`Could not fetch profile for user ${user.userId}:`, err);
            tutorProfiles.push({
              profileId: null,
              userId: user.userId,
              user,
              firstName: user.firstName,
              lastName: user.lastName,
              username: user.username,
              email: user.email,
              profilePicture: user.profilePicture,
              subjects: [],
              hourlyRate: 0,
              rating: 0,
              totalReviews: 0,
            });
          }
        }

        let filteredProfiles = [...tutorProfiles];

        if (subject) {
          filteredProfiles = filteredProfiles.filter(tutor => 
            tutor.subjects?.some(s => 
              typeof s === 'string' 
                ? s.toLowerCase().includes(subject.toLowerCase())
                : (s.subject || s.name)?.toLowerCase().includes(subject.toLowerCase())
            )
          );
        }

        if (minRating && minRating > 0) {
          filteredProfiles = filteredProfiles.filter(tutor => 
            (tutor.rating || 0) >= minRating
          );
        }

        if (minRate) {
          filteredProfiles = filteredProfiles.filter(tutor => 
            (tutor.hourlyRate || 0) >= minRate
          );
        }

        if (maxRate) {
          filteredProfiles = filteredProfiles.filter(tutor => 
            (tutor.hourlyRate || 0) <= maxRate
          );
        }

        console.log(`Found ${filteredProfiles.length} tutors after filtering`);

        return { success: true, results: filteredProfiles };
      }

      throw new Error('No valid response from API');
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to search tutors';
      console.error('Error searching tutors:', message, err);
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
      const message = err.response?.data?.message || 'Failed to get tutor profile';
      console.error(`Error getting tutor profile ${profileId}:`, message, err);
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const refreshProfile = async () => {
    return await loadTutorProfile(true);
  };

  return (
    <TutorProfileContext.Provider
      value={{
        tutorProfile,
        loading,
        error,
        profileExists,
        createProfile,
        updateProfile,
        searchTutors,
        getTutorProfile,
        refreshProfile,
        loadTutorProfile
      }}
    >
      {children}
    </TutorProfileContext.Provider>
  );
}; 
