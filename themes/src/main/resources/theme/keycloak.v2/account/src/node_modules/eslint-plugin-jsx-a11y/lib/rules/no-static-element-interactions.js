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

var _schemas = require("../util/schemas");

var _getElementType = _interopRequireDefault(require("../util/getElementType"));

var _isAbstractRole = _interopRequireDefault(require("../util/isAbstractRole"));

var _isHiddenFromScreenReader = _interopRequireDefault(require("../util/isHiddenFromScreenReader"));

var _isInteractiveElement = _interopRequireDefault(require("../util/isInteractiveElement"));

var _isInteractiveRole = _interopRequireDefault(require("../util/isInteractiveRole"));

var _isNonInteractiveElement = _interopRequireDefault(require("../util/isNonInteractiveElement"));

var _isNonInteractiveRole = _interopRequireDefault(require("../util/isNonInteractiveRole"));

var _isNonLiteralProperty = _interopRequireDefault(require("../util/isNonLiteralProperty"));

var _isPresentationRole = _interopRequireDefault(require("../util/isPresentationRole"));

/**
 * @fileoverview Enforce static elements have no interactive handlers.
 * @author Ethan Cohen
 * 
 */
// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------
var errorMessage = 'Avoid non-native interactive elements. If using native HTML is not possible, add an appropriate role and support for tabbing, mouse, keyboard, and touch inputs to an interactive content element.';
var domElements = (0, _toConsumableArray2["default"])(_ariaQuery.dom.keys());
var defaultInteractiveProps = [].concat(_jsxAstUtils.eventHandlersByType.focus, _jsxAstUtils.eventHandlersByType.keyboard, _jsxAstUtils.eventHandlersByType.mouse);
var schema = (0, _schemas.generateObjSchema)({
  handlers: _schemas.arraySchema
});
var _default = {
  meta: {
    docs: {
      url: 'https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/tree/HEAD/docs/rules/no-static-element-interactions.md',
      description: 'Enforce that non-interactive, visible elements (such as `<div>`) that have click handlers use the role attribute.',
      errorOptions: true
    },
    schema: [schema]
  },
  create: function create(context) {
    var options = context.options;
    var elementType = (0, _getElementType["default"])(context);
    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var attributes = node.attributes;
        var type = elementType(node);

        var _ref = options[0] || {},
            allowExpressionValues = _ref.allowExpressionValues,
            _ref$handlers = _ref.handlers,
            handlers = _ref$handlers === void 0 ? defaultInteractiveProps : _ref$handlers;

        var hasInteractiveProps = handlers.some(function (prop) {
          return (0, _jsxAstUtils.hasProp)(attributes, prop) && (0, _jsxAstUtils.getPropValue)((0, _jsxAstUtils.getProp)(attributes, prop)) != null;
        });

        if (!(0, _arrayIncludes["default"])(domElements, type)) {
          // Do not test higher level JSX components, as we do not know what
          // low-level DOM element this maps to.
          return;
        }

        if (!hasInteractiveProps || (0, _isHiddenFromScreenReader["default"])(type, attributes) || (0, _isPresentationRole["default"])(type, attributes)) {
          // Presentation is an intentional signal from the author that this
          // element is not meant to be perceivable. For example, a click screen
          // to close a dialog .
          return;
        }

        if ((0, _isInteractiveElement["default"])(type, attributes) || (0, _isInteractiveRole["default"])(type, attributes) || (0, _isNonInteractiveElement["default"])(type, attributes) || (0, _isNonInteractiveRole["default"])(type, attributes) || (0, _isAbstractRole["default"])(type, attributes)) {
          // This rule has no opinion about abstract roles.
          return;
        }

        if (allowExpressionValues === true && (0, _isNonLiteralProperty["default"])(attributes, 'role')) {
          // Special case if role is assigned using ternary with literals on both side
          var roleProp = (0, _jsxAstUtils.getProp)(attributes, 'role');

          if (roleProp && roleProp.type === 'JSXAttribute' && roleProp.value.type === 'JSXExpressionContainer') {
            if (roleProp.value.expression.type === 'ConditionalExpression') {
              if (roleProp.value.expression.consequent.type === 'Literal' && roleProp.value.expression.alternate.type === 'Literal') {
                return;
              }
            }
          }

          return;
        } // Visible, non-interactive elements should not have an interactive handler.


        context.report({
          node,
          message: errorMessage
        });
      }
    };
  }
};
exports["default"] = _default;
module.exports = exports.default;