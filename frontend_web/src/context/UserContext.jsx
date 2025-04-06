import { createContext, useContext, useState, useEffect } from 'react';
import { authApi, userApi } from '../api/api';
import { toast } from 'react-toastify';
import { USER_ROLES } from '../types';

const UserContext = createContext(null);

export const useUser = () => useContext(UserContext);

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Initialize user from localStorage on mount
  useEffect(() => {
    const loadUser = async () => {
      try {
        const storedUser = localStorage.getItem('user');
        const token = localStorage.getItem('token');

        if (storedUser && token) {
          // Parse the stored user
          const parsedUser = JSON.parse(storedUser);
          
          // Ensure that profile images are loaded
          if (parsedUser) {
            // If missing profileImage in localStorage but present in state, add it
            if (user && user.profileImage && !parsedUser.profileImage) {
              parsedUser.profileImage = user.profileImage;
            }
          }
          
          setUser(parsedUser);
          
          // Verify the token and refresh user data
          try {
            await verifyAndRefreshUser();
          } catch (verifyErr) {
            console.error('Error verifying user token:', verifyErr);
            // If verification fails, we don't immediately logout
            // This allows offline app usage with last known user state
          }
        }
      } catch (err) {
        console.error('Error loading user:', err);
        logout();
      } finally {
        setLoading(false);
      }
    };

    loadUser();
  }, []);

  const verifyAndRefreshUser = async () => {
    try {
      // Check if we have stored user data
      const userData = localStorage.getItem('user');
      if (!userData) {
        console.log('No stored user data found during verification');
        logout();
        return;
      }
      
      const user = JSON.parse(userData);
      if (!user.userId) {
        console.log('Invalid user data during verification - missing userId');
        logout();
        return;
      }
      
      // Save current profile image
      const currentProfileImage = user.profileImage;
      
      // Try to fetch user data with the ID to validate token works
      try {
        console.log(`Verifying user with ID: ${user.userId}`);
        const response = await userApi.getCurrentUser();
        
        if (response && response.data) {
          const refreshedUserData = response.data;
          console.log('Successfully refreshed user data:', refreshedUserData);
          
          // Preserve profile image if it exists in the current state but not in the refreshed data
          if (currentProfileImage && !refreshedUserData.profileImage) {
            refreshedUserData.profileImage = currentProfileImage;
          }
          
          // Update user state with fresh data
          setUser(refreshedUserData);
          localStorage.setItem('user', JSON.stringify(refreshedUserData));
        } else {
          console.warn('User verification returned empty response');
        }
      } catch (apiErr) {
        console.error("Failed to refresh user data:", apiErr);
        // If getting user data fails, token may be invalid - logout
        logout();
        throw apiErr;
      }
    } catch (err) {
      console.error("Error verifying user:", err);
      logout();
      throw err;
    }
  };

  const login = async (email, password) => {
    setLoading(true);
    setError(null);
    
    try {
      console.log(`Attempting to log in user: ${email}`);
      const response = await authApi.login(email, password);
      
      // Log response data for debugging
      console.log("Login response details:", {
        status: response.status,
        statusText: response.statusText,
        data: response.data
      });
      
      // Extract authentication result from response
      const authData = response.data;
      
      // Check if authentication was successful
      if (!authData || !authData.authenticated) {
        console.error("Authentication failed:", authData);
        const errorMsg = authData?.message || 'Invalid email or password';
        setError(errorMsg);
        return { success: false, message: errorMsg };
      }
      
      // Authentication successful - extract user data
      const userDetails = {
        userId: authData.userId, 
        email: authData.email || email,
        username: authData.username || '',
        firstName: authData.firstName || '',
        lastName: authData.lastName || '',
        role: authData.role || (email.includes('tutor') ? 'TUTOR' : 'STUDENT'),
        isAuthenticated: true
      };
      
      // Ensure userId is always set - crucial for API calls
      if (!userDetails.userId) {
        console.warn("Auth response missing userId, using fallback");
        userDetails.userId = parseInt(authData.id || 0);
      }
      
      // Verify we have a valid userId
      if (!userDetails.userId) {
        console.error("Failed to obtain userId from auth response", authData);
        setError('Authentication response missing user ID');
        return { success: false, message: 'Authentication error: Missing user ID' };
      }
      
      console.log("Processed user data:", userDetails);
      
      // Save token and user data to localStorage
      const token = authData.token || `mock-token-${Date.now()}`;
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(userDetails));
      
      // Update app state
      setUser(userDetails);
      
      return { success: true };
    } catch (err) {
      console.error("Login error details:", {
        message: err.message,
        response: err.response,
        status: err.response?.status,
        data: err.response?.data
      });
      
      // For development/testing only: Create mock response if all API attempts fail
      if (process.env.NODE_ENV !== 'production' && (err.message?.includes('Network Error') || err.message?.includes('404'))) {
        console.warn("Creating mock login for development mode");
        
        // Mock user for development/testing
        const mockUser = {
          userId: 1,
          email: email,
          username: email.split('@')[0],
          firstName: 'Test',
          lastName: 'User',
          role: email.includes('tutor') ? 'TUTOR' : 'STUDENT',
          isAuthenticated: true
        };
        
        // Save mock data to localStorage
        const mockToken = `dev-token-${Date.now()}`;
        localStorage.setItem('token', mockToken);
        localStorage.setItem('user', JSON.stringify(mockUser));
        
        // Update app state
        setUser(mockUser);
        
        return { success: true };
      }
      
      const errorMessage = err.response?.data?.message || err.message || 'Login failed';
      setError(errorMessage);
      return { 
        success: false, 
        message: errorMessage
      };
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    setLoading(true);
    setError(null);
    
    try {
      // Log the registration data for debugging - mask password for security
      const logData = { ...userData, password: userData.password ? '********' : null };
      console.log('Registration data before sending:', logData);
      
      // Make sure password is properly set - critical check
      if (!userData.password || userData.password.trim() === '') {
        console.error("Registration error: Password is missing or empty");
        setError('Password is required for registration');
        return { 
          success: false, 
          message: 'Password is required for registration' 
        };
      }
      
      // Make sure role is set properly to match backend expectations
      if (!userData.role) {
        if (userData.userType) {
          userData.role = userData.userType === 'student' ? 'STUDENT' : 'TUTOR';
          delete userData.userType; // Remove userType as backend expects role
        } else {
          // Default to STUDENT if no role is specified
          userData.role = 'STUDENT';
        }
      }
      
      // Make sure username is set
      if (!userData.username) {
        userData.username = userData.email.split('@')[0];
        console.warn("Username was not provided - generated from email:", userData.username);
      } else {
        console.log("Using provided username:", userData.username);
      }
      
      // Explicitly set password - critical for backend
      // The database column is password_hash but the entity field is password
      if (userData.passwordHash && !userData.password) {
        userData.password = userData.passwordHash;
        delete userData.passwordHash;
      }
      
      // Convert any null values to empty strings
      Object.keys(userData).forEach(key => {
        if (userData[key] === null) {
          userData[key] = '';
        }
      });
      
      // Final check of required fields
      const requiredFields = ['firstName', 'lastName', 'email', 'username', 'password', 'role'];
      const missingFields = requiredFields.filter(field => !userData[field] || userData[field].trim() === '');
      
      if (missingFields.length > 0) {
        console.error("Registration error: Missing required fields", missingFields);
        setError(`Missing required fields: ${missingFields.join(', ')}`);
        return { 
          success: false, 
          message: `Missing required fields: ${missingFields.join(', ')}` 
        };
      }
      
      // Log that we're about to send the data (with password masked)
      console.log('Final registration data (password masked):', { ...userData, password: '********' });
      
      // Send the registration request
      const response = await authApi.register(userData);
      console.log('Registration successful:', response.data);
      return { success: true, message: response.data.message || 'Registration successful!' };
    } catch (err) {
      console.error("Registration error:", err);
      
      if (err.response) {
        console.error("Server response:", err.response.data);
        console.error("Status code:", err.response.status);
      }
      
      setError(err.response?.data?.message || err.message || 'Registration failed');
      return { 
        success: false, 
        message: err.response?.data?.message || err.message || 'Registration failed' 
      };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  const updateProfile = async (userData) => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await userApi.updateUser(user.userId, userData);
      const updatedUser = response.data;
      
      // Update user state and localStorage
      setUser(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));
      
      toast.success('Profile updated successfully');
      return { success: true };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to update profile';
      setError(message);
      toast.error(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const uploadProfilePicture = async (file) => {
    setLoading(true);
    setError(null);
    
    try {
      // Create FormData for file upload
      const formData = new FormData();
      formData.append('file', file);
      
      // For development/testing, we'll create a mock implementation
      if (process.env.NODE_ENV !== 'production' || !user?.userId) {
        // Create object URL for direct display
        const reader = new FileReader();
        
        return new Promise((resolve) => {
          reader.onloadend = () => {
            // Get base64 data URL
            const profileImage = reader.result;
            
            // Update user state with the new profile picture URL
            const updatedUser = { ...user, profileImage };
            setUser(updatedUser);
            
            // Save to localStorage
            localStorage.setItem('user', JSON.stringify(updatedUser));
            
            toast.success('Profile picture updated successfully');
            resolve({ success: true, profileImage });
          };
          
          reader.readAsDataURL(file);
        });
      }
      
      // Use the API to upload the file
      const response = await userApi.uploadProfilePicture(user.userId, formData);
      
      if (response && response.data) {
        // Update user state with the new profile picture URL
        const profileImage = response.data.profilePicture || response.data.profileImage;
        const updatedUser = { ...user, profileImage };
        setUser(updatedUser);
        
        // Save to localStorage
        localStorage.setItem('user', JSON.stringify(updatedUser));
        
        toast.success('Profile picture updated successfully');
        return { success: true, profileImage };
      }
      
      throw new Error('Failed to update profile picture');
    } catch (err) {
      console.error('Error uploading profile picture:', err);
      toast.error('Failed to update profile picture');
      setError('Failed to update profile picture');
      return { success: false, message: err.message };
    } finally {
      setLoading(false);
    }
  };

  const requestPasswordReset = async (email) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await authApi.resetPassword(email);
      return { success: true, message: response.data.message };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to request password reset';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  // Check if the user is a tutor
  const isTutor = () => {
    if (!user) return false;
    
    // Compare normalizing case
    const userRole = (user.role || '').toUpperCase();
    return userRole === 'TUTOR' || userRole === USER_ROLES.TUTOR;
  };
  
  // Check if the user is a student
  const isStudent = () => {
    if (!user) return false;
    
    // Compare normalizing case
    const userRole = (user.role || '').toUpperCase();
    return userRole === 'STUDENT' || userRole === USER_ROLES.STUDENT;
  };
  
  // Check if the user is an admin
  const isAdmin = () => {
    if (!user) return false;
    
    // Compare normalizing case
    const userRole = (user.role || '').toUpperCase();
    return userRole === 'ADMIN' || userRole === USER_ROLES.ADMIN;
  };

  const value = {
    user,
    loading,
    error,
    login,
    register,
    logout,
    updateProfile,
    uploadProfilePicture,
    requestPasswordReset,
    isTutor,
    isStudent,
    isAdmin
  };

  return (
    <UserContext.Provider
      value={value}
    >
      {children}
    </UserContext.Provider>
  );
}; 