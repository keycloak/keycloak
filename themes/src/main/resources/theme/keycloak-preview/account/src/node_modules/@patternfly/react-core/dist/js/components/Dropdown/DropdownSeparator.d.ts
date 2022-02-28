import * as React from 'react';
export interface SeparatorProps extends React.HTMLProps<HTMLAnchorElement> {
    /** Classes applied to root element of dropdown item */
    className?: string;
    /** Click event to pass to InternalDropdownItem */
    onClick?: (event: React.MouseEvent<HTMLAnchorElement> | React.KeyboardEvent | MouseEvent) => void;
}
export declare const DropdownSeparator: React.FunctionComponent<SeparatorProps>;
