"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DropdownItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _InternalDropdownItem = require("./InternalDropdownItem");

var _dropdownConstants = require("./dropdownConstants");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DropdownItem = function DropdownItem(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$component = _ref.component,
      component = _ref$component === void 0 ? 'a' : _ref$component,
      _ref$variant = _ref.variant,
      variant = _ref$variant === void 0 ? 'item' : _ref$variant,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      _ref$isHovered = _ref.isHovered,
      isHovered = _ref$isHovered === void 0 ? false : _ref$isHovered,
      href = _ref.href,
      _ref$tooltip = _ref.tooltip,
      tooltip = _ref$tooltip === void 0 ? null : _ref$tooltip,
      _ref$tooltipProps = _ref.tooltipProps,
      tooltipProps = _ref$tooltipProps === void 0 ? {} : _ref$tooltipProps,
      listItemClassName = _ref.listItemClassName,
      onClick = _ref.onClick,
      ref = _ref.ref,
      additionalChild = _ref.additionalChild,
      customChild = _ref.customChild,
      props = _objectWithoutProperties(_ref, ["children", "className", "component", "variant", "isDisabled", "isHovered", "href", "tooltip", "tooltipProps", "listItemClassName", "onClick", "ref", "additionalChild", "customChild"]);

  return React.createElement(_dropdownConstants.DropdownArrowContext.Consumer, null, function (context) {
    return React.createElement(_InternalDropdownItem.InternalDropdownItem, _extends({
      context: context,
      role: "menuitem",
      tabIndex: -1,
      className: className,
      component: component,
      variant: variant,
      isDisabled: isDisabled,
      isHovered: isHovered,
      href: href,
      tooltip: tooltip,
      tooltipProps: tooltipProps,
      listItemClassName: listItemClassName,
      onClick: onClick,
      additionalChild: additionalChild,
      customChild: customChild
    }, props), children);
  });
};

exports.DropdownItem = DropdownItem;
DropdownItem.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  listItemClassName: _propTypes["default"].string,
  component: _propTypes["default"].node,
  variant: _propTypes["default"].oneOf(['item', 'icon']),
  isDisabled: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  href: _propTypes["default"].string,
  tooltip: _propTypes["default"].node,
  tooltipProps: _propTypes["default"].any,
  additionalChild: _propTypes["default"].node,
  customChild: _propTypes["default"].node
};
//# sourceMappingURL=DropdownItem.js.map