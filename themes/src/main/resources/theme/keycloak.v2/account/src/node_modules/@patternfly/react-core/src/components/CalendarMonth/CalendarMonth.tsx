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

export enum Weekday {
  Sunday = 0,
  Monday,
  Tuesday,
  Wednesday,
  Thursday,
  Friday,
  Saturday
}

export interface CalendarFormat {
  /** How to format months in Select */
  monthFormat?: (date: Date) => React.ReactNode;
  /** How to format week days in header */
  weekdayFormat?: (date: Date) => React.ReactNode;
  /** How to format days in header for screen readers */
  longWeekdayFormat?: (date: Date) => React.ReactNode;
  /** How to format days in buttons in table cells */
  dayFormat?: (date: Date) => React.ReactNode;
  /** If using the default formatters which locale to use. Undefined defaults to current locale. See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation */
  locale?: string;
  /** Day of week that starts the week. 0 is Sunday, 6 is Saturday. */
  weekStart?: 0 | 1 | 2 | 3 | 4 | 5 | 6 | Weekday;
  /** Which date to start range styles from */
  rangeStart?: Date;
  /** Aria-label for the previous month button */
  prevMonthAriaLabel?: string;
  /** Aria-label for the next month button */
  nextMonthAriaLabel?: string;
  /** Aria-label for the year input */
  yearInputAriaLabel?: string;
  /** Aria-label for the date cells */
  cellAriaLabel?: (date: Date) => string;
}

export interface CalendarProps extends CalendarFormat, Omit<React.HTMLProps<HTMLDivElement>, 'onChange'> {
  /** Month/year to base other dates around */
  date?: Date;
  /** Callback when date is selected */
  onChange?: (date: Date) => void;
  /** Functions that returns if a date is valid and selectable */
  validators?: ((date: Date) => boolean)[];
  /** Classname to add to outer div */
  className?: string;
  /** @hide Internal prop to allow pressing escape in select menu to not close popover */
  onSelectToggle?: (open: boolean) => void;
  /** Flag to set browser focus on the passed date **/
  isDateFocused?: boolean;
}

// Must be numeric given current header design
const yearFormat = (date: Date) => date.getFullYear();

const buildCalendar = (year: number, month: number, weekStart: number, validators: ((date: Date) => boolean)[]) => {
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

const isSameDate = (d1: Date, d2: Date) =>
  d1.getFullYear() === d2.getFullYear() && d1.getMonth() === d2.getMonth() && d1.getDate() === d2.getDate();

export const isValidDate = (date: Date) => Boolean(date && !isNaN(date as any));

const today = new Date();

export const CalendarMonth = ({
  date: dateProp,
  locale = undefined,
  monthFormat = date => date.toLocaleDateString(locale, { month: 'long' }),
  weekdayFormat = date => date.toLocaleDateString(locale, { weekday: 'narrow' }),
  longWeekdayFormat = date => date.toLocaleDateString(locale, { weekday: 'long' }),
  dayFormat = date => date.getDate(),
  weekStart = 0, // Use the American Sunday as a default
  onChange = () => {},
  validators = [() => true],
  className,
  onSelectToggle = () => {},
  rangeStart,
  prevMonthAriaLabel = 'Previous month',
  nextMonthAriaLabel = 'Next month',
  yearInputAriaLabel = 'Select year',
  cellAriaLabel,
  isDateFocused = false,
  ...props
}: CalendarProps) => {
  const longMonths = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map(monthNum => new Date(1990, monthNum)).map(monthFormat);
  const [isSelectOpen, setIsSelectOpen] = React.useState(false);
  // eslint-disable-next-line prefer-const
  const [focusedDate, setFocusedDate] = React.useState(() => {
    const initDate = new Date(dateProp);
    if (isValidDate(initDate)) {
      return initDate;
    } else {
      if (isValidDate(rangeStart)) {
        return rangeStart;
      } else {
        return today;
      }
    }
  });
  const [hoveredDate, setHoveredDate] = React.useState(new Date(focusedDate));
  const focusRef = React.useRef<HTMLButtonElement>();
  const [hiddenMonthId] = React.useState(getUniqueId('hidden-month-span'));
  const [shouldFocus, setShouldFocus] = React.useState(false);

  const isValidated = (date: Date) => validators.every(validator => validator(date));
  const focusedDateValidated = isValidated(focusedDate);
  useEffect(() => {
    if (isValidDate(dateProp) && !isSameDate(focusedDate, dateProp)) {
      setFocusedDate(dateProp);
    } else if (!dateProp) {
      setFocusedDate(today);
    }
  }, [dateProp]);

  useEffect(() => {
    // Calendar month should not be focused on page load
    // Datepicker should place focus in calendar month when opened
    if ((shouldFocus || isDateFocused) && focusedDateValidated && focusRef.current) {
      focusRef.current.focus();
    } else {
      setShouldFocus(true);
    }
  }, [focusedDate, isDateFocused, focusedDateValidated, focusRef]);

  const onMonthClick = (newDate: Date) => {
    setFocusedDate(newDate);
    setHoveredDate(newDate);
    setShouldFocus(false);
  };

  const onKeyDown = (ev: React.KeyboardEvent<HTMLTableSectionElement>) => {
    const newDate = new Date(focusedDate);
    if (ev.key === 'ArrowUp') {
      newDate.setDate(newDate.getDate() - 7);
    } else if (ev.key === 'ArrowRight') {
      newDate.setDate(newDate.getDate() + 1);
    } else if (ev.key === 'ArrowDown') {
      newDate.setDate(newDate.getDate() + 7);
    } else if (ev.key === 'ArrowLeft') {
      newDate.setDate(newDate.getDate() - 1);
    }
    if (newDate.getTime() !== focusedDate.getTime() && isValidated(newDate)) {
      ev.preventDefault();
      setFocusedDate(newDate);
      setHoveredDate(newDate);
      setShouldFocus(true);
    }
  };

  const addMonth = (toAdd: -1 | 1) => {
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
  return (
    <div className={css(styles.calendarMonth, className)} {...props}>
      <div className={styles.calendarMonthHeader}>
        <div className={css(styles.calendarMonthHeaderNavControl, styles.modifiers.prevMonth)}>
          <Button variant="plain" aria-label={prevMonthAriaLabel} onClick={() => onMonthClick(prevMonth)}>
            <AngleLeftIcon aria-hidden={true} />
          </Button>
        </div>
        <InputGroup>
          <div className={styles.calendarMonthHeaderMonth}>
            <span id={hiddenMonthId} hidden>
              Month
            </span>
            <Select
              // Max width with "September"
              width="140px"
              aria-labelledby={hiddenMonthId}
              isOpen={isSelectOpen}
              onToggle={() => {
                setIsSelectOpen(!isSelectOpen);
                onSelectToggle(!isSelectOpen);
              }}
              onSelect={(_ev, monthNum) => {
                // When we put CalendarMonth in a Popover we want the Popover's onDocumentClick
                // to see the SelectOption as a child so it doesn't close the Popover.
                setTimeout(() => {
                  setIsSelectOpen(false);
                  onSelectToggle(false);
                  const newDate = new Date(focusedDate);
                  newDate.setMonth(Number(monthNum as string));
                  setFocusedDate(newDate);
                  setHoveredDate(newDate);
                  setShouldFocus(false);
                }, 0);
              }}
              variant="single"
              selections={monthFormatted}
            >
              {longMonths.map((longMonth, index) => (
                <SelectOption key={index} value={index} isSelected={longMonth === monthFormatted}>
                  {longMonth}
                </SelectOption>
              ))}
            </Select>
          </div>
          <div className={styles.calendarMonthHeaderYear}>
            <TextInput
              aria-label={yearInputAriaLabel}
              type="number"
              value={yearFormatted}
              onChange={year => {
                const newDate = new Date(focusedDate);
                newDate.setFullYear(+year);
                setFocusedDate(newDate);
                setHoveredDate(newDate);
                setShouldFocus(false);
              }}
            />
          </div>
        </InputGroup>
        <div className={css(styles.calendarMonthHeaderNavControl, styles.modifiers.nextMonth)}>
          <Button variant="plain" aria-label={nextMonthAriaLabel} onClick={() => onMonthClick(nextMonth)}>
            <AngleRightIcon aria-hidden={true} />
          </Button>
        </div>
      </div>
      <table className={styles.calendarMonthCalendar}>
        <thead className={styles.calendarMonthDays}>
          <tr>
            {calendar[0].map(({ date }, index) => (
              <th key={index} className={styles.calendarMonthDay} scope="col">
                <span className="pf-screen-reader">{longWeekdayFormat(date)}</span>
                <span aria-hidden>{weekdayFormat(date)}</span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody onKeyDown={onKeyDown}>
          {calendar.map((week, index) => (
            <tr key={index} className={styles.calendarMonthDatesRow}>
              {week.map(({ date, isValid }, index) => {
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
                } else if (isValidDate(rangeStart) && isHoveredDateValid) {
                  if (hoveredDate > rangeStart || isSameDate(hoveredDate, rangeStart)) {
                    isInRange = date > rangeStart && date < hoveredDate;
                    isRangeEnd = isSameDate(date, hoveredDate);
                  }
                  // Don't handle focused dates before start dates for now.
                  // Core would likely need new styles
                }

                return (
                  <td
                    key={index}
                    className={css(
                      styles.calendarMonthDatesCell,
                      isAdjacentMonth && styles.modifiers.adjacentMonth,
                      isToday && styles.modifiers.current,
                      (isSelected || isRangeStart) && styles.modifiers.selected,
                      !isValid && styles.modifiers.disabled,
                      (isInRange || isRangeStart || isRangeEnd) && styles.modifiers.inRange,
                      isRangeStart && styles.modifiers.startRange,
                      isRangeEnd && styles.modifiers.endRange
                    )}
                  >
                    <button
                      className={css(
                        styles.calendarMonthDate,
                        isRangeEnd && styles.modifiers.hover,
                        !isValid && styles.modifiers.disabled
                      )}
                      type="button"
                      onClick={() => onChange(date)}
                      onMouseOver={() => setHoveredDate(date)}
                      tabIndex={isFocused ? 0 : -1}
                      disabled={!isValid}
                      aria-label={
                        cellAriaLabel ? cellAriaLabel(date) : `${dayFormatted} ${monthFormatted} ${yearFormatted}`
                      }
                      {...(isFocused && { ref: focusRef })}
                    >
                      {dayFormatted}
                    </button>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
