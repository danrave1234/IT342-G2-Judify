import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// Context providers
import { UserProvider } from './context/UserContext';
import { TutorProfileProvider } from './context/TutorProfileContext';
import { SessionProvider } from './context/SessionContext';
import { MessageProvider } from './context/MessageContext';
import { NotificationProvider } from './context/NotificationContext';
import { StudentProfileProvider } from './context/StudentProfileContext';
import { PaymentProvider } from './context/PaymentContext';

// Layouts
import AuthLayout from './components/layout/AuthLayout';
import MainLayout from './components/layout/MainLayout';

// Auth pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import ForgotPassword from './pages/auth/ForgotPassword';
import OAuth2Callback from './pages/auth/OAuth2Callback';

// Public pages
import LandingPage from './pages/LandingPage';
import HowItWorks from './pages/HowItWorks';
import FindTutors from './pages/FindTutors';
import TutorDetails from './pages/TutorDetails';
import Pricing from './pages/Pricing';

// Student pages
import StudentDashboard from './pages/student/Dashboard';
import StudentSessions from './pages/student/Sessions';
import SessionDetail from './pages/student/SessionDetail';
import SessionReview from './pages/student/SessionReview';
import BookSession from './pages/student/BookSession';
import StudentPayments from './pages/student/Payments';

// Tutor pages
import TutorDashboard from './pages/tutor/Dashboard';
import TutorSessions from './pages/tutor/Sessions';
import TutorSessionDetails from './pages/tutor/SessionDetails';
import TutorAvailability from './pages/tutor/Availability';
import TutorPayments from './pages/tutor/Payments';

// Common pages
import Messages from './pages/Messages';
import NotFound from './pages/NotFound';
import ProfilePage from './pages/ProfilePage';

// Guards
import PrivateRoute from './components/guards/PrivateRoute';
import StudentRoute from './components/guards/StudentRoute';
import TutorRoute from './components/guards/TutorRoute';

function App() {
  return (
    <BrowserRouter>
      <UserProvider>
        <TutorProfileProvider>
          <SessionProvider>
            <MessageProvider>
              <NotificationProvider>
                <StudentProfileProvider>
                  <PaymentProvider>
                    <ToastContainer position="top-right" autoClose={3000} />
                    <Routes>
                      {/* Public Routes */}
                      <Route path="/" element={<LandingPage />} />
                      <Route path="/how-it-works" element={<HowItWorks />} />
                      <Route path="/find-tutors" element={<FindTutors />} />
                      <Route path="/tutors/:tutorId" element={<TutorDetails />} />
                      <Route path="/pricing" element={<Pricing />} />
                      
                      {/* Auth Routes */}
                      <Route element={<AuthLayout />}>
                        <Route path="/auth/login" element={<Login />} />
                        <Route path="/login" element={<Login />} />
                        <Route path="/auth/register" element={<Register />} />
                        <Route path="/register" element={<Register />} />
                        <Route path="/auth/forgot-password" element={<ForgotPassword />} />
                        <Route path="/forgot-password" element={<ForgotPassword />} />
                      </Route>
                      
                      {/* OAuth2 Routes */}
                      <Route path="/oauth2-callback" element={<OAuth2Callback />} />
                      
                      {/* Protected Layout for all authenticated routes */}
                      <Route element={<PrivateRoute><MainLayout /></PrivateRoute>}>
                        {/* Common Routes */}
                        <Route path="/profile" element={<ProfilePage />} />
                        
                        {/* Student Routes */}
                        <Route path="/student" element={<StudentRoute><StudentDashboard /></StudentRoute>} />
                        <Route path="/student/profile" element={<StudentRoute><ProfilePage /></StudentRoute>} />
                        <Route path="/student/find-tutors" element={<StudentRoute><FindTutors /></StudentRoute>} />
                        <Route path="/student/tutors/:tutorId" element={<StudentRoute><TutorDetails /></StudentRoute>} />
                        <Route path="/student/sessions" element={<StudentRoute><StudentSessions /></StudentRoute>} />
                        <Route path="/student/sessions/:sessionId" element={<StudentRoute><SessionDetail /></StudentRoute>} />
                        <Route path="/student/review/session/:sessionId" element={<StudentRoute><SessionReview /></StudentRoute>} />
                        <Route path="/student/book/:tutorId" element={<StudentRoute><BookSession /></StudentRoute>} />
                        <Route path="/student/messages" element={<StudentRoute><Messages /></StudentRoute>} />
                        <Route path="/student/payments" element={<StudentRoute><StudentPayments /></StudentRoute>} />
                        
                        {/* Tutor Routes */}
                        <Route path="/tutor" element={<TutorRoute><TutorDashboard /></TutorRoute>} />
                        <Route path="/tutor/profile" element={<TutorRoute><ProfilePage /></TutorRoute>} />
                        <Route path="/tutor/sessions" element={<TutorRoute><TutorSessions /></TutorRoute>} />
                        <Route path="/tutor/sessions/:sessionId" element={<TutorRoute><TutorSessionDetails /></TutorRoute>} />
                        <Route path="/tutor/availability" element={<TutorRoute><TutorAvailability /></TutorRoute>} />
                        <Route path="/tutor/messages" element={<TutorRoute><Messages /></TutorRoute>} />
                        <Route path="/tutor/payments" element={<TutorRoute><TutorPayments /></TutorRoute>} />
                      </Route>
                      
                      {/* 404 Route */}
                      <Route path="*" element={<NotFound />} />
                    </Routes>
                  </PaymentProvider>
                </StudentProfileProvider>
              </NotificationProvider>
            </MessageProvider>
          </SessionProvider>
        </TutorProfileProvider>
      </UserProvider>
    </BrowserRouter>
  );
}

export default App;
