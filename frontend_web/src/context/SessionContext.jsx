import { createContext, useContext, useState } from 'react';
import { tutoringSessionApi } from '../api/api';
import { useUser } from './UserContext';
import { toast } from 'react-toastify';

const SessionContext = createContext(null);

export const useSession = () => useContext(SessionContext);

export const SessionProvider = ({ children }) => {
  const { user } = useUser();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const createSession = async (sessionData) => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await tutoringSessionApi.createSession(sessionData);
      toast.success('Session scheduled successfully');
      return { success: true, session: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to schedule session';
      setError(message);
      toast.error(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const updateSessionStatus = async (sessionId, status) => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await tutoringSessionApi.updateSessionStatus(sessionId, status);
      toast.success(`Session ${status.toLowerCase()} successfully`);
      return { success: true, session: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to update session status';
      setError(message);
      toast.error(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const getSessionById = async (sessionId) => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await tutoringSessionApi.getSessionById(sessionId);
      return { success: true, session: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to fetch session details';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const getTutorSessions = async (params = {}) => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await tutoringSessionApi.getTutorSessions(user.userId, params);
      return { success: true, sessions: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to fetch tutor sessions';
      setError(message);
      return { success: false, message, sessions: [] };
    } finally {
      setLoading(false);
    }
  };

  const getStudentSessions = async (params = {}) => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await tutoringSessionApi.getStudentSessions(user.userId, params);
      return { success: true, sessions: response.data };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to fetch student sessions';
      setError(message);
      return { success: false, message, sessions: [] };
    } finally {
      setLoading(false);
    }
  };

  return (
    <SessionContext.Provider
      value={{
        loading,
        error,
        createSession,
        updateSessionStatus,
        getSessionById,
        getTutorSessions,
        getStudentSessions,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}; 