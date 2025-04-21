import * as React from 'react';
export interface NotificationDrawerListItemBodyProps extends React.HTMLProps<HTMLDivElement> {
    /**  Content rendered inside the list item body */
    children?: React.ReactNode;
    /**  Additional classes added to the list item body */
    className?: string;
    /**  List item timestamp */
    timestamp?: React.ReactNode;
}
export declare const NotificationDrawerListItemBody: React.FunctionComponent<NotificationDrawerListItemBodyProps>;
//# sourceMappingURL=NotificationDrawerListItemBody.d.ts.map