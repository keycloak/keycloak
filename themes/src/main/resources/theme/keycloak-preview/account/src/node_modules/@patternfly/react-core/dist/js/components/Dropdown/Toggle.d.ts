import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface ToggleProps {
    /** HTML ID of dropdown toggle */
    id: string;
    /** Type to put on the button */
    type?: 'button' | 'submit' | 'reset';
    /** Anything which can be rendered as dropdown toggle */
    children?: React.ReactNode;
    /** Classes applied to root element of dropdown toggle */
    className?: string;
    /** Flag to indicate if menu is opened */
    isOpen?: boolean;
    /** Callback called when toggle is clicked */
    onToggle?: (isOpen: boolean, event: MouseEvent | TouchEvent | KeyboardEvent | React.KeyboardEvent<any> | React.MouseEvent<HTMLButtonElement>) => void;
    /** Callback called when the Enter key is pressed */
    onEnter?: () => void;
    /** Element which wraps toggle */
    parentRef?: any;
    /** Forces focus state */
    isFocused?: boolean;
    /** Forces hover state */
    isHovered?: boolean;
    /** Forces active state */
    isActive?: boolean;
    /** Disables the dropdown toggle */
    isDisabled?: boolean;
    /** Display the toggle with no border or background */
    isPlain?: boolean;
    /** Display the toggle with a primary button style */
    isPrimary?: boolean;
    /** Style the toggle as a child of a split button */
    isSplitButton?: boolean;
    /** Flag for aria popup */
    ariaHasPopup?: boolean | 'listbox' | 'menu' | 'dialog' | 'grid' | 'listbox' | 'tree';
    /** Allows selecting toggle to select parent */
    bubbleEvent?: boolean;
}
export declare class Toggle extends React.Component<ToggleProps> {
    private buttonRef;
    static defaultProps: PickOptional<ToggleProps>;
    componentDidMount: () => void;
    componentWillUnmount: () => void;
    onDocClick: (event: MouseEvent | TouchEvent) => void;
    onEscPress: (event: KeyboardEvent) => void;
    onKeyDown: (event: React.KeyboardEvent<any>) => void;
    render(): JSX.Element;
}
