"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _jsxAstUtils = require("jsx-ast-utils");

var _getElementType = _interopRequireDefault(require("../util/getElementType"));

var _schemas = require("../util/schemas");

/**
 * @fileoverview Enforce iframe elements have a title attribute.
 * @author Ethan Cohen
 */
// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------
var errorMessage = '<iframe> elements must have a unique title property.';
var schema = (0, _schemas.generateObjSchema)();
var _default = {
  meta: {
    docs: {
      url: 'https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/tree/HEAD/docs/rules/iframe-has-title.md',
      description: 'Enforce iframe elements have a title attribute.'
    },
    schema: [schema]
  },
  create: function create(context) {
    var elementType = (0, _getElementType["default"])(context);
    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var type = elementType(node);

        if (type && type !== 'iframe') {
          return;
        }

        var title = (0, _jsxAstUtils.getPropValue)((0, _jsxAstUtils.getProp)(node.attributes, 'title'));

        if (title && typeof title === 'string') {
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