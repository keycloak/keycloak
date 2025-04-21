"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _ariaQuery = require("aria-query");

var _jsxAstUtils = require("jsx-ast-utils");

var _schemas = require("../util/schemas");

/**
 * @fileoverview Enforce onmouseover/onmouseout are
 *  accompanied by onfocus/onblur.
 * @author Ethan Cohen
 */
// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------
var mouseOverErrorMessage = 'onMouseOver must be accompanied by onFocus for accessibility.';
var mouseOutErrorMessage = 'onMouseOut must be accompanied by onBlur for accessibility.';
var schema = (0, _schemas.generateObjSchema)();
var _default = {
  meta: {
    docs: {
      url: 'https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/tree/HEAD/docs/rules/mouse-events-have-key-events.md',
      description: 'Enforce that `onMouseOver`/`onMouseOut` are accompanied by `onFocus`/`onBlur` for keyboard-only users.'
    },
    schema: [schema]
  },
  create: function create(context) {
    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var name = node.name.name;

        if (!_ariaQuery.dom.get(name)) {
          return;
        }

        var attributes = node.attributes; // Check onmouseover / onfocus pairing.

        var onMouseOver = (0, _jsxAstUtils.getProp)(attributes, 'onMouseOver');
        var onMouseOverValue = (0, _jsxAstUtils.getPropValue)(onMouseOver);

        if (onMouseOver && onMouseOverValue != null) {
          var hasOnFocus = (0, _jsxAstUtils.getProp)(attributes, 'onFocus');
          var onFocusValue = (0, _jsxAstUtils.getPropValue)(hasOnFocus);

          if (hasOnFocus === false || onFocusValue === null || onFocusValue === undefined) {
            context.report({
              node,
              message: mouseOverErrorMessage
            });
          }
        } // Checkout onmouseout / onblur pairing


        var onMouseOut = (0, _jsxAstUtils.getProp)(attributes, 'onMouseOut');
        var onMouseOutValue = (0, _jsxAstUtils.getPropValue)(onMouseOut);

        if (onMouseOut && onMouseOutValue != null) {
          var hasOnBlur = (0, _jsxAstUtils.getProp)(attributes, 'onBlur');
          var onBlurValue = (0, _jsxAstUtils.getPropValue)(hasOnBlur);

          if (hasOnBlur === false || onBlurValue === null || onBlurValue === undefined) {
            context.report({
              node,
              message: mouseOutErrorMessage
            });
          }
        }
      }
    };
  }
};
exports["default"] = _default;
module.exports = exports.default;