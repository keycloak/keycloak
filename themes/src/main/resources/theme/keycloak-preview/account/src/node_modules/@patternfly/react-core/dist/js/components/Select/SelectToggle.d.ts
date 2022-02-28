import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface SelectToggleProps extends React.HTMLProps<HTMLElement> {
    /** HTML ID of dropdown toggle */
    id: string;
    /** Anything which can be rendered as dropdown toggle */
    children: React.ReactNode;
    /** Classes applied to root element of dropdown toggle */
    className?: string;
    /** Flag to indicate if select is expanded */
    isExpanded?: boolean;
    /** Callback called when toggle is clicked */
    onToggle?: (isExpanded: boolean) => void;
    /** Callback for toggle open on keyboard entry */
    onEnter?: () => void;
    /** Callback for toggle close */
    onClose?: () => void;
    /** Internal callback for toggle keyboard navigation */
    handleTypeaheadKeys?: (position: string) => void;
    /** Element which wraps toggle */
    parentRef: React.RefObject<HTMLDivElement>;
    /** Forces focus state */
    isFocused?: boolean;
    /** Forces hover state */
    isHovered?: boolean;
    /** Forces active state */
    isActive?: boolean;
    /** Display the toggle with no border or background */
    isPlain?: boolean;
    /** Flag indicating if select is disabled */
    isDisabled?: boolean;
    /** Type of the toggle button, defaults to 'button' */
    type?: 'reset' | 'button' | 'submit' | undefined;
    /** Id of label for the Select aria-labelledby */
    ariaLabelledBy?: string;
    /** Label for toggle of select variants */
    ariaLabelToggle?: string;
    /** Flag for variant, determines toggle rules and interaction */
    variant?: 'single' | 'checkbox' | 'typeahead' | 'typeaheadmulti';
    /** Flag indicating if select toggle has an clear button */
    hasClearButton?: boolean;
}
export declare class SelectToggle extends React.Component<SelectToggleProps> {
    private toggle;
    static defaultProps: PickOptional<SelectToggleProps>;
    constructor(props: SelectToggleProps);
    componentDidMount(): void;
    componentWillUnmount(): void;
    onDocClick: (event: Event) => void;
    onEscPress: (event: KeyboardEvent) => void;
    onKeyDown: (event: React.KeyboardEvent<Element>) => void;
    render(): JSX.Element;
}
