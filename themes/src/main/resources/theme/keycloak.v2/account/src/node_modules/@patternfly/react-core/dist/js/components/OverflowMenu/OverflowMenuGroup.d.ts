import * as React from 'react';
export interface OverflowMenuGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Any elements that can be rendered in the menu */
    children?: any;
    /** Additional classes added to the OverflowMenuGroup */
    className?: string;
    /** Modifies the overflow menu group visibility */
    isPersistent?: boolean;
    /** Indicates a button or icon group */
    groupType?: 'button' | 'icon';
}
export declare const OverflowMenuGroup: React.FunctionComponent<OverflowMenuGroupProps>;
//# sourceMappingURL=OverflowMenuGroup.d.ts.map