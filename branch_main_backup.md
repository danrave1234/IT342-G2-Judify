# Backend Documentation

This is a detailed documentation of the backend codebase for the Judify application. The information provided here will help in resolving merge conflicts between the web and mobile branches.

## Table of Contents
1. [Security Configuration](#security-configuration)
2. [User Module](#user-module)
3. [Tutor Profile Module](#tutor-profile-module)
4. [Tutoring Session Module](#tutoring-session-module)
5. [Tutor Availability Module](#tutor-availability-module)
6. [Conversation Module](#conversation-module)
7. [Configuration Files](#configuration-files)

## Security Configuration

### SecurityConfig.java
- **Package**: `edu.cit.Judify.config`
- **Purpose**: Configures Spring Security settings for the application
- **Key Features**:
  - CSRF protection disabled for development
  - Public endpoints for Swagger UI and authentication
  - All requests are currently permitted for development purposes
  - Password encoder bean (BCrypt) for secure password storage
- **Notes**:
  - For production, the security should be tightened to require authentication for non-public endpoints

### JwtConfig.java
- **Package**: `edu.cit.Judify.config`
- **Purpose**: JWT Configuration for authentication
- **Key Features**:
  - Creates a secure key for JWT token signing using HS256 algorithm

## User Module

### UserEntity.java
- **Package**: `edu.cit.Judify.User`
- **Entity Name**: `users`
- **Properties**:
  - `userId`: Long (Primary Key, Auto-increment)
  - `username`: String (Unique, Not Null)
  - `email`: String (Unique, Not Null)
  - `password`: String (stored as password_hash, Not Null)
  - `firstName`: String (Not Null)
  - `lastName`: String (Not Null)
  - `role`: UserRole enum (Not Null)
  - `profilePicture`: String
  - `contactDetails`: String
  - `createdAt`: Date (Not Null, Auto-set)
  - `updatedAt`: Date (Auto-updated)
- **Relationships**:
  - One-to-One with TutorProfileEntity (optional, only if user is a tutor)
- **Lifecycle Hooks**:
  - `onCreate`: Sets createdAt and updatedAt timestamps
  - `onUpdate`: Updates the updatedAt timestamp

### UserRole.java
- **Package**: `edu.cit.Judify.User`
- **Purpose**: Enum defining possible user roles
- **Values**:
  - `STUDENT`: Student users looking for tutoring
  - `TUTOR`: Tutor users providing tutoring services
  - `ADMIN`: Administrative users

### UserController.java
- **Package**: `edu.cit.Judify.User`
- **Base URL**: `/api/users`
- **Endpoints**:
  - `POST /addUser`: Create a new user
  - `GET /findById/{id}`: Get user by ID
  - `GET /findByEmail/{email}`: Get user by email
  - `GET /find-by-email?email=`: Get user by email (query param)
  - `GET /getAllUsers`: Get all users
  - `GET /findByRole/{role}`: Get users by role
  - `PUT /updateUser/{id}`: Update user details
  - `DELETE /deleteUser/{id}`: Delete a user
  - `PUT /updateRole/{id}`: Update user role
  - `POST /authenticate`: Authenticate user (login)
- **Notes**:
  - All endpoints have proper Swagger annotations
  - Cross-origin requests are allowed from all origins

### UserService.java
- **Package**: `edu.cit.Judify.User`
- **Key Operations**:
  - Creating and managing user accounts
  - Authentication with password verification
  - Role management
  - Token generation for authenticated users

## Tutor Profile Module

### TutorProfileEntity.java
- **Package**: `edu.cit.Judify.TutorProfile`
- **Entity Name**: `tutor_profiles`
- **Properties**:
  - `id`: Long (Primary Key, Auto-increment)
  - `user`: UserEntity (Foreign Key to users, Not Null)
  - `biography`: Text
  - `expertise`: String
  - `hourlyRate`: Double
  - `rating`: Double (defaults to 0.0)
  - `totalReviews`: Integer (defaults to 0)
  - `latitude`: Double
  - `longitude`: Double
  - `createdAt`: Date
- **Relationships**:
  - One-to-One with UserEntity (mandatory)
  - One-to-Many with TutorSubjectEntity (subjects offered by tutor)
- **Helper Methods**:
  - `getSubjects()`: Returns a set of subject names
  - `setSubjects(Set<String>)`: Creates and associates subject entities

### TutorProfileController.java
- **Package**: `edu.cit.Judify.TutorProfile`
- **Base URL**: `/api/tutors`
- **Key Endpoints**:
  - `GET /getAllProfiles`: Get all tutor profiles
  - `GET /getAllProfilesPaginated`: Get paginated tutor profiles with filtering
  - `GET /findById/{tutorId}`: Get tutor profile by ID
  - `GET /getUserId/{tutorId}`: Get user ID associated with tutor ID
  - `GET /getTutorId/{userId}`: Get tutor ID associated with user ID
  - `GET /findByUserId/{userId}`: Get tutor profile by user ID
  - `POST /createProfile/user/{userId}`: Create a new tutor profile
  - `PUT /updateProfile/{id}`: Update a tutor profile
  - `DELETE /deleteProfile/{id}`: Delete a tutor profile
  - `GET /searchBySubject`: Search tutor profiles by subject
  - `PUT /updateRating/{id}`: Update tutor rating
  - `PUT /updateLocation/{id}`: Update tutor location
  - `GET /{tutorProfileId}/subjects`: Get subjects for a tutor
  - `POST /{tutorProfileId}/subjects`: Add subjects to a tutor profile
  - `DELETE /{tutorProfileId}/subjects`: Delete all subjects for a tutor
  - `GET /random`: Get random tutor profiles
  - `POST /register`: Register a new tutor (creates user and profile)
- **Notes**: 
  - Extensive filtering options for tutor search
  - Support for both creating profiles for existing users and new user registration

### TutorProfileService.java
- **Package**: `edu.cit.Judify.TutorProfile`
- **Key Operations**:
  - Create, update, and delete tutor profiles
  - Associate subjects with tutors
  - Search and filter tutors
  - Convert between entity and DTO
  - Handle ratings and location updates
  - Register new tutors (create user + profile)

## Tutoring Session Module

### TutoringSessionEntity.java
- **Package**: `edu.cit.Judify.TutoringSession`
- **Entity Name**: `tutoring_sessions`
- **Properties**:
  - `sessionId`: Long (Primary Key)
  - `tutor`: UserEntity (Foreign Key, Not Null)
  - `student`: UserEntity (Foreign Key, Not Null)
  - `startTime`: Date
  - `endTime`: Date
  - `subject`: String
  - `status`: String
  - `price`: Double
  - `notes`: String
  - `locationData`: String (for in-person sessions)
  - `meetingLink`: String (for online sessions)
  - `createdAt`: Date (Not Null, Auto-set)
  - `updatedAt`: Date (Auto-updated)
  - `tutorAccepted`: Boolean
  - `studentAccepted`: Boolean
  - `conversation`: ConversationEntity (Optional)
- **Lifecycle Hooks**:
  - `onCreate`: Sets createdAt and updatedAt timestamps
  - `onUpdate`: Updates the updatedAt timestamp

### TutoringSessionController.java
- **Package**: `edu.cit.Judify.TutoringSession`
- **Base URL**: `/api/sessions`
- **Key Endpoints**:
  - Endpoints for creating, retrieving, updating, and deleting tutoring sessions
  - Specialized endpoints for session acceptance/rejection by tutors and students
  - Session search by various criteria (tutor, student, status, date range)
  - Status management endpoints

### TutoringSessionService.java
- **Package**: `edu.cit.Judify.TutoringSession`
- **Key Operations**:
  - CRUD operations for tutoring sessions
  - Business logic for session status transitions
  - Filtering and searching sessions
  - Validations for session creation and updates

## Tutor Availability Module

### TutorAvailabilityEntity.java
- **Package**: `edu.cit.Judify.TutorAvailability`
- **Entity Name**: `tutor_availabilities`
- **Properties**:
  - `availabilityId`: Long (Primary Key)
  - `tutor`: UserEntity (Foreign Key, Not Null)
  - `dayOfWeek`: String (Not Null)
  - `startTime`: String (Not Null)
  - `endTime`: String (Not Null)
  - `additionalNotes`: String
- **Purpose**:
  - Stores tutor availability slots for scheduling

### TutorAvailabilityController.java
- **Package**: `edu.cit.Judify.TutorAvailability`
- **Base URL**: `/api/tutor-availability`
- **Key Endpoints**:
  - `POST /createAvailability`: Create a new availability slot
  - `GET /findById/{id}`: Get availability slot by ID
  - `GET /findByTutor/{tutorId}`: Get tutor's availability slots
  - `GET /findByDay/{dayOfWeek}`: Get availability slots by day
  - `GET /findByTutorAndDay/{tutorId}/{dayOfWeek}`: Get tutor's availability by day
  - `PUT /updateAvailability/{id}`: Update an availability slot
  - `DELETE /deleteAvailability/{id}`: Delete an availability slot
  - `DELETE /deleteAllForTutor/{tutorId}`: Delete all availability slots for a tutor
  - `GET /checkAvailability`: Check if a time slot is available for a tutor
- **Notes**:
  - All endpoints properly documented with Swagger annotations
  - Cross-origin requests allowed from all origins

### TutorAvailabilityService.java
- **Package**: `edu.cit.Judify.TutorAvailability`
- **Key Operations**:
  - Creating, retrieving, updating, and deleting availability slots
  - Retrieving availability by tutor and day
  - Checking for time slot conflicts

## Conversation Module

### ConversationEntity.java
- **Package**: `edu.cit.Judify.Conversation`
- **Entity Name**: `conversations`
- **Properties**:
  - `conversationId`: Long (Primary Key)
  - `student`: UserEntity (Foreign Key, Not Null)
  - `tutor`: UserEntity (Foreign Key, Not Null)
  - `createdAt`: Date (Not Null, Auto-set)
  - `updatedAt`: Date (Auto-updated)
- **Lifecycle Hooks**:
  - `onCreate`: Sets createdAt and updatedAt timestamps
  - `onUpdate`: Updates the updatedAt timestamp

### ConversationController.java
- **Package**: `edu.cit.Judify.Conversation`
- **Base URL**: `/api/conversations`
- **Key Endpoints**:
  - `POST /createConversation`: Create a new conversation or find existing
  - `GET /findBetweenUsers/{userId1}/{userId2}`: Find conversation between two users
  - `GET /findById/{id}`: Get conversation by ID
  - `GET /findByUser/{userId}`: Get user's conversations
  - `DELETE /deleteConversation/{id}`: Delete a conversation
  - `POST /createWithTutor/{studentUserId}/{tutorId}`: Create conversation with tutor
  - `PUT /updateNames/{conversationId}`: Update conversation names
- **Notes**:
  - Extensive error handling and debugging logs
  - Specialized endpoint for student-tutor conversations

### ConversationService.java
- **Package**: `edu.cit.Judify.Conversation`
- **Key Operations**:
  - Finding or creating conversations between users
  - CRUD operations for conversations
  - Specialized methods for student-tutor conversations

## Configuration Files

### application.properties
- **Location**: `backend/src/main/resources/application.properties`
- **Key Configurations**:
  - Database Configuration: PostgreSQL (Neon)
  - Hibernate: ddl-auto=update (auto-creates/updates schema)
  - Security: Basic admin credentials
  - Swagger/OpenAPI: Path and UI configuration
  - Error handling: No stack traces in responses

### pom.xml
- **Location**: `backend/pom.xml`
- **Key Dependencies**:
  - Spring Boot Starter (Web, Data JPA, Security)
  - PostgreSQL Driver
  - Spring Boot DevTools
  - JWT Authentication (jjwt)
  - OpenAPI/Swagger Documentation
  - Validation
  - H2 Database (for testing)
- **Java Version**: 17
- **Spring Boot Version**: 3.4.2

## Notable Observations

1. **Student Profile Structure**: There is no StudentProfileEntity in the backend despite a Profile.jsx component in the frontend that makes API calls to `/api/students/profile/${user.id}`. This suggests a potential implementation gap.

2. **UserRole Enum**: Contains STUDENT, TUTOR, and ADMIN roles, but only TutorProfileEntity exists, while no StudentProfileEntity is present.

3. **Security Configuration**: Currently allows all requests for development purposes - should be tightened for production.

4. **Hibernate Auto-DDL**: Set to "update" which automatically creates and updates tables based on entity classes. This might have created the student_profiles table without an explicit entity class.

5. **Frontend-Backend Mismatch**: The frontend web app makes API calls to endpoints that don't appear to exist in the backend (`/api/students/profile/`).