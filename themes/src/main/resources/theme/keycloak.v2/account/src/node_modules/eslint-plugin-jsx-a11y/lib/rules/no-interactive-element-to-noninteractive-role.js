"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _toConsumableArray2 = _interopRequireDefault(require("@babel/runtime/helpers/toConsumableArray"));

var _ariaQuery = require("aria-query");

var _jsxAstUtils = require("jsx-ast-utils");

var _arrayIncludes = _interopRequireDefault(require("array-includes"));

var _has = _interopRequireDefault(require("has"));

var _getElementType = _interopRequireDefault(require("../util/getElementType"));

var _isInteractiveElement = _interopRequireDefault(require("../util/isInteractiveElement"));

var _isNonInteractiveRole = _interopRequireDefault(require("../util/isNonInteractiveRole"));

var _isPresentationRole = _interopRequireDefault(require("../util/isPresentationRole"));

/**
 * @fileoverview Disallow inherently interactive elements to be assigned
 * non-interactive roles.
 * @author Jesse Beach
 * 
 */
// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------
var errorMessage = 'Interactive elements should not be assigned non-interactive roles.';
var domElements = (0, _toConsumableArray2["default"])(_ariaQuery.dom.keys());
var _default = {
  meta: {
    docs: {
      url: 'https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/tree/HEAD/docs/rules/no-interactive-element-to-noninteractive-role.md',
      description: 'Interactive elements should not be assigned non-interactive roles.',
      errorOptions: true
    },
    schema: [{
      type: 'object',
      additionalProperties: {
        type: 'array',
        items: {
          type: 'string'
        },
        uniqueItems: true
      }
    }]
  },
  create: function create(context) {
    var options = context.options;
    var elementType = (0, _getElementType["default"])(context);
    return {
      JSXAttribute: function JSXAttribute(attribute) {
        var attributeName = (0, _jsxAstUtils.propName)(attribute); // $FlowFixMe: [TODO] Mark propName as a JSXIdentifier, not a string.

        if (attributeName !== 'role') {
          return;
        }

        var node = attribute.parent;
        var attributes = node.attributes;
        var type = elementType(node);
        var role = (0, _jsxAstUtils.getLiteralPropValue)((0, _jsxAstUtils.getProp)(node.attributes, 'role'));

        if (!(0, _arrayIncludes["default"])(domElements, type)) {
          // Do not test higher level JSX components, as we do not know what
          // low-level DOM element this maps to.
          return;
        } // Allow overrides from rule configuration for specific elements and
        // roles.


        var allowedRoles = options[0] || {};

        if ((0, _has["default"])(allowedRoles, type) && (0, _arrayIncludes["default"])(allowedRoles[type], role)) {
          return;
        }

        if ((0, _isInteractiveElement["default"])(type, attributes) && ((0, _isNonInteractiveRole["default"])(type, attributes) || (0, _isPresentationRole["default"])(type, attributes))) {
          // Visible, non-interactive elements should not have an interactive handler.
          context.report({
            node: attribute,
            message: errorMessage
          });
        }
      }
    };
  }
};
exports["default"] = _default;
module.exports = exports.default;