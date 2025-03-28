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
          setUser(JSON.parse(storedUser));
          
          // Verify the token and refresh user data
          await verifyAndRefreshUser();
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
      // Verify the token
      await authApi.verify();
      
      // If valid, get fresh user data
      const response = await userApi.getCurrentUser();
      const userData = response.data;
      
      // Update user state and localStorage
      setUser(userData);
      localStorage.setItem('user', JSON.stringify(userData));
    } catch (err) {
      // If token is invalid, logout
      logout();
      throw err;
    }
  };

  const login = async (email, password) => {
    setLoading(true);
    setError(null);
    
    try {
      // The backend endpoint uses query parameters, not JSON body
      const response = await authApi.login(email, password);
      
      // Log response data for debugging
      console.log("Login response:", response.data);
      
      const { token, ...userData } = response.data;
      
      // Save token and user data
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(userData));
      
      setUser(userData);
      
      return { success: true };
    } catch (err) {
      console.error("Login error:", err.response?.data || err.message);
      setError(err.response?.data?.message || 'Login failed');
      return { 
        success: false, 
        message: err.response?.data?.message || 'Login failed' 
      };
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    setLoading(true);
    setError(null);
    
    try {
      // Convert userType to UserRole enum value expected by backend
      const userDataToSend = {
        ...userData,
        // Make sure the username is set if not provided
        username: userData.username || userData.email.split('@')[0]
      };
      
      // Ensure role is correctly set as expected by backend
      if (!userDataToSend.role && userDataToSend.userType) {
        userDataToSend.role = userDataToSend.userType === 'student' ? 'STUDENT' : 'TUTOR';
        delete userDataToSend.userType; // Remove userType as backend expects role
      }
      
      const response = await authApi.register(userDataToSend);
      return { success: true, message: response.data.message };
    } catch (err) {
      console.error("Registration error:", err.response?.data || err.message);
      setError(err.response?.data?.message || 'Registration failed');
      return { 
        success: false, 
        message: err.response?.data?.message || 'Registration failed' 
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
    if (!user) return { success: false, message: 'No user logged in' };
    if (!file) return { success: false, message: 'No file selected' };
    
    setLoading(true);
    setError(null);
    
    try {
      const formData = new FormData();
      formData.append('profilePicture', file);
      
      const response = await userApi.uploadProfilePicture(user.userId, formData);
      const updatedUser = response.data;
      
      // Update user state and localStorage
      setUser(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));
      
      toast.success('Profile picture updated successfully');
      return { success: true };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to upload profile picture';
      setError(message);
      toast.error(message);
      return { success: false, message };
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
    return user.role === USER_ROLES.TUTOR || user.role === 'TUTOR';
  };
  
  // Check if the user is a student
  const isStudent = () => {
    if (!user) return false;
    return user.role === USER_ROLES.STUDENT || user.role === 'STUDENT';
  };
  
  // Check if the user is an admin
  const isAdmin = () => {
    if (!user) return false;
    return user.role === USER_ROLES.ADMIN || user.role === 'ADMIN';
  };

  return (
    <UserContext.Provider
      value={{
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
      }}
    >
      {children}
    </UserContext.Provider>
  );
}; 