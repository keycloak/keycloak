"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DrawerPanelContent = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _drawer = _interopRequireDefault(require("@patternfly/react-styles/css/components/Drawer/drawer"));

var _reactStyles = require("@patternfly/react-styles");

var _Drawer = require("./Drawer");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DrawerPanelContent = function DrawerPanelContent(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      children = _ref.children,
      _ref$hasBorder = _ref.hasBorder,
      hasBorder = _ref$hasBorder === void 0 ? false : _ref$hasBorder,
      width = _ref.width,
      widthOnLg = _ref.widthOnLg,
      widthOnXl = _ref.widthOnXl,
      widthOn2Xl = _ref.widthOn2Xl,
      props = _objectWithoutProperties(_ref, ["className", "children", "hasBorder", "width", "widthOnLg", "widthOnXl", "widthOn2Xl"]);

  return React.createElement(_Drawer.DrawerContext.Consumer, null, function (_ref2) {
    var isExpanded = _ref2.isExpanded;
    return React.createElement("div", _extends({
      className: (0, _reactStyles.css)(_drawer["default"].drawerPanel, hasBorder && _drawer["default"].modifiers.border, width && _drawer["default"].modifiers["width_".concat(width)], widthOnLg && _drawer["default"].modifiers["width_".concat(widthOnLg, "OnLg")], widthOnXl && _drawer["default"].modifiers["width_".concat(widthOnXl, "OnXl")], widthOn2Xl && _drawer["default"].modifiers["width_".concat(widthOn2Xl, "On_2xl")], className),
      hidden: !isExpanded,
      "aria-hidden": !isExpanded,
      "aria-expanded": isExpanded
    }, props), children);
  });
};

exports.DrawerPanelContent = DrawerPanelContent;
DrawerPanelContent.propTypes = {
  className: _propTypes["default"].string,
  children: _propTypes["default"].node,
  hasBorder: _propTypes["default"].bool,
  width: _propTypes["default"].oneOf([25, 33, 50, 66, 75, 100]),
  widthOnLg: _propTypes["default"].oneOf([25, 33, 50, 66, 75, 100]),
  widthOnXl: _propTypes["default"].oneOf([25, 33, 50, 66, 75, 100]),
  widthOn2Xl: _propTypes["default"].oneOf([25, 33, 50, 66, 75, 100])
};
//# sourceMappingURL=DrawerPanelContent.js.map