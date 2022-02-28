"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.capitalize = capitalize;
exports.getUniqueId = getUniqueId;
exports.debounce = debounce;
exports.isElementInView = isElementInView;
exports.sideElementIsOutOfView = sideElementIsOutOfView;
exports.fillTemplate = fillTemplate;
exports.keyHandler = keyHandler;
exports.getNextIndex = getNextIndex;
exports.pluralize = pluralize;
exports.canUseDOM = exports.formatBreakpointMods = void 0;

var ReactDOM = _interopRequireWildcard(require("react-dom"));

var _constants = require("./constants");

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function isNativeReflectConstruct() { if (typeof Reflect === "undefined" || !Reflect.construct) return false; if (Reflect.construct.sham) return false; if (typeof Proxy === "function") return true; try { Date.prototype.toString.call(Reflect.construct(Date, [], function () {})); return true; } catch (e) { return false; } }

function _construct(Parent, args, Class) { if (isNativeReflectConstruct()) { _construct = Reflect.construct; } else { _construct = function _construct(Parent, args, Class) { var a = [null]; a.push.apply(a, args); var Constructor = Function.bind.apply(Parent, a); var instance = new Constructor(); if (Class) _setPrototypeOf(instance, Class.prototype); return instance; }; } return _construct.apply(null, arguments); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance"); }

function _iterableToArray(iter) { if (Symbol.iterator in Object(iter) || Object.prototype.toString.call(iter) === "[object Arguments]") return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = new Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } }

/**
 * @param {string} input - String to capitalize
 */
function capitalize(input) {
  return input[0].toUpperCase() + input.substring(1);
}
/**
 * @param {string} prefix - String to prefix ID with
 */


function getUniqueId() {
  var prefix = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : 'pf';
  var uid = new Date().getTime() + Math.random().toString(36).slice(2);
  return "".concat(prefix, "-").concat(uid);
}
/**
 * @param { any } this - "This" reference
 * @param { Function } func - Function to debounce
 * @param { number } wait - Debounce amount
 */


function debounce(func, wait) {
  var _this = this;

  var timeout;
  return function () {
    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    clearTimeout(timeout);
    timeout = setTimeout(function () {
      return func.apply(_this, args);
    }, wait);
  };
}
/** This function returns whether or not an element is within the viewable area of a container. If partial is true,
 * then this function will return true even if only part of the element is in view.
 *
 * @param {HTMLElement} container  The container to check if the element is in view of.
 * @param {HTMLElement} element    The element to check if it is view
 * @param {boolean} partial   true if partial view is allowed
 *
 * @returns { boolean } True if the component is in View.
 */


function isElementInView(container, element, partial) {
  var containerBounds = container.getBoundingClientRect();
  var elementBounds = element.getBoundingClientRect();
  var containerBoundsLeft = Math.floor(containerBounds.left);
  var containerBoundsRight = Math.floor(containerBounds.right);
  var elementBoundsLeft = Math.floor(elementBounds.left);
  var elementBoundsRight = Math.floor(elementBounds.right); // Check if in view

  var isTotallyInView = elementBoundsLeft >= containerBoundsLeft && elementBoundsRight <= containerBoundsRight;
  var isPartiallyInView = partial && (elementBoundsLeft < containerBoundsLeft && elementBoundsRight > containerBoundsLeft || elementBoundsRight > containerBoundsRight && elementBoundsLeft < containerBoundsRight); // Return outcome

  return isTotallyInView || isPartiallyInView;
}
/** This function returns the side the element is out of view on (right, left or both)
 *
 * @param {HTMLElement} container    The container to check if the element is in view of.
 * @param {HTMLElement} element      The element to check if it is view
 *
 * @returns {string} right if the element is of the right, left if element is off the left or both if it is off on both sides.
 */


function sideElementIsOutOfView(container, element) {
  var containerBounds = container.getBoundingClientRect();
  var elementBounds = element.getBoundingClientRect();
  var containerBoundsLeft = Math.floor(containerBounds.left);
  var containerBoundsRight = Math.floor(containerBounds.right);
  var elementBoundsLeft = Math.floor(elementBounds.left);
  var elementBoundsRight = Math.floor(elementBounds.right); // Check if in view

  var isOffLeft = elementBoundsLeft < containerBoundsLeft;
  var isOffRight = elementBoundsRight > containerBoundsRight;
  var side = _constants.SIDE.NONE;

  if (isOffRight && isOffLeft) {
    side = _constants.SIDE.BOTH;
  } else if (isOffRight) {
    side = _constants.SIDE.RIGHT;
  } else if (isOffLeft) {
    side = _constants.SIDE.LEFT;
  } // Return outcome


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
 * @param {object} templateString  The string passed by the consumer
 * @param {object} templateVars The variables passed to the string
 *
 * @returns {string} The template string literal result
 */


function fillTemplate(templateString, templateVars) {
  var func = _construct(Function, _toConsumableArray(Object.keys(templateVars)).concat(["return `".concat(templateString, "`;")]));

  return func.apply(void 0, _toConsumableArray(Object.values(templateVars)));
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


function keyHandler(index, innerIndex, position, refsCollection, kids) {
  var custom = arguments.length > 5 && arguments[5] !== undefined ? arguments[5] : false;

  if (!Array.isArray(kids)) {
    return;
  }

  var isMultiDimensional = refsCollection.filter(function (ref) {
    return ref;
  })[0].constructor === Array;
  var nextIndex = index;
  var nextInnerIndex = innerIndex;

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

  if (refsCollection[nextIndex] === null || refsCollection[nextIndex] === undefined || isMultiDimensional && (refsCollection[nextIndex][nextInnerIndex] === null || refsCollection[nextIndex][nextInnerIndex] === undefined)) {
    keyHandler(nextIndex, nextInnerIndex, position, refsCollection, kids, custom);
  } else if (custom) {
    if (refsCollection[nextIndex].focus) {
      refsCollection[nextIndex].focus();
    } // eslint-disable-next-line react/no-find-dom-node


    var element = ReactDOM.findDOMNode(refsCollection[nextIndex]);
    element.focus();
  } else {
    if (isMultiDimensional) {
      refsCollection[nextIndex][nextInnerIndex].focus();
    } else {
      refsCollection[nextIndex].focus();
    }
  }
}
/** This function is a helper for keyboard navigation through dropdowns.
 *
 * @param {number} index The index of the element you're on
 * @param {string} position The orientation of the dropdown
 * @param {string[]} collection Array of refs to the items in the dropdown
 */


function getNextIndex(index, position, collection) {
  var nextIndex;

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

  if (collection[nextIndex] === null) {
    getNextIndex(nextIndex, position, collection);
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


function pluralize(i, singular, plural) {
  if (!plural) {
    plural = "".concat(singular, "s");
  }

  return "".concat(i || 0, " ").concat(i === 1 ? singular : plural);
}
/** This function is a helper for turning arrays of breakpointMod objects for data toolbar and flex into classes
 *
 * @param {(DataToolbarBreakpointMod | FlexBreakpointMod | FlexItemBreakpointMod)[]} breakpointMods The modifiers object
 * @param {any} styles The appropriate styles object for the component
 */


var formatBreakpointMods = function formatBreakpointMods(breakpointMods, styles) {
  return breakpointMods.reduce(function (acc, curr) {
    return "".concat(acc).concat(acc && ' ').concat((0, _reactStyles.getModifier)(styles, "".concat(curr.modifier).concat(curr.breakpoint ? "-on-".concat(curr.breakpoint) : '')));
  }, '');
};

exports.formatBreakpointMods = formatBreakpointMods;
var canUseDOM = !!(typeof window !== 'undefined' && window.document && window.document.createElement);
exports.canUseDOM = canUseDOM;
//# sourceMappingURL=util.js.map