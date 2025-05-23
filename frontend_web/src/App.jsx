import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// Context providers
import { UserProvider } from './context/UserContext';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { NotificationProvider } from './context/NotificationContext';
import { MessageProvider } from './context/MessageContext';
import { TutorProfileProvider } from './context/TutorProfileContext';
import { SessionProvider } from './context/SessionContext';
import { StudentProfileProvider } from './context/StudentProfileContext';
import { PaymentProvider } from './context/PaymentContext';

// Layouts
import AuthLayout from './components/layout/AuthLayout';
import MainLayout from './components/layout/MainLayout';
import GlobalLayout from './components/layout/GlobalLayout';

// Auth pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import ForgotPassword from './pages/auth/ForgotPassword';
import OAuth2Callback from './pages/auth/OAuth2Callback';
import OAuth2Register from './pages/auth/OAuth2Register';

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
import TutorMessages from './pages/tutor/TutorMessages';

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
      <AuthProvider>
        <ThemeProvider>
          <UserProvider>
            <NotificationProvider>
              <MessageProvider>
                <TutorProfileProvider>
                  <SessionProvider>
                    <StudentProfileProvider>
                      <PaymentProvider>
                        <ToastContainer position="top-right" autoClose={3000} />
                        <Routes>
                          {/* Global Layout for all routes except auth */}
                          <Route element={<GlobalLayout />}>
                            {/* Public Routes */}
                            <Route path="/" element={<LandingPage />} />
                            <Route path="/how-it-works" element={<HowItWorks />} />
                            <Route path="/find-tutors" element={<FindTutors />} />
                            <Route path="/tutors/:tutorId" element={<TutorDetails />} />
                            <Route path="/pricing" element={<Pricing />} />

                            {/* OAuth2 Routes */}
                            <Route path="/auth/oauth2-callback" element={<OAuth2Callback />} />
                            <Route path="/oauth2-callback" element={<OAuth2Callback />} />

                            {/* 404 Route */}
                            <Route path="*" element={<NotFound />} />
                          </Route>

                          {/* Auth Routes */}
                          <Route element={<AuthLayout />}>
                            <Route path="/auth/login" element={<Login />} />
                            <Route path="/login" element={<Login />} />
                            <Route path="/auth/register" element={<Register />} />
                            <Route path="/register" element={<Register />} />
                            <Route path="/auth/forgot-password" element={<ForgotPassword />} />
                            <Route path="/forgot-password" element={<ForgotPassword />} />
                            <Route path="/oauth2-register" element={<OAuth2Register />} />
                          </Route>

                          {/* Protected Layout for all authenticated routes */}
                          <Route element={<PrivateRoute><MainLayout /></PrivateRoute>}>
                            {/* Common Routes */}
                            <Route path="/profile" element={<ProfilePage />} />
                            <Route path="/messages/:conversationId" element={<Messages />} />

                            {/* Student Routes */}
                            <Route path="/student" element={<StudentRoute><StudentDashboard /></StudentRoute>} />
                            <Route path="/student/profile" element={<StudentRoute><ProfilePage /></StudentRoute>} />
                            <Route path="/student/find-tutors" element={<StudentRoute><FindTutors /></StudentRoute>} />
                            <Route path="/student/tutors/:tutorId" element={<StudentRoute><TutorDetails /></StudentRoute>} />
                            <Route path="/student/sessions" element={<StudentRoute><StudentSessions /></StudentRoute>} />
                            {/* Redirect session detail route to messages by catching it in Sessions component */}
                            <Route path="/student/sessions/:sessionId" element={<StudentRoute><StudentSessions /></StudentRoute>} />
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
                            <Route path="/tutor/messages" element={<TutorRoute><TutorMessages /></TutorRoute>} />
                            <Route path="/tutor/payments" element={<TutorRoute><TutorPayments /></TutorRoute>} />
                          </Route>
                        </Routes>
                      </PaymentProvider>
                    </StudentProfileProvider>
                  </SessionProvider>
                </TutorProfileProvider>
              </MessageProvider>
            </NotificationProvider>
          </UserProvider>
        </ThemeProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
