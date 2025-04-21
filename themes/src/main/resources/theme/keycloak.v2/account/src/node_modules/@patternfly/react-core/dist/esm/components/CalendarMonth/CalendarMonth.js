import { __rest } from "tslib";
import React, { useEffect } from 'react';
import { TextInput } from '../TextInput/TextInput';
import { Button } from '../Button/Button';
import { Select, SelectOption } from '../Select';
import { InputGroup } from '../InputGroup';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/CalendarMonth/calendar-month';
import { getUniqueId } from '../../helpers/util';
export var Weekday;
(function (Weekday) {
    Weekday[Weekday["Sunday"] = 0] = "Sunday";
    Weekday[Weekday["Monday"] = 1] = "Monday";
    Weekday[Weekday["Tuesday"] = 2] = "Tuesday";
    Weekday[Weekday["Wednesday"] = 3] = "Wednesday";
    Weekday[Weekday["Thursday"] = 4] = "Thursday";
    Weekday[Weekday["Friday"] = 5] = "Friday";
    Weekday[Weekday["Saturday"] = 6] = "Saturday";
})(Weekday || (Weekday = {}));
// Must be numeric given current header design
const yearFormat = (date) => date.getFullYear();
const buildCalendar = (year, month, weekStart, validators) => {
    const defaultDate = new Date(year, month);
    const firstDayOfWeek = new Date(defaultDate);
    firstDayOfWeek.setDate(firstDayOfWeek.getDate() - firstDayOfWeek.getDay() + weekStart);
    // We will show a maximum of 6 weeks like Google calendar
    // Assume we just want the numbers for now...
    const calendarWeeks = [];
    for (let i = 0; i < 6; i++) {
        const week = [];
        for (let j = 0; j < 7; j++) {
            const date = new Date(firstDayOfWeek);
            week.push({
                date,
                isValid: validators.every(validator => validator(date))
            });
            firstDayOfWeek.setDate(firstDayOfWeek.getDate() + 1);
        }
        calendarWeeks.push(week);
        if (firstDayOfWeek.getMonth() !== defaultDate.getMonth()) {
            break;
        }
    }
    return calendarWeeks;
};
const isSameDate = (d1, d2) => d1.getFullYear() === d2.getFullYear() && d1.getMonth() === d2.getMonth() && d1.getDate() === d2.getDate();
export const isValidDate = (date) => Boolean(date && !isNaN(date));
const today = new Date();
export const CalendarMonth = (_a) => {
    var { date: dateProp, locale = undefined, monthFormat = date => date.toLocaleDateString(locale, { month: 'long' }), weekdayFormat = date => date.toLocaleDateString(locale, { weekday: 'narrow' }), longWeekdayFormat = date => date.toLocaleDateString(locale, { weekday: 'long' }), dayFormat = date => date.getDate(), weekStart = 0, // Use the American Sunday as a default
    onChange = () => { }, validators = [() => true], className, onSelectToggle = () => { }, rangeStart, prevMonthAriaLabel = 'Previous month', nextMonthAriaLabel = 'Next month', yearInputAriaLabel = 'Select year', cellAriaLabel, isDateFocused = false } = _a, props = __rest(_a, ["date", "locale", "monthFormat", "weekdayFormat", "longWeekdayFormat", "dayFormat", "weekStart", "onChange", "validators", "className", "onSelectToggle", "rangeStart", "prevMonthAriaLabel", "nextMonthAriaLabel", "yearInputAriaLabel", "cellAriaLabel", "isDateFocused"]);
    const longMonths = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map(monthNum => new Date(1990, monthNum)).map(monthFormat);
    const [isSelectOpen, setIsSelectOpen] = React.useState(false);
    // eslint-disable-next-line prefer-const
    const [focusedDate, setFocusedDate] = React.useState(() => {
        const initDate = new Date(dateProp);
        if (isValidDate(initDate)) {
            return initDate;
        }
        else {
            if (isValidDate(rangeStart)) {
                return rangeStart;
            }
            else {
                return today;
            }
        }
    });
    const [hoveredDate, setHoveredDate] = React.useState(new Date(focusedDate));
    const focusRef = React.useRef();
    const [hiddenMonthId] = React.useState(getUniqueId('hidden-month-span'));
    const [shouldFocus, setShouldFocus] = React.useState(false);
    const isValidated = (date) => validators.every(validator => validator(date));
    const focusedDateValidated = isValidated(focusedDate);
    useEffect(() => {
        if (isValidDate(dateProp) && !isSameDate(focusedDate, dateProp)) {
            setFocusedDate(dateProp);
        }
        else if (!dateProp) {
            setFocusedDate(today);
        }
    }, [dateProp]);
    useEffect(() => {
        // Calendar month should not be focused on page load
        // Datepicker should place focus in calendar month when opened
        if ((shouldFocus || isDateFocused) && focusedDateValidated && focusRef.current) {
            focusRef.current.focus();
        }
        else {
            setShouldFocus(true);
        }
    }, [focusedDate, isDateFocused, focusedDateValidated, focusRef]);
    const onMonthClick = (newDate) => {
        setFocusedDate(newDate);
        setHoveredDate(newDate);
        setShouldFocus(false);
    };
    const onKeyDown = (ev) => {
        const newDate = new Date(focusedDate);
        if (ev.key === 'ArrowUp') {
            newDate.setDate(newDate.getDate() - 7);
        }
        else if (ev.key === 'ArrowRight') {
            newDate.setDate(newDate.getDate() + 1);
        }
        else if (ev.key === 'ArrowDown') {
            newDate.setDate(newDate.getDate() + 7);
        }
        else if (ev.key === 'ArrowLeft') {
            newDate.setDate(newDate.getDate() - 1);
        }
        if (newDate.getTime() !== focusedDate.getTime() && isValidated(newDate)) {
            ev.preventDefault();
            setFocusedDate(newDate);
            setHoveredDate(newDate);
            setShouldFocus(true);
        }
    };
    const addMonth = (toAdd) => {
        const newDate = new Date(focusedDate);
        newDate.setMonth(newDate.getMonth() + toAdd);
        return newDate;
    };
    const prevMonth = addMonth(-1);
    const nextMonth = addMonth(1);
    const focusedYear = focusedDate.getFullYear();
    const focusedMonth = focusedDate.getMonth();
    const calendar = React.useMemo(() => buildCalendar(focusedYear, focusedMonth, weekStart, validators), [
        focusedYear,
        focusedMonth,
        weekStart,
        validators
    ]);
    if (!focusedDateValidated) {
        const toFocus = calendar
            .reduce((acc, cur) => [...acc, ...cur], [])
            .filter(({ date, isValid }) => isValid && date.getMonth() === focusedMonth)
            .map(({ date }) => ({ date, days: Math.abs(focusedDate.getTime() - date.getTime()) }))
            .sort((o1, o2) => o1.days - o2.days)
            .map(({ date }) => date)[0];
        if (toFocus) {
            setFocusedDate(toFocus);
            setHoveredDate(toFocus);
        }
    }
    const isHoveredDateValid = isValidated(hoveredDate);
    const monthFormatted = monthFormat(focusedDate);
    const yearFormatted = yearFormat(focusedDate);
    return (React.createElement("div", Object.assign({ className: css(styles.calendarMonth, className) }, props),
        React.createElement("div", { className: styles.calendarMonthHeader },
            React.createElement("div", { className: css(styles.calendarMonthHeaderNavControl, styles.modifiers.prevMonth) },
                React.createElement(Button, { variant: "plain", "aria-label": prevMonthAriaLabel, onClick: () => onMonthClick(prevMonth) },
                    React.createElement(AngleLeftIcon, { "aria-hidden": true }))),
            React.createElement(InputGroup, null,
                React.createElement("div", { className: styles.calendarMonthHeaderMonth },
                    React.createElement("span", { id: hiddenMonthId, hidden: true }, "Month"),
                    React.createElement(Select
                    // Max width with "September"
                    , { 
                        // Max width with "September"
                        width: "140px", "aria-labelledby": hiddenMonthId, isOpen: isSelectOpen, onToggle: () => {
                            setIsSelectOpen(!isSelectOpen);
                            onSelectToggle(!isSelectOpen);
                        }, onSelect: (_ev, monthNum) => {
                            // When we put CalendarMonth in a Popover we want the Popover's onDocumentClick
                            // to see the SelectOption as a child so it doesn't close the Popover.
                            setTimeout(() => {
                                setIsSelectOpen(false);
                                onSelectToggle(false);
                                const newDate = new Date(focusedDate);
                                newDate.setMonth(Number(monthNum));
                                setFocusedDate(newDate);
                                setHoveredDate(newDate);
                                setShouldFocus(false);
                            }, 0);
                        }, variant: "single", selections: monthFormatted }, longMonths.map((longMonth, index) => (React.createElement(SelectOption, { key: index, value: index, isSelected: longMonth === monthFormatted }, longMonth))))),
                React.createElement("div", { className: styles.calendarMonthHeaderYear },
                    React.createElement(TextInput, { "aria-label": yearInputAriaLabel, type: "number", value: yearFormatted, onChange: year => {
                            const newDate = new Date(focusedDate);
                            newDate.setFullYear(+year);
                            setFocusedDate(newDate);
                            setHoveredDate(newDate);
                            setShouldFocus(false);
                        } }))),
            React.createElement("div", { className: css(styles.calendarMonthHeaderNavControl, styles.modifiers.nextMonth) },
                React.createElement(Button, { variant: "plain", "aria-label": nextMonthAriaLabel, onClick: () => onMonthClick(nextMonth) },
                    React.createElement(AngleRightIcon, { "aria-hidden": true })))),
        React.createElement("table", { className: styles.calendarMonthCalendar },
            React.createElement("thead", { className: styles.calendarMonthDays },
                React.createElement("tr", null, calendar[0].map(({ date }, index) => (React.createElement("th", { key: index, className: styles.calendarMonthDay, scope: "col" },
                    React.createElement("span", { className: "pf-screen-reader" }, longWeekdayFormat(date)),
                    React.createElement("span", { "aria-hidden": true }, weekdayFormat(date))))))),
            React.createElement("tbody", { onKeyDown: onKeyDown }, calendar.map((week, index) => (React.createElement("tr", { key: index, className: styles.calendarMonthDatesRow }, week.map(({ date, isValid }, index) => {
                const dayFormatted = dayFormat(date);
                const isToday = isSameDate(date, today);
                const isSelected = isValidDate(dateProp) && isSameDate(date, dateProp);
                const isFocused = isSameDate(date, focusedDate);
                const isAdjacentMonth = date.getMonth() !== focusedDate.getMonth();
                const isRangeStart = isValidDate(rangeStart) && isSameDate(date, rangeStart);
                let isInRange = false;
                let isRangeEnd = false;
                if (isValidDate(rangeStart) && isValidDate(dateProp)) {
                    isInRange = date > rangeStart && date < dateProp;
                    isRangeEnd = isSameDate(date, dateProp);
                }
                else if (isValidDate(rangeStart) && isHoveredDateValid) {
                    if (hoveredDate > rangeStart || isSameDate(hoveredDate, rangeStart)) {
                        isInRange = date > rangeStart && date < hoveredDate;
                        isRangeEnd = isSameDate(date, hoveredDate);
                    }
                    // Don't handle focused dates before start dates for now.
                    // Core would likely need new styles
                }
                return (React.createElement("td", { key: index, className: css(styles.calendarMonthDatesCell, isAdjacentMonth && styles.modifiers.adjacentMonth, isToday && styles.modifiers.current, (isSelected || isRangeStart) && styles.modifiers.selected, !isValid && styles.modifiers.disabled, (isInRange || isRangeStart || isRangeEnd) && styles.modifiers.inRange, isRangeStart && styles.modifiers.startRange, isRangeEnd && styles.modifiers.endRange) },
                    React.createElement("button", Object.assign({ className: css(styles.calendarMonthDate, isRangeEnd && styles.modifiers.hover, !isValid && styles.modifiers.disabled), type: "button", onClick: () => onChange(date), onMouseOver: () => setHoveredDate(date), tabIndex: isFocused ? 0 : -1, disabled: !isValid, "aria-label": cellAriaLabel ? cellAriaLabel(date) : `${dayFormatted} ${monthFormatted} ${yearFormatted}` }, (isFocused && { ref: focusRef })), dayFormatted)));
            }))))))));
};
//# sourceMappingURL=CalendarMonth.js.map