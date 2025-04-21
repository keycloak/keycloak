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
    /** Text describing that current loading status or progress */
    'aria-valuetext'?: string;
    /** Whether to use an SVG (new) rather than a span (old) */
    isSVG?: boolean;
    /** Diameter of spinner set as CSS variable */
    diameter?: string;
    /** Accessible label to describe what is loading */
    'aria-label'?: string;
    /** Id of element which describes what is being loaded */
    'aria-labelledBy'?: string;
}
export declare const Spinner: React.FunctionComponent<SpinnerProps>;
//# sourceMappingURL=Spinner.d.ts.map