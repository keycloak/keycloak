'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _ariaQuery = require('aria-query');

var _arrayIncludes = require('array-includes');

var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }

var domElements = [].concat(_toConsumableArray(_ariaQuery.dom.keys()));

/**
 * Returns boolean indicating whether the given element is a DOM element.
 */
var isDOMElement = function isDOMElement(tagName) {
  return (0, _arrayIncludes2.default)(domElements, tagName);
};

exports.default = isDOMElement;