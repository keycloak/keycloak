import * as React from 'react';
export declare enum OptionsMenuPosition {
    right = "right",
    left = "left"
}
export declare enum OptionsMenuDirection {
    up = "up",
    down = "down"
}
export interface OptionsMenuProps extends React.HTMLProps<HTMLDivElement> {
    /** Classes applied to root element of the options menu */
    className?: string;
    /** Id of the root element of the options menu */
    id: string;
    /** Array of OptionsMenuItem and/or OptionMenuGroup nodes that will be rendered in the options menu list */
    menuItems: React.ReactNode[];
    /** Either an OptionsMenuToggle or an OptionsMenuToggleWithText to use to toggle the options menu */
    toggle: React.ReactElement;
    /** Flag to indicate the toggle has no border or background */
    isPlain?: boolean;
    /** Flag to indicate if menu is open */
    isOpen?: boolean;
    /** Flag to indicate if toggle is textual toggle */
    isText?: boolean;
    /** Flag to indicate if menu is groupped */
    isGrouped?: boolean;
    /** Provides an accessible name for the options menu */
    ariaLabelMenu?: string;
    /** Indicates where menu will be aligned horizontally */
    position?: 'right' | 'left';
    /** Menu will open up or open down from the options menu toggle */
    direction?: 'up' | 'down';
}
export declare const OptionsMenu: React.FunctionComponent<OptionsMenuProps>;
