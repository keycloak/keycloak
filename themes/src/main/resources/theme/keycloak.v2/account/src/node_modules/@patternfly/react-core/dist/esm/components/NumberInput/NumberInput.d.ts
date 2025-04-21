import * as React from 'react';
import { ButtonProps } from '../Button';
export interface NumberInputProps extends React.HTMLProps<HTMLDivElement> {
    /** Value of the number input */
    value?: number;
    /** Additional classes added to the number input */
    className?: string;
    /** Sets the width of the number input to a number of characters */
    widthChars?: number;
    /** Indicates the whole number input should be disabled */
    isDisabled?: boolean;
    /** Callback for the minus button */
    onMinus?: (event: React.MouseEvent, name?: string) => void;
    /** Callback for the text input changing */
    onChange?: (event: React.FormEvent<HTMLInputElement>) => void;
    /** Callback function when text input is blurred (focus leaves) */
    onBlur?: (event?: any) => void;
    /** Callback for the plus button */
    onPlus?: (event: React.MouseEvent, name?: string) => void;
    /** Adds the given unit to the number input */
    unit?: React.ReactNode;
    /** Position of the number input unit in relation to the number input */
    unitPosition?: 'before' | 'after';
    /** Minimum value of the number input, disabling the minus button when reached */
    min?: number;
    /** Maximum value of the number input, disabling the plus button when reached */
    max?: number;
    /** Name of the input */
    inputName?: string;
    /** Aria label of the input */
    inputAriaLabel?: string;
    /** Aria label of the minus button */
    minusBtnAriaLabel?: string;
    /** Aria label of the plus button */
    plusBtnAriaLabel?: string;
    /** Additional properties added to the text input */
    inputProps?: any;
    /** Additional properties added to the minus button */
    minusBtnProps?: ButtonProps;
    /** Additional properties added to the plus button */
    plusBtnProps?: ButtonProps;
}
export declare const NumberInput: React.FunctionComponent<NumberInputProps>;
//# sourceMappingURL=NumberInput.d.ts.map