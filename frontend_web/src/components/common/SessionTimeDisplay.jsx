import React from 'react';
import PropTypes from 'prop-types';
import { formatSessionTime } from '../../utils/dateUtils';

/**
 * A component for consistently displaying session times across the application
 * This ensures all session times are displayed uniformly
 */
const SessionTimeDisplay = ({ startTime, endTime, showDate = true, showTime = true, className = '' }) => {
  const { date, time } = formatSessionTime(startTime, endTime);
  
  return (
    <div className={`session-time-display ${className}`}>
      {showDate && (
        <div className="session-date">{date}</div>
      )}
      {showTime && (
        <div className="session-time">{time}</div>
      )}
    </div>
  );
};

SessionTimeDisplay.propTypes = {
  startTime: PropTypes.string.isRequired,
  endTime: PropTypes.string.isRequired,
  showDate: PropTypes.bool,
  showTime: PropTypes.bool,
  className: PropTypes.string
};

export default SessionTimeDisplay; 