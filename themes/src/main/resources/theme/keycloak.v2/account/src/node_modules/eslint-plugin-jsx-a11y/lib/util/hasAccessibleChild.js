"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = hasAccessibleChild;

var _jsxAstUtils = require("jsx-ast-utils");

var _isHiddenFromScreenReader = _interopRequireDefault(require("./isHiddenFromScreenReader"));

function hasAccessibleChild(node, elementType) {
  return node.children.some(function (child) {
    switch (child.type) {
      case 'Literal':
        return Boolean(child.value);
      // $FlowFixMe JSXText is missing in ast-types-flow

      case 'JSXText':
        return Boolean(child.value);

      case 'JSXElement':
        return !(0, _isHiddenFromScreenReader["default"])(elementType(child.openingElement), child.openingElement.attributes);

      case 'JSXExpressionContainer':
        if (child.expression.type === 'Identifier') {
          return child.expression.name !== 'undefined';
        }

        return true;

      default:
        return false;
    }
  }) || (0, _jsxAstUtils.hasAnyProp)(node.openingElement.attributes, ['dangerouslySetInnerHTML', 'children']);
}

module.exports = exports.default;