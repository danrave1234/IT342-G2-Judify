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
            
            // Use mock data for development
            if (process.env.NODE_ENV !== 'production') {
              const mockProfile = {
                profileId: 0,
                userId: user.userId,
                bio: '',
                expertise: '',
                subjects: [],
                hourlyRate: 0,
                rating: 0,
                totalReviews: 0,
                isVerified: false,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
              };
              setTutorProfile(mockProfile);
            } else {
              setError('No tutor profile found. Please create one.');
            }
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
      
      // Use mock implementation for development
      if (process.env.NODE_ENV !== 'production') {
        console.log('Creating mock tutor profile', data);
        
        const mockProfile = {
          profileId: Date.now(),
          userId: user.userId,
          bio: data.bio || '',
          expertise: data.expertise || '',
          subjects: data.subjects || [],
          hourlyRate: data.hourlyRate || 0,
          rating: 0,
          totalReviews: 0,
          isVerified: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        };
        
        setTutorProfile(mockProfile);
        setProfileExists(true);
        toast.success('Tutor profile created successfully');
        return { success: true, profile: mockProfile };
      }
      
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
    
    setLoading(true);
    setError(null);
    
    try {
      // Use mock implementation for development
      if (process.env.NODE_ENV !== 'production') {
        console.log('Updating mock tutor profile', profileData);
        
        const updatedProfile = {
          ...tutorProfile,
          ...profileData,
          updatedAt: new Date().toISOString()
        };
        
        setTutorProfile(updatedProfile);
        toast.success('Tutor profile updated successfully');
        return { success: true, profile: updatedProfile };
      }
      
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
      
      // For development, generate mock data if API calls fail
      if (process.env.NODE_ENV !== 'production') {
        const mockTutors = generateMockTutors();
        return { success: true, message: 'Using mock data', results: mockTutors };
      }
      
      return { success: false, message, results: [] };
    } finally {
      setLoading(false);
    }
  };

  // Helper function to generate mock tutor data
  const generateMockTutors = () => {
    return [
      {
        profileId: 1,
        userId: 1,
        firstName: 'Sarah',
        lastName: 'Johnson',
        profilePicture: 'https://randomuser.me/api/portraits/women/44.jpg',
        bio: 'Experienced math tutor with 5+ years of teaching experience',
        expertise: 'Mathematics Expert',
        subjects: ['Calculus', 'Algebra', 'Statistics'],
        hourlyRate: 45,
        rating: 4.9,
        totalReviews: 120,
        user: {
          firstName: 'Sarah',
          lastName: 'Johnson'
        }
      },
      {
        profileId: 2,
        userId: 2,
        firstName: 'Michael',
        lastName: 'Chen',
        profilePicture: 'https://randomuser.me/api/portraits/men/22.jpg',
        bio: 'Physics PhD student offering advanced tutoring',
        expertise: 'Physics Tutor',
        subjects: ['Physics', 'Mathematics'],
        hourlyRate: 50,
        rating: 4.7,
        totalReviews: 85,
        user: {
          firstName: 'Michael',
          lastName: 'Chen'
        }
      },
      {
        profileId: 3,
        userId: 3,
        firstName: 'Emily',
        lastName: 'Rodriguez',
        profilePicture: 'https://randomuser.me/api/portraits/women/66.jpg',
        bio: 'Chemistry specialist with experience teaching all levels',
        expertise: 'Chemistry Expert',
        subjects: ['Chemistry', 'Organic Chemistry', 'Biochemistry'],
        hourlyRate: 55,
        rating: 4.8,
        totalReviews: 95,
        user: {
          firstName: 'Emily',
          lastName: 'Rodriguez'
        }
      }
    ];
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