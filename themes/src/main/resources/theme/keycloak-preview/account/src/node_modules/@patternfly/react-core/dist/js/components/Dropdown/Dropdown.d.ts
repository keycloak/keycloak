import * as React from 'react';
import { DropdownPosition, DropdownDirection } from './dropdownConstants';
export interface DropdownProps extends React.HTMLProps<HTMLDivElement> {
    /** Anything which can be rendered in a dropdown */
    children?: React.ReactNode;
    /** Classes applied to root element of dropdown */
    className?: string;
    /** Array of DropdownItem nodes that will be rendered in the dropdown Menu list */
    dropdownItems?: any[];
    /** Flag to indicate if menu is opened */
    isOpen?: boolean;
    /** Display the toggle with no border or background */
    isPlain?: boolean;
    /** Indicates where menu will be aligned horizontally */
    position?: DropdownPosition | 'right' | 'left';
    /** Display menu above or below dropdown toggle */
    direction?: DropdownDirection | 'up' | 'down';
    /** Flag to indicate if dropdown has groups */
    isGrouped?: boolean;
    /** Toggle for the dropdown, examples: <DropdownToggle> or <DropdownToggleCheckbox> */
    toggle: React.ReactElement<any>;
    /** Function callback called when user selects item */
    onSelect?: (event?: React.SyntheticEvent<HTMLDivElement>) => void;
    /** Flag to indicate if the first dropdown item should gain initial focus, set false when adding
     * a specific auto-focus item (like a current selection) otherwise leave as true
     */
    autoFocus?: boolean;
    ouiaComponentType?: string;
}
export declare const Dropdown: React.FunctionComponent<DropdownProps>;
