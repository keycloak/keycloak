'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = mayHaveAccessibleLabel;

var _arrayIncludes = require('array-includes');

var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);

var _jsxAstUtils = require('jsx-ast-utils');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } } /**
                                                                                                                                                                                                     * Returns true if a labelling element is found or if it cannot determine if
                                                                                                                                                                                                     * a label is present because of expression containers or spread attributes.
                                                                                                                                                                                                     * A false return value means that the node definitely does not have a label,
                                                                                                                                                                                                     * but a true return return value means that the node may or may not have a
                                                                                                                                                                                                     * label.
                                                                                                                                                                                                     *
                                                                                                                                                                                                     * 
                                                                                                                                                                                                     */

function hasLabellingProp(openingElement) {
  var additionalLabellingProps = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : [];

  var labellingProps = ['alt', // Assume alt is used correctly on an image
  'aria-label', 'aria-labelledby'].concat(_toConsumableArray(additionalLabellingProps));
  return openingElement.attributes.some(function (attribute) {
    // We must assume that a spread value contains a labelling prop.
    if (attribute.type !== 'JSXAttribute') {
      return true;
    }
    // Attribute matches.
    if ((0, _arrayIncludes2.default)(labellingProps, (0, _jsxAstUtils.propName)(attribute)) && !!(0, _jsxAstUtils.getPropValue)(attribute)) {
      return true;
    }
    return false;
  });
}

function mayHaveAccessibleLabel(root) {
  var maxDepth = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 1;
  var additionalLabellingProps = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : [];

  function checkElement(node, depth) {
    // Bail when maxDepth is exceeded.
    if (depth > maxDepth) {
      return false;
    }
    // Check for literal text.
    if (node.type === 'Literal' && !!node.value) {
      return true;
    }
    // Assume an expression container renders a label. It is the best we can
    // do in this case.
    if (node.type === 'JSXExpressionContainer') {
      return true;
    }
    // Check for JSXText.
    // $FlowFixMe Remove after updating ast-types-flow
    if (node.type === 'JSXText' && !!node.value) {
      return true;
    }
    // Check for labelling props.
    if (node.openingElement
    /* $FlowFixMe */
    && hasLabellingProp(node.openingElement, additionalLabellingProps)) {
      return true;
    }
    // Recurse into the child element nodes.
    if (node.children) {
      /* $FlowFixMe */
      for (var i = 0; i < node.children.length; i += 1) {
        /* $FlowFixMe */
        if (checkElement(node.children[i], depth + 1)) {
          return true;
        }
      }
    }
    return false;
  }
  return checkElement(root, 0);
}