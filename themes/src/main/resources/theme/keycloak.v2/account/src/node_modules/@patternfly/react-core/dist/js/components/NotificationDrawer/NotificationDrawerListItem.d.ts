import * as React from 'react';
export interface NotificationDrawerListItemProps extends React.HTMLProps<HTMLLIElement> {
    /**  Content rendered inside the list item */
    children?: React.ReactNode;
    /**  Additional classes added to the list item */
    className?: string;
    /**  Modifies the list item to include hover styles on :hover */
    isHoverable?: boolean;
    /**  Adds styling to the list item to indicate it has been read */
    isRead?: boolean;
    /**  Callback for when a list item is clicked */
    onClick?: (event: any) => void;
    /**  Tab index for the list item */
    tabIndex?: number;
    /**  Variant indicates the severity level */
    variant?: 'default' | 'success' | 'danger' | 'warning' | 'info';
}
export declare const NotificationDrawerListItem: React.FunctionComponent<NotificationDrawerListItemProps>;
//# sourceMappingURL=NotificationDrawerListItem.d.ts.map