import { createContext, useContext, useState, useEffect } from 'react';
import { notificationApi } from '../api/api';
import { useUser } from './UserContext';

const NotificationContext = createContext(null);

export const useNotification = () => useContext(NotificationContext);

export const NotificationProvider = ({ children }) => {
  const { user } = useUser();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Load notifications when user changes
  useEffect(() => {
    if (user) {
      loadNotifications().catch(err => {
        console.error("Error loading notifications:", err);
        // Set empty notifications but don't block app flow
        setNotifications([]);
        setUnreadCount(0);
      });
    } else {
      setNotifications([]);
      setUnreadCount(0);
    }
  }, [user]);

  const loadNotifications = async (params = { page: 0, size: 10 }) => {
    // Ensure user exists and has a userId
    if (!user) {
      console.warn("Cannot load notifications: user is not logged in");
      return { success: false, message: 'No user logged in', notifications: [] };
    }
    
    if (!user.userId) {
      console.warn("Cannot load notifications: user ID is missing");
      return { success: false, message: 'User ID is missing', notifications: [] };
    }
    
    setLoading(true);
    setError(null);
    
    try {
      console.log(`Loading notifications for user ID: ${user.userId} with params:`, params);
      
      const response = await notificationApi.getNotifications(user.userId, params);
      
      // Check if the response has expected structure
      const notificationsData = response.data?.content || response.data || [];
      
      console.log(`Retrieved ${Array.isArray(notificationsData) ? notificationsData.length : 0} notifications`);
      
      // Ensure we always have an array
      const safeNotifications = Array.isArray(notificationsData) ? notificationsData : [];
      setNotifications(safeNotifications);
      
      // Calculate unread count
      const unread = safeNotifications.filter(n => !n.isRead).length;
      setUnreadCount(unread);
      
      return { 
        success: true, 
        notifications: safeNotifications, 
        totalPages: response.data?.totalPages || 1,
        totalElements: response.data?.totalElements || safeNotifications.length
      };
    } catch (err) {
      console.error("Failed to load notifications:", err);
      console.error("Error details:", err.response?.data);
      
      // Don't block application flow on notification errors
      setNotifications([]);
      setUnreadCount(0);
      
      const message = err.response?.data?.message || 'Failed to load notifications';
      setError(message);
      
      return { 
        success: false, 
        message, 
        notifications: [] 
      };
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = async (notificationId) => {
    if (!user) return { success: false, message: 'No user logged in' };
    if (!notificationId) return { success: false, message: 'No notification specified' };
    
    setLoading(true);
    setError(null);
    
    try {
      await notificationApi.markAsRead(notificationId);
      
      // Update notification in state
      setNotifications(prevNotifications => 
        prevNotifications.map(note => 
          note.notificationId === notificationId ? { ...note, isRead: true } : note
        )
      );
      
      // Decrease unread count
      setUnreadCount(prev => Math.max(0, prev - 1));
      
      return { success: true };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to mark notification as read';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  const markAllAsRead = async () => {
    if (!user) return { success: false, message: 'No user logged in' };
    
    setLoading(true);
    setError(null);
    
    try {
      await notificationApi.markAllAsRead(user.userId);
      
      // Update all notifications in state as read
      setNotifications(prevNotifications => 
        prevNotifications.map(note => ({ ...note, isRead: true }))
      );
      
      // Reset unread count
      setUnreadCount(0);
      
      return { success: true };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to mark all notifications as read';
      setError(message);
      return { success: false, message };
    } finally {
      setLoading(false);
    }
  };

  // Function to handle new notifications (e.g., from WebSocket)
  const addNotification = (notification) => {
    setNotifications(prev => [notification, ...prev]);
    if (!notification.isRead) {
      setUnreadCount(prev => prev + 1);
    }
  };

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        loading,
        error,
        loadNotifications,
        markAsRead,
        markAllAsRead,
        addNotification
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
}; 