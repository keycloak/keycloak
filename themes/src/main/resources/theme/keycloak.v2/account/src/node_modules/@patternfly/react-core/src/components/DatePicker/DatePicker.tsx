import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DatePicker/date-picker';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { TextInput, TextInputProps } from '../TextInput/TextInput';
import { Popover, PopoverProps } from '../Popover/Popover';
import { InputGroup } from '../InputGroup/InputGroup';
import OutlinedCalendarAltIcon from '@patternfly/react-icons/dist/esm/icons/outlined-calendar-alt-icon';
import { CalendarMonth, CalendarFormat, isValidDate } from '../CalendarMonth';
import { useImperativeHandle } from 'react';
import { KeyTypes } from '../../helpers';

export interface DatePickerProps
  extends CalendarFormat,
    Omit<React.HTMLProps<HTMLInputElement>, 'onChange' | 'onFocus' | 'onBlur' | 'disabled' | 'ref'> {
  /** Additional classes added to the date time picker. */
  className?: string;
  /** Accessible label for the date picker */
  'aria-label'?: string;
  /** How to format the date in the TextInput */
  dateFormat?: (date: Date) => string;
  /** How to format the date in the TextInput */
  dateParse?: (value: string) => Date;
  /** Flag indicating the date picker is disabled*/
  isDisabled?: boolean;
  /** String to display in the empty date picker field as a hint for the expected date format */
  placeholder?: string;
  /** Value of TextInput */
  value?: string;
  /** Error message to display when the TextInput cannot be parsed. */
  invalidFormatText?: string;
  /** Callback called every time the input value changes */
  onChange?: (value: string, date?: Date) => void;
  /** Callback called every time the input loses focus */
  onBlur?: (value: string, date?: Date) => void;
  /** Text for label */
  helperText?: React.ReactNode;
  /** Aria label for the button to open the date picker */
  buttonAriaLabel?: string;
  /** The container to append the menu to. Defaults to 'parent'.
   * If your menu is being cut off you can append it to an element higher up the DOM tree.
   * Some examples:
   * menuAppendTo={() => document.body}
   * menuAppendTo={document.getElementById('target')}
   */
  appendTo?: HTMLElement | ((ref?: HTMLElement) => HTMLElement) | 'parent';
  /** Props to pass to the Popover */
  popoverProps?: Omit<PopoverProps, 'appendTo'>;
  /** Functions that returns an error message if a date is invalid */
  validators?: ((date: Date) => string)[];
  /** Additional props for input field */
  inputProps?: TextInputProps;
}

export interface DatePickerRef {
  /** Sets the calendar open status */
  setCalendarOpen: (isOpen: boolean) => void;
  /** Toggles the calendar open status. If no parameters are passed, the calendar will simply toggle its open status.
   * If the isOpen parameter is passed, that will set the calendar open status to the value of the isOpen parameter.
   * If the eventKey parameter is set to 'Escape', that will invoke the date pickers onEscapePress event to toggle the
   * correct control appropriately. */
  toggleCalendar: (isOpen?: boolean, eventKey?: string) => void;
  /** Current calendar open status */
  isCalendarOpen: boolean;
}

export const yyyyMMddFormat = (date: Date) =>
  `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date
    .getDate()
    .toString()
    .padStart(2, '0')}`;

const DatePickerBase = (
  {
    className,
    locale = undefined,
    dateFormat = yyyyMMddFormat,
    dateParse = (val: string) => val.split('-').length === 3 && new Date(`${val}T00:00:00`),
    isDisabled = false,
    placeholder = 'YYYY-MM-DD',
    value: valueProp = '',
    'aria-label': ariaLabel = 'Date picker',
    buttonAriaLabel = 'Toggle date picker',
    onChange = (): any => undefined,
    onBlur = (): any => undefined,
    invalidFormatText = 'Invalid date',
    helperText,
    appendTo = 'parent',
    popoverProps,
    monthFormat,
    weekdayFormat,
    longWeekdayFormat,
    dayFormat,
    weekStart,
    validators = [],
    rangeStart,
    style: styleProps = {},
    inputProps = {},
    ...props
  }: DatePickerProps,
  ref: React.Ref<DatePickerRef>
) => {
  const [value, setValue] = React.useState(valueProp);
  const [valueDate, setValueDate] = React.useState(dateParse(value));
  const [errorText, setErrorText] = React.useState('');
  const [popoverOpen, setPopoverOpen] = React.useState(false);
  const [selectOpen, setSelectOpen] = React.useState(false);
  const [pristine, setPristine] = React.useState(true);
  const widthChars = React.useMemo(() => Math.max(dateFormat(new Date()).length, placeholder.length), [dateFormat]);
  const style = { '--pf-c-date-picker__input--c-form-control--width-chars': widthChars, ...styleProps };
  const buttonRef = React.useRef<HTMLButtonElement>();
  const datePickerWrapperRef = React.useRef<HTMLDivElement>();

  React.useEffect(() => {
    setValue(valueProp);
    setValueDate(dateParse(valueProp));
  }, [valueProp]);

  React.useEffect(() => {
    setPristine(!value);
  }, [value]);

  const setError = (date: Date) => setErrorText(validators.map(validator => validator(date)).join('\n') || '');

  const onTextInput = (value: string) => {
    setValue(value);
    setErrorText('');
    const newValueDate = dateParse(value);
    setValueDate(newValueDate);
    if (isValidDate(newValueDate)) {
      onChange(value, new Date(newValueDate));
    } else {
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
    } else {
      onBlur(value);
      setErrorText(invalidFormatText);
    }
  };

  const onDateClick = (newValueDate: Date) => {
    const newValue = dateFormat(newValueDate);
    setValue(newValue);
    setValueDate(newValueDate);
    setError(newValueDate);
    setPopoverOpen(false);
    onChange(newValue, new Date(newValueDate));
  };

  const onKeyPress = (ev: React.KeyboardEvent<HTMLInputElement>) => {
    if (ev.key === 'Enter' && value) {
      if (isValidDate(valueDate)) {
        setError(valueDate);
      } else {
        setErrorText(invalidFormatText);
      }
    }
  };

  useImperativeHandle<DatePickerRef, DatePickerRef>(
    ref,
    () => ({
      setCalendarOpen: (isOpen: boolean) => setPopoverOpen(isOpen),
      toggleCalendar: (setOpen?: boolean, eventKey?: string) => {
        if (eventKey === KeyTypes.Escape && popoverOpen && !selectOpen) {
          setPopoverOpen(prev => (setOpen !== undefined ? setOpen : !prev));
        }
      },
      isCalendarOpen: popoverOpen
    }),
    [setPopoverOpen, popoverOpen, selectOpen]
  );

  const getParentElement = () =>
    datePickerWrapperRef && datePickerWrapperRef.current ? datePickerWrapperRef.current : null;

  return (
    <div className={css(styles.datePicker, className)} ref={datePickerWrapperRef} style={style} {...props}>
      <Popover
        position="bottom"
        bodyContent={
          <CalendarMonth
            date={valueDate}
            onChange={onDateClick}
            locale={locale}
            // Use truthy values of strings
            validators={validators.map(validator => (date: Date) => !validator(date))}
            onSelectToggle={open => setSelectOpen(open)}
            monthFormat={monthFormat}
            weekdayFormat={weekdayFormat}
            longWeekdayFormat={longWeekdayFormat}
            dayFormat={dayFormat}
            weekStart={weekStart}
            rangeStart={rangeStart}
            isDateFocused
          />
        }
        showClose={false}
        isVisible={popoverOpen}
        shouldClose={(_1, _2, event) => {
          event = event as KeyboardEvent;
          if (event.key === KeyTypes.Escape && selectOpen) {
            event.stopPropagation();
            setSelectOpen(false);
            return false;
          }
          // Let our button handle toggling
          if (buttonRef.current && buttonRef.current.contains(event.target as Node)) {
            return false;
          }
          setPopoverOpen(false);
          if (event.key === KeyTypes.Escape && popoverOpen) {
            event.stopPropagation();
          }
          return true;
        }}
        withFocusTrap
        hasNoPadding
        hasAutoWidth
        appendTo={appendTo === 'parent' ? getParentElement() : appendTo}
        {...popoverProps}
      >
        <div className={styles.datePickerInput}>
          <InputGroup>
            <TextInput
              isDisabled={isDisabled}
              aria-label={ariaLabel}
              placeholder={placeholder}
              validated={errorText ? 'error' : 'default'}
              value={value}
              onChange={onTextInput}
              onBlur={onInputBlur}
              onKeyPress={onKeyPress}
              {...inputProps}
            />
            <button
              ref={buttonRef}
              className={css(buttonStyles.button, buttonStyles.modifiers.control)}
              aria-label={buttonAriaLabel}
              type="button"
              onClick={() => setPopoverOpen(!popoverOpen)}
              disabled={isDisabled}
            >
              <OutlinedCalendarAltIcon />
            </button>
          </InputGroup>
        </div>
      </Popover>
      {helperText && <div className={styles.datePickerHelperText}>{helperText}</div>}
      {errorText.trim() && <div className={css(styles.datePickerHelperText, styles.modifiers.error)}>{errorText}</div>}
    </div>
  );
};

export const DatePicker = React.forwardRef<DatePickerRef, DatePickerProps>(DatePickerBase);
DatePicker.displayName = 'DatePicker';
