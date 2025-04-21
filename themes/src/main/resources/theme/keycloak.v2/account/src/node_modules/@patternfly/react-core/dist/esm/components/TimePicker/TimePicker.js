import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import datePickerStyles from '@patternfly/react-styles/css/components/DatePicker/date-picker';
import formStyles from '@patternfly/react-styles/css/components/FormControl/form-control';
import menuStyles from '@patternfly/react-styles/css/components/Menu/menu';
import { getUniqueId } from '../../helpers';
import { Popper } from '../../helpers/Popper/Popper';
import { Menu, MenuContent, MenuList, MenuItem } from '../Menu';
import { InputGroup } from '../InputGroup';
import { TextInput } from '../TextInput';
import { KeyTypes } from '../../helpers/constants';
import { parseTime, validateTime, makeTimeOptions, amSuffix, pmSuffix, getHours, getMinutes, isWithinMinMax, getSeconds } from './TimePickerUtils';
export class TimePicker extends React.Component {
    constructor(props) {
        super(props);
        this.baseComponentRef = React.createRef();
        this.toggleRef = React.createRef();
        this.inputRef = React.createRef();
        this.menuRef = React.createRef();
        this.onDocClick = (event) => {
            var _a, _b, _c, _d;
            const clickedOnToggle = (_b = (_a = this.toggleRef) === null || _a === void 0 ? void 0 : _a.current) === null || _b === void 0 ? void 0 : _b.contains(event.target);
            const clickedWithinMenu = (_d = (_c = this.menuRef) === null || _c === void 0 ? void 0 : _c.current) === null || _d === void 0 ? void 0 : _d.contains(event.target);
            if (this.state.isTimeOptionsOpen && !(clickedOnToggle || clickedWithinMenu)) {
                this.onToggle(false);
            }
        };
        this.handleGlobalKeys = (event) => {
            var _a, _b, _c, _d;
            const { isTimeOptionsOpen, focusedIndex, scrollIndex } = this.state;
            // keyboard pressed while focus on toggle
            if ((_b = (_a = this.inputRef) === null || _a === void 0 ? void 0 : _a.current) === null || _b === void 0 ? void 0 : _b.contains(event.target)) {
                if (!isTimeOptionsOpen && event.key !== KeyTypes.Tab && event.key !== KeyTypes.Escape) {
                    this.onToggle(true);
                }
                else if (isTimeOptionsOpen) {
                    if (event.key === KeyTypes.Escape || event.key === KeyTypes.Tab) {
                        this.onToggle(false);
                    }
                    else if (event.key === KeyTypes.Enter) {
                        if (focusedIndex !== null) {
                            this.focusSelection(focusedIndex);
                            event.stopPropagation();
                        }
                        else {
                            this.onToggle(false);
                        }
                    }
                    else if (event.key === KeyTypes.ArrowDown || event.key === KeyTypes.ArrowUp) {
                        this.focusSelection(scrollIndex);
                        this.updateFocusedIndex(0);
                        event.preventDefault();
                    }
                }
                // keyboard pressed while focus on menu item
            }
            else if ((_d = (_c = this.menuRef) === null || _c === void 0 ? void 0 : _c.current) === null || _d === void 0 ? void 0 : _d.contains(event.target)) {
                if (event.key === KeyTypes.ArrowDown) {
                    this.updateFocusedIndex(1);
                    event.preventDefault();
                }
                else if (event.key === KeyTypes.ArrowUp) {
                    this.updateFocusedIndex(-1);
                    event.preventDefault();
                }
                else if (event.key === KeyTypes.Escape || event.key === KeyTypes.Tab) {
                    this.inputRef.current.focus();
                    this.onToggle(false);
                }
            }
        };
        this.updateFocusedIndex = (increment) => {
            this.setState(prevState => {
                const maxIndex = this.getOptions().length - 1;
                let nextIndex = prevState.focusedIndex !== null ? prevState.focusedIndex + increment : prevState.scrollIndex + increment;
                if (nextIndex < 0) {
                    nextIndex = maxIndex;
                }
                else if (nextIndex > maxIndex) {
                    nextIndex = 0;
                }
                this.scrollToIndex(nextIndex);
                return {
                    focusedIndex: nextIndex
                };
            });
        };
        // fixes issue where menutAppendTo="inline" results in the menu item that should be scrolled to being out of view; this will select the menu item that comes before the intended one, causing that before-item to be placed out of view instead
        this.getIndexToScroll = (index) => {
            if (this.props.menuAppendTo === 'inline') {
                return index > 0 ? index - 1 : 0;
            }
            return index;
        };
        this.scrollToIndex = (index) => {
            this.getOptions()[index].closest(`.${menuStyles.menuContent}`).scrollTop = this.getOptions()[this.getIndexToScroll(index)].offsetTop;
        };
        this.focusSelection = (index) => {
            var _a;
            const indexToFocus = index !== -1 ? index : 0;
            if ((_a = this.menuRef) === null || _a === void 0 ? void 0 : _a.current) {
                this.getOptions()[indexToFocus].querySelector(`.${menuStyles.menuItem}`).focus();
            }
        };
        this.scrollToSelection = (time) => {
            const { delimiter, is24Hour } = this.props;
            let splitTime = time.split(this.props.delimiter);
            let focusedIndex = null;
            // build out the rest of the time assuming hh:00 if it's a partial time
            if (splitTime.length < 2) {
                time = `${time}${delimiter}00`;
                splitTime = time.split(delimiter);
                // due to only the input including seconds when includeSeconds=true, we need to build a temporary time here without those seconds so that an exact or close match can be scrolled to within the menu (which does not include seconds in any of the options)
            }
            else if (splitTime.length > 2) {
                time = parseTime(time, this.state.timeRegex, delimiter, !is24Hour, false);
                splitTime = time.split(delimiter);
            }
            // for 12hr variant, autoscroll to pm if it's currently the afternoon, otherwise autoscroll to am
            if (!is24Hour && splitTime.length > 1 && splitTime[1].length < 2) {
                const minutes = splitTime[1].length === 0 ? '00' : splitTime[1] + '0';
                time = `${splitTime[0]}${delimiter}${minutes}${new Date().getHours() > 11 ? pmSuffix : amSuffix}`;
            }
            else if (!is24Hour &&
                splitTime.length > 1 &&
                splitTime[1].length === 2 &&
                !time.toUpperCase().includes(amSuffix.toUpperCase().trim()) &&
                !time.toUpperCase().includes(pmSuffix.toUpperCase().trim())) {
                time = `${time}${new Date().getHours() > 11 ? pmSuffix : amSuffix}`;
            }
            let scrollIndex = this.getOptions().findIndex(option => option.innerText === time);
            // if we found an exact match, scroll to match and return index of match for focus
            if (scrollIndex !== -1) {
                this.scrollToIndex(scrollIndex);
                focusedIndex = scrollIndex;
            }
            else if (splitTime.length === 2) {
                // no exact match, scroll to closest hour but don't return index for focus
                let amPm = '';
                if (!is24Hour) {
                    if (splitTime[1].toUpperCase().includes('P')) {
                        amPm = pmSuffix;
                    }
                    else if (splitTime[1].toUpperCase().includes('A')) {
                        amPm = amSuffix;
                    }
                }
                time = `${splitTime[0]}${delimiter}00${amPm}`;
                scrollIndex = this.getOptions().findIndex(option => option.innerText === time);
                if (scrollIndex !== -1) {
                    this.scrollToIndex(scrollIndex);
                }
            }
            this.setState({
                focusedIndex,
                scrollIndex
            });
        };
        this.getRegExp = (includeSeconds = true) => {
            const { is24Hour, delimiter } = this.props;
            let baseRegex = `\\s*(\\d\\d?)${delimiter}([0-5]\\d)`;
            if (includeSeconds) {
                baseRegex += `${delimiter}?([0-5]\\d)?`;
            }
            return new RegExp(`^${baseRegex}${is24Hour ? '' : '\\s*([AaPp][Mm])?'}\\s*$`);
        };
        this.getOptions = () => {
            var _a;
            return (((_a = this.menuRef) === null || _a === void 0 ? void 0 : _a.current)
                ? Array.from(this.menuRef.current.querySelectorAll(`.${menuStyles.menuListItem}`))
                : []);
        };
        this.isValidFormat = (time) => {
            if (this.props.validateTime) {
                return this.props.validateTime(time);
            }
            const { delimiter, is24Hour, includeSeconds } = this.props;
            return validateTime(time, this.getRegExp(includeSeconds), delimiter, !is24Hour);
        };
        this.isValidTime = (time) => {
            const { delimiter, includeSeconds } = this.props;
            const { minTimeState, maxTimeState } = this.state;
            return isWithinMinMax(minTimeState, maxTimeState, time, delimiter, includeSeconds);
        };
        this.isValid = (time) => this.isValidFormat(time) && this.isValidTime(time);
        this.onToggle = (isOpen) => {
            // on close, parse and validate input
            this.setState(prevState => {
                const { timeRegex, isInvalid } = prevState;
                const { delimiter, is24Hour, includeSeconds } = this.props;
                const time = parseTime(prevState.timeState, timeRegex, delimiter, !is24Hour, includeSeconds);
                return {
                    isTimeOptionsOpen: isOpen,
                    timeState: time,
                    isInvalid: isOpen ? isInvalid : !this.isValid(time)
                };
            });
            this.props.setIsOpen(isOpen);
            if (!isOpen) {
                this.inputRef.current.focus();
            }
        };
        this.onSelect = (e) => {
            const { timeRegex, timeState } = this.state;
            const { delimiter, is24Hour, includeSeconds, setIsOpen } = this.props;
            const time = parseTime(e.target.textContent, timeRegex, delimiter, !is24Hour, includeSeconds);
            if (time !== timeState) {
                this.onInputChange(time);
            }
            this.inputRef.current.focus();
            this.setState({
                isTimeOptionsOpen: false,
                isInvalid: false
            });
            setIsOpen(false);
        };
        this.onInputClick = (e) => {
            if (!this.state.isTimeOptionsOpen) {
                this.onToggle(true);
            }
            e.stopPropagation();
        };
        this.onInputChange = (newTime) => {
            const { onChange } = this.props;
            const { timeRegex } = this.state;
            if (onChange) {
                onChange(newTime, getHours(newTime, timeRegex), getMinutes(newTime, timeRegex), getSeconds(newTime, timeRegex), this.isValid(newTime));
            }
            this.scrollToSelection(newTime);
            this.setState({
                timeState: newTime
            });
        };
        this.onBlur = (event) => {
            const { timeRegex } = this.state;
            const { delimiter, is24Hour, includeSeconds } = this.props;
            const time = parseTime(event.currentTarget.value, timeRegex, delimiter, !is24Hour, includeSeconds);
            this.setState({
                isInvalid: !this.isValid(time)
            });
        };
        const { is24Hour, delimiter, time, includeSeconds, isOpen } = this.props;
        let { minTime, maxTime } = this.props;
        if (minTime === '') {
            const minSeconds = includeSeconds ? `${delimiter}00` : '';
            minTime = is24Hour ? `00${delimiter}00${minSeconds}` : `12${delimiter}00${minSeconds} AM`;
        }
        if (maxTime === '') {
            const maxSeconds = includeSeconds ? `${delimiter}59` : '';
            maxTime = is24Hour ? `23${delimiter}59${maxSeconds}` : `11${delimiter}59${maxSeconds} PM`;
        }
        const timeRegex = this.getRegExp();
        this.state = {
            isInvalid: false,
            isTimeOptionsOpen: isOpen,
            timeState: parseTime(time, timeRegex, delimiter, !is24Hour, includeSeconds),
            focusedIndex: null,
            scrollIndex: 0,
            timeRegex,
            minTimeState: parseTime(minTime, timeRegex, delimiter, !is24Hour, includeSeconds),
            maxTimeState: parseTime(maxTime, timeRegex, delimiter, !is24Hour, includeSeconds)
        };
    }
    componentDidMount() {
        document.addEventListener('mousedown', this.onDocClick);
        document.addEventListener('touchstart', this.onDocClick);
        document.addEventListener('keydown', this.handleGlobalKeys);
    }
    componentWillUnmount() {
        document.removeEventListener('mousedown', this.onDocClick);
        document.removeEventListener('touchstart', this.onDocClick);
        document.removeEventListener('keydown', this.handleGlobalKeys);
    }
    componentDidUpdate(prevProps, prevState) {
        const { timeState, isTimeOptionsOpen, isInvalid, timeRegex } = this.state;
        const { time, is24Hour, delimiter, includeSeconds, isOpen } = this.props;
        if (prevProps.isOpen !== isOpen) {
            this.onToggle(isOpen);
        }
        if (isTimeOptionsOpen && !prevState.isTimeOptionsOpen && timeState && !isInvalid) {
            this.scrollToSelection(timeState);
        }
        if (delimiter !== prevProps.delimiter) {
            this.setState({
                timeRegex: this.getRegExp()
            });
        }
        if (time !== '' && time !== prevProps.time) {
            this.setState({
                timeState: parseTime(time, timeRegex, delimiter, !is24Hour, includeSeconds)
            });
        }
    }
    render() {
        const _a = this.props, { 'aria-label': ariaLabel, isDisabled, className, placeholder, id, menuAppendTo, is24Hour, invalidFormatErrorMessage, invalidMinMaxErrorMessage, stepMinutes, width, delimiter, inputProps, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        onChange, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        setIsOpen, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        isOpen, time, validateTime, minTime, maxTime, includeSeconds } = _a, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        props = __rest(_a, ['aria-label', "isDisabled", "className", "placeholder", "id", "menuAppendTo", "is24Hour", "invalidFormatErrorMessage", "invalidMinMaxErrorMessage", "stepMinutes", "width", "delimiter", "inputProps", "onChange", "setIsOpen", "isOpen", "time", "validateTime", "minTime", "maxTime", "includeSeconds"]);
        const { timeState, isTimeOptionsOpen, isInvalid, minTimeState, maxTimeState } = this.state;
        const style = { '--pf-c-date-picker__input--c-form-control--Width': width };
        const options = makeTimeOptions(stepMinutes, !is24Hour, delimiter, minTimeState, maxTimeState, includeSeconds);
        const isValidFormat = this.isValidFormat(timeState);
        const randomId = id || getUniqueId('time-picker');
        const getParentElement = () => {
            if (this.baseComponentRef && this.baseComponentRef.current) {
                return this.baseComponentRef.current.parentElement;
            }
            return null;
        };
        const menuContainer = (React.createElement(Menu, { ref: this.menuRef, isScrollable: true },
            React.createElement(MenuContent, { maxMenuHeight: "200px" },
                React.createElement(MenuList, { "aria-label": ariaLabel }, options.map((option, index) => (React.createElement(MenuItem, { onClick: this.onSelect, key: option, id: `${randomId}-option-${index}` }, option)))))));
        const textInput = (React.createElement(TextInput, Object.assign({ "aria-haspopup": "menu", className: css(formStyles.formControl), id: `${randomId}-input`, "aria-label": ariaLabel, validated: isInvalid ? 'error' : 'default', placeholder: placeholder, value: timeState || '', type: "text", iconVariant: "clock", onClick: this.onInputClick, onChange: this.onInputChange, onBlur: this.onBlur, autoComplete: "off", isDisabled: isDisabled, ref: this.inputRef }, inputProps)));
        return (React.createElement("div", { ref: this.baseComponentRef, className: css(datePickerStyles.datePicker, className) },
            React.createElement("div", Object.assign({ className: css(datePickerStyles.datePickerInput), style: style }, props),
                React.createElement(InputGroup, null,
                    React.createElement("div", { id: randomId },
                        React.createElement("div", { ref: this.toggleRef, style: { paddingLeft: '0' } }, menuAppendTo !== 'inline' ? (React.createElement(Popper, { appendTo: menuAppendTo === 'parent' ? getParentElement() : menuAppendTo, trigger: textInput, popper: menuContainer, isVisible: isTimeOptionsOpen })) : (textInput)),
                        isTimeOptionsOpen && menuAppendTo === 'inline' && menuContainer)),
                isInvalid && (React.createElement("div", { className: css(datePickerStyles.datePickerHelperText, datePickerStyles.modifiers.error) }, !isValidFormat ? invalidFormatErrorMessage : invalidMinMaxErrorMessage)))));
    }
}
TimePicker.displayName = 'TimePicker';
TimePicker.defaultProps = {
    className: '',
    isDisabled: false,
    time: '',
    is24Hour: false,
    invalidFormatErrorMessage: 'Invalid time format',
    invalidMinMaxErrorMessage: 'Invalid time entered',
    placeholder: 'hh:mm',
    delimiter: ':',
    'aria-label': 'Time picker',
    width: '150px',
    menuAppendTo: 'inline',
    stepMinutes: 30,
    inputProps: {},
    minTime: '',
    maxTime: '',
    setIsOpen: () => { }
};
//# sourceMappingURL=TimePicker.js.map