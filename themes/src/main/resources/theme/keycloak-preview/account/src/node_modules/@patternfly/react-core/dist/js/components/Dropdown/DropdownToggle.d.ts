import * as React from 'react';
export interface DropdownToggleProps extends React.HTMLProps<HTMLButtonElement> {
    /** HTML ID of dropdown toggle */
    id?: string;
    /** Anything which can be rendered as dropdown toggle button */
    children?: React.ReactNode;
    /** Classes applied to root element of dropdown toggle button */
    className?: string;
    /** Flag to indicate if menu is opened */
    isOpen?: boolean;
    /** Callback called when toggle is clicked */
    onToggle?: (isOpen: boolean) => void;
    /** Element which wraps toggle */
    parentRef?: HTMLElement;
    /** Forces focus state */
    isFocused?: boolean;
    /** Forces hover state */
    isHovered?: boolean;
    /** Forces active state */
    isActive?: boolean;
    /** Display the toggle with no border or background */
    isPlain?: boolean;
    /** Whether or not the <div> has a disabled state */
    isDisabled?: boolean;
    /** Whether or not the dropdown toggle button should have primary button styling */
    isPrimary?: boolean;
    /** The icon to display for the toggle. Defaults to CaretDownIcon. Set to null to not show an icon. */
    iconComponent?: React.ElementType | null;
    /** Elements to display before the toggle button. When included, renders the toggle as a split button. */
    splitButtonItems?: React.ReactNode[];
    /** Variant of split button toggle */
    splitButtonVariant?: 'action' | 'checkbox';
    /** Accessible label for the dropdown toggle button */
    'aria-label'?: string;
    /** Accessibility property to indicate correct has popup */
    ariaHasPopup?: boolean | 'listbox' | 'menu' | 'dialog' | 'grid' | 'listbox' | 'tree';
    /** Type to put on the button */
    type?: 'button' | 'submit' | 'reset';
    /** Callback called when the Enter key is pressed */
    onEnter?: (event?: React.MouseEvent<HTMLButtonElement>) => void;
}
export declare const DropdownToggle: React.FunctionComponent<DropdownToggleProps>;
