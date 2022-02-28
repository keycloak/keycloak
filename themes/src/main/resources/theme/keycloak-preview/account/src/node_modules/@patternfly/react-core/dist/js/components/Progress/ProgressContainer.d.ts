import * as React from 'react';
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
    info = "info"
}
export interface ProgressContainerProps extends Omit<React.HTMLProps<HTMLDivElement>, 'label'> {
    /** Properties needed for aria support */
    ariaProps?: AriaProps;
    /** Progress component DOM ID. */
    parentId: string;
    /** Progress title. */
    title?: string;
    /** Label to indicate what progress is showing. */
    label?: React.ReactNode;
    /** Type of progress status. */
    variant?: 'danger' | 'success' | 'info';
    /** Location of progress value. */
    measureLocation?: 'outside' | 'inside' | 'top' | 'none';
    /** Actual progress value. */
    value: number;
}
export declare const ProgressContainer: React.FunctionComponent<ProgressContainerProps>;
