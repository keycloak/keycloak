"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DatePicker = exports.yyyyMMddFormat = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const date_picker_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DatePicker/date-picker"));
const button_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Button/button"));
const TextInput_1 = require("../TextInput/TextInput");
const Popover_1 = require("../Popover/Popover");
const InputGroup_1 = require("../InputGroup/InputGroup");
const outlined_calendar_alt_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/outlined-calendar-alt-icon'));
const CalendarMonth_1 = require("../CalendarMonth");
const react_1 = require("react");
const helpers_1 = require("../../helpers");
const yyyyMMddFormat = (date) => `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date
    .getDate()
    .toString()
    .padStart(2, '0')}`;
exports.yyyyMMddFormat = yyyyMMddFormat;
const DatePickerBase = (_a, ref) => {
    var { className, locale = undefined, dateFormat = exports.yyyyMMddFormat, dateParse = (val) => val.split('-').length === 3 && new Date(`${val}T00:00:00`), isDisabled = false, placeholder = 'YYYY-MM-DD', value: valueProp = '', 'aria-label': ariaLabel = 'Date picker', buttonAriaLabel = 'Toggle date picker', onChange = () => undefined, onBlur = () => undefined, invalidFormatText = 'Invalid date', helperText, appendTo = 'parent', popoverProps, monthFormat, weekdayFormat, longWeekdayFormat, dayFormat, weekStart, validators = [], rangeStart, style: styleProps = {}, inputProps = {} } = _a, props = tslib_1.__rest(_a, ["className", "locale", "dateFormat", "dateParse", "isDisabled", "placeholder", "value", 'aria-label', "buttonAriaLabel", "onChange", "onBlur", "invalidFormatText", "helperText", "appendTo", "popoverProps", "monthFormat", "weekdayFormat", "longWeekdayFormat", "dayFormat", "weekStart", "validators", "rangeStart", "style", "inputProps"]);
    const [value, setValue] = React.useState(valueProp);
    const [valueDate, setValueDate] = React.useState(dateParse(value));
    const [errorText, setErrorText] = React.useState('');
    const [popoverOpen, setPopoverOpen] = React.useState(false);
    const [selectOpen, setSelectOpen] = React.useState(false);
    const [pristine, setPristine] = React.useState(true);
    const widthChars = React.useMemo(() => Math.max(dateFormat(new Date()).length, placeholder.length), [dateFormat]);
    const style = Object.assign({ '--pf-c-date-picker__input--c-form-control--width-chars': widthChars }, styleProps);
    const buttonRef = React.useRef();
    const datePickerWrapperRef = React.useRef();
    React.useEffect(() => {
        setValue(valueProp);
        setValueDate(dateParse(valueProp));
    }, [valueProp]);
    React.useEffect(() => {
        setPristine(!value);
    }, [value]);
    const setError = (date) => setErrorText(validators.map(validator => validator(date)).join('\n') || '');
    const onTextInput = (value) => {
        setValue(value);
        setErrorText('');
        const newValueDate = dateParse(value);
        setValueDate(newValueDate);
        if (CalendarMonth_1.isValidDate(newValueDate)) {
            onChange(value, new Date(newValueDate));
        }
        else {
            onChange(value);
        }
    };
    const onInputBlur = () => {
        if (pristine) {
            return;
        }
        const newValueDate = dateParse(value);
        if (CalendarMonth_1.isValidDate(newValueDate)) {
            onBlur(value, new Date(newValueDate));
            setError(newValueDate);
        }
        else {
            onBlur(value);
            setErrorText(invalidFormatText);
        }
    };
    const onDateClick = (newValueDate) => {
        const newValue = dateFormat(newValueDate);
        setValue(newValue);
        setValueDate(newValueDate);
        setError(newValueDate);
        setPopoverOpen(false);
        onChange(newValue, new Date(newValueDate));
    };
    const onKeyPress = (ev) => {
        if (ev.key === 'Enter' && value) {
            if (CalendarMonth_1.isValidDate(valueDate)) {
                setError(valueDate);
            }
            else {
                setErrorText(invalidFormatText);
            }
        }
    };
    react_1.useImperativeHandle(ref, () => ({
        setCalendarOpen: (isOpen) => setPopoverOpen(isOpen),
        toggleCalendar: (setOpen, eventKey) => {
            if (eventKey === helpers_1.KeyTypes.Escape && popoverOpen && !selectOpen) {
                setPopoverOpen(prev => (setOpen !== undefined ? setOpen : !prev));
            }
        },
        isCalendarOpen: popoverOpen
    }), [setPopoverOpen, popoverOpen, selectOpen]);
    const getParentElement = () => datePickerWrapperRef && datePickerWrapperRef.current ? datePickerWrapperRef.current : null;
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(date_picker_1.default.datePicker, className), ref: datePickerWrapperRef, style: style }, props),
        React.createElement(Popover_1.Popover, Object.assign({ position: "bottom", bodyContent: React.createElement(CalendarMonth_1.CalendarMonth, { date: valueDate, onChange: onDateClick, locale: locale, 
                // Use truthy values of strings
                validators: validators.map(validator => (date) => !validator(date)), onSelectToggle: open => setSelectOpen(open), monthFormat: monthFormat, weekdayFormat: weekdayFormat, longWeekdayFormat: longWeekdayFormat, dayFormat: dayFormat, weekStart: weekStart, rangeStart: rangeStart, isDateFocused: true }), showClose: false, isVisible: popoverOpen, shouldClose: (_1, _2, event) => {
                event = event;
                if (event.key === helpers_1.KeyTypes.Escape && selectOpen) {
                    event.stopPropagation();
                    setSelectOpen(false);
                    return false;
                }
                // Let our button handle toggling
                if (buttonRef.current && buttonRef.current.contains(event.target)) {
                    return false;
                }
                setPopoverOpen(false);
                if (event.key === helpers_1.KeyTypes.Escape && popoverOpen) {
                    event.stopPropagation();
                }
                return true;
            }, withFocusTrap: true, hasNoPadding: true, hasAutoWidth: true, appendTo: appendTo === 'parent' ? getParentElement() : appendTo }, popoverProps),
            React.createElement("div", { className: date_picker_1.default.datePickerInput },
                React.createElement(InputGroup_1.InputGroup, null,
                    React.createElement(TextInput_1.TextInput, Object.assign({ isDisabled: isDisabled, "aria-label": ariaLabel, placeholder: placeholder, validated: errorText ? 'error' : 'default', value: value, onChange: onTextInput, onBlur: onInputBlur, onKeyPress: onKeyPress }, inputProps)),
                    React.createElement("button", { ref: buttonRef, className: react_styles_1.css(button_1.default.button, button_1.default.modifiers.control), "aria-label": buttonAriaLabel, type: "button", onClick: () => setPopoverOpen(!popoverOpen), disabled: isDisabled },
                        React.createElement(outlined_calendar_alt_icon_1.default, null))))),
        helperText && React.createElement("div", { className: date_picker_1.default.datePickerHelperText }, helperText),
        errorText.trim() && React.createElement("div", { className: react_styles_1.css(date_picker_1.default.datePickerHelperText, date_picker_1.default.modifiers.error) }, errorText)));
};
exports.DatePicker = React.forwardRef(DatePickerBase);
exports.DatePicker.displayName = 'DatePicker';
//# sourceMappingURL=DatePicker.js.map