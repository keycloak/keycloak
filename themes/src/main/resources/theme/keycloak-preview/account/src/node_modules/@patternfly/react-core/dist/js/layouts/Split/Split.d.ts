import * as React from 'react';
export interface SplitProps extends React.HTMLProps<HTMLDivElement> {
    /** Adds space between children. */
    gutter?: 'sm' | 'md' | 'lg';
    /** content rendered inside the Split layout */
    children?: React.ReactNode;
    /** additional classes added to the Split layout */
    className?: string;
    /** Sets the base component to render. defaults to div */
    component?: React.ReactNode;
}
export declare const Split: React.FunctionComponent<SplitProps>;
