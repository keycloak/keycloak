import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface SelectToggleProps extends React.HTMLProps<HTMLElement> {
    /** HTML ID of dropdown toggle */
    id: string;
    /** Anything which can be rendered as dropdown toggle */
    children: React.ReactNode;
    /** Classes applied to root element of dropdown toggle */
    className?: string;
    /** Flag to indicate if select is open */
    isOpen?: boolean;
    /** Callback called when toggle is clicked */
    onToggle?: (isExpanded: boolean, event: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent | Event) => void;
    /** Callback for toggle open on keyboard entry */
    onEnter?: () => void;
    /** Callback for toggle close */
    onClose?: () => void;
    /** Callback for toggle blur */
    onBlur?: (event?: any) => void;
    /** @hide Internal callback for toggle keyboard navigation */
    handleTypeaheadKeys?: (position: string, shiftKey?: boolean) => void;
    /** @hide Internal callback to move focus to last menu item */
    moveFocusToLastMenuItem?: () => void;
    /** Element which wraps toggle */
    parentRef: React.RefObject<HTMLDivElement>;
    /** The menu element */
    menuRef?: React.RefObject<HTMLElement>;
    /** The menu footer element */
    footerRef?: React.RefObject<HTMLDivElement>;
    /** Forces active state */
    isActive?: boolean;
    /** Display the toggle with no border or background */
    isPlain?: boolean;
    /** Flag indicating if select is disabled */
    isDisabled?: boolean;
    /** Flag indicating if placeholder styles should be applied */
    hasPlaceholderStyle?: boolean;
    /** Type of the toggle button, defaults to 'button' */
    type?: 'reset' | 'button' | 'submit' | undefined;
    /** Id of label for the Select aria-labelledby */
    'aria-labelledby'?: string;
    /** Label for toggle of select variants */
    'aria-label'?: string;
    /** Flag for variant, determines toggle rules and interaction */
    variant?: 'single' | 'checkbox' | 'typeahead' | 'typeaheadmulti';
    /** Flag indicating if select toggle has an clear button */
    hasClearButton?: boolean;
    /** Flag indicating if select menu has a footer */
    hasFooter?: boolean;
    /** @hide Internal callback for handling focus when typeahead toggle button clicked. */
    onClickTypeaheadToggleButton?: () => void;
}
export declare class SelectToggle extends React.Component<SelectToggleProps> {
    static displayName: string;
    private toggle;
    static defaultProps: PickOptional<SelectToggleProps>;
    constructor(props: SelectToggleProps);
    componentDidMount(): void;
    componentWillUnmount(): void;
    onDocClick: (event: Event) => void;
    handleGlobalKeys: (event: KeyboardEvent) => void;
    onKeyDown: (event: React.KeyboardEvent) => void;
    render(): JSX.Element;
}
//# sourceMappingURL=SelectToggle.d.ts.map