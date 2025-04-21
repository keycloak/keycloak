import * as React from 'react';
import { TooltipPosition } from '../Tooltip';
export interface NotificationDrawerGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'title'> {
    /**  Content rendered inside the group */
    children?: React.ReactNode;
    /**  Additional classes added to the group */
    className?: string;
    /**  Notification drawer group count */
    count: number;
    /**  Adds styling to the group to indicate expanded state */
    isExpanded: boolean;
    /**  Adds styling to the group to indicate whether it has been read */
    isRead?: boolean;
    /**  Callback for when group button is clicked to expand */
    onExpand?: (event: any, value: boolean) => void;
    /**  Notification drawer group title */
    title: string | React.ReactNode;
    /** Truncate title to number of lines */
    truncateTitle?: number;
    /** Position of the tooltip which is displayed if text is truncated */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
}
export declare const NotificationDrawerGroup: React.FunctionComponent<NotificationDrawerGroupProps>;
//# sourceMappingURL=NotificationDrawerGroup.d.ts.map