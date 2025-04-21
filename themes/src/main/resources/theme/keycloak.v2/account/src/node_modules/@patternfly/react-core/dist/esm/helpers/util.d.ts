/**
 * @param {string} input - String to capitalize first letter
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
 * @param {boolean} strict    true if strict mode is set, never consider the container width and element width
 *
 * @returns { boolean } True if the component is in View.
 */
export declare function isElementInView(container: HTMLElement, element: HTMLElement, partial: boolean, strict?: boolean): boolean;
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
 * @param {string} templateString  The string passed by the consumer
 * @param {object} templateVars The variables passed to the string
 *
 * @returns {string} The template string literal result
 */
export declare function fillTemplate(templateString: string, templateVars: any): string;
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
/** This function returns a list of tabbable items in a container
 *
 *  @param {any} containerRef to the container
 *  @param {string} tababbleSelectors CSS selector string of tabbable items
 */
export declare function findTabbableElements(containerRef: any, tababbleSelectors: string): any[];
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
/**
 * This function is a helper for turning arrays of breakpointMod objects for flex and grid into style object
 *
 * @param {object} mods The modifiers object
 * @param {string} css-variable The appropriate css variable for the component
 */
export declare const setBreakpointCssVars: (mods: {
    default?: string;
    sm?: string;
    md?: string;
    lg?: string;
    xl?: string;
    '2xl'?: string;
    '3xl'?: string;
}, cssVar: string) => React.CSSProperties;
export interface Mods {
    default?: string;
    sm?: string;
    md?: string;
    lg?: string;
    xl?: string;
    '2xl'?: string;
    '3xl'?: string;
}
/**
 * This function is a helper for turning arrays of breakpointMod objects for data toolbar and flex into classes
 *
 * @param {object} mods The modifiers object
 * @param {any} styles The appropriate styles object for the component
 */
export declare const formatBreakpointMods: (mods: Mods, styles: any, stylePrefix?: string, breakpoint?: 'default' | 'sm' | 'md' | 'lg' | 'xl' | '2xl') => any;
/**
 * Return the breakpoint for the given width
 *
 * @param {number | null} width The width to check
 * @returns {'default' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'} The breakpoint
 */
export declare const getBreakpoint: (width: number) => 'default' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
/**
 *
 * @param {string} s string to make camelCased
 */
export declare const toCamel: (s: string) => string;
/**
 * Copied from exenv
 */
export declare const canUseDOM: boolean;
/**
 * Calculate the width of the text
 * Example:
 * getTextWidth('my text', node)
 *
 * @param {string} text The text to calculate the width for
 * @param {HTMLElement} node The HTML element
 */
export declare const getTextWidth: (text: string, node: HTMLElement) => number;
/**
 * Get the inner dimensions of an element
 *
 * @param {HTMLElement} node HTML element to calculate the inner dimensions for
 */
export declare const innerDimensions: (node: HTMLElement) => {
    height: number;
    width: number;
};
/**
 * This function is a helper for truncating text content on the left, leaving the right side of the content in view
 *
 * @param {HTMLElement} node HTML element
 * @param {string} value The original text value
 */
export declare const trimLeft: (node: HTMLElement, value: string) => void;
/**
 * @param {string[]} events - Operations to prevent when disabled
 */
export declare const preventedEvents: (events: string[]) => {};
//# sourceMappingURL=util.d.ts.map