"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _jsxAstUtils = require("jsx-ast-utils");

var _schemas = require("../util/schemas");

var _getElementType = _interopRequireDefault(require("../util/getElementType"));

var _mayContainChildComponent = _interopRequireDefault(require("../util/mayContainChildComponent"));

var _mayHaveAccessibleLabel = _interopRequireDefault(require("../util/mayHaveAccessibleLabel"));

/**
 * @fileoverview Enforce label tags have an associated control.
 * @author Jesse Beach
 *
 * 
 */
// ----------------------------------------------------------------------------
// Rule Definition
// ----------------------------------------------------------------------------
var errorMessage = 'A form label must be associated with a control.';
var schema = (0, _schemas.generateObjSchema)({
  labelComponents: _schemas.arraySchema,
  labelAttributes: _schemas.arraySchema,
  controlComponents: _schemas.arraySchema,
  assert: {
    description: 'Assert that the label has htmlFor, a nested label, both or either',
    type: 'string',
    "enum": ['htmlFor', 'nesting', 'both', 'either']
  },
  depth: {
    description: 'JSX tree depth limit to check for accessible label',
    type: 'integer',
    minimum: 0
  }
});

var validateId = function validateId(node) {
  var htmlForAttr = (0, _jsxAstUtils.getProp)(node.attributes, 'htmlFor');
  var htmlForValue = (0, _jsxAstUtils.getPropValue)(htmlForAttr);
  return htmlForAttr !== false && !!htmlForValue;
};

var _default = {
  meta: {
    docs: {
      description: 'Enforce that a `label` tag has a text label and an associated control.',
      url: 'https://github.com/jsx-eslint/eslint-plugin-jsx-a11y/blob/main/docs/rules/label-has-associated-control.md'
    },
    schema: [schema]
  },
  create: function create(context) {
    var options = context.options[0] || {};
    var labelComponents = options.labelComponents || [];
    var assertType = options.assert || 'either';
    var componentNames = ['label'].concat(labelComponents);
    var elementType = (0, _getElementType["default"])(context);

    var rule = function rule(node) {
      if (componentNames.indexOf(elementType(node.openingElement)) === -1) {
        return;
      }

      var controlComponents = ['input', 'meter', 'output', 'progress', 'select', 'textarea'].concat(options.controlComponents || []); // Prevent crazy recursion.

      var recursionDepth = Math.min(options.depth === undefined ? 2 : options.depth, 25);
      var hasLabelId = validateId(node.openingElement); // Check for multiple control components.

      var hasNestedControl = controlComponents.some(function (name) {
        return (0, _mayContainChildComponent["default"])(node, name, recursionDepth, elementType);
      });
      var hasAccessibleLabel = (0, _mayHaveAccessibleLabel["default"])(node, recursionDepth, options.labelAttributes);

      if (hasAccessibleLabel) {
        switch (assertType) {
          case 'htmlFor':
            if (hasLabelId) {
              return;
            }

            break;

          case 'nesting':
            if (hasNestedControl) {
              return;
            }

            break;

          case 'both':
            if (hasLabelId && hasNestedControl) {
              return;
            }

            break;

          case 'either':
            if (hasLabelId || hasNestedControl) {
              return;
            }

            break;

          default:
            break;
        }
      } // htmlFor case


      context.report({
        node: node.openingElement,
        message: errorMessage
      });
    }; // Create visitor selectors.


    return {
      JSXElement: rule
    };
  }
};
exports["default"] = _default;
module.exports = exports.default;