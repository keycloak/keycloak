import * as React from 'react';
import { ButtonProps } from '../Button';
export interface NotificationBadgeProps extends ButtonProps {
    /**  Adds styling to the notification badge to indicate it has been read */
    isRead?: boolean;
    /** content rendered inside the Notification Badge */
    children?: React.ReactNode;
    /** additional classes added to the Notification Badge */
    className?: string;
    /** Adds accessible text to the Notification Badge. */
    'aria-label'?: string;
}
export declare const NotificationBadge: React.FunctionComponent<NotificationBadgeProps>;
