import * as React from 'react';
export interface LabelProps extends React.HTMLProps<HTMLSpanElement> {
    /** Content rendered inside the label. */
    children: React.ReactNode;
    /** Additional classes added to the label. */
    className?: string;
    /** Flag to show if the label is compact. */
    isCompact?: boolean;
}
export declare const Label: React.FunctionComponent<LabelProps>;
