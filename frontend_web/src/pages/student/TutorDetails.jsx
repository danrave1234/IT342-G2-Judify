import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  FaStar, 
  FaMapMarkerAlt, 
  FaVideo, 
  FaChalkboardTeacher,
  FaCalendarAlt,
  FaClock,
  FaDollarSign,
  FaChevronLeft,
  FaCheck
} from 'react-icons/fa';
import { toast } from 'react-toastify';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import axios from 'axios';
import { useUser } from '../../context/UserContext';

const TutorDetails = () => {
  const { id } = useParams();
  const { user } = useUser();
  const [tutor, setTutor] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [availableTimeSlots, setAvailableTimeSlots] = useState([]);
  const [selectedTimeSlot, setSelectedTimeSlot] = useState(null);
  const [sessionType, setSessionType] = useState('online');
  const [submitting, setSubmitting] = useState(false);
  const [userLocation, setUserLocation] = useState(null);
  const [distance, setDistance] = useState(null);

  useEffect(() => {
    // Get user's location
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        (error) => {
          console.error('Error getting location:', error);
        }
      );
    }

    const fetchTutorDetails = async () => {
      try {
        const token = localStorage.getItem('judify_token');
        const config = {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        };

        const tutorRes = await axios.get(`/api/tutors/${id}`, config);
        setTutor(tutorRes.data);
        
        const reviewsRes = await axios.get(`/api/tutors/${id}/reviews`, config);
        setReviews(reviewsRes.data);
        
        setLoading(false);
      } catch (error) {
        toast.error('Failed to load tutor details');
        setLoading(false);
      }
    };

    fetchTutorDetails();
  }, [id]);

  useEffect(() => {
    if (userLocation && tutor?.location) {
      const dist = calculateDistance(
        userLocation.latitude,
        userLocation.longitude,
        tutor.location.latitude,
        tutor.location.longitude
      );
      setDistance(dist);
    }
  }, [userLocation, tutor]);

  useEffect(() => {
    if (!tutor || !selectedDate) return;
    
    // Get available time slots based on tutor's availability
    const dayOfWeek = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][selectedDate.getDay()];
    
    const availableSlots = [];
    
    // Find matching availability for selected day
    const dayAvailability = tutor.availabilities?.filter(a => a.dayOfWeek === dayOfWeek) || [];
    
    // Generate 1-hour slots within each availability window
    dayAvailability.forEach(availability => {
      const [startHour, startMinute] = availability.startTime.split(':').map(Number);
      const [endHour, endMinute] = availability.endTime.split(':').map(Number);
      
      const startTime = new Date(selectedDate);
      startTime.setHours(startHour, startMinute, 0, 0);
      
      const endTime = new Date(selectedDate);
      endTime.setHours(endHour, endMinute, 0, 0);
      
      // Create 1-hour slots
      const currentTime = new Date(startTime);
      while (currentTime < endTime) {
        const slotEndTime = new Date(currentTime);
        slotEndTime.setHours(currentTime.getHours() + 1);
        
        // Don't add slots that extend beyond availability end time
        if (slotEndTime <= endTime) {
          availableSlots.push({
            start: new Date(currentTime),
            end: slotEndTime
          });
        }
        
        // Move to next hour
        currentTime.setHours(currentTime.getHours() + 1);
      }
    });
    
    setAvailableTimeSlots(availableSlots);
    setSelectedTimeSlot(null);
  }, [tutor, selectedDate]);

  const handleBookSession = async () => {
    if (!selectedTimeSlot) {
      toast.error('Please select a time slot');
      return;
    }

    setSubmitting(true);
    try {
      const token = localStorage.getItem('judify_token');
      const config = {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      };

      const sessionData = {
        tutorId: tutor.id,
        studentId: user.id,
        startTime: selectedTimeSlot.start.toISOString(),
        endTime: selectedTimeSlot.end.toISOString(),
        sessionType: sessionType,
        status: 'PENDING'
      };

      await axios.post('/api/sessions', sessionData, config);
      toast.success('Session request sent!');
      setSelectedTimeSlot(null);
    } catch (error) {
      toast.error('Failed to book session. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  // Calculate distance between two points using Haversine formula
  const calculateDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371; // Earth radius in km
    const dLat = deg2rad(lat2 - lat1);
    const dLon = deg2rad(lon2 - lon1);
    const a = 
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
      Math.sin(dLon / 2) * Math.sin(dLon / 2); 
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); 
    const distance = R * c; // Distance in km
    return distance;
  };

  const deg2rad = (deg) => {
    return deg * (Math.PI / 180);
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="w-12 h-12 border-t-4 border-blue-500 border-solid rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!tutor) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="text-center py-12 bg-gray-50 rounded-lg">
          <h3 className="text-lg font-medium text-gray-900 mb-2">Tutor not found</h3>
          <p className="text-gray-500 mb-4">The tutor you're looking for does not exist or has been removed.</p>
          <Link
            to="/student/find-tutors"
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
          >
            <FaChevronLeft className="mr-2" /> Back to Search
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {/* Back Link */}
      <div className="mb-6">
        <Link
          to="/student/find-tutors"
          className="inline-flex items-center text-sm font-medium text-blue-600 hover:text-blue-800"
        >
          <FaChevronLeft className="mr-1" /> Back to Search
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2">
          {/* Tutor Header */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <div className="sm:flex items-start">
              <div className="flex-shrink-0 h-24 w-24 sm:h-32 sm:w-32 rounded-full bg-gray-300 flex items-center justify-center text-gray-700 overflow-hidden mb-4 sm:mb-0">
                {tutor.user?.profileImage ? (
                  <img
                    src={tutor.user.profileImage}
                    alt={`${tutor.user?.firstName} ${tutor.user?.lastName}`}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <FaChalkboardTeacher size={64} />
                )}
              </div>
              <div className="sm:ml-6">
                <h1 className="text-2xl font-bold text-gray-900">
                  {tutor.user?.firstName} {tutor.user?.lastName}
                </h1>
                <p className="text-xl text-gray-600 mt-1">{tutor.title}</p>
                
                <div className="flex items-center mt-2">
                  {[...Array(5)].map((_, i) => (
                    <FaStar
                      key={i}
                      className={`text-lg ${
                        i < Math.round(tutor.averageRating || 0)
                          ? 'text-yellow-400'
                          : 'text-gray-300'
                      }`}
                    />
                  ))}
                  <span className="ml-2 text-gray-600">
                    {tutor.averageRating?.toFixed(1) || 'New'} ({tutor.reviewCount || 0} reviews)
                  </span>
                </div>
                
                <div className="flex flex-wrap items-center mt-3 text-gray-600">
                  <div className="flex items-center mr-6 mb-2">
                    <FaDollarSign className="mr-1" />
                    <span className="font-medium">${tutor.hourlyRate}/hour</span>
                  </div>
                  
                  {tutor.isOnlineAvailable && (
                    <div className="flex items-center mr-6 mb-2">
                      <FaVideo className="text-blue-500 mr-1" />
                      <span>Online Available</span>
                    </div>
                  )}
                  
                  {tutor.isInPersonAvailable && tutor.location && (
                    <div className="flex items-center mb-2">
                      <FaMapMarkerAlt className="text-red-500 mr-1" />
                      <span>
                        {tutor.location.city}, {tutor.location.state}
                        {distance && ` (${distance.toFixed(1)} km away)`}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
          
          {/* Subjects & Expertise */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">Subjects & Expertise</h2>
            <div className="flex flex-wrap gap-2">
              {tutor.subjects?.map((subject, index) => (
                <div
                  key={index}
                  className="px-3 py-1 rounded-full text-sm bg-blue-100 text-blue-800"
                >
                  {subject.name} <span className="text-blue-600">({subject.expertiseLevel.toLowerCase()})</span>
                </div>
              ))}
            </div>
          </div>
          
          {/* About */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">About</h2>
            <p className="text-gray-600 whitespace-pre-line">{tutor.bio}</p>
          </div>
          
          {/* Education & Experience */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Education</h2>
              <p className="text-gray-600 whitespace-pre-line">{tutor.education}</p>
            </div>
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-4">Experience</h2>
              <p className="text-gray-600 whitespace-pre-line">{tutor.experience}</p>
            </div>
          </div>
          
          {/* Reviews */}
          <div className="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">
              Reviews ({reviews.length})
            </h2>
            {reviews.length === 0 ? (
              <p className="text-gray-500">No reviews yet.</p>
            ) : (
              <div className="space-y-6">
                {reviews.map((review) => (
                  <div key={review.id} className="border-b pb-6 last:border-b-0 last:pb-0">
                    <div className="flex items-start">
                      <div className="flex-shrink-0 h-10 w-10 rounded-full bg-gray-300 flex items-center justify-center overflow-hidden">
                        {review.student?.user?.profileImage ? (
                          <img
                            src={review.student.user.profileImage}
                            alt={`${review.student?.user?.firstName || 'Student'}`}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <FaUser />
                        )}
                      </div>
                      <div className="ml-4">
                        <h4 className="text-sm font-medium text-gray-900">
                          {review.student?.user?.firstName} {review.student?.user?.lastName?.charAt(0)}.
                        </h4>
                        <div className="flex items-center mt-1">
                          {[...Array(5)].map((_, i) => (
                            <FaStar
                              key={i}
                              className={`text-xs ${
                                i < review.rating ? 'text-yellow-400' : 'text-gray-300'
                              }`}
                            />
                          ))}
                          <span className="ml-1 text-xs text-gray-500">
                            {new Date(review.createdDate).toLocaleDateString()}
                          </span>
                        </div>
                        <p className="mt-2 text-sm text-gray-600">{review.comment}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
        
        {/* Booking Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-6 sticky top-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">Book a Session</h2>
            
            {/* Date Selection */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <FaCalendarAlt className="inline mr-2" /> Select Date
              </label>
              <DatePicker
                selected={selectedDate}
                onChange={(date) => setSelectedDate(date)}
                minDate={new Date()}
                className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                dateFormat="MMMM d, yyyy"
              />
            </div>
            
            {/* Time Slot Selection */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <FaClock className="inline mr-2" /> Available Time Slots
              </label>
              {availableTimeSlots.length === 0 ? (
                <p className="text-red-500 text-sm">No available time slots for this day.</p>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  {availableTimeSlots.map((slot, index) => (
                    <button
                      key={index}
                      type="button"
                      onClick={() => setSelectedTimeSlot(slot)}
                      className={`px-3 py-2 text-sm border rounded-md ${
                        selectedTimeSlot === slot
                          ? 'bg-blue-100 border-blue-500 text-blue-700'
                          : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                      }`}
                    >
                      {slot.start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - 
                      {slot.end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </button>
                  ))}
                </div>
              )}
            </div>
            
            {/* Session Type Selection */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">Session Type</label>
              <div className="grid grid-cols-2 gap-3">
                {tutor.isOnlineAvailable && (
                  <button
                    type="button"
                    onClick={() => setSessionType('online')}
                    className={`flex items-center justify-center px-4 py-2 border rounded-md ${
                      sessionType === 'online'
                        ? 'bg-blue-100 border-blue-500 text-blue-700'
                        : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <FaVideo className="mr-2" /> Online
                  </button>
                )}
                {tutor.isInPersonAvailable && (
                  <button
                    type="button"
                    onClick={() => setSessionType('inPerson')}
                    className={`flex items-center justify-center px-4 py-2 border rounded-md ${
                      sessionType === 'inPerson'
                        ? 'bg-blue-100 border-blue-500 text-blue-700'
                        : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <FaMapMarkerAlt className="mr-2" /> In-person
                  </button>
                )}
              </div>
              {sessionType === 'inPerson' && distance && distance > 50 && (
                <p className="mt-2 text-sm text-yellow-600">
                  <span role="img" aria-label="warning">⚠️</span> This tutor is {distance.toFixed(1)} km away. Consider online tutoring for better convenience.
                </p>
              )}
            </div>
            
            {/* Pricing */}
            <div className="border-t border-gray-200 pt-4 mb-6">
              <div className="flex justify-between">
                <span className="text-gray-600">1 hour session</span>
                <span className="text-gray-900 font-medium">${tutor.hourlyRate}</span>
              </div>
            </div>
            
            {/* Book Button */}
            <button
              type="button"
              onClick={handleBookSession}
              disabled={!selectedTimeSlot || submitting}
              className="w-full flex items-center justify-center px-6 py-3 border border-transparent rounded-md shadow-sm text-base font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? (
                <>
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Processing...
                </>
              ) : (
                <>
                  Book Session
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TutorDetails; 