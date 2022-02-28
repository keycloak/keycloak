import * as React from 'react';
import { Instance as TippyInstance, Props as TippyProps } from 'tippy.js';
import '@patternfly/react-styles/css/components/Tooltip/tippy.css';
import '@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css';
import { ReactElement } from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export declare enum TooltipPosition {
    auto = "auto",
    top = "top",
    bottom = "bottom",
    left = "left",
    right = "right"
}
export interface TooltipProps {
    /** The element to append the tooltip to, defaults to body */
    appendTo?: Element | ((ref: Element) => Element);
    /** Aria-labelledby or aria-describedby for tooltip */
    aria?: 'describedby' | 'labelledby';
    /** If enableFlip is true, the tooltip responds to this boundary */
    boundary?: 'scrollParent' | 'window' | 'viewport' | HTMLElement;
    /** The reference element to which the tooltip is relatively placed to */
    children: ReactElement<any>;
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
    /** Delay in ms before the tooltip disappears */
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
    flipBehavior?: 'flip' | ('top' | 'bottom' | 'left' | 'right')[];
    /** If true, displays as an application launcher */
    isAppLauncher?: boolean;
    /** Maximum width of the tooltip (default 12.5rem) */
    maxWidth?: string;
    /**
     * Tooltip position. Note: With 'enableFlip' set to true,
     * it will change the position if there is not enough space for the starting position.
     * The behavior of where it flips to can be controlled through the flipBehavior prop.
     */
    position?: 'auto' | 'top' | 'bottom' | 'left' | 'right';
    /** Tooltip trigger: click, mouseenter, focus, manual  */
    trigger?: string;
    /** Flag to indicate that the text content is left aligned */
    isContentLeftAligned?: boolean;
    /** value for visibility when trigger is 'manual' */
    isVisible?: boolean;
    /** z-index of the tooltip */
    zIndex?: number;
    /** additional Props to pass through to tippy.js */
    tippyProps?: Partial<TippyProps>;
    /** ID */
    id?: string;
}
export declare class Tooltip extends React.Component<TooltipProps> {
    private tip;
    static defaultProps: PickOptional<TooltipProps>;
    storeTippyInstance: (tip: TippyInstance<TippyProps>) => void;
    handleEscKeyClick: (event: KeyboardEvent) => void;
    componentDidMount(): void;
    componentWillUnmount(): void;
    extendChildren(): React.ReactElement<any, string | ((props: any) => React.ReactElement<any, string | any | (new (props: any) => React.Component<any, any, any>)>) | (new (props: any) => React.Component<any, any, any>)>;
    render(): JSX.Element;
}
