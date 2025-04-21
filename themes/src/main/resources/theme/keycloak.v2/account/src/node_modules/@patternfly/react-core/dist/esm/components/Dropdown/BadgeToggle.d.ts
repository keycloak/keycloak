import * as React from 'react';
import { DropdownToggleProps } from './DropdownToggle';
import { BadgeProps } from '../Badge';
export interface BadgeToggleProps extends DropdownToggleProps {
    /** HTML ID of dropdown toggle */
    id?: string;
    /** Anything which can be rendered as dropdown toggle */
    children?: React.ReactNode;
    /** Badge specific properties */
    badgeProps?: BadgeProps;
    /** Classess applied to root element of dropdown toggle */
    className?: string;
    /** Flag to indicate if menu is opened */
    isOpen?: boolean;
    /** Label Toggle button */
    'aria-label'?: string;
    /** Callback called when toggle is clicked */
    onToggle?: (isOpen: boolean) => void;
    /** Element which wraps toggle */
    parentRef?: any;
    /** The menu element */
    getMenuRef?: () => HTMLElement;
    /** Forces active state */
    isActive?: boolean;
    /** Disables the dropdown toggle */
    isDisabled?: boolean;
    /** Display the toggle with no border or background */
    isPlain?: boolean;
    /** Type to put on the button */
    type?: 'button' | 'submit' | 'reset';
    /** Allows selecting toggle to select parent */
    bubbleEvent?: boolean;
}
export declare const BadgeToggle: React.FunctionComponent<BadgeToggleProps>;
//# sourceMappingURL=BadgeToggle.d.ts.map