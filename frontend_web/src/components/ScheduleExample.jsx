import React, { useState } from 'react';
import Timetable from './Timetable';

const ScheduleExample = () => {
  // Sample availability data
  const [availabilities, setAvailabilities] = useState([
    { day: 'Monday', startTime: '9:00', endTime: '12:00' },
    { day: 'Monday', startTime: '14:00', endTime: '16:30' },
    { day: 'Tuesday', startTime: '10:30', endTime: '15:00' },
    { day: 'Wednesday', startTime: '8:00', endTime: '11:30' },
    { day: 'Wednesday', startTime: '13:00', endTime: '17:00' },
    { day: 'Thursday', startTime: '9:00', endTime: '18:00' },
    { day: 'Friday', startTime: '12:00', endTime: '16:00' },
    { day: 'Saturday', startTime: '10:00', endTime: '13:00' },
  ]);

  return (
    <div className="schedule-container">
      <h2>Weekly Schedule</h2>
      <p>Green cells indicate available time slots</p>
      <Timetable availabilities={availabilities} />
    </div>
  );
};

export default ScheduleExample; 