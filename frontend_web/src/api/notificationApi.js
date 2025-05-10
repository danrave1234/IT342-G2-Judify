import api from './baseApi';

// Notification API endpoints
export const notificationApi = {
  getNotifications: (userId, params = {}, userRole = null) => {
    if (!userId) {
      console.error('Cannot fetch notifications: userId is undefined');
      return Promise.reject(new Error('User ID is required for notifications'));
    }

    // Get user role from localStorage if not provided
    if (!userRole) {
      try {
        const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
        userRole = storedUser.role;
      } catch (e) {
        console.error('Error parsing user from localStorage:', e);
      }
    }

    // Use different endpoints based on user role
    const isTutor = userRole && userRole.toUpperCase() === 'TUTOR';

    console.log(`Fetching notifications for ${isTutor ? 'tutor' : 'user'} ${userId}`);

    // For tutors, use the tutor-specific endpoint
    if (isTutor) {
      return api.get(`/notifications/tutor/${userId}`, { params })
        .catch(error => {
          console.log('Error with tutor-specific endpoint, trying generic endpoint');
          return api.get(`/notifications/user/${userId}`, { params });
        });
    }

    // For students and other users, use the standard endpoint
    return api.get(`/notifications/user/${userId}`, { params })
      .catch(error => {
        console.log('Error with /notifications/user endpoint, trying fallback endpoint');
        // Fall back to original endpoint if first attempt fails
        return api.get(`/notifications/findByUser/${userId}`, { params });
      });
  },
  markAsRead: (notificationId) => {
    if (!notificationId) {
      console.error('Cannot mark notification as read: notificationId is undefined');
      return Promise.reject(new Error('Notification ID is required'));
    }
    return api.patch(`/notifications/${notificationId}/read`);
  },
  markAllAsRead: (userId, userRole = null) => {
    if (!userId) {
      console.error('Cannot mark all notifications as read: userId is undefined');
      return Promise.reject(new Error('User ID is required'));
    }

    // Get user role from localStorage if not provided
    if (!userRole) {
      try {
        const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
        userRole = storedUser.role;
      } catch (e) {
        console.error('Error parsing user from localStorage:', e);
      }
    }

    // Use different endpoints based on user role
    const isTutor = userRole && userRole.toUpperCase() === 'TUTOR';

    console.log(`Marking all notifications as read for ${isTutor ? 'tutor' : 'user'} ${userId}`);

    // For tutors, use the tutor-specific endpoint
    if (isTutor) {
      return api.patch(`/notifications/tutor/${userId}/read-all`)
        .catch(error => {
          console.log('Error with tutor-specific endpoint, trying generic endpoint');
          return api.patch(`/notifications/user/${userId}/read-all`);
        });
    }

    // For students and other users, use the standard endpoint
    return api.patch(`/notifications/user/${userId}/read-all`);
  }
}; 