import * as React from 'react';
export interface NotificationDrawerListProps extends React.HTMLProps<HTMLUListElement> {
    /**  Content rendered inside the notification drawer list body */
    children?: React.ReactNode;
    /**  Additional classes added to the notification drawer list body */
    className?: string;
    /**  Adds styling to the notification drawer list to indicate expand/hide state */
    isHidden?: boolean;
}
export declare const NotificationDrawerList: React.FunctionComponent<NotificationDrawerListProps>;
//# sourceMappingURL=NotificationDrawerList.d.ts.map