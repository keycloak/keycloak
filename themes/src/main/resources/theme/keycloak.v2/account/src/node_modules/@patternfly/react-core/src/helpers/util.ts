import * as ReactDOM from 'react-dom';
import { SIDE } from './constants';

/**
 * @param {string} input - String to capitalize first letter
 */
export function capitalize(input: string) {
  return input[0].toUpperCase() + input.substring(1);
}

/**
 * @param {string} prefix - String to prefix ID with
 */
export function getUniqueId(prefix = 'pf') {
  const uid =
    new Date().getTime() +
    Math.random()
      .toString(36)
      .slice(2);
  return `${prefix}-${uid}`;
}

/**
 * @param { any } this - "This" reference
 * @param { Function } func - Function to debounce
 * @param { number } wait - Debounce amount
 */
export function debounce(this: any, func: (...args: any[]) => any, wait: number) {
  let timeout: number;
  return (...args: any[]) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait) as any;
  };
}

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
export function isElementInView(
  container: HTMLElement,
  element: HTMLElement,
  partial: boolean,
  strict: boolean = false
): boolean {
  if (!container || !element) {
    return false;
  }
  const containerBounds = container.getBoundingClientRect();
  const elementBounds = element.getBoundingClientRect();
  const containerBoundsLeft = Math.ceil(containerBounds.left);
  const containerBoundsRight = Math.floor(containerBounds.right);
  const elementBoundsLeft = Math.ceil(elementBounds.left);
  const elementBoundsRight = Math.floor(elementBounds.right);

  // Check if in view
  const isTotallyInView = elementBoundsLeft >= containerBoundsLeft && elementBoundsRight <= containerBoundsRight;
  const isPartiallyInView =
    (partial || (!strict && containerBounds.width < elementBounds.width)) &&
    ((elementBoundsLeft < containerBoundsLeft && elementBoundsRight > containerBoundsLeft) ||
      (elementBoundsRight > containerBoundsRight && elementBoundsLeft < containerBoundsRight));

  // Return outcome
  return isTotallyInView || isPartiallyInView;
}

/** This function returns the side the element is out of view on (right, left or both)
 *
 * @param {HTMLElement} container    The container to check if the element is in view of.
 * @param {HTMLElement} element      The element to check if it is view
 *
 * @returns {string} right if the element is of the right, left if element is off the left or both if it is off on both sides.
 */
export function sideElementIsOutOfView(container: HTMLElement, element: HTMLElement): string {
  const containerBounds = container.getBoundingClientRect();
  const elementBounds = element.getBoundingClientRect();
  const containerBoundsLeft = Math.floor(containerBounds.left);
  const containerBoundsRight = Math.floor(containerBounds.right);
  const elementBoundsLeft = Math.floor(elementBounds.left);
  const elementBoundsRight = Math.floor(elementBounds.right);

  // Check if in view
  const isOffLeft = elementBoundsLeft < containerBoundsLeft;
  const isOffRight = elementBoundsRight > containerBoundsRight;

  let side = SIDE.NONE;

  if (isOffRight && isOffLeft) {
    side = SIDE.BOTH;
  } else if (isOffRight) {
    side = SIDE.RIGHT;
  } else if (isOffLeft) {
    side = SIDE.LEFT;
  }
  // Return outcome
  return side;
}

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
export function fillTemplate(templateString: string, templateVars: any) {
  return templateString.replace(/\${(.*?)}/g, (_, match) => templateVars[match] || '');
}

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
export function keyHandler(
  index: number,
  innerIndex: number,
  position: string,
  refsCollection: any[],
  kids: any[],
  custom = false
) {
  if (!Array.isArray(kids)) {
    return;
  }
  const isMultiDimensional = refsCollection.filter(ref => ref)[0].constructor === Array;
  let nextIndex = index;
  let nextInnerIndex = innerIndex;
  if (position === 'up') {
    if (index === 0) {
      // loop back to end
      nextIndex = kids.length - 1;
    } else {
      nextIndex = index - 1;
    }
  } else if (position === 'down') {
    if (index === kids.length - 1) {
      // loop back to beginning
      nextIndex = 0;
    } else {
      nextIndex = index + 1;
    }
  } else if (position === 'left') {
    if (innerIndex === 0) {
      nextInnerIndex = refsCollection[index].length - 1;
    } else {
      nextInnerIndex = innerIndex - 1;
    }
  } else if (position === 'right') {
    if (innerIndex === refsCollection[index].length - 1) {
      nextInnerIndex = 0;
    } else {
      nextInnerIndex = innerIndex + 1;
    }
  }
  if (
    refsCollection[nextIndex] === null ||
    refsCollection[nextIndex] === undefined ||
    (isMultiDimensional &&
      (refsCollection[nextIndex][nextInnerIndex] === null || refsCollection[nextIndex][nextInnerIndex] === undefined))
  ) {
    keyHandler(nextIndex, nextInnerIndex, position, refsCollection, kids, custom);
  } else if (custom) {
    if (refsCollection[nextIndex].focus) {
      refsCollection[nextIndex].focus();
    }
    // eslint-disable-next-line react/no-find-dom-node
    const element = ReactDOM.findDOMNode(refsCollection[nextIndex]) as HTMLElement;
    element.focus();
  } else if (position !== 'tab') {
    if (isMultiDimensional) {
      refsCollection[nextIndex][nextInnerIndex].focus();
    } else {
      refsCollection[nextIndex].focus();
    }
  }
}

/** This function returns a list of tabbable items in a container
 *
 *  @param {any} containerRef to the container
 *  @param {string} tababbleSelectors CSS selector string of tabbable items
 */
export function findTabbableElements(containerRef: any, tababbleSelectors: string): any[] {
  const tabbable = containerRef.current.querySelectorAll(tababbleSelectors);
  const list = Array.prototype.filter.call(tabbable, function(item) {
    return item.tabIndex >= '0';
  });
  return list;
}

/** This function is a helper for keyboard navigation through dropdowns.
 *
 * @param {number} index The index of the element you're on
 * @param {string} position The orientation of the dropdown
 * @param {string[]} collection Array of refs to the items in the dropdown
 */
export function getNextIndex(index: number, position: string, collection: any[]): number {
  let nextIndex;
  if (position === 'up') {
    if (index === 0) {
      // loop back to end
      nextIndex = collection.length - 1;
    } else {
      nextIndex = index - 1;
    }
  } else if (index === collection.length - 1) {
    // loop back to beginning
    nextIndex = 0;
  } else {
    nextIndex = index + 1;
  }
  if (collection[nextIndex] === undefined || collection[nextIndex][0] === null) {
    return getNextIndex(nextIndex, position, collection);
  } else {
    return nextIndex;
  }
}

/** This function is a helper for pluralizing strings.
 *
 * @param {number} i The quantity of the string you want to pluralize
 * @param {string} singular The singular version of the string
 * @param {string} plural The change to the string that should occur if the quantity is not equal to 1.
 *                 Defaults to adding an 's'.
 */
export function pluralize(i: number, singular: string, plural?: string) {
  if (!plural) {
    plural = `${singular}s`;
  }
  return `${i || 0} ${i === 1 ? singular : plural}`;
}

/**
 * This function is a helper for turning arrays of breakpointMod objects for flex and grid into style object
 *
 * @param {object} mods The modifiers object
 * @param {string} css-variable The appropriate css variable for the component
 */
export const setBreakpointCssVars = (
  mods: {
    default?: string;
    sm?: string;
    md?: string;
    lg?: string;
    xl?: string;
    '2xl'?: string;
    '3xl'?: string;
  },
  cssVar: string
): React.CSSProperties =>
  Object.entries(mods || {}).reduce(
    (acc, [breakpoint, value]) =>
      breakpoint === 'default' ? { ...acc, [cssVar]: value } : { ...acc, [`${cssVar}-on-${breakpoint}`]: value },
    {}
  );

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
export const formatBreakpointMods = (
  mods: Mods,
  styles: any,
  stylePrefix: string = '',
  breakpoint?: 'default' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'
) => {
  if (!mods) {
    return '';
  }
  if (breakpoint) {
    if (breakpoint in mods) {
      return styles.modifiers[toCamel(`${stylePrefix}${mods[breakpoint as keyof Mods]}`)];
    }
    // the current breakpoint is not specified in mods, so we try to find the next nearest
    const breakpointsOrder = ['2xl', 'xl', 'lg', 'md', 'sm', 'default'];
    const breakpointsIndex = breakpointsOrder.indexOf(breakpoint);
    for (let i = breakpointsIndex; i < breakpointsOrder.length; i++) {
      if (breakpointsOrder[i] in mods) {
        return styles.modifiers[toCamel(`${stylePrefix}${mods[breakpointsOrder[i] as keyof Mods]}`)];
      }
    }
    return '';
  }
  return Object.entries(mods || {})
    .map(([breakpoint, mod]) => `${stylePrefix}${mod}${breakpoint !== 'default' ? `-on-${breakpoint}` : ''}`)
    .map(toCamel)
    .map(mod => mod.replace(/-?(\dxl)/gi, (_res, group) => `_${group}`))
    .map(modifierKey => styles.modifiers[modifierKey])
    .filter(Boolean)
    .join(' ');
};

/**
 * Return the breakpoint for the given width
 *
 * @param {number | null} width The width to check
 * @returns {'default' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'} The breakpoint
 */
export const getBreakpoint = (width: number): 'default' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' => {
  if (width === null) {
    return null;
  }
  if (width >= 1450) {
    return '2xl';
  }
  if (width >= 1200) {
    return 'xl';
  }
  if (width >= 992) {
    return 'lg';
  }
  if (width >= 768) {
    return 'md';
  }
  if (width >= 576) {
    return 'sm';
  }
  return 'default';
};

const camelize = (s: string) =>
  s
    .toUpperCase()
    .replace('-', '')
    .replace('_', '');
/**
 *
 * @param {string} s string to make camelCased
 */
export const toCamel = (s: string) => s.replace(/([-_][a-z])/gi, camelize);

/**
 * Copied from exenv
 */
export const canUseDOM = !!(typeof window !== 'undefined' && window.document && window.document.createElement);

/**
 * Calculate the width of the text
 * Example:
 * getTextWidth('my text', node)
 *
 * @param {string} text The text to calculate the width for
 * @param {HTMLElement} node The HTML element
 */
export const getTextWidth = (text: string, node: HTMLElement) => {
  const computedStyle = getComputedStyle(node);
  // Firefox returns the empty string for .font, so this function creates the .font property manually
  const getFontFromComputedStyle = () => {
    let computedFont = '';
    // Firefox uses percentages for font-stretch, but Canvas does not accept percentages
    // so convert to keywords, as listed at:
    // https://developer.mozilla.org/en-US/docs/Web/CSS/font-stretch
    const fontStretchLookupTable = {
      '50%': 'ultra-condensed',
      '62.5%': 'extra-condensed',
      '75%': 'condensed',
      '87.5%': 'semi-condensed',
      '100%': 'normal',
      '112.5%': 'semi-expanded',
      '125%': 'expanded',
      '150%': 'extra-expanded',
      '200%': 'ultra-expanded'
    };
    // If the retrieved font-stretch percentage isn't found in the lookup table, use
    // 'normal' as a last resort.
    let fontStretch;
    if (computedStyle.fontStretch in fontStretchLookupTable) {
      fontStretch = (fontStretchLookupTable as any)[computedStyle.fontStretch];
    } else {
      fontStretch = 'normal';
    }
    computedFont =
      computedStyle.fontStyle +
      ' ' +
      computedStyle.fontVariant +
      ' ' +
      computedStyle.fontWeight +
      ' ' +
      fontStretch +
      ' ' +
      computedStyle.fontSize +
      '/' +
      computedStyle.lineHeight +
      ' ' +
      computedStyle.fontFamily;
    return computedFont;
  };

  const canvas = document.createElement('canvas');
  const context = canvas.getContext('2d');
  context.font = computedStyle.font || getFontFromComputedStyle();

  return context.measureText(text).width;
};

/**
 * Get the inner dimensions of an element
 *
 * @param {HTMLElement} node HTML element to calculate the inner dimensions for
 */
export const innerDimensions = (node: HTMLElement) => {
  const computedStyle = getComputedStyle(node);

  let width = node.clientWidth; // width with padding
  let height = node.clientHeight; // height with padding

  height -= parseFloat(computedStyle.paddingTop) + parseFloat(computedStyle.paddingBottom);
  width -= parseFloat(computedStyle.paddingLeft) + parseFloat(computedStyle.paddingRight);
  return { height, width };
};

/**
 * This function is a helper for truncating text content on the left, leaving the right side of the content in view
 *
 * @param {HTMLElement} node HTML element
 * @param {string} value The original text value
 */
export const trimLeft = (node: HTMLElement, value: string) => {
  const availableWidth = innerDimensions(node).width;
  let newValue = value;
  if (getTextWidth(value, node) > availableWidth) {
    // we have text overflow, trim the text to the left and add ... in the front until it fits
    while (getTextWidth(`...${newValue}`, node) > availableWidth) {
      newValue = newValue.substring(1);
    }
    // replace text with our truncated text
    if ((node as HTMLInputElement).value) {
      (node as HTMLInputElement).value = `...${newValue}`;
    } else {
      node.innerText = `...${newValue}`;
    }
  } else {
    if ((node as HTMLInputElement).value) {
      (node as HTMLInputElement).value = value;
    } else {
      node.innerText = value;
    }
  }
};

/**
 * @param {string[]} events - Operations to prevent when disabled
 */
export const preventedEvents = (events: string[]) =>
  events.reduce(
    (handlers, eventToPrevent) => ({
      ...handlers,
      [eventToPrevent]: (event: React.SyntheticEvent<HTMLElement>) => {
        event.preventDefault();
      }
    }),
    {}
  );
