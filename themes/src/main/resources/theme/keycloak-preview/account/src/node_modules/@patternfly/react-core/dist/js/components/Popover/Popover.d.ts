import * as React from 'react';
import { Instance as TippyInstance, Props as TippyProps } from 'tippy.js';
import '@patternfly/react-styles/css/components/Tooltip/tippy.css';
import '@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css';
import { ReactElement } from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export declare enum PopoverPosition {
    auto = "auto",
    top = "top",
    bottom = "bottom",
    left = "left",
    right = "right"
}
export interface PopoverProps {
    /** Accessible label, required when header is not present */
    'aria-label'?: string;
    /** The element to append the popover to, defaults to body */
    appendTo?: Element | ((ref: Element) => Element);
    /** Body content */
    bodyContent: React.ReactNode;
    /** If enableFlip is true, the popover responds to this boundary */
    boundary?: 'scrollParent' | 'window' | 'viewport' | HTMLElement;
    /** The reference element to which the popover is relatively placed to */
    children: ReactElement<any>;
    /** Popover additional class */
    className?: string;
    /** Aria label for the Close button */
    closeBtnAriaLabel?: string;
    /** Distance of the popover to its target, defaults to 25 */
    distance?: number;
    /** If true, tries to keep the popover in view by flipping it if necessary */
    enableFlip?: boolean;
    /**
     * The desired position to flip the popover to if the initial position is not possible.
     * By setting this prop to 'flip' it attempts to flip the popover to the opposite side if there is no space.
     * You can also pass an array of positions that determines the flip order. It should contain the initial position
     * followed by alternative positions if that position is unavailable.
     * Example: Initial position is 'top'. Button with popover is in the top right corner. 'flipBehavior' is set to
     * ['top', 'right', 'left']. Since there is no space to the top, it checks if right is available. There's also no
     * space to the right, so it finally shows the popover on the left.
     */
    flipBehavior?: 'flip' | ('top' | 'bottom' | 'left' | 'right')[];
    /** Footer content */
    footerContent?: React.ReactNode;
    /** Header content, leave empty for no header */
    headerContent?: React.ReactNode;
    /** Hides the popover when a click occurs outside (only works if isVisible is not controlled by the user) */
    hideOnOutsideClick?: boolean;
    /**
     * True to show the popover programmatically. Used in conjunction with the shouldClose prop.
     * By default, the popover child element handles click events automatically. If you want to control this programmatically,
     * the popover will not auto-close if the Close button is clicked, ESC key is used, or if a click occurs outside the popover.
     * Instead, the consumer is responsible for closing the popover themselves by adding a callback listener for the shouldClose prop.
     */
    isVisible?: boolean;
    /** Minimum width of the popover (default 6.25rem) */
    minWidth?: string;
    /** Maximum width of the popover (default 18.75rem) */
    maxWidth?: string;
    /** Lifecycle function invoked when the popover has fully transitioned out. */
    onHidden?: (tip: TippyInstance) => void;
    /** Lifecycle function invoked when the popover begins to transition out. */
    onHide?: (tip: TippyInstance) => void;
    /** Lifecycle function invoked when the popover has been mounted to the DOM. */
    onMount?: (tip: TippyInstance) => void;
    /** Lifecycle function invoked when the popover begins to transition in. */
    onShow?: (tip: TippyInstance) => void;
    /** Lifecycle function invoked when the popover has fully transitioned in. */
    onShown?: (tip: TippyInstance) => void;
    /**
     * Popover position. Note: With 'enableFlip' set to true,
     * it will change the position if there is not enough space for the starting position.
     * The behavior of where it flips to can be controlled through the flipBehavior prop.
     */
    position?: 'auto' | 'top' | 'bottom' | 'left' | 'right';
    /**
     * Callback function that is only invoked when isVisible is also controlled. Called when the popover Close button is
     * clicked or the ESC key is used
     */
    shouldClose?: (tip: TippyInstance) => void;
    /** z-index of the popover */
    zIndex?: number;
    /** additional Props to pass through to tippy.js */
    tippyProps?: Partial<TippyProps>;
}
export interface PopoverState {
    isOpen: boolean;
    focusTrapActive: boolean;
}
export declare class Popover extends React.Component<PopoverProps, PopoverState> {
    private tip;
    static defaultProps: PickOptional<PopoverProps>;
    constructor(props: PopoverProps);
    hideOrNotify: () => void;
    handleEscOrEnterKey: (event: KeyboardEvent) => void;
    componentDidMount(): void;
    componentWillUnmount(): void;
    storeTippyInstance: (tip: TippyInstance<TippyProps>) => void;
    closePopover: () => void;
    hideAllPopovers: () => void;
    onHide: (tip: TippyInstance<TippyProps>) => void;
    onHidden: (tip: TippyInstance<TippyProps>) => void;
    onMount: (tip: TippyInstance<TippyProps>) => void;
    onShow: (tip: TippyInstance<TippyProps>) => void;
    onShown: (tip: TippyInstance<TippyProps>) => void;
    onContentMouseDown: () => void;
    render(): JSX.Element | Error;
}
