import * as React from 'react';
export interface OptionsMenuItemProps extends Omit<React.HTMLProps<HTMLAnchorElement>, 'onSelect' | 'onClick' | 'onKeyDown' | 'type'> {
    /** Anything which can be rendered as an options menu item */
    children?: React.ReactNode;
    /** Classes applied to root element of an options menu item */
    className?: string;
    /** Render options menu item as selected */
    isSelected?: boolean;
    /** Render options menu item as disabled option */
    isDisabled?: boolean;
    /** Callback for when this options menu item is selected */
    onSelect?: (event?: React.MouseEvent<HTMLAnchorElement> | React.KeyboardEvent) => void;
    /** Unique id of this options menu item */
    id?: string;
}
export declare const OptionsMenuItem: React.FunctionComponent<OptionsMenuItemProps>;
//# sourceMappingURL=OptionsMenuItem.d.ts.map