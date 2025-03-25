import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

// Layouts
import MainLayout from './components/layout/MainLayout';
import AuthLayout from './components/layout/AuthLayout';

// Auth Pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import ForgotPassword from './pages/auth/ForgotPassword';

// Tutor Pages
import TutorDashboard from './pages/tutor/Dashboard';
import TutorProfile from './pages/tutor/Profile';
import TutorSessions from './pages/tutor/Sessions';
import TutorEarnings from './pages/tutor/Earnings';

// Student Pages
import StudentDashboard from './pages/student/Dashboard';
import FindTutors from './pages/student/FindTutors';
import TutorDetails from './pages/student/TutorDetails';
import StudentSessions from './pages/student/Sessions';

// Landing Page
import LandingPage from './pages/LandingPage';

function App() {
  return (
    <Router>
      <ToastContainer position="top-right" autoClose={5000} />
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<LandingPage />} />
        
        {/* Auth Routes */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
        </Route>
        
        {/* Tutor Routes */}
        <Route path="/tutor" element={<MainLayout userType="tutor" />}>
          <Route index element={<TutorDashboard />} />
          <Route path="profile" element={<TutorProfile />} />
          <Route path="sessions" element={<TutorSessions />} />
          <Route path="earnings" element={<TutorEarnings />} />
        </Route>
        
        {/* Student Routes */}
        <Route path="/student" element={<MainLayout userType="student" />}>
          <Route index element={<StudentDashboard />} />
          <Route path="find-tutors" element={<FindTutors />} />
          <Route path="tutor/:id" element={<TutorDetails />} />
          <Route path="sessions" element={<StudentSessions />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
