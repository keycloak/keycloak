import React from 'react';
export declare enum Weekday {
    Sunday = 0,
    Monday = 1,
    Tuesday = 2,
    Wednesday = 3,
    Thursday = 4,
    Friday = 5,
    Saturday = 6
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
export declare const isValidDate: (date: Date) => boolean;
export declare const CalendarMonth: ({ date: dateProp, locale, monthFormat, weekdayFormat, longWeekdayFormat, dayFormat, weekStart, onChange, validators, className, onSelectToggle, rangeStart, prevMonthAriaLabel, nextMonthAriaLabel, yearInputAriaLabel, cellAriaLabel, isDateFocused, ...props }: CalendarProps) => JSX.Element;
//# sourceMappingURL=CalendarMonth.d.ts.map