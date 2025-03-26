import { createContext, useState, useEffect, useContext } from 'react';
import axios from 'axios';

const UserContext = createContext();

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Check if user is logged in on page load
    const checkUserLoggedIn = async () => {
      try {
        const token = localStorage.getItem('judify_token');
        if (!token) {
          setLoading(false);
          return;
        }

        const config = {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        };

        const res = await axios.get('/api/users/me', config);
        setUser(res.data);
        setLoading(false);
      } catch (err) {
        localStorage.removeItem('judify_token');
        setError(err.response?.data?.message || 'An error occurred');
        setLoading(false);
      }
    };

    checkUserLoggedIn();
  }, []);

  // Login user
  const login = async (email, password) => {
    try {
      const res = await axios.post('/api/auth/login', { email, password });
      localStorage.setItem('judify_token', res.data.token);
      setUser(res.data.user);
      return { success: true };
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials');
      return { 
        success: false, 
        message: err.response?.data?.message || 'Invalid credentials' 
      };
    }
  };

  // Register user
  const register = async (userData) => {
    try {
      const res = await axios.post('/api/auth/register', userData);
      localStorage.setItem('judify_token', res.data.token);
      setUser(res.data.user);
      return { success: true };
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
      return { 
        success: false, 
        message: err.response?.data?.message || 'Registration failed' 
      };
    }
  };

  // Logout user
  const logout = () => {
    localStorage.removeItem('judify_token');
    setUser(null);
  };

  // Update user profile
  const updateProfile = async (userData) => {
    try {
      const token = localStorage.getItem('judify_token');
      const config = {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      };

      const res = await axios.put('/api/users/profile', userData, config);
      setUser(res.data);
      return { success: true };
    } catch (err) {
      setError(err.response?.data?.message || 'Profile update failed');
      return { 
        success: false, 
        message: err.response?.data?.message || 'Profile update failed' 
      };
    }
  };

  // Check if user is a tutor
  const isTutor = () => {
    return user && user.roles?.includes('TUTOR');
  };

  // Check if user is a student
  const isStudent = () => {
    return user && user.roles?.includes('STUDENT');
  };

  // Check if user is an admin
  const isAdmin = () => {
    return user && user.roles?.includes('ADMIN');
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
        isTutor,
        isStudent,
        isAdmin,
      }}
    >
      {children}
    </UserContext.Provider>
  );
};

export const useUser = () => useContext(UserContext);

export default UserContext; 