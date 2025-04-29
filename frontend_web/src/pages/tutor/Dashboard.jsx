import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { tutoringSessionApi } from '../../api/api';

const Dashboard = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { currentUser } = useAuth();

  useEffect(() => {
    const fetchTutorSessions = async () => {
      if (!currentUser || !currentUser.userId) {
        setLoading(false);
        return;
      }

      try {
        const response = await tutoringSessionApi.getTutorSessions(currentUser.userId);
        setSessions(response.data);
      } catch (err) {
        console.error('Error fetching tutor sessions:', err);
        setError('Failed to load your sessions. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchTutorSessions();
  }, [currentUser]);

  // Format date for display
  const formatDate = (dateString) => {
    const options = { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' };
    return new Date(dateString).toLocaleDateString(undefined, options);
  };

  return (
    <div className="container mx-auto py-8">
      <h1 className="text-2xl font-bold mb-6">Tutor Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Upcoming Sessions</h2>
          {loading ? (
            <p className="text-gray-500">Loading sessions...</p>
          ) : error ? (
            <p className="text-red-500">{error}</p>
          ) : sessions.length === 0 ? (
            <p className="text-gray-500">You have no upcoming sessions.</p>
          ) : (
            <div className="space-y-4">
              {sessions.map((session) => (
                <div key={session.sessionId} className="border-b pb-3">
                  <p className="font-medium">{session.subject || 'Untitled Session'}</p>
                  <p className="text-sm text-gray-600">
                    Student: {session.studentName || 'Unknown Student'}
                  </p>
                  <p className="text-sm text-gray-600">
                    Date: {formatDate(session.sessionDate)}
                  </p>
                  <p className="text-sm text-gray-600">
                    Status: <span className={`font-medium ${
                      session.status === 'CONFIRMED' ? 'text-green-600' : 
                      session.status === 'PENDING' ? 'text-yellow-600' : 
                      session.status === 'CANCELLED' ? 'text-red-600' : 'text-gray-600'
                    }`}>{session.status}</span>
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Recent Earnings</h2>
          <p className="text-gray-500">No recent earnings to display.</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-lg font-semibold mb-4">Profile Completion</h2>
          <p className="text-gray-500">Complete your profile to attract more students.</p>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 
