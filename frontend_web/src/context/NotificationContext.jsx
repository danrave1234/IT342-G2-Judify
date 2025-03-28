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
      loadNotifications();
    } else {
      setNotifications([]);
      setUnreadCount(0);
    }
  }, [user]);

  const loadNotifications = async (params = { page: 0, size: 10 }) => {
    if (!user) return { success: false, message: 'No user logged in', notifications: [] };
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await notificationApi.getNotifications(user.userId, params);
      setNotifications(response.data.content);
      
      // Calculate unread count
      const unread = response.data.content.filter(n => !n.isRead).length;
      setUnreadCount(unread);
      
      return { success: true, notifications: response.data.content, totalPages: response.data.totalPages };
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to load notifications';
      setError(message);
      return { success: false, message, notifications: [] };
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