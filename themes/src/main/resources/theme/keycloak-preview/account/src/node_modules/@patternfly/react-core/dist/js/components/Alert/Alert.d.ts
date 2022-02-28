import * as React from 'react';
import { InjectedOuiaProps } from '../withOuia';
export declare enum AlertVariant {
    success = "success",
    danger = "danger",
    warning = "warning",
    info = "info",
    default = "default"
}
export interface AlertProps extends Omit<React.HTMLProps<HTMLDivElement>, 'action' | 'title'> {
    /** Adds Alert variant styles  */
    variant?: 'success' | 'danger' | 'warning' | 'info' | 'default';
    /** Flag to indicate if the Alert is inline */
    isInline?: boolean;
    /** Title of the Alert  */
    title: React.ReactNode;
    /** Action button to put in the Alert. Should be <AlertActionLink> or <AlertActionCloseButton> */
    action?: React.ReactNode;
    /** Content rendered inside the Alert */
    children?: React.ReactNode;
    /** Additional classes added to the Alert  */
    className?: string;
    /** Adds accessible text to the Alert */
    'aria-label'?: string;
    /** Variant label text for screen readers */
    variantLabel?: string;
    /** Flag to indicate if the Alert is in a live region */
    isLiveRegion?: boolean;
}
declare const AlertWithOuiaContext: React.FunctionComponent<AlertProps & InjectedOuiaProps>;
export { AlertWithOuiaContext as Alert };
