import * as React from 'react';
export interface CheckboxSelectGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Checkboxes within group */
    children?: React.ReactNode;
    /** Additional classes added to the CheckboxSelectGroup control */
    className?: string;
    /** Group label */
    label?: string;
    /** ID for title label */
    titleId?: string;
}
export declare const CheckboxSelectGroup: React.FunctionComponent<CheckboxSelectGroupProps>;
