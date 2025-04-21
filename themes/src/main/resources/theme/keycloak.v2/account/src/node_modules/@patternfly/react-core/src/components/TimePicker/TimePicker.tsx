import * as React from 'react';
import { css } from '@patternfly/react-styles';
import datePickerStyles from '@patternfly/react-styles/css/components/DatePicker/date-picker';
import formStyles from '@patternfly/react-styles/css/components/FormControl/form-control';
import menuStyles from '@patternfly/react-styles/css/components/Menu/menu';
import { getUniqueId } from '../../helpers';
import { Popper } from '../../helpers/Popper/Popper';
import { Menu, MenuContent, MenuList, MenuItem } from '../Menu';
import { InputGroup } from '../InputGroup';
import { TextInput, TextInputProps } from '../TextInput';
import { KeyTypes } from '../../helpers/constants';
import {
  parseTime,
  validateTime,
  makeTimeOptions,
  amSuffix,
  pmSuffix,
  getHours,
  getMinutes,
  isWithinMinMax,
  getSeconds
} from './TimePickerUtils';

export interface TimePickerProps
  extends Omit<React.HTMLProps<HTMLDivElement>, 'onChange' | 'onFocus' | 'onBlur' | 'disabled' | 'ref'> {
  /** Additional classes added to the time picker. */
  className?: string;
  /** Accessible label for the time picker */
  'aria-label'?: string;
  /** Flag indicating the time picker is disabled */
  isDisabled?: boolean;
  /** String to display in the empty time picker field as a hint for the expected time format */
  placeholder?: string;
  /** Character to display between the hour and minute */
  delimiter?: string;
  /** A time string. The format could be  an ISO 8601 formatted date string or in 'HH{delimiter}MM' format */
  time?: string | Date;
  /** Error message to display when the time is provided in an invalid format. */
  invalidFormatErrorMessage?: string;
  /** Error message to display when the time provided is not within the minTime/maxTime constriants */
  invalidMinMaxErrorMessage?: string;
  /** True if the time is 24 hour time. False if the time is 12 hour time */
  is24Hour?: boolean;
  /** Optional event handler called each time the value in the time picker input changes. */
  onChange?: (time: string, hour?: number, minute?: number, seconds?: number, isValid?: boolean) => void;
  /** Optional validator can be provided to override the internal time validator. */
  validateTime?: (time: string) => boolean;
  /** Id of the time picker */
  id?: string;
  /** Width of the time picker. */
  width?: string;
  /** The container to append the menu to. Defaults to 'inline'.
   * If your menu is being cut off you can append it to an element higher up the DOM tree.
   * Some examples:
   * menuAppendTo="parent"
   * menuAppendTo={() => document.body}
   * menuAppendTo={document.getElementById('target')}
   */
  menuAppendTo?: HTMLElement | (() => HTMLElement) | 'inline' | 'parent';
  /** Size of step between time options in minutes.*/
  stepMinutes?: number;
  /** Additional props for input field */
  inputProps?: TextInputProps;
  /** A time string indicating the minimum value allowed. The format could be an ISO 8601 formatted date string or in 'HH{delimiter}MM' format */
  minTime?: string | Date;
  /** A time string indicating the maximum value allowed. The format could be an ISO 8601 formatted date string or in 'HH{delimiter}MM' format */
  maxTime?: string | Date;
  /** Includes number of seconds with the chosen time and allows users to manually edit the seconds value. */
  includeSeconds?: boolean;
  /** Flag to control the opened state of the time picker menu */
  isOpen?: boolean;
  /** Handler invoked each time the open state of time picker updates */
  setIsOpen?: (isOpen?: boolean) => void;
}

interface TimePickerState {
  isInvalid: boolean;
  isTimeOptionsOpen: boolean;
  timeState: string;
  focusedIndex: number;
  scrollIndex: number;
  timeRegex: RegExp;
  minTimeState: string;
  maxTimeState: string;
}

export class TimePicker extends React.Component<TimePickerProps, TimePickerState> {
  static displayName = 'TimePicker';
  private baseComponentRef = React.createRef<any>();
  private toggleRef = React.createRef<HTMLDivElement>();
  private inputRef = React.createRef<HTMLInputElement>();
  private menuRef = React.createRef<HTMLDivElement>();

  static defaultProps = {
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
    setIsOpen: () => {}
  };

  constructor(props: TimePickerProps) {
    super(props);
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

  onDocClick = (event: MouseEvent | TouchEvent) => {
    const clickedOnToggle = this.toggleRef?.current?.contains(event.target as Node);
    const clickedWithinMenu = this.menuRef?.current?.contains(event.target as Node);
    if (this.state.isTimeOptionsOpen && !(clickedOnToggle || clickedWithinMenu)) {
      this.onToggle(false);
    }
  };

  handleGlobalKeys = (event: KeyboardEvent) => {
    const { isTimeOptionsOpen, focusedIndex, scrollIndex } = this.state;
    // keyboard pressed while focus on toggle
    if (this.inputRef?.current?.contains(event.target as Node)) {
      if (!isTimeOptionsOpen && event.key !== KeyTypes.Tab && event.key !== KeyTypes.Escape) {
        this.onToggle(true);
      } else if (isTimeOptionsOpen) {
        if (event.key === KeyTypes.Escape || event.key === KeyTypes.Tab) {
          this.onToggle(false);
        } else if (event.key === KeyTypes.Enter) {
          if (focusedIndex !== null) {
            this.focusSelection(focusedIndex);
            event.stopPropagation();
          } else {
            this.onToggle(false);
          }
        } else if (event.key === KeyTypes.ArrowDown || event.key === KeyTypes.ArrowUp) {
          this.focusSelection(scrollIndex);
          this.updateFocusedIndex(0);
          event.preventDefault();
        }
      }
      // keyboard pressed while focus on menu item
    } else if (this.menuRef?.current?.contains(event.target as Node)) {
      if (event.key === KeyTypes.ArrowDown) {
        this.updateFocusedIndex(1);
        event.preventDefault();
      } else if (event.key === KeyTypes.ArrowUp) {
        this.updateFocusedIndex(-1);
        event.preventDefault();
      } else if (event.key === KeyTypes.Escape || event.key === KeyTypes.Tab) {
        this.inputRef.current.focus();
        this.onToggle(false);
      }
    }
  };

  componentDidUpdate(prevProps: TimePickerProps, prevState: TimePickerState) {
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

  updateFocusedIndex = (increment: number) => {
    this.setState(prevState => {
      const maxIndex = this.getOptions().length - 1;
      let nextIndex =
        prevState.focusedIndex !== null ? prevState.focusedIndex + increment : prevState.scrollIndex + increment;
      if (nextIndex < 0) {
        nextIndex = maxIndex;
      } else if (nextIndex > maxIndex) {
        nextIndex = 0;
      }
      this.scrollToIndex(nextIndex);
      return {
        focusedIndex: nextIndex
      };
    });
  };

  // fixes issue where menutAppendTo="inline" results in the menu item that should be scrolled to being out of view; this will select the menu item that comes before the intended one, causing that before-item to be placed out of view instead
  getIndexToScroll = (index: number) => {
    if (this.props.menuAppendTo === 'inline') {
      return index > 0 ? index - 1 : 0;
    }
    return index;
  };

  scrollToIndex = (index: number) => {
    this.getOptions()[index].closest(`.${menuStyles.menuContent}`).scrollTop = this.getOptions()[
      this.getIndexToScroll(index)
    ].offsetTop;
  };

  focusSelection = (index: number) => {
    const indexToFocus = index !== -1 ? index : 0;

    if (this.menuRef?.current) {
      (this.getOptions()[indexToFocus].querySelector(`.${menuStyles.menuItem}`) as HTMLElement).focus();
    }
  };

  scrollToSelection = (time: string) => {
    const { delimiter, is24Hour } = this.props;
    let splitTime = time.split(this.props.delimiter);
    let focusedIndex = null;

    // build out the rest of the time assuming hh:00 if it's a partial time
    if (splitTime.length < 2) {
      time = `${time}${delimiter}00`;
      splitTime = time.split(delimiter);
      // due to only the input including seconds when includeSeconds=true, we need to build a temporary time here without those seconds so that an exact or close match can be scrolled to within the menu (which does not include seconds in any of the options)
    } else if (splitTime.length > 2) {
      time = parseTime(time, this.state.timeRegex, delimiter, !is24Hour, false);
      splitTime = time.split(delimiter);
    }

    // for 12hr variant, autoscroll to pm if it's currently the afternoon, otherwise autoscroll to am
    if (!is24Hour && splitTime.length > 1 && splitTime[1].length < 2) {
      const minutes = splitTime[1].length === 0 ? '00' : splitTime[1] + '0';
      time = `${splitTime[0]}${delimiter}${minutes}${new Date().getHours() > 11 ? pmSuffix : amSuffix}`;
    } else if (
      !is24Hour &&
      splitTime.length > 1 &&
      splitTime[1].length === 2 &&
      !time.toUpperCase().includes(amSuffix.toUpperCase().trim()) &&
      !time.toUpperCase().includes(pmSuffix.toUpperCase().trim())
    ) {
      time = `${time}${new Date().getHours() > 11 ? pmSuffix : amSuffix}`;
    }
    let scrollIndex = this.getOptions().findIndex(option => option.innerText === time);

    // if we found an exact match, scroll to match and return index of match for focus
    if (scrollIndex !== -1) {
      this.scrollToIndex(scrollIndex);
      focusedIndex = scrollIndex;
    } else if (splitTime.length === 2) {
      // no exact match, scroll to closest hour but don't return index for focus
      let amPm = '';
      if (!is24Hour) {
        if (splitTime[1].toUpperCase().includes('P')) {
          amPm = pmSuffix;
        } else if (splitTime[1].toUpperCase().includes('A')) {
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

  getRegExp = (includeSeconds: boolean = true) => {
    const { is24Hour, delimiter } = this.props;
    let baseRegex = `\\s*(\\d\\d?)${delimiter}([0-5]\\d)`;

    if (includeSeconds) {
      baseRegex += `${delimiter}?([0-5]\\d)?`;
    }

    return new RegExp(`^${baseRegex}${is24Hour ? '' : '\\s*([AaPp][Mm])?'}\\s*$`);
  };

  getOptions = () =>
    (this.menuRef?.current
      ? Array.from(this.menuRef.current.querySelectorAll(`.${menuStyles.menuListItem}`))
      : []) as HTMLElement[];

  isValidFormat = (time: string) => {
    if (this.props.validateTime) {
      return this.props.validateTime(time);
    }

    const { delimiter, is24Hour, includeSeconds } = this.props;
    return validateTime(time, this.getRegExp(includeSeconds), delimiter, !is24Hour);
  };

  isValidTime = (time: string) => {
    const { delimiter, includeSeconds } = this.props;
    const { minTimeState, maxTimeState } = this.state;

    return isWithinMinMax(minTimeState, maxTimeState, time, delimiter, includeSeconds);
  };

  isValid = (time: string) => this.isValidFormat(time) && this.isValidTime(time);

  onToggle = (isOpen: boolean) => {
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

  onSelect = (e: any) => {
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

  onInputClick = (e: any) => {
    if (!this.state.isTimeOptionsOpen) {
      this.onToggle(true);
    }
    e.stopPropagation();
  };

  onInputChange = (newTime: string) => {
    const { onChange } = this.props;
    const { timeRegex } = this.state;

    if (onChange) {
      onChange(
        newTime,
        getHours(newTime, timeRegex),
        getMinutes(newTime, timeRegex),
        getSeconds(newTime, timeRegex),
        this.isValid(newTime)
      );
    }
    this.scrollToSelection(newTime);
    this.setState({
      timeState: newTime
    });
  };

  onBlur = (event: React.FocusEvent<HTMLInputElement>) => {
    const { timeRegex } = this.state;
    const { delimiter, is24Hour, includeSeconds } = this.props;
    const time = parseTime(event.currentTarget.value, timeRegex, delimiter, !is24Hour, includeSeconds);

    this.setState({
      isInvalid: !this.isValid(time)
    });
  };

  render() {
    const {
      'aria-label': ariaLabel,
      isDisabled,
      className,
      placeholder,
      id,
      menuAppendTo,
      is24Hour,
      invalidFormatErrorMessage,
      invalidMinMaxErrorMessage,
      stepMinutes,
      width,
      delimiter,
      inputProps,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      onChange,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      setIsOpen,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      isOpen,
      time,
      validateTime,
      minTime,
      maxTime,
      includeSeconds,
      /* eslint-enable @typescript-eslint/no-unused-vars */
      ...props
    } = this.props;
    const { timeState, isTimeOptionsOpen, isInvalid, minTimeState, maxTimeState } = this.state;
    const style = { '--pf-c-date-picker__input--c-form-control--Width': width } as React.CSSProperties;
    const options = makeTimeOptions(stepMinutes, !is24Hour, delimiter, minTimeState, maxTimeState, includeSeconds);
    const isValidFormat = this.isValidFormat(timeState);
    const randomId = id || getUniqueId('time-picker');

    const getParentElement = () => {
      if (this.baseComponentRef && this.baseComponentRef.current) {
        return this.baseComponentRef.current.parentElement;
      }
      return null;
    };

    const menuContainer = (
      <Menu ref={this.menuRef} isScrollable>
        <MenuContent maxMenuHeight="200px">
          <MenuList aria-label={ariaLabel}>
            {options.map((option, index) => (
              <MenuItem onClick={this.onSelect} key={option} id={`${randomId}-option-${index}`}>
                {option}
              </MenuItem>
            ))}
          </MenuList>
        </MenuContent>
      </Menu>
    );

    const textInput = (
      <TextInput
        aria-haspopup="menu"
        className={css(formStyles.formControl)}
        id={`${randomId}-input`}
        aria-label={ariaLabel}
        validated={isInvalid ? 'error' : 'default'}
        placeholder={placeholder}
        value={timeState || ''}
        type="text"
        iconVariant="clock"
        onClick={this.onInputClick}
        onChange={this.onInputChange}
        onBlur={this.onBlur}
        autoComplete="off"
        isDisabled={isDisabled}
        ref={this.inputRef}
        {...inputProps}
      />
    );

    return (
      <div ref={this.baseComponentRef} className={css(datePickerStyles.datePicker, className)}>
        <div className={css(datePickerStyles.datePickerInput)} style={style} {...props}>
          <InputGroup>
            <div id={randomId}>
              <div ref={this.toggleRef} style={{ paddingLeft: '0' }}>
                {menuAppendTo !== 'inline' ? (
                  <Popper
                    appendTo={menuAppendTo === 'parent' ? getParentElement() : menuAppendTo}
                    trigger={textInput}
                    popper={menuContainer}
                    isVisible={isTimeOptionsOpen}
                  />
                ) : (
                  textInput
                )}
              </div>
              {isTimeOptionsOpen && menuAppendTo === 'inline' && menuContainer}
            </div>
          </InputGroup>
          {isInvalid && (
            <div className={css(datePickerStyles.datePickerHelperText, datePickerStyles.modifiers.error)}>
              {!isValidFormat ? invalidFormatErrorMessage : invalidMinMaxErrorMessage}
            </div>
          )}
        </div>
      </div>
    );
  }
}
