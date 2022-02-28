"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.KebabToggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _ellipsisVIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/ellipsis-v-icon"));

var _Toggle = require("./Toggle");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var KebabToggle = function KebabToggle(_ref) {
  var _ref$id = _ref.id,
      id = _ref$id === void 0 ? '' : _ref$id,
      _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$isOpen = _ref.isOpen,
      isOpen = _ref$isOpen === void 0 ? false : _ref$isOpen,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? 'Actions' : _ref$ariaLabel,
      _ref$parentRef = _ref.parentRef,
      parentRef = _ref$parentRef === void 0 ? null : _ref$parentRef,
      _ref$isFocused = _ref.isFocused,
      isFocused = _ref$isFocused === void 0 ? false : _ref$isFocused,
      _ref$isHovered = _ref.isHovered,
      isHovered = _ref$isHovered === void 0 ? false : _ref$isHovered,
      _ref$isActive = _ref.isActive,
      isActive = _ref$isActive === void 0 ? false : _ref$isActive,
      _ref$isPlain = _ref.isPlain,
      isPlain = _ref$isPlain === void 0 ? false : _ref$isPlain,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      _ref$bubbleEvent = _ref.bubbleEvent,
      bubbleEvent = _ref$bubbleEvent === void 0 ? false : _ref$bubbleEvent,
      _ref$onToggle = _ref.onToggle,
      onToggle = _ref$onToggle === void 0 ? function () {
    return undefined;
  } : _ref$onToggle,
      ref = _ref.ref,
      props = _objectWithoutProperties(_ref, ["id", "children", "className", "isOpen", "aria-label", "parentRef", "isFocused", "isHovered", "isActive", "isPlain", "isDisabled", "bubbleEvent", "onToggle", "ref"]);

  return React.createElement(_Toggle.Toggle, _extends({
    id: id,
    className: className,
    isOpen: isOpen,
    "aria-label": ariaLabel,
    parentRef: parentRef,
    isFocused: isFocused,
    isHovered: isHovered,
    isActive: isActive,
    isPlain: isPlain,
    isDisabled: isDisabled,
    onToggle: onToggle,
    bubbleEvent: bubbleEvent
  }, props), React.createElement(_ellipsisVIcon["default"], null));
};

exports.KebabToggle = KebabToggle;
KebabToggle.propTypes = {
  id: _propTypes["default"].string,
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  isOpen: _propTypes["default"].bool,
  'aria-label': _propTypes["default"].string,
  onToggle: _propTypes["default"].func,
  parentRef: _propTypes["default"].any,
  isFocused: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  isActive: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isPlain: _propTypes["default"].bool,
  type: _propTypes["default"].oneOf(['button', 'submit', 'reset']),
  bubbleEvent: _propTypes["default"].bool
};
//# sourceMappingURL=KebabToggle.js.map