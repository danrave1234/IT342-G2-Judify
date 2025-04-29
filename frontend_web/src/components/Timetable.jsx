import React, { useMemo } from 'react';
import moment from 'moment';
import '../assets/css/Timetable.css';

const Timetable = ({ availabilities }) => {
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  
  // Create time slots from 7:00 AM to 10:00 PM with 30-minute intervals
  const timeSlots = useMemo(() => {
    const slots = [];
    for (let hour = 7; hour <= 22; hour++) {
      slots.push(`${hour}:00`);
      if (hour < 22) {
        slots.push(`${hour}:30`);
      }
    }
    return slots;
  }, []);
  
  // Format time slot for display (12-hour format with AM/PM)
  const formatTimeSlot = (timeSlot) => {
    const [hour, minute] = timeSlot.split(':').map(Number);
    const period = hour >= 12 ? 'PM' : 'AM';
    const displayHour = hour % 12 || 12;
    return `${displayHour}:${minute.toString().padStart(2, '0')} ${period}`;
  };
  
  // Map availabilities to day and time slots
  const availabilityMap = useMemo(() => {
    const map = {};
    
    // Initialize all slots to false
    days.forEach(day => {
      map[day] = {};
      timeSlots.forEach(time => {
        map[day][time] = false;
      });
    });
    
    // Set availabilities to true
    if (availabilities && availabilities.length > 0) {
      availabilities.forEach(availability => {
        const { day, startTime, endTime } = availability;
        // Handle both "dayOfWeek" and "day" property names
        const dayOfWeek = day || availability.dayOfWeek;
        
        if (!days.includes(dayOfWeek)) return;
        
        // Handle time formats (with or without leading zeros)
        const formattedStartTime = startTime.length === 4 ? `0${startTime}` : startTime;
        const formattedEndTime = endTime.length === 4 ? `0${endTime}` : endTime;
        
        const start = moment(formattedStartTime, 'HH:mm');
        const end = moment(formattedEndTime, 'HH:mm');
        
        timeSlots.forEach(timeSlot => {
          const [hours, minutes] = timeSlot.split(':').map(Number);
          const slotTime = moment().hours(hours).minutes(minutes);
          
          // Check if the time slot is within the availability period
          if (slotTime.isSameOrAfter(start) && slotTime.isBefore(end)) {
            map[dayOfWeek][timeSlot] = true;
          }
        });
      });
    }
    
    return map;
  }, [availabilities, days, timeSlots]);
  
  return (
    <div className="timetable-container">
      <div className="timetable-grid">
        <div className="timetable-header">
          <div className="header-cell time-column">Time</div>
          {days.map(day => (
            <div key={day} className="header-cell">
              {day}
            </div>
          ))}
        </div>
        
        {timeSlots.map(timeSlot => (
          <div key={timeSlot} className="timetable-row">
            <div className="time-slot">
              {formatTimeSlot(timeSlot)}
            </div>
            {days.map(day => (
              <div
                key={`${day}-${timeSlot}`}
                className={`timetable-cell ${availabilityMap[day][timeSlot] ? 'available' : ''}`}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};

export default Timetable; 