import * as React from 'react';
import { OUIAProps } from '../../helpers';
export declare enum TextInputTypes {
    text = "text",
    date = "date",
    datetimeLocal = "datetime-local",
    email = "email",
    month = "month",
    number = "number",
    password = "password",
    search = "search",
    tel = "tel",
    time = "time",
    url = "url"
}
export interface TextInputProps extends Omit<React.HTMLProps<HTMLInputElement>, 'onChange' | 'onFocus' | 'onBlur' | 'disabled' | 'ref'>, OUIAProps {
    /** Additional classes added to the TextInput. */
    className?: string;
    /** Flag to show if the input is disabled. */
    isDisabled?: boolean;
    /** Flag to show if the input is read only. */
    isReadOnly?: boolean;
    /** Flag to show if the input is required. */
    isRequired?: boolean;
    /** Value to indicate if the input is modified to show that validation state.
     * If set to success, input will be modified to indicate valid state.
     * If set to error,  input will be modified to indicate error state.
     */
    validated?: 'success' | 'warning' | 'error' | 'default';
    /** A callback for when the input value changes. */
    onChange?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
    /** Type that the input accepts. */
    type?: 'text' | 'date' | 'datetime-local' | 'email' | 'month' | 'number' | 'password' | 'search' | 'tel' | 'time' | 'url';
    /** Value of the input. */
    value?: string | number;
    /** Aria-label. The input requires an associated id or aria-label. */
    'aria-label'?: string;
    /** A reference object to attach to the input box. */
    innerRef?: React.RefObject<any>;
    /** Trim text on left */
    isLeftTruncated?: boolean;
    /** Callback function when input is focused */
    onFocus?: (event?: any) => void;
    /** Callback function when input is blurred (focus leaves) */
    onBlur?: (event?: any) => void;
    /** icon variant */
    iconVariant?: 'calendar' | 'clock' | 'search';
    /** Use the external file instead of a data URI */
    isIconSprite?: boolean;
    /** Custom icon url to set as the input's background-image */
    customIconUrl?: string;
    /** Dimensions for the custom icon set as the input's background-size */
    customIconDimensions?: string;
}
interface TextInputState {
    ouiaStateId: string;
}
export declare class TextInputBase extends React.Component<TextInputProps, TextInputState> {
    static displayName: string;
    static defaultProps: TextInputProps;
    inputRef: React.RefObject<HTMLInputElement>;
    observer: any;
    constructor(props: TextInputProps);
    handleChange: (event: React.FormEvent<HTMLInputElement>) => void;
    componentDidMount(): void;
    componentWillUnmount(): void;
    handleResize: () => void;
    restoreText: () => void;
    onFocus: (event?: any) => void;
    onBlur: (event?: any) => void;
    render(): JSX.Element;
    private sanitizeInputValue;
}
export declare const TextInput: React.ForwardRefExoticComponent<TextInputProps & React.RefAttributes<HTMLInputElement>>;
export {};
//# sourceMappingURL=TextInput.d.ts.map