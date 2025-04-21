import * as React from 'react';
import { Placement } from './thirdparty/popper-core';
import '@patternfly/react-styles/css/components/Popper/Popper.css';
/** @deprecated Please use the menuAppendTo prop directly from within the PF component which uses it. */
export interface ToggleMenuBaseProps {
    /** The container to append the menu to. Defaults to 'inline'
     * If your menu is being cut off you can append it to an element higher up the DOM tree.
     * Some examples:
     * menuAppendTo="parent"
     * menuAppendTo={() => document.body}
     * menuAppendTo={document.getElementById('target')}
     */
    menuAppendTo?: HTMLElement | (() => HTMLElement) | 'parent' | 'inline';
}
export declare const getOpacityTransition: (animationDuration: number) => string;
export interface PopperProps {
    /**
     * The reference element to which the Popover is relatively placed to.
     * Use either trigger or reference, not both.
     */
    trigger?: React.ReactNode;
    /**
     * The reference element to which the Popover is relatively placed to.
     * Use either trigger or reference, not both.
     */
    reference?: HTMLElement | (() => HTMLElement) | React.RefObject<any>;
    /** The popper (menu/tooltip/popover) element */
    popper: React.ReactElement;
    /** True to set the width of the popper element to the trigger element's width */
    popperMatchesTriggerWidth?: boolean;
    /** popper direction */
    direction?: 'up' | 'down';
    /** popper position */
    position?: 'right' | 'left' | 'center';
    /** Instead of direction and position can set the placement of the popper */
    placement?: Placement;
    /** The container to append the popper to. Defaults to 'document.body' */
    appendTo?: HTMLElement | (() => HTMLElement);
    /** z-index of the popper element */
    zIndex?: number;
    /** True to make the popper visible */
    isVisible?: boolean;
    /**
     * Map class names to positions, for example:
     * {
     *   top: styles.modifiers.top,
     *   bottom: styles.modifiers.bottom,
     *   left: styles.modifiers.left,
     *   right: styles.modifiers.right
     * }
     */
    positionModifiers?: {
        top?: string;
        right?: string;
        bottom?: string;
        left?: string;
        topStart?: string;
        topEnd?: string;
        bottomStart?: string;
        bottomEnd?: string;
        leftStart?: string;
        leftEnd?: string;
        rightStart?: string;
        rightEnd?: string;
    };
    /** Distance of the popper to the trigger */
    distance?: number;
    /** Callback function when mouse enters trigger */
    onMouseEnter?: (event?: MouseEvent) => void;
    /** Callback function when mouse leaves trigger */
    onMouseLeave?: (event?: MouseEvent) => void;
    /** Callback function when trigger is focused */
    onFocus?: (event?: FocusEvent) => void;
    /** Callback function when trigger is blurred (focus leaves) */
    onBlur?: (event?: FocusEvent) => void;
    /** Callback function when trigger is clicked */
    onTriggerClick?: (event?: MouseEvent) => void;
    /** Callback function when Enter key is used on trigger */
    onTriggerEnter?: (event?: KeyboardEvent) => void;
    /** Callback function when popper is clicked */
    onPopperClick?: (event?: MouseEvent) => void;
    /** Callback function when mouse enters popper content */
    onPopperMouseEnter?: (event?: MouseEvent) => void;
    /** Callback function when mouse leaves popper content */
    onPopperMouseLeave?: (event?: MouseEvent) => void;
    /** Callback function when document is clicked */
    onDocumentClick?: (event?: MouseEvent, triggerElement?: HTMLElement, popperElement?: HTMLElement) => void;
    /** Callback function when keydown event occurs on document */
    onDocumentKeyDown?: (event?: KeyboardEvent) => void;
    /** Enable to flip the popper when it reaches the boundary */
    enableFlip?: boolean;
    /** The behavior of how the popper flips when it reaches the boundary */
    flipBehavior?: 'flip' | ('top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end')[];
}
export declare const Popper: React.FunctionComponent<PopperProps>;
//# sourceMappingURL=Popper.d.ts.map