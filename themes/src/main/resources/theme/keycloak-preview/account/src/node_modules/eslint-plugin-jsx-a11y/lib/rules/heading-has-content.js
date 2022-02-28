'use strict';

var _jsxAstUtils = require('jsx-ast-utils');

var _schemas = require('../util/schemas');

var _hasAccessibleChild = require('../util/hasAccessibleChild');

var _hasAccessibleChild2 = _interopRequireDefault(_hasAccessibleChild);

var _isHiddenFromScreenReader = require('../util/isHiddenFromScreenReader');

var _isHiddenFromScreenReader2 = _interopRequireDefault(_isHiddenFromScreenReader);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * @fileoverview Enforce heading (h1, h2, etc) elements contain accessible content.
 * @author Ethan Cohen
 */

// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------

var errorMessage = 'Headings must have content and the content must be accessible by a screen reader.';

var headings = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'];

var schema = (0, _schemas.generateObjSchema)({ components: _schemas.arraySchema });

module.exports = {
  meta: {
    docs: {
      url: 'https://github.com/evcohen/eslint-plugin-jsx-a11y/tree/master/docs/rules/heading-has-content.md'
    },
    schema: [schema]
  },

  create: function create(context) {
    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var options = context.options[0] || {};
        var componentOptions = options.components || [];
        var typeCheck = headings.concat(componentOptions);
        var nodeType = (0, _jsxAstUtils.elementType)(node);

        // Only check 'h*' elements and custom types.
        if (typeCheck.indexOf(nodeType) === -1) {
          return;
        }
        if ((0, _hasAccessibleChild2.default)(node.parent)) {
          return;
        }
        if ((0, _isHiddenFromScreenReader2.default)(nodeType, node.attributes)) {
          return;
        }

        context.report({
          node: node,
          message: errorMessage
        });
      }
    };
  }
};