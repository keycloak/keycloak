import * as React from 'react';
export interface AriaProps {
    'aria-labelledby'?: string;
    'aria-label'?: string;
    'aria-valuemin'?: number;
    'aria-valuenow'?: number;
    'aria-valuemax'?: number;
    'aria-valuetext'?: string;
}
export interface ProgressBarProps extends React.HTMLProps<HTMLDivElement> {
    /** What should be rendered inside progress bar. */
    children?: React.ReactNode;
    /** Additional classes for Progres bar. */
    className?: string;
    /** Actual progress value. */
    value: number;
    /** Minimal value of progress. */
    progressBarAriaProps?: AriaProps;
}
export declare const ProgressBar: React.FunctionComponent<ProgressBarProps>;
//# sourceMappingURL=ProgressBar.d.ts.map