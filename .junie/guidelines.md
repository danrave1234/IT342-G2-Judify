# Project Guidelines for Judify

## Project Overview
Judify is a hybrid tutoring platform that connects students with tutors for both online and in-person learning sessions. The platform facilitates session scheduling, real-time communication, payments, and feedback between students and tutors.

## Project Structure
The project is divided into three main components:

### Backend
- Located in the `backend` directory
- Built using Spring Boot framework
- Handles authentication, data storage, and business logic
- Exposes REST APIs for frontend consumption
- Uses Maven for dependency management and building

### Web Frontend
- Located in the `frontend_web` directory
- Built using ReactJS and Tailwind CSS
- Uses Vite as the build tool
- Provides interfaces for students, tutors, and administrators
- Features include user registration, tutor search, session scheduling, video conferencing, and messaging

### Mobile Frontend
- Located in the `frontend_mobile` directory
- Built as an Android application using Kotlin
- Provides mobile-optimized interfaces for the platform
- Features include GPS-based tutor search, push notifications, and mobile payment processing

## Key Technologies
- **Backend**: Java, Spring Boot, Maven
- **Web Frontend**: ReactJS, Tailwind CSS, Vite
- **Mobile Frontend**: Kotlin, Android SDK
- **Database**: NeonDB
- **Authentication**: OAuth2
- **Video Conferencing**: Jitsi Meet API
- **Payment Processing**: Stripe
- **Maps Integration**: Google Maps API
- **Calendar Integration**: Google Calendar API

## Development Guidelines

### General Guidelines
- Follow the existing code structure and naming conventions
- Write clean, maintainable, and well-documented code
- Ensure all changes are properly tested before submission
- Make optimized, efficient, and working fixes
- Update PROGRESS.md whenever a major change is made

### Backend Guidelines
- Follow RESTful API design principles
- Implement proper error handling and validation
- Use DTOs for data transfer between layers
- Ensure proper authorization checks for all endpoints
- Write unit tests for all new functionality

### Web Frontend Guidelines
- Follow component-based architecture
- Use React hooks for state management
- Ensure responsive design for all screen sizes
- Implement proper form validation
- Use environment variables for configuration

### Mobile Frontend Guidelines
- Follow Android development best practices
- Implement proper error handling for network operations
- Ensure efficient resource usage for better performance
- Optimize UI for different screen sizes
- Implement proper caching for offline functionality

## Testing Guidelines
- Write unit tests for all new functionality
- Ensure all tests pass before submitting changes
- Use appropriate mocking for external dependencies
- Test edge cases and error scenarios

## Deployment Guidelines
- Build the project before submitting changes
- Ensure all environment variables are properly configured
- Follow the deployment process specific to each component:
  - Backend: Maven build and deployment to cloud
  - Web Frontend: Vite build and deployment to Vercel
  - Mobile Frontend: Gradle build for APK generation

## Documentation Guidelines
- Update API documentation for any changes to endpoints
- Document complex logic and algorithms
- Keep README files up to date
- Update system architecture documentation for significant changes

By following these guidelines, you'll help maintain the quality and consistency of the Judify platform.