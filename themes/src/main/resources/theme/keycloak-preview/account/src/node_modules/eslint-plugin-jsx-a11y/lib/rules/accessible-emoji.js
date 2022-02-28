'use strict';

var _emojiRegex = require('emoji-regex');

var _emojiRegex2 = _interopRequireDefault(_emojiRegex);

var _jsxAstUtils = require('jsx-ast-utils');

var _schemas = require('../util/schemas');

var _isHiddenFromScreenReader = require('../util/isHiddenFromScreenReader');

var _isHiddenFromScreenReader2 = _interopRequireDefault(_isHiddenFromScreenReader);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * @fileoverview Enforce emojis are wrapped in <span> and provide screenreader access.
 * @author Ethan Cohen
 */

// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------

var errorMessage = 'Emojis should be wrapped in <span>, have role="img", and have an accessible description with aria-label or aria-labelledby.';

var schema = (0, _schemas.generateObjSchema)();

module.exports = {
  meta: {
    docs: {
      url: 'https://github.com/evcohen/eslint-plugin-jsx-a11y/tree/master/docs/rules/accessible-emoji.md'
    },
    schema: [schema]
  },

  create: function create(context) {
    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var literalChildValue = node.parent.children.find(function (child) {
          return child.type === 'Literal' || child.type === 'JSXText';
        });

        if (literalChildValue && (0, _emojiRegex2.default)().test(literalChildValue.value)) {
          var elementIsHidden = (0, _isHiddenFromScreenReader2.default)((0, _jsxAstUtils.elementType)(node), node.attributes);
          if (elementIsHidden) {
            return; // emoji is decorative
          }

          var rolePropValue = (0, _jsxAstUtils.getLiteralPropValue)((0, _jsxAstUtils.getProp)(node.attributes, 'role'));
          var ariaLabelProp = (0, _jsxAstUtils.getProp)(node.attributes, 'aria-label');
          var arialLabelledByProp = (0, _jsxAstUtils.getProp)(node.attributes, 'aria-labelledby');
          var hasLabel = ariaLabelProp !== undefined || arialLabelledByProp !== undefined;
          var isSpan = (0, _jsxAstUtils.elementType)(node) === 'span';

          if (hasLabel === false || rolePropValue !== 'img' || isSpan === false) {
            context.report({
              node: node,
              message: errorMessage
            });
          }
        }
      }
    };
  }
};