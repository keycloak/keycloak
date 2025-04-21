import * as React from 'react';
import { TooltipPosition } from '../Tooltip';
export declare const variantIcons: {
    success: React.ComponentClass<import("@patternfly/react-icons/dist/esm/createIcon").SVGIconProps, any>;
    danger: React.ComponentClass<import("@patternfly/react-icons/dist/esm/createIcon").SVGIconProps, any>;
    warning: React.ComponentClass<import("@patternfly/react-icons/dist/esm/createIcon").SVGIconProps, any>;
    info: React.ComponentClass<import("@patternfly/react-icons/dist/esm/createIcon").SVGIconProps, any>;
    default: React.ComponentClass<import("@patternfly/react-icons/dist/esm/createIcon").SVGIconProps, any>;
};
export interface NotificationDrawerListItemHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /**  Actions rendered inside the notification drawer list item header */
    children?: React.ReactNode;
    /**  Additional classes for notification drawer list item header. */
    className?: string;
    /**  Add custom icon for notification drawer list item header */
    icon?: React.ReactNode;
    /**  Notification drawer list item header screen reader title */
    srTitle?: string;
    /**  Notification drawer list item title */
    title: string;
    /**  Variant indicates the severity level */
    variant?: 'success' | 'danger' | 'warning' | 'info' | 'default';
    /** Truncate title to number of lines */
    truncateTitle?: number;
    /** Position of the tooltip which is displayed if text is truncated */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
}
export declare const NotificationDrawerListItemHeader: React.FunctionComponent<NotificationDrawerListItemHeaderProps>;
//# sourceMappingURL=NotificationDrawerListItemHeader.d.ts.map