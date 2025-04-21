import * as React from 'react';
import { ReactElement } from 'react';
import { Props as TippyProps } from '../../helpers/Popper/DeprecatedTippyTypes';
export declare enum TooltipPosition {
    auto = "auto",
    top = "top",
    bottom = "bottom",
    left = "left",
    right = "right",
    topStart = "top-start",
    topEnd = "top-end",
    bottomStart = "bottom-start",
    bottomEnd = "bottom-end",
    leftStart = "left-start",
    leftEnd = "left-end",
    rightStart = "right-start",
    rightEnd = "right-end"
}
export interface TooltipProps extends Omit<React.HTMLProps<HTMLDivElement>, 'content'> {
    /** The element to append the tooltip to, defaults to body */
    appendTo?: HTMLElement | ((ref?: HTMLElement) => HTMLElement);
    /**
     * aria-labelledby or aria-describedby for tooltip.
     * The trigger will be cloned to add the aria attribute, and the corresponding id in the form of 'pf-tooltip-#' is added to the content container.
     * If you don't want that or prefer to add the aria attribute yourself on the trigger, set aria to 'none'.
     */
    aria?: 'describedby' | 'labelledby' | 'none';
    /**
     * Determines whether the tooltip is an aria-live region. If the reference prop is passed in the
     * default behavior is 'polite' in order to ensure the tooltip contents is announced to
     * assistive technologies. Otherwise the default behavior is 'off'.
     */
    'aria-live'?: 'off' | 'polite';
    /**
     * The reference element to which the Tooltip is relatively placed to.
     * If you cannot wrap the reference with the Tooltip, you can use the reference prop instead.
     * Usage: <Tooltip><Button>Reference</Button></Tooltip>
     */
    children?: ReactElement<any>;
    /**
     * The reference element to which the Tooltip is relatively placed to.
     * If you can wrap the reference with the Tooltip, you can use the children prop instead.
     * Usage: <Tooltip reference={() => document.getElementById('reference-element')} />
     */
    reference?: HTMLElement | (() => HTMLElement) | React.RefObject<any>;
    /** Tooltip additional class */
    className?: string;
    /** Tooltip content */
    content: React.ReactNode;
    /** Distance of the tooltip to its target, defaults to 15 */
    distance?: number;
    /** If true, tries to keep the tooltip in view by flipping it if necessary */
    enableFlip?: boolean;
    /** Delay in ms before the tooltip appears */
    entryDelay?: number;
    /** Delay in ms before the tooltip disappears, Avoid passing in a value of "0", as users should
     * be given ample time to move their mouse from the trigger to the tooltip content without the content
     * being hidden.
     */
    exitDelay?: number;
    /**
     * The desired position to flip the tooltip to if the initial position is not possible.
     * By setting this prop to 'flip' it attempts to flip the tooltip to the opposite side if there is no space.
     * You can also pass an array of positions that determines the flip order. It should contain the initial position
     * followed by alternative positions if that position is unavailable.
     * Example: Initial position is 'top'. Button with tooltip is in the top right corner. 'flipBehavior' is set to
     * ['top', 'right', 'left']. Since there is no space to the top, it checks if right is available. There's also no
     * space to the right, so it finally shows the tooltip on the left.
     */
    flipBehavior?: 'flip' | ('top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end')[];
    /** Maximum width of the tooltip (default 18.75rem) */
    maxWidth?: string;
    /**
     * Tooltip position. Note: With 'enableFlip' set to true,
     * it will change the position if there is not enough space for the starting position.
     * The behavior of where it flips to can be controlled through the flipBehavior prop.
     * The 'auto' position chooses the side with the most space.
     * The 'auto' position requires the 'enableFlip' prop to be true.
     */
    position?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
    /**
     * Tooltip trigger: click, mouseenter, focus, manual
     * Set to manual to trigger tooltip programmatically (through the isVisible prop)
     */
    trigger?: string;
    /** Flag to indicate that the text content is left aligned */
    isContentLeftAligned?: boolean;
    /** value for visibility when trigger is 'manual' */
    isVisible?: boolean;
    /** z-index of the tooltip */
    zIndex?: number;
    /** id of the tooltip */
    id?: string;
    /** CSS fade transition animation duration */
    animationDuration?: number;
    /** @deprecated - no longer used. if you want to constrain the popper to a specific element use the appendTo prop instead */
    boundary?: 'scrollParent' | 'window' | 'viewport' | HTMLElement;
    /** @deprecated - no longer used */
    isAppLauncher?: boolean;
    /** @deprecated - no longer used */
    tippyProps?: Partial<TippyProps>;
}
export declare const Tooltip: React.FunctionComponent<TooltipProps>;
//# sourceMappingURL=Tooltip.d.ts.map