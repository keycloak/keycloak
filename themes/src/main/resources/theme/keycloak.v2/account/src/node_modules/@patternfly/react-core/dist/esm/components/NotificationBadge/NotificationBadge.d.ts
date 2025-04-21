import * as React from 'react';
import { ButtonProps } from '../Button';
export declare enum NotificationBadgeVariant {
    read = "read",
    unread = "unread",
    attention = "attention"
}
export interface NotificationBadgeProps extends Omit<ButtonProps, 'variant'> {
    /** @deprecated Use the variant prop instead - Adds styling to the notification badge to indicate it has been read */
    isRead?: boolean;
    /** Determines the variant of the notification badge */
    variant?: NotificationBadgeVariant | 'read' | 'unread' | 'attention';
    /** A number displayed in the badge alongside the icon */
    count?: number;
    /** content rendered inside the notification badge */
    children?: React.ReactNode;
    /** additional classes added to the notification badge */
    className?: string;
    /** Adds accessible text to the notification badge. */
    'aria-label'?: string;
    /** Icon to display for attention variant */
    attentionIcon?: React.ReactNode;
    /** Icon do display in notification badge */
    icon?: React.ReactNode;
}
export declare const NotificationBadge: React.FunctionComponent<NotificationBadgeProps>;
//# sourceMappingURL=NotificationBadge.d.ts.map