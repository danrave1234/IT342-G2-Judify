import React, { useState, useEffect } from 'react';
import '../styles/weeklySchedule.css';

const WeeklySchedule = ({ availabilities = [], selectedDate = new Date(), onTimeSlotClick }) => {
  // Create time slots from 7 AM to 10 PM in 30-minute intervals
  const generateTimeSlots = () => {
    const slots = [];
    for (let hour = 7; hour <= 22; hour++) {
      // Full hour
      slots.push({
        hour,
        minute: 0,
        displayHour: hour > 12 ? hour - 12 : hour,
        period: hour >= 12 ? 'PM' : 'AM',
        isHalfHour: false
      });
      
      // Half hour (except for the last hour which is 10 PM)
      if (hour < 22) {
        slots.push({
          hour,
          minute: 30,
          displayHour: hour > 12 ? hour - 12 : hour,
          period: hour >= 12 ? 'PM' : 'AM',
          isHalfHour: true
        });
      }
    }
    return slots;
  };

  const timeSlots = generateTimeSlots();
  
  // Days of the week
  const days = [
    { name: 'Monday', value: 1 },
    { name: 'Tuesday', value: 2 },
    { name: 'Wednesday', value: 3 },
    { name: 'Thursday', value: 4 },
    { name: 'Friday', value: 5 },
    { name: 'Saturday', value: 6 },
    { name: 'Sunday', value: 0 }
  ];

  // Function to check if a time slot is booked
  const isTimeSlotBooked = (day, hour, minute) => {
    return availabilities.some(availability => {
      const availabilityDate = new Date(availability.date);
      const availabilityDay = availabilityDate.getDay();
      const availabilityHour = availabilityDate.getHours();
      const availabilityMinute = availabilityDate.getMinutes();
      
      return availabilityDay === day && 
             availabilityHour === hour && 
             availabilityMinute === minute;
    });
  };

  // Function to get availability for a specific time slot
  const getAvailabilityForTimeSlot = (day, hour, minute) => {
    return availabilities.find(availability => {
      const availabilityDate = new Date(availability.date);
      const availabilityDay = availabilityDate.getDay();
      const availabilityHour = availabilityDate.getHours();
      const availabilityMinute = availabilityDate.getMinutes();
      
      return availabilityDay === day && 
             availabilityHour === hour && 
             availabilityMinute === minute;
    });
  };

  // Format time for display
  const formatTime = (hour, minute, period) => {
    return `${hour}:${minute === 0 ? '00' : minute} ${period}`;
  };

  // Handle time slot click
  const handleTimeSlotClick = (day, timeSlot) => {
    if (onTimeSlotClick) {
      // Create a date object for the selected time slot
      const date = new Date(selectedDate);
      
      // Calculate the difference between selected day and current day
      const currentDay = date.getDay();
      const dayDiff = day - currentDay;
      
      // Set the date to the correct day
      date.setDate(date.getDate() + dayDiff);
      
      // Set the time
      date.setHours(timeSlot.hour, timeSlot.minute, 0, 0);
      
      onTimeSlotClick(date);
    }
  };

  return (
    <div className="weekly-schedule-container">
      <table className="weekly-schedule">
        <thead>
          <tr>
            <th className="time-header"></th>
            {days.map((day) => (
              <th key={day.value} className="day-header">
                {day.name}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {timeSlots.map((timeSlot, index) => (
            <tr 
              key={`${timeSlot.hour}-${timeSlot.minute}`} 
              className={timeSlot.isHalfHour ? 'half-hour-row' : 'hour-row'}
            >
              <td className="time-cell">
                {!timeSlot.isHalfHour ? (
                  <div className="time-main">
                    {timeSlot.displayHour}:00 {timeSlot.period}
                  </div>
                ) : (
                  <div className="time-main">
                    {timeSlot.displayHour}:30 {timeSlot.period}
                  </div>
                )}
              </td>
              
              {days.map((day) => {
                const isBooked = isTimeSlotBooked(day.value, timeSlot.hour, timeSlot.minute);
                const availability = getAvailabilityForTimeSlot(day.value, timeSlot.hour, timeSlot.minute);
                
                return (
                  <td 
                    key={`${day.value}-${timeSlot.hour}-${timeSlot.minute}`}
                    className={`schedule-cell ${isBooked ? 'booked' : ''}`}
                    onClick={() => handleTimeSlotClick(day.value, timeSlot)}
                  >
                    {isBooked && availability && (
                      <div className="availability-content">
                        Available
                      </div>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default WeeklySchedule; 