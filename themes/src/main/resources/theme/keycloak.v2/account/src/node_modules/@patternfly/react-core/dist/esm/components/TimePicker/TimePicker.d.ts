import * as React from 'react';
import { TextInputProps } from '../TextInput';
export interface TimePickerProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onChange' | 'onFocus' | 'onBlur' | 'disabled' | 'ref'> {
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
export declare class TimePicker extends React.Component<TimePickerProps, TimePickerState> {
    static displayName: string;
    private baseComponentRef;
    private toggleRef;
    private inputRef;
    private menuRef;
    static defaultProps: {
        className: string;
        isDisabled: boolean;
        time: string;
        is24Hour: boolean;
        invalidFormatErrorMessage: string;
        invalidMinMaxErrorMessage: string;
        placeholder: string;
        delimiter: string;
        'aria-label': string;
        width: string;
        menuAppendTo: string;
        stepMinutes: number;
        inputProps: {};
        minTime: string;
        maxTime: string;
        setIsOpen: () => void;
    };
    constructor(props: TimePickerProps);
    componentDidMount(): void;
    componentWillUnmount(): void;
    onDocClick: (event: MouseEvent | TouchEvent) => void;
    handleGlobalKeys: (event: KeyboardEvent) => void;
    componentDidUpdate(prevProps: TimePickerProps, prevState: TimePickerState): void;
    updateFocusedIndex: (increment: number) => void;
    getIndexToScroll: (index: number) => number;
    scrollToIndex: (index: number) => void;
    focusSelection: (index: number) => void;
    scrollToSelection: (time: string) => void;
    getRegExp: (includeSeconds?: boolean) => RegExp;
    getOptions: () => HTMLElement[];
    isValidFormat: (time: string) => boolean;
    isValidTime: (time: string) => boolean;
    isValid: (time: string) => boolean;
    onToggle: (isOpen: boolean) => void;
    onSelect: (e: any) => void;
    onInputClick: (e: any) => void;
    onInputChange: (newTime: string) => void;
    onBlur: (event: React.FocusEvent<HTMLInputElement>) => void;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=TimePicker.d.ts.map