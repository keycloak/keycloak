import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DatePicker/date-picker';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { TextInput } from '../TextInput/TextInput';
import { Popover } from '../Popover/Popover';
import { InputGroup } from '../InputGroup/InputGroup';
import OutlinedCalendarAltIcon from '@patternfly/react-icons/dist/esm/icons/outlined-calendar-alt-icon';
import { CalendarMonth, isValidDate } from '../CalendarMonth';
import { useImperativeHandle } from 'react';
import { KeyTypes } from '../../helpers';
export const yyyyMMddFormat = (date) => `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date
    .getDate()
    .toString()
    .padStart(2, '0')}`;
const DatePickerBase = (_a, ref) => {
    var { className, locale = undefined, dateFormat = yyyyMMddFormat, dateParse = (val) => val.split('-').length === 3 && new Date(`${val}T00:00:00`), isDisabled = false, placeholder = 'YYYY-MM-DD', value: valueProp = '', 'aria-label': ariaLabel = 'Date picker', buttonAriaLabel = 'Toggle date picker', onChange = () => undefined, onBlur = () => undefined, invalidFormatText = 'Invalid date', helperText, appendTo = 'parent', popoverProps, monthFormat, weekdayFormat, longWeekdayFormat, dayFormat, weekStart, validators = [], rangeStart, style: styleProps = {}, inputProps = {} } = _a, props = __rest(_a, ["className", "locale", "dateFormat", "dateParse", "isDisabled", "placeholder", "value", 'aria-label', "buttonAriaLabel", "onChange", "onBlur", "invalidFormatText", "helperText", "appendTo", "popoverProps", "monthFormat", "weekdayFormat", "longWeekdayFormat", "dayFormat", "weekStart", "validators", "rangeStart", "style", "inputProps"]);
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
        if (isValidDate(newValueDate)) {
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
        if (isValidDate(newValueDate)) {
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
            if (isValidDate(valueDate)) {
                setError(valueDate);
            }
            else {
                setErrorText(invalidFormatText);
            }
        }
    };
    useImperativeHandle(ref, () => ({
        setCalendarOpen: (isOpen) => setPopoverOpen(isOpen),
        toggleCalendar: (setOpen, eventKey) => {
            if (eventKey === KeyTypes.Escape && popoverOpen && !selectOpen) {
                setPopoverOpen(prev => (setOpen !== undefined ? setOpen : !prev));
            }
        },
        isCalendarOpen: popoverOpen
    }), [setPopoverOpen, popoverOpen, selectOpen]);
    const getParentElement = () => datePickerWrapperRef && datePickerWrapperRef.current ? datePickerWrapperRef.current : null;
    return (React.createElement("div", Object.assign({ className: css(styles.datePicker, className), ref: datePickerWrapperRef, style: style }, props),
        React.createElement(Popover, Object.assign({ position: "bottom", bodyContent: React.createElement(CalendarMonth, { date: valueDate, onChange: onDateClick, locale: locale, 
                // Use truthy values of strings
                validators: validators.map(validator => (date) => !validator(date)), onSelectToggle: open => setSelectOpen(open), monthFormat: monthFormat, weekdayFormat: weekdayFormat, longWeekdayFormat: longWeekdayFormat, dayFormat: dayFormat, weekStart: weekStart, rangeStart: rangeStart, isDateFocused: true }), showClose: false, isVisible: popoverOpen, shouldClose: (_1, _2, event) => {
                event = event;
                if (event.key === KeyTypes.Escape && selectOpen) {
                    event.stopPropagation();
                    setSelectOpen(false);
                    return false;
                }
                // Let our button handle toggling
                if (buttonRef.current && buttonRef.current.contains(event.target)) {
                    return false;
                }
                setPopoverOpen(false);
                if (event.key === KeyTypes.Escape && popoverOpen) {
                    event.stopPropagation();
                }
                return true;
            }, withFocusTrap: true, hasNoPadding: true, hasAutoWidth: true, appendTo: appendTo === 'parent' ? getParentElement() : appendTo }, popoverProps),
            React.createElement("div", { className: styles.datePickerInput },
                React.createElement(InputGroup, null,
                    React.createElement(TextInput, Object.assign({ isDisabled: isDisabled, "aria-label": ariaLabel, placeholder: placeholder, validated: errorText ? 'error' : 'default', value: value, onChange: onTextInput, onBlur: onInputBlur, onKeyPress: onKeyPress }, inputProps)),
                    React.createElement("button", { ref: buttonRef, className: css(buttonStyles.button, buttonStyles.modifiers.control), "aria-label": buttonAriaLabel, type: "button", onClick: () => setPopoverOpen(!popoverOpen), disabled: isDisabled },
                        React.createElement(OutlinedCalendarAltIcon, null))))),
        helperText && React.createElement("div", { className: styles.datePickerHelperText }, helperText),
        errorText.trim() && React.createElement("div", { className: css(styles.datePickerHelperText, styles.modifiers.error) }, errorText)));
};
export const DatePicker = React.forwardRef(DatePickerBase);
DatePicker.displayName = 'DatePicker';
//# sourceMappingURL=DatePicker.js.map