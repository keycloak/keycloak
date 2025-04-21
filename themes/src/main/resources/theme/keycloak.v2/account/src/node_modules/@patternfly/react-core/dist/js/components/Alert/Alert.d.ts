import * as React from 'react';
import { OUIAProps } from '../../helpers';
import { TooltipPosition } from '../Tooltip';
export declare enum AlertVariant {
    success = "success",
    danger = "danger",
    warning = "warning",
    info = "info",
    default = "default"
}
export interface AlertProps extends Omit<React.HTMLProps<HTMLDivElement>, 'action' | 'title'>, OUIAProps {
    /** Adds alert variant styles  */
    variant?: 'success' | 'danger' | 'warning' | 'info' | 'default';
    /** Flag to indicate if the alert is inline */
    isInline?: boolean;
    /** Flag to indicate if the alert is plain */
    isPlain?: boolean;
    /** Title of the alert  */
    title: React.ReactNode;
    /** Sets the heading level to use for the alert title. Default is h4. */
    titleHeadingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
    /** Close button; use the alertActionCloseButton component  */
    actionClose?: React.ReactNode;
    /** Action links; use a single alertActionLink component or multiple wrapped in an array or React.Fragment */
    actionLinks?: React.ReactNode;
    /** Content rendered inside the alert */
    children?: React.ReactNode;
    /** Additional classes added to the alert  */
    className?: string;
    /** Adds accessible text to the alert */
    'aria-label'?: string;
    /** Variant label text for screen readers */
    variantLabel?: string;
    /** Flag to indicate if the alert is in a live region */
    isLiveRegion?: boolean;
    /** If set to true, the timeout is 8000 milliseconds. If a number is provided, alert will be dismissed after that amount of time in milliseconds. */
    timeout?: number | boolean;
    /** If the user hovers over the alert and `timeout` expires, this is how long to wait before finally dismissing the alert */
    timeoutAnimation?: number;
    /** Function to be executed on alert timeout. Relevant when the timeout prop is set */
    onTimeout?: () => void;
    /** Truncate title to number of lines */
    truncateTitle?: number;
    /** Position of the tooltip which is displayed if text is truncated */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
    /** Set a custom icon to the alert. If not set the icon is set according to the variant */
    customIcon?: React.ReactNode;
    /** Flag indicating that the alert is expandable */
    isExpandable?: boolean;
    /** Adds accessible text to the alert Toggle */
    toggleAriaLabel?: string;
}
export declare const Alert: React.FunctionComponent<AlertProps>;
//# sourceMappingURL=Alert.d.ts.map