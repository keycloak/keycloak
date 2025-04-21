import * as React from 'react';
export interface OverflowMenuItemProps extends React.HTMLProps<HTMLDivElement> {
    /** Any elements that can be rendered in the menu */
    children?: any;
    /** Additional classes added to the OverflowMenuItem */
    className?: string;
    /** Modifies the overflow menu item visibility */
    isPersistent?: boolean;
}
export declare const OverflowMenuItem: React.FunctionComponent<OverflowMenuItemProps>;
//# sourceMappingURL=OverflowMenuItem.d.ts.map