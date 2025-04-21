import * as React from 'react';
export interface IconProps extends Omit<React.HTMLProps<SVGElement>, 'size'> {
    /** Changes the color of the icon.  */
    color?: string;
}
export interface EmptyStateIconProps extends IconProps {
    /** Additional classes added to the EmptyState */
    className?: string;
    /** Icon component to be rendered inside the EmptyState on icon variant
     * Usually a CheckCircleIcon, ExclamationCircleIcon, LockIcon, PlusCircleIcon, RocketIcon
     * SearchIcon, or WrenchIcon */
    icon?: React.ComponentType<any>;
    /** Component to be rendered inside the EmptyState on container variant */
    component?: React.ComponentType<any>;
    /** Adds empty state icon variant styles  */
    variant?: 'icon' | 'container';
}
export declare const EmptyStateIcon: React.FunctionComponent<EmptyStateIconProps>;
//# sourceMappingURL=EmptyStateIcon.d.ts.map