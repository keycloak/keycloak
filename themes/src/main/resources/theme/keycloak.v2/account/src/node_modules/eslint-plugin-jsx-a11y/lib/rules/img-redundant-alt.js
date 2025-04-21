"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _jsxAstUtils = require("jsx-ast-utils");

var _schemas = require("../util/schemas");

var _getElementType = _interopRequireDefault(require("../util/getElementType"));

var _isHiddenFromScreenReader = _interopRequireDefault(require("../util/isHiddenFromScreenReader"));

/**
 * @fileoverview Enforce img alt attribute does not have the word image, picture, or photo.
 * @author Ethan Cohen
 */
// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------
var REDUNDANT_WORDS = ['image', 'photo', 'picture'];
var errorMessage = 'Redundant alt attribute. Screen-readers already announce `img` tags as an image. You don’t need to use the words `image`, `photo,` or `picture` (or any specified custom words) in the alt prop.';
var schema = (0, _schemas.generateObjSchema)({
  components: _schemas.arraySchema,
  words: _schemas.arraySchema
});
var _default = {
  meta: {
    docs: {
      url: 'https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/tree/HEAD/docs/rules/img-redundant-alt.md',
      description: 'Enforce `<img>` alt prop does not contain the word "image", "picture", or "photo".'
    },
    schema: [schema]
  },
  create: function create(context) {
    var elementType = (0, _getElementType["default"])(context);
    return {
      JSXOpeningElement: function JSXOpeningElement(node) {
        var options = context.options[0] || {};
        var componentOptions = options.components || [];
        var typesToValidate = ['img'].concat(componentOptions);
        var nodeType = elementType(node); // Only check 'label' elements and custom types.

        if (typesToValidate.indexOf(nodeType) === -1) {
          return;
        }

        var altProp = (0, _jsxAstUtils.getProp)(node.attributes, 'alt'); // Return if alt prop is not present.

        if (altProp === undefined) {
          return;
        }

        var value = (0, _jsxAstUtils.getLiteralPropValue)(altProp);
        var isVisible = (0, _isHiddenFromScreenReader["default"])(nodeType, node.attributes) === false;
        var _options$words = options.words,
            words = _options$words === void 0 ? [] : _options$words;
        var redundantWords = REDUNDANT_WORDS.concat(words);

        if (typeof value === 'string' && isVisible) {
          var hasRedundancy = new RegExp("(?!{)\\b(".concat(redundantWords.join('|'), ")\\b(?!})"), 'i').test(value);

          if (hasRedundancy === true) {
            context.report({
              node,
              message: errorMessage
            });
          }
        }
      }
    };
  }
};
exports["default"] = _default;
module.exports = exports.default;