"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = void 0;

var _has = _interopRequireDefault(require("has"));

var _jsxAstUtils = require("jsx-ast-utils");

var getElementType = function getElementType(context) {
  var _settings$jsxA11y;

  var settings = context.settings;
  var componentMap = (_settings$jsxA11y = settings['jsx-a11y']) === null || _settings$jsxA11y === void 0 ? void 0 : _settings$jsxA11y.components;

  if (!componentMap) {
    return _jsxAstUtils.elementType;
  }

  return function (node) {
    var rawType = (0, _jsxAstUtils.elementType)(node);
    return (0, _has["default"])(componentMap, rawType) ? componentMap[rawType] : rawType;
  };
};

var _default = getElementType;
exports["default"] = _default;
module.exports = exports.default;