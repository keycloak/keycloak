import * as React from 'react';
export interface TextInputGroupMainProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onChange'> {
    /** Content rendered inside the text input group main div */
    children?: React.ReactNode;
    /** Additional classes applied to the text input group main container */
    className?: string;
    /** Icon to be shown on the left side of the text input group main container */
    icon?: React.ReactNode;
    /** Type that the input accepts. */
    type?: 'text' | 'date' | 'datetime-local' | 'email' | 'month' | 'number' | 'password' | 'search' | 'tel' | 'time' | 'url';
    /** Suggestion that will show up like a placeholder even with text in the input */
    hint?: string;
    /** Callback for when there is a change in the input field*/
    onChange?: (value: string, event: React.FormEvent<HTMLInputElement>) => void;
    /** Callback for when the input field is focused*/
    onFocus?: (event?: any) => void;
    /** Callback for when focus is lost on the input field*/
    onBlur?: (event?: any) => void;
    /** Accessibility label for the input */
    'aria-label'?: string;
    /** Value for the input */
    value?: string | number;
    /** Placeholder value for the input */
    placeholder?: string;
    /** @hide A reference object to attach to the input box */
    innerRef?: React.RefObject<any>;
}
export declare const TextInputGroupMain: React.FunctionComponent<TextInputGroupMainProps>;
//# sourceMappingURL=TextInputGroupMain.d.ts.map