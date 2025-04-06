import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { 
  FaMapMarkerAlt, 
  FaSave, 
  FaSync, 
  FaUser, 
  FaEdit, 
  FaStar, 
  FaGraduationCap, 
  FaSchool, 
  FaHeart,
  FaCamera
} from 'react-icons/fa';
import { useUser } from '../context/UserContext';
import { useStudentProfile } from '../context/StudentProfileContext';
import { useTutorProfile } from '../context/TutorProfileContext';

const ProfilePage = () => {
  const { user, uploadProfilePicture, loading: userLoading } = useUser();
  const { profile: studentProfile, loading: studentLoading, error: studentError, updateProfile: updateStudentProfile, refreshProfile: refreshStudentProfile } = useStudentProfile();
  const { tutorProfile, loading: tutorLoading, updateProfile: updateTutorProfile } = useTutorProfile();
  
  const [activeTab, setActiveTab] = useState('view');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [interests, setInterests] = useState([]);
  const [newInterest, setNewInterest] = useState('');
  const [uploadingProfile, setUploadingProfile] = useState(false);
  
  const { register, handleSubmit, setValue, formState: { errors }, reset } = useForm();
  
  const isTutor = user?.role === 'TUTOR';
  const profile = isTutor ? tutorProfile : studentProfile;
  const loading = isTutor ? tutorLoading : studentLoading || userLoading;
  const error = isTutor ? null : studentError;

  // Set form values when profile data loads
  useEffect(() => {
    if (profile) {
      // Reset form with profile data
      if (isTutor) {
        reset({
          bio: profile.bio || '',
          expertise: profile.expertise || '',
          hourlyRate: profile.hourlyRate || '',
        });
      } else {
        reset({
          bio: profile.bio || '',
          gradeLevel: profile.gradeLevel || '',
          school: profile.school || '',
          location: {
            city: profile.location?.city || '',
            state: profile.location?.state || '',
            country: profile.location?.country || '',
            latitude: profile.location?.latitude || '',
            longitude: profile.location?.longitude || ''
          }
        });
        
        // Set interests
        if (profile.interests && Array.isArray(profile.interests)) {
          setInterests(profile.interests);
        }
      }
    } else if (isTutor) {
      // Set default values for tutor if no profile exists
      reset({
        bio: '',
        expertise: '',
        hourlyRate: '0',
      });
    }
  }, [profile, reset, isTutor]);

  const onSubmit = async (data) => {
    if (!user || !user.userId) {
      toast.error('Unable to update profile - user information missing');
      return;
    }
    
    setIsSubmitting(true);
    
    try {
      if (isTutor) {
        const profileData = {
          ...data,
          userId: user.userId
        };
        
        const result = await updateTutorProfile(profileData);
        
        if (result.success) {
          toast.success('Tutor profile updated successfully');
          setActiveTab('view');
        } else {
          toast.error(result.message || 'Failed to update profile');
        }
      } else {
        const profileData = {
          ...data,
          interests,
          userId: user.userId
        };
        
        const result = await updateStudentProfile(profileData);
        
        if (result.success) {
          toast.success('Profile updated successfully');
          setActiveTab('view');
        } else {
          toast.error(result.message || 'Failed to update profile');
        }
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      toast.error('Failed to update profile');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRefresh = async () => {
    try {
      if (isTutor) {
        // For tutors, use the refresh functionality directly
        window.location.reload(); // Full page reload to ensure all data is fresh
      } else {
        // For students, use the student profile refresh
        await refreshStudentProfile();
        toast.success('Profile refreshed');
      }
    } catch (error) {
      toast.error('Failed to refresh profile');
    }
  };

  const handleAddInterest = () => {
    if (newInterest.trim() === '') {
      toast.error('Please enter an interest');
      return;
    }

    setInterests([...interests, newInterest]);
    setNewInterest('');
  };

  const handleRemoveInterest = (index) => {
    const updatedInterests = [...interests];
    updatedInterests.splice(index, 1);
    setInterests(updatedInterests);
  };

  const handleGetCurrentLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setValue('location.latitude', position.coords.latitude);
          setValue('location.longitude', position.coords.longitude);
          toast.success('Location updated');
        },
        (error) => {
          toast.error('Error getting location: ' + error.message);
        }
      );
    } else {
      toast.error('Geolocation is not supported by this browser');
    }
  };

  // Handle profile picture upload
  const handleProfilePictureUpload = async (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      
      // Validate file type
      const validTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/gif'];
      if (!validTypes.includes(file.type)) {
        toast.error('Please select a valid image file (JPEG, PNG, or GIF)');
        return;
      }
      
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast.error('Image size should be less than 5MB');
        return;
      }
      
      try {
        setUploadingProfile(true);
        const result = await uploadProfilePicture(file);
        if (result.success) {
          toast.success('Profile picture updated successfully');
        } else {
          toast.error(result.message || 'Failed to upload profile picture');
        }
      } catch (error) {
        console.error('Error uploading profile picture:', error);
        toast.error('Failed to upload profile picture');
      } finally {
        setUploadingProfile(false);
      }
    }
  };

  if (!user) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="bg-white shadow rounded-lg p-6 mb-6">
          <h1 className="text-2xl font-bold text-gray-800 mb-6">Profile</h1>
          <p className="text-gray-700">Please log in to view your profile.</p>
        </div>
      </div>
    );
  }
  
  if (loading) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="bg-white shadow rounded-lg p-6 mb-6">
          <h1 className="text-2xl font-bold text-gray-800 mb-6">Profile</h1>
          <div className="flex justify-center items-center h-40">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            <p className="ml-2">Loading profile...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white shadow rounded-lg overflow-hidden">
        {/* Profile Header */}
        <div className="relative h-48 flex items-end">
          {/* Simple Background Instead of Cover Photo */}
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600 to-indigo-700">
          </div>
          
          <div className="absolute bottom-0 left-0 w-full h-24 bg-gradient-to-t from-black opacity-40"></div>
          <div className="relative z-10 px-6 py-4 flex justify-between items-end w-full">
            <div className="flex items-center">
              {/* Profile Picture with Upload Overlay */}
              <div className="w-24 h-24 rounded-full border-4 border-white bg-white overflow-hidden relative">
                {uploadingProfile ? (
                  <div className="w-full h-full flex items-center justify-center bg-gray-200">
                    <div className="w-8 h-8 border-t-4 border-blue-500 border-solid rounded-full animate-spin"></div>
                  </div>
                ) : user?.profileImage ? (
                  <img
                    src={user.profileImage}
                    alt="Profile"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-gray-200">
                    <FaUser className="text-gray-500 text-4xl" />
                  </div>
                )}
                
                {/* Profile Picture Upload Button - Only visible in edit mode */}
                {activeTab === 'edit' && (
                  <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                    <label 
                      htmlFor="profile-upload" 
                      className={`cursor-pointer text-white ${uploadingProfile ? 'cursor-not-allowed opacity-70' : ''}`}
                    >
                      <input
                        type="file"
                        id="profile-upload"
                        accept="image/*"
                        className="hidden"
                        onChange={handleProfilePictureUpload}
                        disabled={uploadingProfile}
                      />
                      {uploadingProfile ? (
                        <div className="w-4 h-4 border-t-2 border-white border-solid rounded-full animate-spin"></div>
                      ) : (
                        <FaCamera className="text-2xl" />
                      )}
                    </label>
                  </div>
                )}
              </div>
              <div className="ml-4 text-white">
                <h1 className="text-2xl font-bold">{user?.firstName} {user?.lastName || user?.email}</h1>
                <p>{user?.username && <span className="text-gray-200">@{user.username}</span>}</p>
                <p>{isTutor ? 'Tutor' : 'Student'}</p>
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleRefresh}
                className="flex items-center px-3 py-1 bg-white text-blue-700 rounded-lg hover:bg-blue-50"
              >
                <FaSync className="mr-1" /> Refresh
              </button>
              <button
                onClick={() => setActiveTab(activeTab === 'view' ? 'edit' : 'view')}
                className="flex items-center px-3 py-1 bg-white text-blue-700 rounded-lg hover:bg-blue-50"
              >
                {activeTab === 'view' ? (
                  <>
                    <FaEdit className="mr-1" /> Edit
                  </>
                ) : (
                  <>
                    Cancel
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Profile Content */}
        <div className="p-6">
          {error && (
            <div className="mb-6 p-4 bg-red-50 text-red-700 rounded-lg">
              <p className="font-medium">Error loading profile</p>
              <p className="text-sm">{error}</p>
              <p className="text-sm mt-2">
                User ID: {user?.userId || 'Missing'}
              </p>
            </div>
          )}

          {isTutor && (!profile || Object.keys(profile).length === 0) && activeTab === 'view' && (
            <div className="mb-6 p-4 bg-yellow-50 text-yellow-700 rounded-lg">
              <p className="font-medium">No tutor profile found</p>
              <p className="text-sm">Please click the Edit button to create your tutor profile.</p>
              <button
                onClick={() => setActiveTab('edit')}
                className="mt-2 px-3 py-1 bg-yellow-100 text-yellow-800 rounded hover:bg-yellow-200"
              >
                Create Profile
              </button>
            </div>
          )}

          {activeTab === 'view' ? (
            /* View Profile */
            <div className="space-y-8">
              {/* Bio Section */}
              <div>
                <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                  <FaUser className="mr-2 text-blue-600" /> About Me
                </h2>
                <p className="text-gray-700 whitespace-pre-line">{profile?.bio || 'No bio provided'}</p>
              </div>
              
              {isTutor ? (
                /* Tutor Specific Info */
                <>
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaGraduationCap className="mr-2 text-blue-600" /> Expertise
                    </h2>
                    <p className="text-gray-700">{profile?.expertise || 'No expertise specified'}</p>
                  </div>
                  
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaStar className="mr-2 text-blue-600" /> Rating
                    </h2>
                    <div className="flex items-center">
                      <div className="flex items-center mr-2">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <FaStar
                            key={i}
                            className={`${
                              i < Math.floor(profile?.rating || 0)
                                ? 'text-yellow-400'
                                : 'text-gray-300'
                            }`}
                          />
                        ))}
                      </div>
                      <span className="text-gray-700">{profile?.rating || 0} ({profile?.totalReviews || 0} reviews)</span>
                    </div>
                  </div>
                  
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaSchool className="mr-2 text-blue-600" /> Hourly Rate
                    </h2>
                    <p className="text-gray-700">${profile?.hourlyRate || 0}/hour</p>
                  </div>
                  
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaGraduationCap className="mr-2 text-blue-600" /> Subjects
                    </h2>
                    <div className="flex flex-wrap gap-2">
                      {profile?.subjects?.length > 0 ? (
                        profile.subjects.map((subject, index) => (
                          <span key={index} className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full">
                            {subject}
                          </span>
                        ))
                      ) : (
                        <p className="text-gray-500">No subjects specified</p>
                      )}
                    </div>
                  </div>
                </>
              ) : (
                /* Student Specific Info */
                <>
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaGraduationCap className="mr-2 text-blue-600" /> Education
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <h3 className="text-gray-600 font-medium">Grade Level</h3>
                        <p className="text-gray-700">{profile?.gradeLevel || 'Not specified'}</p>
                      </div>
                      <div>
                        <h3 className="text-gray-600 font-medium">School</h3>
                        <p className="text-gray-700">{profile?.school || 'Not specified'}</p>
                      </div>
                    </div>
                  </div>
                  
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaMapMarkerAlt className="mr-2 text-blue-600" /> Location
                    </h2>
                    <div>
                      {profile?.location ? (
                        <p className="text-gray-700">
                          {[
                            profile.location.city,
                            profile.location.state,
                            profile.location.country
                          ]
                            .filter(Boolean)
                            .join(', ')}
                        </p>
                      ) : (
                        <p className="text-gray-500">No location specified</p>
                      )}
                    </div>
                  </div>
                  
                  <div>
                    <h2 className="text-xl font-bold text-gray-800 mb-2 flex items-center">
                      <FaHeart className="mr-2 text-blue-600" /> Interests
                    </h2>
                    <div className="flex flex-wrap gap-2">
                      {profile?.interests?.length > 0 ? (
                        profile.interests.map((interest, index) => (
                          <span key={index} className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full">
                            {interest}
                          </span>
                        ))
                      ) : (
                        <p className="text-gray-500">No interests specified</p>
                      )}
                    </div>
                  </div>
                </>
              )}
            </div>
          ) : (
            /* Edit Profile */
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Bio section */}
              <div>
                <label htmlFor="bio" className="block text-sm font-medium text-gray-700 mb-1">Bio</label>
                <textarea
                  id="bio"
                  rows="4"
                  {...register('bio')}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  placeholder="Tell us about yourself..."
                ></textarea>
              </div>

              {isTutor ? (
                /* Tutor Form Fields */
                <>
                  <div>
                    <label htmlFor="expertise" className="block text-sm font-medium text-gray-700 mb-1">Expertise</label>
                    <input
                      type="text"
                      id="expertise"
                      {...register('expertise')}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Your areas of expertise"
                    />
                  </div>
                  
                  <div>
                    <label htmlFor="hourlyRate" className="block text-sm font-medium text-gray-700 mb-1">Hourly Rate ($)</label>
                    <input
                      type="number"
                      id="hourlyRate"
                      min="0"
                      step="0.01"
                      {...register('hourlyRate')}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      placeholder="Your hourly rate"
                    />
                  </div>
                </>
              ) : (
                /* Student Form Fields */
                <>
                  {/* Education section */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label htmlFor="gradeLevel" className="block text-sm font-medium text-gray-700 mb-1">Grade Level</label>
                      <input
                        type="text"
                        id="gradeLevel"
                        {...register('gradeLevel')}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        placeholder="e.g., Freshman, Sophomore, etc."
                      />
                    </div>
                    <div>
                      <label htmlFor="school" className="block text-sm font-medium text-gray-700 mb-1">School</label>
                      <input
                        type="text"
                        id="school"
                        {...register('school')}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        placeholder="Your school or university name"
                      />
                    </div>
                  </div>

                  {/* Location section */}
                  <div>
                    <div className="flex justify-between items-center mb-2">
                      <h3 className="text-lg font-medium text-gray-800">Location</h3>
                      <button
                        type="button"
                        onClick={handleGetCurrentLocation}
                        className="flex items-center text-sm text-blue-600 hover:text-blue-800"
                      >
                        <FaMapMarkerAlt className="mr-1" /> Use current location
                      </button>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                      <div>
                        <label htmlFor="location.city" className="block text-sm font-medium text-gray-700 mb-1">City</label>
                        <input
                          type="text"
                          id="location.city"
                          {...register('location.city')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                      <div>
                        <label htmlFor="location.state" className="block text-sm font-medium text-gray-700 mb-1">State/Province</label>
                        <input
                          type="text"
                          id="location.state"
                          {...register('location.state')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                      <div>
                        <label htmlFor="location.country" className="block text-sm font-medium text-gray-700 mb-1">Country</label>
                        <input
                          type="text"
                          id="location.country"
                          {...register('location.country')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        />
                      </div>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label htmlFor="location.latitude" className="block text-sm font-medium text-gray-700 mb-1">Latitude</label>
                        <input
                          type="text"
                          id="location.latitude"
                          {...register('location.latitude')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                          readOnly
                        />
                      </div>
                      <div>
                        <label htmlFor="location.longitude" className="block text-sm font-medium text-gray-700 mb-1">Longitude</label>
                        <input
                          type="text"
                          id="location.longitude"
                          {...register('location.longitude')}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                          readOnly
                        />
                      </div>
                    </div>
                  </div>

                  {/* Interests section */}
                  <div>
                    <h3 className="text-lg font-medium text-gray-800 mb-2">Interests</h3>
                    <div className="flex mb-2">
                      <input
                        type="text"
                        value={newInterest}
                        onChange={(e) => setNewInterest(e.target.value)}
                        className="flex-1 px-3 py-2 border border-gray-300 rounded-l-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                        placeholder="Add an interest (e.g., Programming, Music, etc.)"
                      />
                      <button
                        type="button"
                        onClick={handleAddInterest}
                        className="px-4 py-2 bg-blue-600 text-white rounded-r-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                      >
                        Add
                      </button>
                    </div>
                    <div className="flex flex-wrap gap-2 mt-2">
                      {interests.map((interest, index) => (
                        <div key={index} className="flex items-center bg-blue-100 text-blue-800 rounded-full px-3 py-1">
                          <span>{interest}</span>
                          <button
                            type="button"
                            onClick={() => handleRemoveInterest(index)}
                            className="ml-2 text-blue-600 hover:text-blue-800 focus:outline-none"
                          >
                            &times;
                          </button>
                        </div>
                      ))}
                      {interests.length === 0 && (
                        <p className="text-gray-500 text-sm">No interests added yet.</p>
                      )}
                    </div>
                  </div>
                </>
              )}

              {/* Submit button */}
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                >
                  {isSubmitting ? (
                    <>
                      <div className="animate-spin h-4 w-4 mr-2 border-2 border-white border-t-transparent rounded-full"></div>
                      Saving...
                    </>
                  ) : (
                    <>
                      <FaSave className="mr-2" /> Save Profile
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProfilePage; 