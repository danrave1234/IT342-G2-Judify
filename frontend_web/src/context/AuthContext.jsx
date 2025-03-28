import { createContext, useContext, useState, useEffect } from 'react';
import { userApi } from '../api/api';

const AuthContext = createContext();

export const useAuth = () => {
  return useContext(AuthContext);
};

export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [token, setToken] = useState(localStorage.getItem('token') || '');

  useEffect(() => {
    // If we have a token, attempt to get the current user
    if (token) {
      fetchCurrentUser();
    } else {
      setLoading(false);
    }
  }, [token]);

  const fetchCurrentUser = async () => {
    try {
      setLoading(true);
      const response = await userApi.getCurrentUser();
      setCurrentUser(response.data);
      setError('');
    } catch (err) {
      console.error('Error fetching current user:', err);
      setError('Failed to authenticate user. Please log in again.');
      logout();
    } finally {
      setLoading(false);
    }
  };

  const login = async (email, password) => {
    try {
      setLoading(true);
      const response = await userApi.login({ email, password });
      
      // Save the token to local storage
      const authToken = response.data.token;
      localStorage.setItem('token', authToken);
      setToken(authToken);
      
      // Fetch user info
      await fetchCurrentUser();
      
      return { success: true };
    } catch (err) {
      console.error('Login error:', err);
      const message = err.response?.data?.message || 'Login failed. Please check your credentials.';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    try {
      setLoading(true);
      const response = await userApi.register(userData);
      
      // Automatically log the user in
      const authToken = response.data.token;
      localStorage.setItem('token', authToken);
      setToken(authToken);
      
      // Fetch user info
      await fetchCurrentUser();
      
      return { success: true };
    } catch (err) {
      console.error('Registration error:', err);
      const message = err.response?.data?.message || 'Registration failed. Please try again.';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken('');
    setCurrentUser(null);
  };

  const updateProfile = async (profileData) => {
    try {
      setLoading(true);
      const response = await userApi.updateProfile(profileData);
      setCurrentUser(response.data);
      return { success: true };
    } catch (err) {
      console.error('Update profile error:', err);
      const message = err.response?.data?.message || 'Failed to update profile.';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const value = {
    currentUser,
    loading,
    error,
    login,
    register,
    logout,
    updateProfile,
    isAuthenticated: !!currentUser,
    isStudent: () => currentUser?.role === 'STUDENT',
    isTutor: () => currentUser?.role === 'TUTOR',
    isAdmin: () => currentUser?.role === 'ADMIN'
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext; 