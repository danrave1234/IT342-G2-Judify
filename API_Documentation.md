# Judify API Documentation

## 1. Create Tutor Profile

**Endpoint Name**: Create Tutor Profile for User
**HTTP Method**: POST
**URL**: `/api/tutors/createProfile/user/{userId}`
**Description**: Creates a new tutor profile for a specific user, automatically updating their role to TUTOR.

**Request Headers**:
- Authorization: Bearer <token>
- Content-Type: application/json

**Request Parameters**:
- userId (Path Variable) - The ID of the user to create a tutor profile for

**Request Body**:
```json
{
    "bio": "Experienced tutor in mathematics and physics",
    "expertise": "Advanced Mathematics, Physics",
    "hourlyRate": 25.00,
    "subjects": ["Mathematics", "Physics", "Chemistry"]
}
```

**Response Example**:
```json
{
    "profileId": 1,
    "userId": 1,
    "username": "johndoe",
    "bio": "Experienced tutor in mathematics and physics",
    "expertise": "Advanced Mathematics, Physics",
    "hourlyRate": 25.00,
    "subjects": ["Mathematics", "Physics", "Chemistry"],
    "rating": 0.0,
    "totalReviews": 0,
    "createdAt": "2024-03-20T10:00:00"
}
```

**Response Codes**:
- 201 Created - Tutor profile successfully created
- 400 Bad Request - Invalid input or user not found
- 401 Unauthorized - Authentication failed
- 500 Internal Server Error - Server error
