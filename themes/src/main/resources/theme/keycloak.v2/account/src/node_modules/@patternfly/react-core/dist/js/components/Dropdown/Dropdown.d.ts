import * as React from 'react';
import { DropdownPosition, DropdownDirection, DropdownContext } from './dropdownConstants';
import { ToggleMenuBaseProps } from '../../helpers/Popper/Popper';
import { OUIAProps } from '../../helpers';
export interface DropdownProps extends Omit<ToggleMenuBaseProps, 'menuAppendTo'>, React.HTMLProps<HTMLDivElement>, OUIAProps {
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
    /** Display the toggle in text only mode */
    isText?: boolean;
    /** Flag indicating that the dropdown should expand to full height */
    isFullHeight?: boolean;
    /** Indicates where menu will be aligned horizontally */
    position?: DropdownPosition | 'right' | 'left';
    /** Indicates how the menu will align at screen size breakpoints. Default alignment is set via the position property. */
    alignments?: {
        sm?: 'right' | 'left';
        md?: 'right' | 'left';
        lg?: 'right' | 'left';
        xl?: 'right' | 'left';
        '2xl'?: 'right' | 'left';
    };
    /** Display menu above or below dropdown toggle */
    direction?: DropdownDirection | 'up' | 'down';
    /** The container to append the menu to. Defaults to 'inline'.
     * If your menu is being cut off you can append it to an element higher up the DOM tree.
     * Some examples:
     * menuAppendTo="parent"
     * menuAppendTo={() => document.body}
     * menuAppendTo={document.getElementById('target')}
     */
    menuAppendTo?: HTMLElement | (() => HTMLElement) | 'inline' | 'parent';
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
    /** Props for extreme customization of dropdown */
    contextProps?: typeof DropdownContext;
    /** Flag for indicating that the dropdown menu should automatically flip vertically when
     * it reaches the boundary. This prop can only be used when the dropdown component is not
     * appended inline, e.g. `menuAppendTo="parent"`
     */
    isFlipEnabled?: boolean;
}
export declare const Dropdown: React.FunctionComponent<DropdownProps>;
//# sourceMappingURL=Dropdown.d.ts.map