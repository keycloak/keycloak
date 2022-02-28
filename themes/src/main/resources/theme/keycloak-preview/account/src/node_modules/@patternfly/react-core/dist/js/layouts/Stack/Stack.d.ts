import * as React from 'react';
export interface StackProps extends React.HTMLProps<HTMLDivElement> {
    /** Adds space between children. */
    gutter?: 'sm' | 'md' | 'lg';
    /** content rendered inside the Stack layout */
    children?: React.ReactNode;
    /** additional classes added to the Stack layout */
    className?: string;
    /** Sets the base component to render. defaults to div */
    component?: React.ReactNode;
}
export declare const Stack: React.FunctionComponent<StackProps>;
