import { DataToolbarBreakpointMod } from '../components/DataToolbar/DataToolbarUtils';
import { FlexBreakpointMod, FlexItemBreakpointMod } from '../layouts/Flex/FlexUtils';
/**
 * @param {string} input - String to capitalize
 */
export declare function capitalize(input: string): string;
/**
 * @param {string} prefix - String to prefix ID with
 */
export declare function getUniqueId(prefix?: string): string;
/**
 * @param { any } this - "This" reference
 * @param { Function } func - Function to debounce
 * @param { number } wait - Debounce amount
 */
export declare function debounce(this: any, func: (...args: any[]) => any, wait: number): (...args: any[]) => void;
/** This function returns whether or not an element is within the viewable area of a container. If partial is true,
 * then this function will return true even if only part of the element is in view.
 *
 * @param {HTMLElement} container  The container to check if the element is in view of.
 * @param {HTMLElement} element    The element to check if it is view
 * @param {boolean} partial   true if partial view is allowed
 *
 * @returns { boolean } True if the component is in View.
 */
export declare function isElementInView(container: HTMLElement, element: HTMLElement, partial: boolean): boolean;
/** This function returns the side the element is out of view on (right, left or both)
 *
 * @param {HTMLElement} container    The container to check if the element is in view of.
 * @param {HTMLElement} element      The element to check if it is view
 *
 * @returns {string} right if the element is of the right, left if element is off the left or both if it is off on both sides.
 */
export declare function sideElementIsOutOfView(container: HTMLElement, element: HTMLElement): string;
/** Interpolates a parameterized templateString using values from a templateVars object.
 * The templateVars object should have keys and values which match the templateString's parameters.
 * Example:
 *    const templateString: 'My name is ${firstName} ${lastName}';
 *    const templateVars: {
 *      firstName: 'Jon'
 *      lastName: 'Dough'
 *    };
 *    const result = fillTemplate(templateString, templateVars);
 *    // "My name is Jon Dough"
 *
 * @param {object} templateString  The string passed by the consumer
 * @param {object} templateVars The variables passed to the string
 *
 * @returns {string} The template string literal result
 */
export declare function fillTemplate(templateString: string, templateVars: any): any;
/**
 * This function allows for keyboard navigation through dropdowns. The custom argument is optional.
 *
 * @param {number} index The index of the element you're on
 * @param {number} innerIndex Inner index number
 * @param {string} position The orientation of the dropdown
 * @param {string[]} refsCollection Array of refs to the items in the dropdown
 * @param {object[]} kids Array of items in the dropdown
 * @param {boolean} [custom] Allows for handling of flexible content
 */
export declare function keyHandler(index: number, innerIndex: number, position: string, refsCollection: any[], kids: any[], custom?: boolean): void;
/** This function is a helper for keyboard navigation through dropdowns.
 *
 * @param {number} index The index of the element you're on
 * @param {string} position The orientation of the dropdown
 * @param {string[]} collection Array of refs to the items in the dropdown
 */
export declare function getNextIndex(index: number, position: string, collection: any[]): number;
/** This function is a helper for pluralizing strings.
 *
 * @param {number} i The quantity of the string you want to pluralize
 * @param {string} singular The singular version of the string
 * @param {string} plural The change to the string that should occur if the quantity is not equal to 1.
 *                 Defaults to adding an 's'.
 */
export declare function pluralize(i: number, singular: string, plural?: string): string;
/** This function is a helper for turning arrays of breakpointMod objects for data toolbar and flex into classes
 *
 * @param {(DataToolbarBreakpointMod | FlexBreakpointMod | FlexItemBreakpointMod)[]} breakpointMods The modifiers object
 * @param {any} styles The appropriate styles object for the component
 */
export declare const formatBreakpointMods: (breakpointMods: (DataToolbarBreakpointMod | FlexBreakpointMod | FlexItemBreakpointMod)[], styles: any) => string;
export declare const canUseDOM: boolean;
