import * as React from 'react';
import { NavSelectClickHandler } from './Nav';
export interface NavItemProps extends Omit<React.HTMLProps<HTMLAnchorElement>, 'onClick'> {
    /** Content rendered inside the nav item */
    children?: React.ReactNode;
    /** Additional classes added to the nav item */
    className?: string;
    /** Target navigation link */
    to?: string;
    /** Flag indicating whether the item is active */
    isActive?: boolean;
    /** Group identifier, will be returned with the onToggle and onSelect callback passed to the Nav component */
    groupId?: string | number | null;
    /** Item identifier, will be returned with the onToggle and onSelect callback passed to the Nav component */
    itemId?: string | number | null;
    /** If true prevents the default anchor link action to occur. Set to true if you want to handle navigation yourself. */
    preventDefault?: boolean;
    /** Callback for item click */
    onClick?: NavSelectClickHandler;
    /** Component used to render NavItems */
    component?: React.ReactNode;
}
export declare const NavItem: React.FunctionComponent<NavItemProps>;
