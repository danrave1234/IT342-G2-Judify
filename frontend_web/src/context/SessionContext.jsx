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
      // First, try to get the userId from tutorId if available
      if (sessionData.tutorId) {
        try {
          const { tutorProfileApi } = await import('../api/api');
          console.log(`Converting tutorId ${sessionData.tutorId} to userId before creating session`);
          
          const tutorUserIdResponse = await tutorProfileApi.getUserIdFromTutorId(sessionData.tutorId);
          const tutorUserId = tutorUserIdResponse.data;
          
          if (tutorUserId) {
            console.log(`Successfully converted tutorId ${sessionData.tutorId} to userId ${tutorUserId}`);
            
            // Create a new session data object with userId instead of tutorId
            const updatedSessionData = {
              ...sessionData,
              userId: tutorUserId
            };
            
            // Remove tutorId to avoid confusion
            delete updatedSessionData.tutorId;
            
            const response = await tutoringSessionApi.createSession(updatedSessionData);
            toast.success('Session scheduled successfully');
            return { 
              success: true, 
              session: response.data, 
              tutorId: sessionData.tutorId  // Keep original tutorId for reference
            };
          }
        } catch (convError) {
          console.error('Error converting tutorId to userId:', convError);
          // Continue with original sessionData if conversion fails
        }
      }
      
      // Fallback to original method if conversion fails or tutorId not available
      const response = await tutoringSessionApi.createSession(sessionData);
      toast.success('Session scheduled successfully');
      return { 
        success: true, 
        session: response.data, 
        tutorId: sessionData.tutorId 
      };
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
      console.log(`Fetching session details for session ID: ${sessionId}`);
      const response = await tutoringSessionApi.getSessionById(sessionId);
      
      // Normalize the session to ensure it has both id and sessionId fields
      const normalizedSession = {
        ...response.data,
        id: response.data.id || response.data.sessionId,
        sessionId: response.data.sessionId || response.data.id
      };
      
      console.log('Normalized session details:', normalizedSession);
      return { success: true, session: normalizedSession };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to fetch session details';
      console.error(`Error fetching session details: ${message}`, err);
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
      // First, get the tutor's profile to get the tutorId (profileId)
      const { tutorProfileApi } = await import('../api/api');
      const profileResponse = await tutorProfileApi.getProfileByUserId(user.userId);
      
      if (!profileResponse.data || !profileResponse.data.profileId) {
        setError('Tutor profile not found. Please complete your profile first.');
        return { success: false, message: 'Tutor profile not found', sessions: [] };
      }
      
      // Use the profile ID as tutorId for fetching sessions
      const tutorId = profileResponse.data.profileId;
      console.log(`Using tutor profile ID ${tutorId} to fetch sessions`);
      
      const response = await tutoringSessionApi.getTutorSessions(tutorId, params);
      
      // Make sure sessions always have an id field for consistency
      // Some backends return sessionId instead of id
      const normalizedSessions = response.data.map(session => ({
        ...session,
        id: session.id || session.sessionId
      }));
      
      console.log('Normalized sessions:', normalizedSessions);
      return { success: true, sessions: normalizedSessions };
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
      console.log(`Fetching student sessions for student ID: ${user.userId}`);
      const response = await tutoringSessionApi.getStudentSessions(user.userId, params);
      
      // Make sure sessions always have both id and sessionId fields for consistency
      const normalizedSessions = response.data.map(session => ({
        ...session,
        id: session.id || session.sessionId,
        sessionId: session.sessionId || session.id
      }));
      
      console.log('Normalized student sessions:', normalizedSessions);
      return { success: true, sessions: normalizedSessions };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to fetch student sessions';
      console.error(`Error fetching student sessions: ${message}`, err);
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