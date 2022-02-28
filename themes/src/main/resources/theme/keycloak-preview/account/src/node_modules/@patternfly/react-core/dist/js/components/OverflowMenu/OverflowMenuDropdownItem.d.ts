import * as React from 'react';
export interface OverflowMenuDropdownItemProps extends React.HTMLProps<HTMLDivElement> {
    /** Any elements that can be rendered in the menu */
    children?: any;
    /** Indicates when a dropdown item shows and hides the corresponding list item */
    isShared?: boolean;
}
export declare const OverflowMenuDropdownItem: React.SFC<OverflowMenuDropdownItemProps>;
