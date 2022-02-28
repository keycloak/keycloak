import * as React from 'react';
export interface DropdownItemIconProps extends React.HTMLProps<HTMLAnchorElement> {
    /** Icon to be rendered in the dropdown item */
    children?: React.ReactNode;
    /** Classes applied to span element of dropdown icon item */
    className?: string;
}
export declare const DropdownItemIcon: React.FunctionComponent<DropdownItemIconProps>;
