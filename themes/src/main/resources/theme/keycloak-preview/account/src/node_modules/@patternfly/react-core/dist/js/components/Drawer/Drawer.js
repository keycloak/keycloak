"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Drawer = exports.DrawerContext = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _drawer = _interopRequireDefault(require("@patternfly/react-styles/css/components/Drawer/drawer"));

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DrawerContext = React.createContext({
  isExpanded: false
});
exports.DrawerContext = DrawerContext;

var Drawer = function Drawer(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      children = _ref.children,
      _ref$isExpanded = _ref.isExpanded,
      isExpanded = _ref$isExpanded === void 0 ? false : _ref$isExpanded,
      _ref$isInline = _ref.isInline,
      isInline = _ref$isInline === void 0 ? false : _ref$isInline,
      _ref$isStatic = _ref.isStatic,
      isStatic = _ref$isStatic === void 0 ? false : _ref$isStatic,
      _ref$position = _ref.position,
      position = _ref$position === void 0 ? 'right' : _ref$position,
      props = _objectWithoutProperties(_ref, ["className", "children", "isExpanded", "isInline", "isStatic", "position"]);

  return React.createElement(DrawerContext.Provider, {
    value: {
      isExpanded: isExpanded
    }
  }, React.createElement("div", _extends({
    className: (0, _reactStyles.css)(_drawer["default"].drawer, isExpanded && _drawer["default"].modifiers.expanded, isInline && _drawer["default"].modifiers.inline, isStatic && _drawer["default"].modifiers["static"], position === 'left' && _drawer["default"].modifiers.panelLeft, className)
  }, props), children));
};

exports.Drawer = Drawer;
Drawer.propTypes = {
  className: _propTypes["default"].string,
  children: _propTypes["default"].node,
  isExpanded: _propTypes["default"].bool,
  isInline: _propTypes["default"].bool,
  isStatic: _propTypes["default"].bool,
  position: _propTypes["default"].oneOf(['left', 'right'])
};
//# sourceMappingURL=Drawer.js.map