import * as React from 'react';
import { TooltipPosition } from '../Tooltip';
import { AriaProps } from './ProgressBar';
export declare enum ProgressMeasureLocation {
    outside = "outside",
    inside = "inside",
    top = "top",
    none = "none"
}
export declare enum ProgressVariant {
    danger = "danger",
    success = "success",
    warning = "warning"
}
export interface ProgressContainerProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label' | 'title'> {
    /** Properties needed for aria support */
    progressBarAriaProps?: AriaProps;
    /** Progress component DOM ID. */
    parentId: string;
    /** Progress title. The isTitleTruncated property will only affect string titles. Node title truncation must be handled manually. */
    title?: React.ReactNode;
    /** Label to indicate what progress is showing. */
    label?: React.ReactNode;
    /** Type of progress status. */
    variant?: 'danger' | 'success' | 'warning';
    /** Location of progress value. */
    measureLocation?: 'outside' | 'inside' | 'top' | 'none';
    /** Actual progress value. */
    value: number;
    /** Whether string title should be truncated */
    isTitleTruncated?: boolean;
    /** Position of the tooltip which is displayed if title is truncated */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
}
export declare const ProgressContainer: React.FunctionComponent<ProgressContainerProps>;
//# sourceMappingURL=ProgressContainer.d.ts.map