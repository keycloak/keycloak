import * as React from 'react';
export interface InputGroupTextProps extends React.HTMLProps<HTMLSpanElement | HTMLLabelElement> {
    /** Additional classes added to the input group text. */
    className?: string;
    /** Content rendered inside the input group text. */
    children: React.ReactNode;
    /** Component that wraps the input group text. */
    component?: React.ReactNode;
}
export declare const InputGroupText: React.FunctionComponent<InputGroupTextProps>;
