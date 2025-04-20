import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

// Context providers
import { UserProvider } from './context/UserContext';
import { TutorProfileProvider } from './context/TutorProfileContext';
import { SessionProvider } from './context/SessionContext';
import { MessageProvider } from './context/MessageContext';
import { NotificationProvider } from './context/NotificationContext';

// Layouts
import AuthLayout from './components/layout/AuthLayout';
import MainLayout from './components/layout/MainLayout';

// Auth pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import ForgotPassword from './pages/auth/ForgotPassword';

// Public pages
import LandingPage from './pages/LandingPage';
import HowItWorks from './pages/HowItWorks';
import FindTutors from './pages/FindTutors';
import TutorDetails from './pages/TutorDetails';
import Pricing from './pages/Pricing';

// Student pages
import StudentDashboard from './pages/student/Dashboard';
import StudentProfile from './pages/student/Profile';
import Subjects from './pages/student/Subjects';
import SessionDetails from './pages/student/SessionDetails';
import BookSession from './pages/student/BookSession';

// Tutor pages
import TutorDashboard from './pages/tutor/Dashboard';
import TutorProfile from './pages/tutor/Profile';
import TutorSessions from './pages/tutor/Sessions';
import TutorSessionDetails from './pages/tutor/SessionDetails';
import TutorAvailability from './pages/tutor/Availability';

// Common pages
import Messages from './pages/Messages';
import NotFound from './pages/NotFound';

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
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />
                    <Route path="/forgot-password" element={<ForgotPassword />} />
                  </Route>
                  
                  {/* Student Routes */}
                  <Route element={<PrivateRoute><StudentRoute><MainLayout /></StudentRoute></PrivateRoute>}>
                    <Route path="/student" element={<StudentDashboard />} />
                    <Route path="/student/profile" element={<StudentProfile />} />
                    <Route path="/student/sessions" element={<Subjects />} />
                    <Route path="/student/sessions/:sessionId" element={<SessionDetails />} />
                    <Route path="/student/book/:tutorId" element={<BookSession />} />
                    <Route path="/student/messages" element={<Messages />} />
                  </Route>
                  
                  {/* Tutor Routes */}
                  <Route element={<PrivateRoute><TutorRoute><MainLayout /></TutorRoute></PrivateRoute>}>
                    <Route path="/tutor" element={<TutorDashboard />} />
                    <Route path="/tutor/profile" element={<TutorProfile />} />
                    <Route path="/tutor/sessions" element={<TutorSessions />} />
                    <Route path="/tutor/sessions/:sessionId" element={<TutorSessionDetails />} />
                    <Route path="/tutor/availability" element={<TutorAvailability />} />
                    <Route path="/tutor/messages" element={<Messages />} />
                  </Route>
                  
                  {/* 404 Route */}
                  <Route path="*" element={<NotFound />} />
                </Routes>
              </NotificationProvider>
            </MessageProvider>
          </SessionProvider>
        </TutorProfileProvider>
      </UserProvider>
    </BrowserRouter>
  );
}

export default App;
