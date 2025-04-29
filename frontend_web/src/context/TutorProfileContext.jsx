import { createContext, useContext, useState, useEffect } from 'react';
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

  // Load tutor profile if user is a tutor
  useEffect(() => {
    const loadTutorProfile = async () => {
      if (user && user.role === 'TUTOR') {
        setLoading(true);
        try {
          const response = await tutorProfileApi.getProfileByUserId(user.userId);
          setTutorProfile(response.data);
          setProfileExists(true);
        } catch (err) {
          console.error('Error loading tutor profile:', err);

          // Create a default profile if one doesn't exist (404 error)
          if (err.response?.status === 404) {
            setProfileExists(false);

            setError('No tutor profile found. Please create one.');
          } else {
            setError(err.response?.data?.message || 'Failed to load tutor profile');
          }
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
      setProfileExists(true);
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

    // Make sure we have a profile ID
    if (!tutorProfile.profileId) {
      console.error('Cannot update profile: Missing profile ID', tutorProfile);
      return { success: false, message: 'Profile ID is missing, cannot update' };
    }

    setLoading(true);
    setError(null);

    try {
      console.log('Updating tutor profile with ID:', tutorProfile.profileId);
      console.log('Update data:', profileData);

      const response = await tutorProfileApi.updateProfile(tutorProfile.profileId, profileData);

      console.log('Profile update response:', response);
      
      // Update the local state with the response data
      setTutorProfile(response.data);

      // Also update the user's profile picture in localStorage if it's included in the profile data
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
      console.error('Error updating tutor profile:', err);
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

      // Extract search query if present
      const { subject, query, minRating, minRate, maxRate, ...otherParams } = params;

      // Search for users with TUTOR role by name or username
      const searchQuery = query || '';
      const response = await userApi.searchUsers(searchQuery, 'TUTOR');

      // If we get users back, we can try to fetch their tutor profiles
      if (response?.data) {
        console.log('Found users with TUTOR role:', response.data.length);

        // Get users that have TUTOR role
        const tutorUsers = response.data;

        // For each user, try to fetch their tutor profile for additional details
        const tutorProfiles = [];

        for (const user of tutorUsers) {
          try {
            // Try to get tutor profile
            const profileResponse = await tutorProfileApi.getProfileByUserId(user.userId);
            if (profileResponse?.data) {
              // Combine user and profile data
              const combinedTutor = {
                ...profileResponse.data,
                user,
                // Copy some user fields to the top level for backwards compatibility
                firstName: user.firstName,
                lastName: user.lastName,
                username: user.username,
                email: user.email,
                profilePicture: user.profilePicture,
              };
              tutorProfiles.push(combinedTutor);
            } else {
              // If no profile exists, just use the user data
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
            // Still include the user even without profile data
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

        // Apply filters if needed
        let filteredProfiles = [...tutorProfiles];

        // Filter by subject if specified
        if (subject) {
          filteredProfiles = filteredProfiles.filter(tutor => 
            tutor.subjects?.some(s => 
              typeof s === 'string' 
                ? s.toLowerCase().includes(subject.toLowerCase())
                : (s.subject || s.name)?.toLowerCase().includes(subject.toLowerCase())
            )
          );
        }

        // Filter by minimum rating
        if (minRating && minRating > 0) {
          filteredProfiles = filteredProfiles.filter(tutor => 
            (tutor.rating || 0) >= minRating
          );
        }

        // Filter by price range
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
        profileExists,
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
