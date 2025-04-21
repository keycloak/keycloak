import * as React from 'react';
export interface OptionsMenuToggleProps extends React.HTMLProps<HTMLButtonElement> {
    /** Id of the parent options menu component */
    parentId?: string;
    /** Callback for when this options menu is toggled */
    onToggle?: (isOpen: boolean) => void;
    /** Flag to indicate if menu is open */
    isOpen?: boolean;
    /** Flag to indicate if the button is plain */
    isPlain?: boolean;
    isSplitButton?: boolean;
    /** Forces display of the active state of the options menu */
    isActive?: boolean;
    /** Disables the options menu toggle */
    isDisabled?: boolean;
    /** hide the toggle caret */
    hideCaret?: boolean;
    /** Provides an accessible name for the button when an icon is used instead of text */
    'aria-label'?: string;
    /** @hide Internal function to implement enter click */
    onEnter?: (event: React.MouseEvent<HTMLButtonElement>) => void;
    /** @hide Internal parent reference */
    parentRef?: HTMLElement;
    /** Content to be rendered in the options menu toggle button */
    toggleTemplate?: React.ReactNode;
}
export declare const OptionsMenuToggle: React.FunctionComponent<OptionsMenuToggleProps>;
//# sourceMappingURL=OptionsMenuToggle.d.ts.map