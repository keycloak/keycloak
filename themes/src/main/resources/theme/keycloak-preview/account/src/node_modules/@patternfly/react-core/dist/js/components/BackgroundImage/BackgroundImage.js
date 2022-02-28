"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.BackgroundImage = exports.BackgroundImageSrc = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _backgroundImage = _interopRequireDefault(require("@patternfly/react-styles/css/components/BackgroundImage/background-image"));

var _c_background_image_BackgroundImage = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage"));

var _c_background_image_BackgroundImage_2x = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_2x"));

var _c_background_image_BackgroundImage_sm = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm"));

var _c_background_image_BackgroundImage_sm_2x = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm_2x"));

var _c_background_image_BackgroundImage_lg = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_lg"));

var _cssVariables;

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var BackgroundImageSrc;
exports.BackgroundImageSrc = BackgroundImageSrc;

(function (BackgroundImageSrc) {
  BackgroundImageSrc["xs"] = "xs";
  BackgroundImageSrc["xs2x"] = "xs2x";
  BackgroundImageSrc["sm"] = "sm";
  BackgroundImageSrc["sm2x"] = "sm2x";
  BackgroundImageSrc["lg"] = "lg";
  BackgroundImageSrc["filter"] = "filter";
})(BackgroundImageSrc || (exports.BackgroundImageSrc = BackgroundImageSrc = {}));

var cssVariables = (_cssVariables = {}, _defineProperty(_cssVariables, BackgroundImageSrc.xs, _c_background_image_BackgroundImage["default"] && _c_background_image_BackgroundImage["default"].name), _defineProperty(_cssVariables, BackgroundImageSrc.xs2x, _c_background_image_BackgroundImage_2x["default"] && _c_background_image_BackgroundImage_2x["default"].name), _defineProperty(_cssVariables, BackgroundImageSrc.sm, _c_background_image_BackgroundImage_sm["default"] && _c_background_image_BackgroundImage_sm["default"].name), _defineProperty(_cssVariables, BackgroundImageSrc.sm2x, _c_background_image_BackgroundImage_sm_2x["default"] && _c_background_image_BackgroundImage_sm_2x["default"].name), _defineProperty(_cssVariables, BackgroundImageSrc.lg, _c_background_image_BackgroundImage_lg["default"] && _c_background_image_BackgroundImage_lg["default"].name), _cssVariables);

var BackgroundImage = function BackgroundImage(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      src = _ref.src,
      props = _objectWithoutProperties(_ref, ["className", "src"]);

  var srcMap = src; // Default string value to handle all sizes

  if (typeof src === 'string') {
    var _srcMap;

    srcMap = (_srcMap = {}, _defineProperty(_srcMap, BackgroundImageSrc.xs, src), _defineProperty(_srcMap, BackgroundImageSrc.xs2x, src), _defineProperty(_srcMap, BackgroundImageSrc.sm, src), _defineProperty(_srcMap, BackgroundImageSrc.sm2x, src), _defineProperty(_srcMap, BackgroundImageSrc.lg, src), _defineProperty(_srcMap, BackgroundImageSrc.filter, ''), _srcMap);
  } // Build stylesheet string based on cssVariables


  var cssSheet = '';
  Object.keys(cssVariables).forEach(function (size) {
    cssSheet += "".concat(cssVariables[size], ": url('").concat(srcMap[size], "');");
  }); // Create emotion stylesheet to inject new css

  var bgStyles = _reactStyles.StyleSheet.create({
    bgOverrides: "&.pf-c-background-image {\n      ".concat(cssSheet, "\n    }")
  });

  return React.createElement("div", _extends({
    className: (0, _reactStyles.css)(_backgroundImage["default"].backgroundImage, bgStyles.bgOverrides, className)
  }, props), React.createElement("svg", {
    xmlns: "http://www.w3.org/2000/svg",
    className: "pf-c-background-image__filter",
    width: "0",
    height: "0"
  }, React.createElement("filter", {
    id: "image_overlay"
  }, React.createElement("feColorMatrix", {
    type: "matrix",
    values: "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 0 0 0 1 0"
  }), React.createElement("feComponentTransfer", {
    colorInterpolationFilters: "sRGB",
    result: "duotone"
  }, React.createElement("feFuncR", {
    type: "table",
    tableValues: "0.086274509803922 0.43921568627451"
  }), React.createElement("feFuncG", {
    type: "table",
    tableValues: "0.086274509803922 0.43921568627451"
  }), React.createElement("feFuncB", {
    type: "table",
    tableValues: "0.086274509803922 0.43921568627451"
  }), React.createElement("feFuncA", {
    type: "table",
    tableValues: "0 1"
  })))));
};

exports.BackgroundImage = BackgroundImage;
BackgroundImage.propTypes = {
  className: _propTypes["default"].string,
  src: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].shape({
    xs: _propTypes["default"].string.isRequired,
    xs2x: _propTypes["default"].string.isRequired,
    sm: _propTypes["default"].string.isRequired,
    sm2x: _propTypes["default"].string.isRequired,
    lg: _propTypes["default"].string.isRequired,
    filter: _propTypes["default"].string
  })]).isRequired
};
//# sourceMappingURL=BackgroundImage.js.map