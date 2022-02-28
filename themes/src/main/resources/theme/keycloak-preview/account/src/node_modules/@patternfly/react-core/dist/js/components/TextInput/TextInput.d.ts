import * as React from 'react';
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
export interface TextInputProps extends Omit<React.HTMLProps<HTMLInputElement>, 'onChange' | 'disabled' | 'ref'> {
    /** Additional classes added to the TextInput. */
    className?: string;
    /** Flag to show if the input is disabled. */
    isDisabled?: boolean;
    /** Flag to show if the input is read only. */
    isReadOnly?: boolean;
    /** Flag to show if the input is required. */
    isRequired?: boolean;
    /** Flag to show if the input is valid or invalid. This prop will be deprecated. You should use validated instead. */
    isValid?: boolean;
    validated?: 'success' | 'error' | 'default';
    /** A callback for when the input value changes. */
    onChange?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
    /** Type that the input accepts. */
    type?: 'text' | 'date' | 'datetime-local' | 'email' | 'month' | 'number' | 'password' | 'search' | 'tel' | 'time' | 'url';
    /** Value of the input. */
    value?: string | number;
    /** Aria-label. The input requires an associated id or aria-label. */
    'aria-label'?: string;
    /** A reference object to attach to the input box. */
    innerRef?: React.Ref<any>;
}
export declare class TextInputBase extends React.Component<TextInputProps> {
    static defaultProps: TextInputProps;
    constructor(props: TextInputProps);
    handleChange: (event: React.FormEvent<HTMLInputElement>) => void;
    render(): JSX.Element;
}
export declare const TextInput: React.ForwardRefExoticComponent<TextInputProps & React.RefAttributes<HTMLInputElement>>;
