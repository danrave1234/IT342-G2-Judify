/**
 * Date and time utility functions that ensure consistent handling
 * across the application, avoiding timezone issues.
 */

/**
 * Formats a date object or ISO string to a date string without timezone conversion
 * Format: YYYY-MM-DD
 * 
 * @param {Date|string} date - Date object or ISO string
 * @returns {string} Formatted date string
 */
export const formatDateString = (date) => {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  const year = dateObj.getFullYear();
  const month = String(dateObj.getMonth() + 1).padStart(2, '0');
  const day = String(dateObj.getDate()).padStart(2, '0');
  
  return `${year}-${month}-${day}`;
};

/**
 * Formats a date object or ISO string to a time string without timezone conversion
 * Format: HH:MM (24-hour)
 * 
 * @param {Date|string} date - Date object or ISO string
 * @returns {string} Formatted time string
 */
export const formatTimeString = (date) => {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  const hours = String(dateObj.getHours()).padStart(2, '0');
  const minutes = String(dateObj.getMinutes()).padStart(2, '0');
  
  return `${hours}:${minutes}`;
};

/**
 * Formats a date object or ISO string to a date-time string without timezone conversion
 * Format: YYYY-MM-DDTHH:MM:SS
 * 
 * @param {Date|string} date - Date object or ISO string
 * @returns {string} Formatted date-time string
 */
export const formatDateTimeString = (date) => {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  const year = dateObj.getFullYear();
  const month = String(dateObj.getMonth() + 1).padStart(2, '0');
  const day = String(dateObj.getDate()).padStart(2, '0');
  const hours = String(dateObj.getHours()).padStart(2, '0');
  const minutes = String(dateObj.getMinutes()).padStart(2, '0');
  const seconds = String(dateObj.getSeconds()).padStart(2, '0');
  
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
};

/**
 * Formats a date for display to users in readable format
 * 
 * @param {Date|string} date - Date object or ISO string
 * @returns {string} Human readable date string
 */
export const formatDisplayDate = (date) => {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  // Format as: May 12, 2023
  const options = { year: 'numeric', month: 'long', day: 'numeric' };
  return dateObj.toLocaleDateString('en-US', options);
};

/**
 * Formats a time for display to users in readable format
 * 
 * @param {Date|string} date - Date object or ISO string
 * @param {boolean} use12Hour - Whether to use 12-hour format with AM/PM
 * @returns {string} Human readable time string
 */
export const formatDisplayTime = (date, use12Hour = true) => {
  if (!date) return '';
  
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  const options = { 
    hour: '2-digit', 
    minute: '2-digit',
    hour12: use12Hour
  };
  
  return dateObj.toLocaleTimeString('en-US', options);
};

/**
 * Parses a date-time string into a Date object without timezone adjustments
 * 
 * @param {string} dateTimeString - Date-time string in format YYYY-MM-DDTHH:MM:SS
 * @returns {Date} Date object representing the exact time specified
 */
export const parseDateTimeString = (dateTimeString) => {
  if (!dateTimeString) return null;
  
  const [datePart, timePart] = dateTimeString.split('T');
  const [year, month, day] = datePart.split('-').map(Number);
  const [hours, minutes, seconds] = timePart.split(':').map(Number);
  
  // Create date with specified values (months are 0-indexed in JS Date)
  return new Date(year, month - 1, day, hours, minutes, seconds || 0);
};

/**
 * Formats a session time for display, showing date and time range
 * 
 * @param {string} startTime - ISO string representing start time
 * @param {string} endTime - ISO string representing end time
 * @returns {object} Object with formatted date and time properties
 */
export const formatSessionTime = (startTime, endTime) => {
  if (!startTime || !endTime) {
    return { date: 'N/A', time: 'N/A' };
  }
  
  const start = new Date(startTime);
  const end = new Date(endTime);
  
  const dateFormatted = formatDisplayDate(start);
  const startTimeFormatted = formatDisplayTime(start);
  const endTimeFormatted = formatDisplayTime(end);
  
  return {
    date: dateFormatted,
    time: `${startTimeFormatted} - ${endTimeFormatted}`
  };
}; 