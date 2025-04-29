# Backend Structure - Mobile Branch

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
  - BCryptPasswordEncoder bean configured for secure password storage

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/users/authenticate", "/api/users/addUser").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JwtConfig.java

- **Package**: `edu.cit.Judify.config`
- **Purpose**: JWT Configuration for authentication
- **Key Features**:
  - Creates a secure key for JWT token signing using HS256 algorithm

```java
@Configuration
public class JwtConfig {
    @Bean
    public Key jwtSecretKey() {
        return Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
    }
}
```

## User Module

### UserEntity.java

- **Package**: `edu.cit.Judify.User`
- **Table**: `users`
- **Properties**:
  - `userId`: Long (Primary Key, Auto-increment)
  - `username`: String (Unique, Not Null)
  - `email`: String (Unique, Not Null)
  - `password`: String (Stored as password_hash, Not Null)
  - `firstName`: String (Not Null)
  - `lastName`: String (Not Null)
  - `role`: UserRole enum (Not Null)
  - `profilePicture`: String (TEXT)
  - `contactDetails`: String
  - `createdAt`: Date (Not Null, Auto-set)
  - `updatedAt`: Date (Auto-updated)
- **Relationships**:
  - One-to-One with TutorProfileEntity (optional, mapped by "user")
- **Lifecycle Hooks**:
  - `onCreate()`: Sets createdAt and updatedAt timestamps
  - `onUpdate()`: Updates the updatedAt timestamp
- **Validation**:
  - Custom `validate()` method to check required fields

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

#### User Management
- `POST /addUser`: Create a new user
  - Request Body: UserEntity with user details
  - Response: UserDTO of created user
  - Description: Creates a new user account with extensive validation logging

- `GET /findById/{id}`: Get user by ID
  - Path Param: User ID
  - Response: UserDTO or 404 if not found

- `GET /findByEmail/{email}`: Get user by email
  - Path Param: Email address
  - Response: UserDTO or 404 if not found

- `GET /getAllUsers`: Get all users
  - Response: List of UserDTO objects

- `GET /findByRole/{role}`: Get users by role
  - Path Param: Role name (STUDENT, TUTOR, ADMIN)
  - Response: List of UserDTO objects with specified role

- `PUT /updateUser/{id}`: Update user details
  - Path Param: User ID
  - Request Body: UserEntity with updated details
  - Response: Updated UserDTO

- `DELETE /deleteUser/{id}`: Delete a user
  - Path Param: User ID
  - Response: 200 OK or 404 if not found

- `PUT /updateRole/{id}`: Update user role
  - Path Param: User ID
  - Request Body: String with new role
  - Response: Updated UserDTO

#### Authentication
- `POST /authenticate`: Authenticate user
  - Request Params: email, password
  - Response: AuthenticatedUserDTO with user details and JWT token

- `POST /manual-user-create`: Manual user creation with detailed error handling
  - Request Body: UserEntity with user details
  - Response: Created UserDTO or error details

- `POST /register`: Register new user
  - Request Body: Map with user registration details
  - Response: Registered UserDTO or error details

#### Profile Management
- `POST /{userId}/profile-picture`: Upload profile picture
  - Path Param: User ID
  - Request Param: MultipartFile "file"
  - Response: URL of uploaded profile picture

#### OAuth2 Integration
- `GET /oauth2-success`: Handle successful OAuth2 authentication
  - Response: Redirects to frontend with auth token

- `GET /oauth2-failure`: Handle failed OAuth2 authentication
  - Response: Redirects to frontend login page with error

- `GET /check-oauth2-status/{userId}`: Check if user needs to complete OAuth2 registration
  - Path Param: User ID
  - Response: Status object with needsRegistration flag

- `POST /complete-oauth2-registration/{userId}`: Complete OAuth2 user registration
  - Path Param: User ID
  - Request Body: Map with user details (role, etc.)
  - Response: Updated UserDTO

### UserService.java

- **Package**: `edu.cit.Judify.User`
- **Key Operations**:

#### User Management
- `createUser(UserEntity)`: Creates a new user with password hashing
- `getUserById(Long)`: Retrieves a user by ID
- `getUserByEmail(String)`: Retrieves a user by email
- `getAllUsers()`: Retrieves all users
- `getUsersByRole(String)`: Retrieves users by role
- `updateUser(Long, UserEntity)`: Updates an existing user's details
- `deleteUser(Long)`: Deletes a user by ID
- `updateUserRole(Long, String)`: Updates a user's role

#### Authentication
- `authenticateUser(String, String)`: Authenticates user and generates JWT token
- `registerUser(Map<String, Object>)`: Registers a new user with input validation
- `validatePassword(String)`: Validates password strength
- `validateEmail(String)`: Validates email format

#### OAuth2 Operations
- `handleOAuthUser(Map<String, Object>)`: Processes OAuth2 user data
- `isOAuthUserComplete(Long)`: Checks if OAuth2 user has completed registration
- `completeOAuth2Registration(Long, Map<String, Object>)`: Completes OAuth2 user registration

### UserRepository.java

- **Package**: `edu.cit.Judify.User`
- **Interface**: JpaRepository<UserEntity, Long>
- **Custom Methods**:
  - `findByEmail(String)`: Find user by email
  - `findByUsername(String)`: Find user by username
  - `findByRole(UserRole)`: Find users by role

## Tutor Profile Module

### TutorProfileEntity.java

- **Package**: `edu.cit.Judify.TutorProfile`
- **Table**: `tutor_profiles`
- **Properties**:
  - `id`: Long (Primary Key)
  - `user`: UserEntity (One-to-One relationship, Not Null)
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
- **Endpoints**:

#### Profile Retrieval
- `GET /getAllProfiles`: Get all tutor profiles
  - Response: List of TutorProfileDTO objects

- `GET /getAllProfilesPaginated`: Get paginated tutor profiles with filtering
  - Request Params: page, size, expertise, minRate, maxRate, minRating
  - Response: Page of TutorProfileDTO objects

- `GET /findById/{tutorId}`: Get tutor profile by ID
  - Path Param: Tutor profile ID
  - Response: TutorProfileDTO or 404 if not found

- `GET /getUserId/{tutorId}`: Get user ID from tutor ID
  - Path Param: Tutor profile ID
  - Response: User ID or 404 if not found

- `GET /getTutorId/{userId}`: Get tutor ID from user ID
  - Path Param: User ID
  - Response: Tutor ID or 404 if not found

- `GET /findByUserId/{userId}`: Get tutor profile by user ID
  - Path Param: User ID
  - Response: TutorProfileDTO or 404 if not found

- `GET /random`: Get random tutor profiles
  - Request Param: limit (default 10)
  - Response: List of random TutorProfileDTO objects

#### Profile Management
- `POST /createProfile/user/{userId}`: Create a new tutor profile
  - Path Param: User ID
  - Request Body: TutorProfileDTO
  - Response: Created TutorProfileDTO

- `PUT /updateProfile/{id}`: Update a tutor profile
  - Path Param: Tutor profile ID
  - Request Body: TutorProfileDTO with updated details
  - Response: Updated TutorProfileDTO

- `DELETE /deleteProfile/{id}`: Delete a tutor profile
  - Path Param: Tutor profile ID
  - Response: 204 No Content or 404 if not found

- `PUT /updateRating/{id}`: Update tutor rating
  - Path Param: Tutor profile ID
  - Request Param: rating (Double)
  - Response: Updated TutorProfileDTO

- `PUT /updateLocation/{id}`: Update tutor location
  - Path Param: Tutor profile ID
  - Request Params: latitude, longitude
  - Response: Updated TutorProfileDTO

#### Subject Management
- `GET /searchBySubject`: Search tutor profiles by subject
  - Request Param: subject
  - Response: List of TutorProfileDTO objects matching the subject

- `GET /{tutorProfileId}/subjects`: Get subjects for a tutor
  - Path Param: Tutor profile ID
  - Response: List of TutorSubjectDTO objects

- `POST /{tutorProfileId}/subjects`: Add subjects to a tutor profile
  - Path Param: Tutor profile ID
  - Request Body: List of subject strings
  - Response: List of added TutorSubjectDTO objects

- `DELETE /{tutorProfileId}/subjects`: Delete all subjects for a tutor
  - Path Param: Tutor profile ID
  - Response: 204 No Content or 404 if not found

#### Registration
- `POST /register`: Register a new tutor
  - Request Body: TutorRegistrationDTO with user and profile details
  - Response: Created TutorProfileDTO

### TutorProfileService.java

- **Package**: `edu.cit.Judify.TutorProfile`
- **Key Operations**:

#### Profile Retrieval
- `getAllTutorProfiles()`: Gets all tutor profiles
- `getTutorProfileById(Long)`: Gets a profile by ID
- `getTutorProfileByUserId(Long)`: Gets a profile by user ID
- `getUserIdFromTutorId(Long)`: Gets user ID from tutor ID
- `getTutorIdFromUserId(Long)`: Gets tutor ID from user ID
- `getRandomTutorProfiles(int)`: Gets random tutor profiles

#### Profile Management
- `createTutorProfile(TutorProfileDTO)`: Creates a new tutor profile
- `updateTutorProfile(Long, TutorProfileDTO)`: Updates a tutor profile
- `deleteTutorProfile(Long)`: Deletes a tutor profile
- `updateTutorRating(Long, Double)`: Updates a tutor's rating
- `updateTutorLocation(Long, Double, Double)`: Updates a tutor's location

#### Search & Filtering
- `searchTutorProfiles(String)`: Searches profiles by subject
- `getAllTutorProfilesPaginated(...)`: Gets paginated profiles with filtering
- `findByUserId(Long)`: Finds a profile by user ID

#### Tutor Registration
- `registerTutor(TutorRegistrationDTO)`: Registers a new tutor (creates user + profile)

### TutorProfileRepository.java

- **Package**: `edu.cit.Judify.TutorProfile`
- **Interface**: JpaRepository<TutorProfileEntity, Long>, JpaSpecificationExecutor<TutorProfileEntity>
- **Custom Methods**:
  - `findByUser(UserEntity)`: Find profile by user
  - `findByExpertiseLike(String)`: Find profiles by expertise
  - `findByHourlyRateBetween(Double, Double)`: Find profiles by hourly rate range
  - `findByRatingGreaterThanEqual(Double)`: Find profiles by minimum rating
  - `findByUserUserId(Long)`: Find profile by user ID
  - `findBySubjectName(String)`: Find profiles by subject name (JPQL query)
  - `findBySubjectName(String, Pageable)`: Paginated search by subject

## Tutoring Session Module

### TutoringSessionEntity.java

- **Package**: `edu.cit.Judify.TutoringSession`
- **Table**: `tutoring_sessions`
- **Properties**:
  - `sessionId`: Long (Primary Key)
  - `tutor`: UserEntity (Many-to-One, Not Null)
  - `student`: UserEntity (Many-to-One, Not Null)
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
  - `conversation`: ConversationEntity (One-to-One, Optional)
- **Lifecycle Hooks**:
  - `onCreate()`: Sets createdAt and updatedAt timestamps
  - `onUpdate()`: Updates the updatedAt timestamp

### TutoringSessionController.java

- **Package**: `edu.cit.Judify.TutoringSession`
- **Base URL**: `/api/sessions`
- **Endpoints**:

#### Session Management
- `POST /create`: Create a new tutoring session
  - Request Body: TutoringSessionDTO
  - Response: Created TutoringSessionDTO

- `GET /getById/{id}`: Get session by ID
  - Path Param: Session ID
  - Response: TutoringSessionDTO or 404 if not found

- `GET /getByTutor/{tutorId}`: Get sessions by tutor
  - Path Param: Tutor user ID
  - Response: List of TutoringSessionDTO objects

- `GET /getByStudent/{studentId}`: Get sessions by student
  - Path Param: Student user ID
  - Response: List of TutoringSessionDTO objects

- `GET /getBetweenUsers/{tutorId}/{studentId}`: Get sessions between tutor and student
  - Path Params: Tutor user ID, Student user ID
  - Response: List of TutoringSessionDTO objects

- `PUT /update/{id}`: Update a session
  - Path Param: Session ID
  - Request Body: TutoringSessionDTO
  - Response: Updated TutoringSessionDTO

- `DELETE /delete/{id}`: Delete a session
  - Path Param: Session ID
  - Response: 200 OK or 404 if not found

#### Session Status Management
- `PUT /{id}/accept-tutor`: Tutor accepts session
  - Path Param: Session ID
  - Response: Updated TutoringSessionDTO

- `PUT /{id}/accept-student`: Student accepts session
  - Path Param: Session ID
  - Response: Updated TutoringSessionDTO

- `PUT /{id}/reject-tutor`: Tutor rejects session
  - Path Param: Session ID
  - Response: Updated TutoringSessionDTO

- `PUT /{id}/reject-student`: Student rejects session
  - Path Param: Session ID
  - Response: Updated TutoringSessionDTO

- `PUT /{id}/cancel`: Cancel session
  - Path Param: Session ID
  - Response: Updated TutoringSessionDTO

- `PUT /{id}/complete`: Mark session as completed
  - Path Param: Session ID
  - Response: Updated TutoringSessionDTO

#### Filtering & Search
- `GET /search`: Search sessions
  - Request Params: tutorId, studentId, status, fromDate, toDate
  - Response: List of TutoringSessionDTO objects

- `GET /getByStatus/{status}`: Get sessions by status
  - Path Param: Status string
  - Response: List of TutoringSessionDTO objects

- `GET /getByDateRange`: Get sessions in date range
  - Request Params: fromDate, toDate
  - Response: List of TutoringSessionDTO objects

- `GET /upcoming/{userId}`: Get upcoming sessions for user
  - Path Param: User ID
  - Response: List of TutoringSessionDTO objects

- `GET /past/{userId}`: Get past sessions for user
  - Path Param: User ID
  - Response: List of TutoringSessionDTO objects

### TutoringSessionService.java

- **Package**: `edu.cit.Judify.TutoringSession`
- **Key Operations**:

#### Session CRUD
- `createSession(TutoringSessionDTO)`: Creates a new session
- `getSessionById(Long)`: Gets a session by ID
- `updateSession(Long, TutoringSessionDTO)`: Updates a session
- `deleteSession(Long)`: Deletes a session

#### User-Based Retrieval
- `getSessionsByTutor(Long)`: Gets sessions by tutor ID
- `getSessionsByStudent(Long)`: Gets sessions by student ID
- `getSessionsBetweenUsers(Long, Long)`: Gets sessions between tutor and student

#### Status Management
- `acceptSessionByTutor(Long)`: Tutor accepts session
- `acceptSessionByStudent(Long)`: Student accepts session
- `rejectSessionByTutor(Long)`: Tutor rejects session
- `rejectSessionByStudent(Long)`: Student rejects session
- `cancelSession(Long)`: Cancels a session
- `completeSession(Long)`: Marks session as completed

#### Filtering & Search
- `getSessionsByStatus(String)`: Gets sessions by status
- `getSessionsByDateRange(Date, Date)`: Gets sessions in date range
- `searchSessions(...)`: Searches sessions with multiple criteria
- `getUpcomingSessions(Long)`: Gets upcoming sessions for user
- `getPastSessions(Long)`: Gets past sessions for user

### TutoringSessionRepository.java

- **Package**: `edu.cit.Judify.TutoringSession`
- **Interface**: JpaRepository<TutoringSessionEntity, Long>
- **Custom Methods**:
  - `findByTutor_UserId(Long)`: Find sessions by tutor ID
  - `findByStudent_UserId(Long)`: Find sessions by student ID
  - `findByTutor_UserIdAndStudent_UserId(Long, Long)`: Find sessions between tutor and student
  - `findByStatus(String)`: Find sessions by status
  - `findByStartTimeBetween(Date, Date)`: Find sessions in date range
  - `findByEndTimeBefore(Date)`: Find sessions before a date
  - `findByStartTimeAfter(Date)`: Find sessions after a date

## Tutor Availability Module

### TutorAvailabilityEntity.java

- **Package**: `edu.cit.Judify.TutorAvailability`
- **Table**: `tutor_availabilities`
- **Properties**:
  - `availabilityId`: Long (Primary Key)
  - `tutor`: UserEntity (Many-to-One, Not Null)
  - `dayOfWeek`: String (Not Null)
  - `startTime`: String (Not Null)
  - `endTime`: String (Not Null)
  - `additionalNotes`: String
- **Purpose**:
  - Stores tutor availability slots for scheduling

### TutorAvailabilityController.java

- **Package**: `edu.cit.Judify.TutorAvailability`
- **Base URL**: `/api/tutor-availability`
- **Endpoints**:

#### Availability Management
- `POST /createAvailability`: Create a new availability slot
  - Request Body: TutorAvailabilityDTO
  - Response: Created TutorAvailabilityDTO

- `GET /findById/{id}`: Get availability slot by ID
  - Path Param: Availability ID
  - Response: TutorAvailabilityDTO or 404 if not found

- `GET /findByTutor/{tutorId}`: Get tutor's availability slots
  - Path Param: Tutor user ID
  - Response: List of TutorAvailabilityDTO objects

- `GET /findByDay/{dayOfWeek}`: Get availability slots by day
  - Path Param: Day of week string
  - Response: List of TutorAvailabilityDTO objects

- `GET /findByTutorAndDay/{tutorId}/{dayOfWeek}`: Get tutor's availability by day
  - Path Params: Tutor user ID, Day of week string
  - Response: List of TutorAvailabilityDTO objects

- `PUT /updateAvailability/{id}`: Update an availability slot
  - Path Param: Availability ID
  - Request Body: TutorAvailabilityDTO
  - Response: Updated TutorAvailabilityDTO

- `DELETE /deleteAvailability/{id}`: Delete an availability slot
  - Path Param: Availability ID
  - Response: 200 OK or 404 if not found

- `DELETE /deleteAllForTutor/{tutorId}`: Delete all availability for a tutor
  - Path Param: Tutor user ID
  - Response: 200 OK

#### Utility
- `GET /checkAvailability`: Check time slot availability
  - Request Params: tutorId, dayOfWeek, startTime, endTime
  - Response: Boolean indicating availability

### TutorAvailabilityService.java

- **Package**: `edu.cit.Judify.TutorAvailability`
- **Key Operations**:

#### Availability Management
- `createAvailability(TutorAvailabilityEntity)`: Creates a new availability slot
- `getAvailabilityById(Long)`: Gets an availability slot by ID
- `getTutorAvailability(UserEntity)`: Gets availability slots for a tutor
- `getAvailabilityByDay(String)`: Gets availability slots for a day
- `getTutorAvailabilityByDay(UserEntity, String)`: Gets tutor's availability for a day
- `updateAvailability(Long, TutorAvailabilityEntity)`: Updates an availability slot
- `deleteAvailability(Long)`: Deletes an availability slot
- `deleteTutorAvailability(UserEntity)`: Deletes all availability for a tutor

#### Utility
- `isTimeSlotAvailable(...)`: Checks if a time slot is available for a tutor

### TutorAvailabilityRepository.java

- **Package**: `edu.cit.Judify.TutorAvailability`
- **Interface**: JpaRepository<TutorAvailabilityEntity, Long>
- **Custom Methods**:
  - `findByTutor(UserEntity)`: Find availability by tutor
  - `findByDayOfWeek(String)`: Find availability by day
  - `findByTutorAndDayOfWeek(UserEntity, String)`: Find tutor's availability by day

## Conversation Module

### ConversationEntity.java

- **Package**: `edu.cit.Judify.Conversation`
- **Table**: `conversations`
- **Properties**:
  - `conversationId`: Long (Primary Key)
  - `student`: UserEntity (Many-to-One, Not Null)
  - `tutor`: UserEntity (Many-to-One, Not Null)
  - `createdAt`: Date (Not Null, Auto-set)
  - `updatedAt`: Date (Auto-updated)
- **Lifecycle Hooks**:
  - `onCreate()`: Sets createdAt and updatedAt timestamps
  - `onUpdate()`: Updates the updatedAt timestamp

### ConversationController.java

- **Package**: `edu.cit.Judify.Conversation`
- **Base URL**: `/api/conversations`
- **Endpoints**:

#### Conversation Management
- `POST /createConversation`: Create a new conversation or find existing
  - Request Body: ConversationDTO
  - Response: ConversationDTO

- `GET /findBetweenUsers/{userId1}/{userId2}`: Find conversation between users
  - Path Params: First user ID, Second user ID
  - Response: ConversationDTO

- `GET /findById/{id}`: Get conversation by ID
  - Path Param: Conversation ID
  - Response: ConversationDTO or 404 if not found

- `GET /findByUser/{userId}`: Get user's conversations
  - Path Param: User ID
  - Response: List of ConversationDTO objects

- `DELETE /deleteConversation/{id}`: Delete a conversation
  - Path Param: Conversation ID
  - Response: 200 OK or 404 if not found

#### Student-Tutor Specific
- `POST /createWithTutor/{studentUserId}/{tutorId}`: Create conversation with tutor
  - Path Params: Student user ID, Tutor profile ID
  - Response: ConversationDTO

- `PUT /updateNames/{conversationId}`: Update conversation names
  - Path Param: Conversation ID
  - Response: Updated ConversationDTO

### ConversationService.java

- **Package**: `edu.cit.Judify.Conversation`
- **Key Operations**:

#### Conversation Management
- `findOrCreateConversation(UserEntity, UserEntity)`: Finds or creates a conversation
- `findOrCreateConversationByUserIds(Long, Long, UserEntity, UserEntity)`: Finds/creates by user IDs
- `createConversation(ConversationEntity)`: Creates a new conversation
- `getConversationById(Long)`: Gets a conversation by ID
- `getUserConversations(UserEntity)`: Gets conversations for a user
- `deleteConversation(Long)`: Deletes a conversation
- `findOrCreateStudentTutorConversation(UserEntity, UserEntity)`: Finds/creates student-tutor conversation

### ConversationRepository.java

- **Package**: `edu.cit.Judify.Conversation`
- **Interface**: JpaRepository<ConversationEntity, Long>
- **Custom Methods**:
  - `findByStudentOrTutor(UserEntity, UserEntity)`: Find conversations by student or tutor
  - `findConversationBetweenUsers(UserEntity, UserEntity)`: Find conversation between two users

## Configuration Files

### application.properties

- **Location**: `backend/src/main/resources/application.properties`
- **Key Configurations**:
  - **Application Name**: Storix
  - **Database Configuration**:
    - PostgreSQL (Neon) connection
    - JDBC URL with credentials
  - **Hibernate and JPA**:
    - `spring.jpa.show-sql=true`
    - `spring.jpa.hibernate.ddl-auto=update` (auto-creates/updates schema)
  - **Security**:
    - Basic admin credentials
    - Logging level: INFO
  - **Swagger/OpenAPI**:
    - Paths and UI configuration
    - Sorting and filtering options
  - **Email Configuration**:
    - SMTP settings
    - Connection timeouts
  - **OAuth2 Configuration**:
    - Google OAuth2 client settings
    - Scope and redirect configurations

### pom.xml

- **Location**: `backend/pom.xml`
- **Project Info**:
  - GroupId: com
  - ArtifactId: Storix
  - Version: 0.0.1-SNAPSHOT
- **Parent**: Spring Boot Starter Parent (version 3.4.2)
- **Java Version**: 17
- **Key Dependencies**:
  - Spring Boot Starters:
    - Data JPA
    - Web
    - DevTools
    - Security
    - Validation
    - Mail
    - OAuth2 Client
    - WebSocket
  - PostgreSQL Driver (version 42.7.5)
  - JWT Authentication:
    - jjwt-api, jjwt-impl, jjwt-jackson (version 0.11.5)
  - Swagger/OpenAPI:
    - springdoc-openapi-starter-webmvc-ui (version 2.3.0)
  - Google APIs:
    - google-api-client
    - google-oauth-client-jetty
    - google-api-services-calendar
  - Testing:
    - H2 Database
    - Spring Boot Test
    - Spring Security Test