"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CalendarMonth = exports.isValidDate = exports.Weekday = void 0;
const tslib_1 = require("tslib");
const react_1 = tslib_1.__importStar(require("react"));
const TextInput_1 = require("../TextInput/TextInput");
const Button_1 = require("../Button/Button");
const Select_1 = require("../Select");
const InputGroup_1 = require("../InputGroup");
const angle_left_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-left-icon'));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const react_styles_1 = require("@patternfly/react-styles");
const calendar_month_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/CalendarMonth/calendar-month"));
const util_1 = require("../../helpers/util");
var Weekday;
(function (Weekday) {
    Weekday[Weekday["Sunday"] = 0] = "Sunday";
    Weekday[Weekday["Monday"] = 1] = "Monday";
    Weekday[Weekday["Tuesday"] = 2] = "Tuesday";
    Weekday[Weekday["Wednesday"] = 3] = "Wednesday";
    Weekday[Weekday["Thursday"] = 4] = "Thursday";
    Weekday[Weekday["Friday"] = 5] = "Friday";
    Weekday[Weekday["Saturday"] = 6] = "Saturday";
})(Weekday = exports.Weekday || (exports.Weekday = {}));
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
const isValidDate = (date) => Boolean(date && !isNaN(date));
exports.isValidDate = isValidDate;
const today = new Date();
const CalendarMonth = (_a) => {
    var { date: dateProp, locale = undefined, monthFormat = date => date.toLocaleDateString(locale, { month: 'long' }), weekdayFormat = date => date.toLocaleDateString(locale, { weekday: 'narrow' }), longWeekdayFormat = date => date.toLocaleDateString(locale, { weekday: 'long' }), dayFormat = date => date.getDate(), weekStart = 0, // Use the American Sunday as a default
    onChange = () => { }, validators = [() => true], className, onSelectToggle = () => { }, rangeStart, prevMonthAriaLabel = 'Previous month', nextMonthAriaLabel = 'Next month', yearInputAriaLabel = 'Select year', cellAriaLabel, isDateFocused = false } = _a, props = tslib_1.__rest(_a, ["date", "locale", "monthFormat", "weekdayFormat", "longWeekdayFormat", "dayFormat", "weekStart", "onChange", "validators", "className", "onSelectToggle", "rangeStart", "prevMonthAriaLabel", "nextMonthAriaLabel", "yearInputAriaLabel", "cellAriaLabel", "isDateFocused"]);
    const longMonths = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map(monthNum => new Date(1990, monthNum)).map(monthFormat);
    const [isSelectOpen, setIsSelectOpen] = react_1.default.useState(false);
    // eslint-disable-next-line prefer-const
    const [focusedDate, setFocusedDate] = react_1.default.useState(() => {
        const initDate = new Date(dateProp);
        if (exports.isValidDate(initDate)) {
            return initDate;
        }
        else {
            if (exports.isValidDate(rangeStart)) {
                return rangeStart;
            }
            else {
                return today;
            }
        }
    });
    const [hoveredDate, setHoveredDate] = react_1.default.useState(new Date(focusedDate));
    const focusRef = react_1.default.useRef();
    const [hiddenMonthId] = react_1.default.useState(util_1.getUniqueId('hidden-month-span'));
    const [shouldFocus, setShouldFocus] = react_1.default.useState(false);
    const isValidated = (date) => validators.every(validator => validator(date));
    const focusedDateValidated = isValidated(focusedDate);
    react_1.useEffect(() => {
        if (exports.isValidDate(dateProp) && !isSameDate(focusedDate, dateProp)) {
            setFocusedDate(dateProp);
        }
        else if (!dateProp) {
            setFocusedDate(today);
        }
    }, [dateProp]);
    react_1.useEffect(() => {
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
    const calendar = react_1.default.useMemo(() => buildCalendar(focusedYear, focusedMonth, weekStart, validators), [
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
    return (react_1.default.createElement("div", Object.assign({ className: react_styles_1.css(calendar_month_1.default.calendarMonth, className) }, props),
        react_1.default.createElement("div", { className: calendar_month_1.default.calendarMonthHeader },
            react_1.default.createElement("div", { className: react_styles_1.css(calendar_month_1.default.calendarMonthHeaderNavControl, calendar_month_1.default.modifiers.prevMonth) },
                react_1.default.createElement(Button_1.Button, { variant: "plain", "aria-label": prevMonthAriaLabel, onClick: () => onMonthClick(prevMonth) },
                    react_1.default.createElement(angle_left_icon_1.default, { "aria-hidden": true }))),
            react_1.default.createElement(InputGroup_1.InputGroup, null,
                react_1.default.createElement("div", { className: calendar_month_1.default.calendarMonthHeaderMonth },
                    react_1.default.createElement("span", { id: hiddenMonthId, hidden: true }, "Month"),
                    react_1.default.createElement(Select_1.Select
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
                        }, variant: "single", selections: monthFormatted }, longMonths.map((longMonth, index) => (react_1.default.createElement(Select_1.SelectOption, { key: index, value: index, isSelected: longMonth === monthFormatted }, longMonth))))),
                react_1.default.createElement("div", { className: calendar_month_1.default.calendarMonthHeaderYear },
                    react_1.default.createElement(TextInput_1.TextInput, { "aria-label": yearInputAriaLabel, type: "number", value: yearFormatted, onChange: year => {
                            const newDate = new Date(focusedDate);
                            newDate.setFullYear(+year);
                            setFocusedDate(newDate);
                            setHoveredDate(newDate);
                            setShouldFocus(false);
                        } }))),
            react_1.default.createElement("div", { className: react_styles_1.css(calendar_month_1.default.calendarMonthHeaderNavControl, calendar_month_1.default.modifiers.nextMonth) },
                react_1.default.createElement(Button_1.Button, { variant: "plain", "aria-label": nextMonthAriaLabel, onClick: () => onMonthClick(nextMonth) },
                    react_1.default.createElement(angle_right_icon_1.default, { "aria-hidden": true })))),
        react_1.default.createElement("table", { className: calendar_month_1.default.calendarMonthCalendar },
            react_1.default.createElement("thead", { className: calendar_month_1.default.calendarMonthDays },
                react_1.default.createElement("tr", null, calendar[0].map(({ date }, index) => (react_1.default.createElement("th", { key: index, className: calendar_month_1.default.calendarMonthDay, scope: "col" },
                    react_1.default.createElement("span", { className: "pf-screen-reader" }, longWeekdayFormat(date)),
                    react_1.default.createElement("span", { "aria-hidden": true }, weekdayFormat(date))))))),
            react_1.default.createElement("tbody", { onKeyDown: onKeyDown }, calendar.map((week, index) => (react_1.default.createElement("tr", { key: index, className: calendar_month_1.default.calendarMonthDatesRow }, week.map(({ date, isValid }, index) => {
                const dayFormatted = dayFormat(date);
                const isToday = isSameDate(date, today);
                const isSelected = exports.isValidDate(dateProp) && isSameDate(date, dateProp);
                const isFocused = isSameDate(date, focusedDate);
                const isAdjacentMonth = date.getMonth() !== focusedDate.getMonth();
                const isRangeStart = exports.isValidDate(rangeStart) && isSameDate(date, rangeStart);
                let isInRange = false;
                let isRangeEnd = false;
                if (exports.isValidDate(rangeStart) && exports.isValidDate(dateProp)) {
                    isInRange = date > rangeStart && date < dateProp;
                    isRangeEnd = isSameDate(date, dateProp);
                }
                else if (exports.isValidDate(rangeStart) && isHoveredDateValid) {
                    if (hoveredDate > rangeStart || isSameDate(hoveredDate, rangeStart)) {
                        isInRange = date > rangeStart && date < hoveredDate;
                        isRangeEnd = isSameDate(date, hoveredDate);
                    }
                    // Don't handle focused dates before start dates for now.
                    // Core would likely need new styles
                }
                return (react_1.default.createElement("td", { key: index, className: react_styles_1.css(calendar_month_1.default.calendarMonthDatesCell, isAdjacentMonth && calendar_month_1.default.modifiers.adjacentMonth, isToday && calendar_month_1.default.modifiers.current, (isSelected || isRangeStart) && calendar_month_1.default.modifiers.selected, !isValid && calendar_month_1.default.modifiers.disabled, (isInRange || isRangeStart || isRangeEnd) && calendar_month_1.default.modifiers.inRange, isRangeStart && calendar_month_1.default.modifiers.startRange, isRangeEnd && calendar_month_1.default.modifiers.endRange) },
                    react_1.default.createElement("button", Object.assign({ className: react_styles_1.css(calendar_month_1.default.calendarMonthDate, isRangeEnd && calendar_month_1.default.modifiers.hover, !isValid && calendar_month_1.default.modifiers.disabled), type: "button", onClick: () => onChange(date), onMouseOver: () => setHoveredDate(date), tabIndex: isFocused ? 0 : -1, disabled: !isValid, "aria-label": cellAriaLabel ? cellAriaLabel(date) : `${dayFormatted} ${monthFormatted} ${yearFormatted}` }, (isFocused && { ref: focusRef })), dayFormatted)));
            }))))))));
};
exports.CalendarMonth = CalendarMonth;
//# sourceMappingURL=CalendarMonth.js.map