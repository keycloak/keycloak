import * as React from 'react';
export declare enum spinnerSize {
    sm = "sm",
    md = "md",
    lg = "lg",
    xl = "xl"
}
export interface SpinnerProps extends Omit<React.HTMLProps<HTMLSpanElement>, 'size'> {
    /** Additional classes added to the Spinner. */
    className?: string;
    /** Size variant of progress. */
    size?: 'sm' | 'md' | 'lg' | 'xl';
    /** Aria value text */
    'aria-valuetext'?: string;
}
export declare const Spinner: React.FunctionComponent<SpinnerProps>;
