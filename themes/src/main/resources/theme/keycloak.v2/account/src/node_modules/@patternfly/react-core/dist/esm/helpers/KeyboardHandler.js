import * as React from 'react';
import { canUseDOM } from './util';
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
export const handleArrows = (event, navigableElements, isActiveElement = element => document.activeElement.contains(element), getFocusableElement = element => element, validSiblingTags = ['A', 'BUTTON', 'INPUT'], noVerticalArrowHandling = false, noHorizontalArrowHandling = false, updateTabIndex = true, onlyTraverseSiblings = true) => {
    const activeElement = document.activeElement;
    const key = event.key;
    let moveTarget = null;
    // Handle vertical arrow keys. If noVerticalArrowHandling is passed, skip this block
    if (!noVerticalArrowHandling) {
        if (['ArrowUp', 'ArrowDown'].includes(key)) {
            event.preventDefault();
            event.stopImmediatePropagation(); // For menus in menus
            // Traverse navigableElements to find the element which is currently active
            let currentIndex = -1;
            // while (currentIndex === -1) {
            navigableElements.forEach((element, index) => {
                if (isActiveElement(element)) {
                    // Once found, move up or down the array by 1. Determined by the vertical arrow key direction
                    let increment = 0;
                    // keep increasing the increment until you've tried the whole navigableElement
                    while (!moveTarget && increment < navigableElements.length && increment * -1 < navigableElements.length) {
                        key === 'ArrowUp' ? increment-- : increment++;
                        currentIndex = index + increment;
                        if (currentIndex >= navigableElements.length) {
                            currentIndex = 0;
                        }
                        if (currentIndex < 0) {
                            currentIndex = navigableElements.length - 1;
                        }
                        // Set the next target element (undefined if none found)
                        moveTarget = getFocusableElement(navigableElements[currentIndex]);
                    }
                }
            });
            // }
        }
    }
    // Handle horizontal arrow keys. If noHorizontalArrowHandling is passed, skip this block
    if (!noHorizontalArrowHandling) {
        if (['ArrowLeft', 'ArrowRight'].includes(key)) {
            event.preventDefault();
            event.stopImmediatePropagation(); // For menus in menus
            let currentIndex = -1;
            navigableElements.forEach((element, index) => {
                if (isActiveElement(element)) {
                    const activeRow = navigableElements[index].querySelectorAll(validSiblingTags.join(',')); // all focusable elements in my row
                    if (!activeRow.length || onlyTraverseSiblings) {
                        let nextSibling = activeElement;
                        // While a sibling exists, check each sibling to determine if it should be focussed
                        while (nextSibling) {
                            // Set the next checked sibling, determined by the horizontal arrow key direction
                            nextSibling = key === 'ArrowLeft' ? nextSibling.previousElementSibling : nextSibling.nextElementSibling;
                            if (nextSibling) {
                                if (validSiblingTags.includes(nextSibling.tagName)) {
                                    // If the sibling's tag is included in validSiblingTags, set the next target element and break the loop
                                    moveTarget = nextSibling;
                                    break;
                                }
                                // If the sibling's tag is not valid, skip to the next sibling if possible
                            }
                        }
                    }
                    else {
                        activeRow.forEach((focusableElement, index) => {
                            if (event.target === focusableElement) {
                                // Once found, move up or down the array by 1. Determined by the vertical arrow key direction
                                const increment = key === 'ArrowLeft' ? -1 : 1;
                                currentIndex = index + increment;
                                if (currentIndex >= activeRow.length) {
                                    currentIndex = 0;
                                }
                                if (currentIndex < 0) {
                                    currentIndex = activeRow.length - 1;
                                }
                                // Set the next target element
                                moveTarget = activeRow[currentIndex];
                            }
                        });
                    }
                }
            });
        }
    }
    if (moveTarget) {
        // If updateTabIndex is true, set the previously focussed element's tabIndex to -1 and the next focussed element's tabIndex to 0
        // This updates the tabIndex for a roving tabIndex
        if (updateTabIndex) {
            activeElement.tabIndex = -1;
            moveTarget.tabIndex = 0;
        }
        // If a move target has been set by either arrow handler, focus that target
        moveTarget.focus();
    }
};
/**
 * This function is a helper for setting the initial tabIndexes in a roving tabIndex
 *
 * @param {HTMLElement[]} options Array of elements which should have a tabIndex of -1, except for the first element which will have a tabIndex of 0
 */
export const setTabIndex = (options) => {
    if (options && options.length > 0) {
        // Iterate the options and set the tabIndex to -1 on every option
        options.forEach((option) => {
            option.tabIndex = -1;
        });
        // Manually set the tabIndex of the first option to 0
        options[0].tabIndex = 0;
    }
};
export class KeyboardHandler extends React.Component {
    constructor() {
        super(...arguments);
        this.keyHandler = (event) => {
            const { isEventFromContainer } = this.props;
            // If the passed keyboard event is not from the container, ignore the event by returning
            if (isEventFromContainer ? !isEventFromContainer(event) : !this._isEventFromContainer(event)) {
                return;
            }
            const { isActiveElement, getFocusableElement, noVerticalArrowHandling, noHorizontalArrowHandling, noEnterHandling, noSpaceHandling, updateTabIndex, validSiblingTags, additionalKeyHandler, createNavigableElements, onlyTraverseSiblings } = this.props;
            // Pass the event off to be handled by any custom handler
            additionalKeyHandler && additionalKeyHandler(event);
            // Initalize navigableElements from the createNavigableElements callback
            const navigableElements = createNavigableElements();
            if (!navigableElements) {
                // eslint-disable-next-line no-console
                console.warn('No navigable elements have been passed to the KeyboardHandler. Keyboard navigation provided by this component will be ignored.');
                return;
            }
            const key = event.key;
            // Handle enter key. If noEnterHandling is passed, skip this block
            if (!noEnterHandling) {
                if (key === 'Enter') {
                    event.preventDefault();
                    event.stopImmediatePropagation(); // For menus in menus
                    document.activeElement.click();
                }
            }
            // Handle space key. If noSpaceHandling is passed, skip this block
            if (!noSpaceHandling) {
                if (key === ' ') {
                    event.preventDefault();
                    event.stopImmediatePropagation(); // For menus in menus
                    document.activeElement.click();
                }
            }
            // Inject helper handler for arrow navigation
            handleArrows(event, navigableElements, isActiveElement, getFocusableElement, validSiblingTags, noVerticalArrowHandling, noHorizontalArrowHandling, updateTabIndex, onlyTraverseSiblings);
        };
        this._isEventFromContainer = (event) => {
            const { containerRef } = this.props;
            return containerRef.current && containerRef.current.contains(event.target);
        };
    }
    componentDidMount() {
        if (canUseDOM) {
            window.addEventListener('keydown', this.keyHandler);
        }
    }
    componentWillUnmount() {
        if (canUseDOM) {
            window.removeEventListener('keydown', this.keyHandler);
        }
    }
    render() {
        return null;
    }
}
KeyboardHandler.displayName = 'KeyboardHandler';
KeyboardHandler.defaultProps = {
    containerRef: null,
    createNavigableElements: () => null,
    isActiveElement: (navigableElement) => document.activeElement === navigableElement,
    getFocusableElement: (navigableElement) => navigableElement,
    validSiblingTags: ['BUTTON', 'A'],
    onlyTraverseSiblings: true,
    updateTabIndex: true,
    noHorizontalArrowHandling: false,
    noVerticalArrowHandling: false,
    noEnterHandling: false,
    noSpaceHandling: false
};
//# sourceMappingURL=KeyboardHandler.js.map