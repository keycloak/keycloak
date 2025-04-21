import * as React from 'react';
export interface KeyboardHandlerProps {
    /** Reference of the container to apply keyboard interaction */
    containerRef: React.RefObject<any>;
    /** Callback returning an array of navigable elements to be traversable via vertical arrow keys. This array should not include non-navigable elements such as disabled elements. */
    createNavigableElements: () => Element[];
    /** Callback to determine if a given event is from the container. By default the function conducts a basic check to see if the containerRef contains the event target */
    isEventFromContainer?: (event: KeyboardEvent) => boolean;
    /** Additional key handling outside of the included arrow keys, enter, and space handling */
    additionalKeyHandler?: (event: KeyboardEvent) => void;
    /** Callback to determine if a given element from the navigable elements array is the active element of the page */
    isActiveElement?: (navigableElement: Element) => boolean;
    /** Callback returning the focusable element of a given element from the navigable elements array */
    getFocusableElement?: (navigableElement: Element) => Element;
    /** Valid sibling tags that horizontal arrow handling will focus */
    validSiblingTags?: string[];
    /** Flag indicating that the tabIndex of the currently focused element and next focused element should be updated, in the case of using a roving tabIndex */
    updateTabIndex?: boolean;
    /** Flag indicating that next focusable element of a horizontal movement will be this element's sibling */
    onlyTraverseSiblings?: boolean;
    /** Flag indicating that the included vertical arrow key handling should be ignored */
    noVerticalArrowHandling?: boolean;
    /** Flag indicating that the included horizontal arrow key handling should be ignored */
    noHorizontalArrowHandling?: boolean;
    /** Flag indicating that the included enter key handling should be ignored */
    noEnterHandling?: boolean;
    /** Flag indicating that the included space key handling should be ignored */
    noSpaceHandling?: boolean;
}
/**
 * This function is a helper for handling basic arrow keyboard interactions. If a component already has its own key handler and event start up/tear down, this function may be easier to integrate in over the full component.
 *
 * @param {event} event Event triggered by the keyboard
 * @param {element[]} navigableElements Valid traversable elements of the container
 * @param {function} isActiveElement Callback to determine if a given element from the navigable elements array is the active element of the page
 * @param {function} getFocusableElement Callback returning the focusable element of a given element from the navigable elements array
 * @param {string[]} validSiblingTags Valid sibling tags that horizontal arrow handling will focus
 * @param {boolean} noVerticalArrowHandling Flag indicating that the included vertical arrow key handling should be ignored
 * @param {boolean} noHorizontalArrowHandling Flag indicating that the included horizontal arrow key handling should be ignored
 * @param {boolean} updateTabIndex Flag indicating that the tabIndex of the currently focused element and next focused element should be updated, in the case of using a roving tabIndex
 * @param {boolean} onlyTraverseSiblings Flag indicating that next focusable element of a horizontal movement will be this element's sibling
 */
export declare const handleArrows: (event: KeyboardEvent, navigableElements: Element[], isActiveElement?: (element: Element) => boolean, getFocusableElement?: (element: Element) => Element, validSiblingTags?: string[], noVerticalArrowHandling?: boolean, noHorizontalArrowHandling?: boolean, updateTabIndex?: boolean, onlyTraverseSiblings?: boolean) => void;
/**
 * This function is a helper for setting the initial tabIndexes in a roving tabIndex
 *
 * @param {HTMLElement[]} options Array of elements which should have a tabIndex of -1, except for the first element which will have a tabIndex of 0
 */
export declare const setTabIndex: (options: HTMLElement[]) => void;
export declare class KeyboardHandler extends React.Component<KeyboardHandlerProps> {
    static displayName: string;
    static defaultProps: KeyboardHandlerProps;
    componentDidMount(): void;
    componentWillUnmount(): void;
    keyHandler: (event: KeyboardEvent) => void;
    _isEventFromContainer: (event: KeyboardEvent) => any;
    render(): React.ReactNode;
}
//# sourceMappingURL=KeyboardHandler.d.ts.map