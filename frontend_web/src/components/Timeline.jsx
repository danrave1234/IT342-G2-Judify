import React, { useEffect, useState } from 'react';
import TimelineLib from 'react-calendar-timeline';
import moment from 'moment';
import '../styles/timeline.css';

const Timeline = ({ groups, items, ...props }) => {
  const [timelineKey, setTimelineKey] = useState(Date.now());
  
  // Force re-render when items or groups change
  useEffect(() => {
    setTimelineKey(Date.now());
  }, [items, groups]);

  // Calculate the start of week (Monday) and end of week (Sunday)
  const startOfWeek = moment().startOf('week').add(1, 'day'); // Monday
  const endOfWeek = moment().startOf('week').add(7, 'days'); // Sunday
  
  // Fixed time range for the timeline - full week view, 24 hours
  const defaultTimeStart = props.defaultTimeStart || startOfWeek.clone().hour(0);
  const defaultTimeEnd = props.defaultTimeEnd || startOfWeek.clone().hour(24);

  // Set default props if not provided with fixed one-week view
  const defaultProps = {
    groups: groups || [],
    items: items || [],
    defaultTimeStart,
    defaultTimeEnd,
    
    // Make timeline non-interactive
    canMove: false,
    canResize: false,
    canChangeGroup: false,
    canSelect: false,
    dragSnap: 24 * 60 * 60 * 1000, // Snap to day
    
    // Appearance settings
    stackItems: true,
    itemHeightRatio: 0.9,
    lineHeight: 60,
    sidebarWidth: 120,
    
    // Disable scrolling/zooming
    minZoom: 7 * 24 * 60 * 60 * 1000, // One week minimum zoom
    maxZoom: 7 * 24 * 60 * 60 * 1000, // One week maximum zoom
    
    // Fixed buffer to prevent scrolling issues
    buffer: 1,
    
    // Custom styling
    traditionalZoom: false,
    className: "weekly-timeline",
    
    // Disable interactions
    onCanvasClick: () => {}, // No action on click
    onCanvasDoubleClick: () => {}, // No action on double click
    onItemClick: () => {}, // No action on item click
    onItemSelect: () => {}, // No selection
    onItemContextMenu: () => {}, // No context menu
    
    // Show time grid with proper formatting
    timeSteps: {
      day: 1,
      hour: 4, // Show hours at 4-hour intervals
      minute: 60,
      second: 60
    }
  };

  return (
    <div className="timeline-wrapper">
      <TimelineLib key={timelineKey} {...defaultProps} {...props} />
    </div>
  );
};

export default Timeline; 