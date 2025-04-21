import * as React from 'react';
import { TextInputProps } from '../TextInput/TextInput';
import { PopoverProps } from '../Popover/Popover';
import { CalendarFormat } from '../CalendarMonth';
export interface DatePickerProps extends CalendarFormat, Omit<React.HTMLProps<HTMLInputElement>, 'onChange' | 'onFocus' | 'onBlur' | 'disabled' | 'ref'> {
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
export declare const yyyyMMddFormat: (date: Date) => string;
export declare const DatePicker: React.ForwardRefExoticComponent<DatePickerProps & React.RefAttributes<DatePickerRef>>;
//# sourceMappingURL=DatePicker.d.ts.map