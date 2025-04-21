import * as React from 'react';
export declare enum ProgressSize {
    sm = "sm",
    md = "md",
    lg = "lg"
}
export interface ProgressProps extends Omit<React.HTMLProps<HTMLDivElement>, 'size' | 'label' | 'title'> {
    /** Classname for progress component. */
    className?: string;
    /** Size variant of progress. */
    size?: 'sm' | 'md' | 'lg';
    /** Where the measure percent will be located. */
    measureLocation?: 'outside' | 'inside' | 'top' | 'none';
    /** Status variant of progress. */
    variant?: 'danger' | 'success' | 'warning';
    /** Title above progress. The isTitleTruncated property will only affect string titles. Node title truncation must be handled manually. */
    title?: React.ReactNode;
    /** Text description of current progress value to display instead of percentage. */
    label?: React.ReactNode;
    /** Actual value of progress. */
    value?: number;
    /** DOM id for progress component. */
    id?: string;
    /** Minimal value of progress. */
    min?: number;
    /** Maximum value of progress. */
    max?: number;
    /** Accessible text description of current progress value, for when value is not a percentage. Use with label. */
    valueText?: string;
    /** Indicate whether to truncate the string title */
    isTitleTruncated?: boolean;
    /** Position of the tooltip which is displayed if title is truncated */
    tooltipPosition?: 'auto' | 'top' | 'bottom' | 'left' | 'right';
    /** Adds accessible text to the ProgressBar. Required when title not used and there is not any label associated with the progress bar */
    'aria-label'?: string;
    /** Associates the ProgressBar with it's label for accessibility purposes. Required when title not used */
    'aria-labelledby'?: string;
}
export declare class Progress extends React.Component<ProgressProps> {
    static displayName: string;
    static defaultProps: ProgressProps;
    id: string;
    render(): JSX.Element;
}
//# sourceMappingURL=Progress.d.ts.map