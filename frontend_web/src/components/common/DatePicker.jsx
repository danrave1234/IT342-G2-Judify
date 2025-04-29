import { useEffect } from 'react';
import PropTypes from 'prop-types'; // Import PropTypes
import ReactDatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { startOfDay } from 'date-fns'; // Removed addMonths, parseISO as they are not used here
import '../../styles/custom-datepicker.css';

/**
 * Custom DatePicker component.
 * @param {object} props - Component props.
 * @param {Date[]} props.availableDates - Array of available Date objects.
 * @param {Date | null} props.selectedDate - Currently selected Date object.
 * @param {(date: Date | null) => void} props.onSelectDate - Callback when a date is selected.
 * @param {string} [props.placeholderText="Select a date"] - Placeholder text for the input.
 * @param {Date} [props.minDate] - Minimum selectable date.
 * @param {Date} [props.maxDate] - Maximum selectable date.
 */
const DatePicker = ({
                        availableDates = [],
                        selectedDate,
                        onSelectDate,
                        placeholderText = "Select a date",
                        minDate = startOfDay(new Date()), // Default minDate remains
                        maxDate,
                    }) => {

    // Log received props for debugging
    useEffect(() => {
        console.log("DatePicker received availableDates:", availableDates);
        console.log("DatePicker received selectedDate:", selectedDate);
    }, [availableDates, selectedDate]);

    // Ensure availableDates are Date objects at the start of the day for proper comparison
    const highlightDates = availableDates.map(date => startOfDay(date));

    // Filter function to disable unavailable dates
    const isDateAvailable = (date) => {
        const dateStartOfDay = startOfDay(date);
        const isAvailable = highlightDates.some(availableDate => availableDate.getTime() === dateStartOfDay.getTime());
        return isAvailable;
    };

    DatePicker.propTypes = {
        availableDates: PropTypes.arrayOf(PropTypes.instanceOf(Date)),
        selectedDate: PropTypes.instanceOf(Date), // Can be null, so not isRequired
        onSelectDate: PropTypes.func.isRequired,
        placeholderText: PropTypes.string,
        minDate: PropTypes.instanceOf(Date),
        maxDate: PropTypes.instanceOf(Date),
    };

    return (
        <ReactDatePicker
            selected={selectedDate}
            onChange={onSelectDate}
            filterDate={isDateAvailable}
            highlightDates={highlightDates}
            minDate={minDate}
            maxDate={maxDate}
            placeholderText={placeholderText}
            dateFormat="MMMM d, yyyy"
            className="judify-datepicker-input input"
            calendarClassName="judify-datepicker-calendar"
            popperPlacement="bottom-start"
            showPopperArrow={false}
            // renderDayContents={renderDayContents} // Removed this prop as the function is removed
            aria-label="Select a session date"
        />
    );
};

export default DatePicker;