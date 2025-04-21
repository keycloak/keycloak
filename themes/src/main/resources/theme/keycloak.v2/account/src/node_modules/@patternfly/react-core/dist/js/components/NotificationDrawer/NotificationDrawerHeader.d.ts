import * as React from 'react';
export interface NotificationDrawerHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /**  Content rendered inside the drawer */
    children?: React.ReactNode;
    /**  Additional classes for notification drawer header. */
    className?: string;
    /** Adds custom accessible text to the notification drawer close button. */
    closeButtonAriaLabel?: string;
    /**  Notification drawer heading count */
    count?: number;
    /**  Notification drawer heading custom text which can be used instead of providing count/unreadText */
    customText?: string;
    /**  Callback for when close button is clicked */
    onClose?: () => void;
    /**  Notification drawer heading title */
    title?: string;
    /**  Notification drawer heading unread text used in combination with a count */
    unreadText?: string;
}
export declare const NotificationDrawerHeader: React.FunctionComponent<NotificationDrawerHeaderProps>;
//# sourceMappingURL=NotificationDrawerHeader.d.ts.map