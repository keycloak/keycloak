import * as React from 'react';
export interface OptionsMenuToggleWithTextProps extends React.HTMLProps<HTMLDivElement> {
    /** Id of the parent options menu component */
    parentId?: string;
    /** Content to be rendered inside the options menu toggle as text or another non-interactive element */
    toggleText: React.ReactNode;
    /** classes to be added to the options menu toggle text */
    toggleTextClassName?: string;
    /** Content to be rendered inside the options menu toggle button */
    toggleButtonContents?: React.ReactNode;
    /** Classes to be added to the options menu toggle button */
    toggleButtonContentsClassName?: string;
    /** Callback for when this options menu is toggled */
    onToggle?: (event: boolean) => void;
    /** Inner function to indicate open on Enter */
    onEnter?: (event: React.MouseEvent<HTMLButtonElement> | React.KeyboardEvent<Element>) => void;
    /** Flag to indicate if menu is open */
    isOpen?: boolean;
    /** Flag to indicate if the button is plain */
    isPlain?: boolean;
    /** Forces display of the active state of the options menu button */
    isActive?: boolean;
    /** Disables the options menu toggle */
    isDisabled?: boolean;
    /** @hide Internal parent reference */
    parentRef?: React.RefObject<HTMLElement>;
    /** Indicates that the element has a popup context menu or sub-level menu */
    'aria-haspopup'?: boolean | 'dialog' | 'menu' | 'listbox' | 'tree' | 'grid';
    /** Provides an accessible name for the button when an icon is used instead of text */
    'aria-label'?: string;
    /** @hide Display the toggle in text only mode. */
    isText?: boolean;
    /** @hide The menu element */
    getMenuRef?: () => HTMLElement;
}
export declare const OptionsMenuToggleWithText: React.FunctionComponent<OptionsMenuToggleWithTextProps>;
//# sourceMappingURL=OptionsMenuToggleWithText.d.ts.map