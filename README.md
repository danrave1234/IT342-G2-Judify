<div align="center">
Judify: Where Knowledge Meets Convenience

🎓 Connect with tutors anytime, anywhere! 📚
</div>

## 🌟 What is Judify??

Judify is a revolutionary hybrid tutoring platform that bridges the gap between online and offline learning. Whether you prefer face-to-face sessions or the comfort of your own home, Judify has got you covered!

### 🚀 Key Features
#### WEB VIEW
- Tutor Registration & Profile Management
- Session Scheduling & Calendar Sync
- Payment & Billing Automation

#### MOBILE VIEW
-  GPS-Based Tutor Search
-  Real-Time Video Tutoring & Collaboration
-  Session Feedback & Rating System

## 🛠️ Tech Stack
#### FRONTEND
- Tailwind CSS
- ReactJS

#### BACKEND
- Spring Boot Framework
- Jitsi Meet Video Conferencing API

## 👥 Meet the Team!

| Role | Name |
|------|------|
| Team Leader & Mobile Frontend | Danrave Keh |
| Backend Development | Vincent Pacana |
| Frontend Development | Judiel Oppura |

## 🔗 Important Links

- [Figma Designs](your-figma-link-here)
- [System Diagrams](your-diagrams-link-here)

## 🚧 Work in Progress

Stay tuned for more exciting features!

# Judify - Student Profile Implementation

## Recent Changes

We've implemented the Student Profile functionality that was missing from the backend. The following components were added:

### Backend (Java)

1. **Entity**: `StudentProfileEntity` in `backend/src/main/java/edu/cit/Judify/StudentProfile/`
2. **DTOs**: 
   - `StudentProfileDTO`
   - `LocationDTO`
   - `CreateStudentProfileRequest`
3. **Repository**: `StudentProfileRepository` 
4. **Service**: `StudentProfileService`
5. **Controller**: `StudentProfileController` - Provides the API endpoints required by the frontend

### API Endpoints

The following API endpoints were added to match the frontend expectations:

- `GET /api/student-profiles/{id}` - Get a student profile by ID
- `GET /api/student-profiles/user/{userId}` - Get a student profile by user ID
- `POST /api/student-profiles` - Create a new student profile
- `PUT /api/student-profiles/{userId}` - Update an existing student profile
- `DELETE /api/student-profiles/{id}` - Delete a student profile

## How to Test

1. Start the backend server:
   ```
   cd backend
   mvn spring-boot:run
   ```

2. Start the frontend development server:
   ```
   cd frontend_web
   npm run dev
   ```

3. Log in to the application and go to the Student Profile page
4. You should now be able to create, view, and update your student profile

## Troubleshooting

If you still encounter issues with the student profile page:

1. Check the browser console for any error messages
2. Verify that the backend server is running correctly
3. Ensure that the database schema is updated with the new student_profiles table
4. Check that your user account is properly authenticated

<div align="center">
</div>