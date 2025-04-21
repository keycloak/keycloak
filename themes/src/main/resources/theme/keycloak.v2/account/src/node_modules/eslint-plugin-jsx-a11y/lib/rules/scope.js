"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _ariaQuery = require("aria-query");

var _jsxAstUtils = require("jsx-ast-utils");

var _schemas = require("../util/schemas");

var _getElementType = _interopRequireDefault(require("../util/getElementType"));

/**
 * @fileoverview Enforce scope prop is only used on <th> elements.
 * @author Ethan Cohen
 */
// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------
var errorMessage = 'The scope prop can only be used on <th> elements.';
var schema = (0, _schemas.generateObjSchema)();
var _default = {
  meta: {
    docs: {
      url: 'https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/tree/HEAD/docs/rules/scope.md',
      description: 'Enforce `scope` prop is only used on `<th>` elements.',
      errorOptions: true
    },
    schema: [schema]
  },
  create: function create(context) {
    var elementType = (0, _getElementType["default"])(context);
    return {
      JSXAttribute: function JSXAttribute(node) {
        var name = (0, _jsxAstUtils.propName)(node);

        if (name && name.toUpperCase() !== 'SCOPE') {
          return;
        }

        var parent = node.parent;
        var tagName = elementType(parent); // Do not test higher level JSX components, as we do not know what
        // low-level DOM element this maps to.

        if (!_ariaQuery.dom.has(tagName)) {
          return;
        }

        if (tagName && tagName.toUpperCase() === 'TH') {
          return;
        }

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