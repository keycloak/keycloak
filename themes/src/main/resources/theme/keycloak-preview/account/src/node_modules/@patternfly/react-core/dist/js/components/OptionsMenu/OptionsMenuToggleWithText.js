"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.OptionsMenuToggleWithText = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _optionsMenu = _interopRequireDefault(require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var OptionsMenuToggleWithText = function OptionsMenuToggleWithText(_ref) {
  var _ref$parentId = _ref.parentId,
      parentId = _ref$parentId === void 0 ? '' : _ref$parentId,
      toggleText = _ref.toggleText,
      _ref$toggleTextClassN = _ref.toggleTextClassName,
      toggleTextClassName = _ref$toggleTextClassN === void 0 ? '' : _ref$toggleTextClassN,
      toggleButtonContents = _ref.toggleButtonContents,
      _ref$toggleButtonCont = _ref.toggleButtonContentsClassName,
      toggleButtonContentsClassName = _ref$toggleButtonCont === void 0 ? '' : _ref$toggleButtonCont,
      _ref$onToggle = _ref.onToggle,
      onToggle = _ref$onToggle === void 0 ? function () {
    return null;
  } : _ref$onToggle,
      _ref$isOpen = _ref.isOpen,
      isOpen = _ref$isOpen === void 0 ? false : _ref$isOpen,
      _ref$isPlain = _ref.isPlain,
      isPlain = _ref$isPlain === void 0 ? false : _ref$isPlain,
      _ref$isHovered = _ref.isHovered,
      isHovered = _ref$isHovered === void 0 ? false : _ref$isHovered,
      _ref$isActive = _ref.isActive,
      isActive = _ref$isActive === void 0 ? false : _ref$isActive,
      _ref$isFocused = _ref.isFocused,
      isFocused = _ref$isFocused === void 0 ? false : _ref$isFocused,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      ariaHasPopup = _ref.ariaHasPopup,
      parentRef = _ref.parentRef,
      onEnter = _ref.onEnter,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? 'Options menu' : _ref$ariaLabel,
      props = _objectWithoutProperties(_ref, ["parentId", "toggleText", "toggleTextClassName", "toggleButtonContents", "toggleButtonContentsClassName", "onToggle", "isOpen", "isPlain", "isHovered", "isActive", "isFocused", "isDisabled", "ariaHasPopup", "parentRef", "onEnter", "aria-label"]);

  return React.createElement("div", _extends({
    className: (0, _reactStyles.css)(_optionsMenu["default"].optionsMenuToggle, (0, _reactStyles.getModifier)(_optionsMenu["default"], 'text'), isPlain && (0, _reactStyles.getModifier)(_optionsMenu["default"], 'plain'), isHovered && (0, _reactStyles.getModifier)(_optionsMenu["default"], 'hover'), isActive && (0, _reactStyles.getModifier)(_optionsMenu["default"], 'active'), isFocused && (0, _reactStyles.getModifier)(_optionsMenu["default"], 'focus'), isDisabled && (0, _reactStyles.getModifier)(_optionsMenu["default"], 'disabled'))
  }, props), React.createElement("span", {
    className: (0, _reactStyles.css)(_optionsMenu["default"].optionsMenuToggleText, toggleTextClassName)
  }, toggleText), React.createElement("button", {
    className: (0, _reactStyles.css)(_optionsMenu["default"].optionsMenuToggleButton, toggleButtonContentsClassName),
    id: "".concat(parentId, "-toggle"),
    "aria-haspopup": "listbox",
    "aria-label": ariaLabel,
    "aria-expanded": isOpen,
    onClick: function onClick() {
      return onToggle(!isOpen);
    }
  }, toggleButtonContents));
};

exports.OptionsMenuToggleWithText = OptionsMenuToggleWithText;
OptionsMenuToggleWithText.propTypes = {
  parentId: _propTypes["default"].string,
  toggleText: _propTypes["default"].node.isRequired,
  toggleTextClassName: _propTypes["default"].string,
  toggleButtonContents: _propTypes["default"].node,
  toggleButtonContentsClassName: _propTypes["default"].string,
  onToggle: _propTypes["default"].func,
  onEnter: _propTypes["default"].func,
  isOpen: _propTypes["default"].bool,
  isPlain: _propTypes["default"].bool,
  isFocused: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  isActive: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  parentRef: _propTypes["default"].any,
  ariaHasPopup: _propTypes["default"].oneOfType([_propTypes["default"].bool, _propTypes["default"].oneOf(['dialog']), _propTypes["default"].oneOf(['menu']), _propTypes["default"].oneOf(['false']), _propTypes["default"].oneOf(['true']), _propTypes["default"].oneOf(['listbox']), _propTypes["default"].oneOf(['tree']), _propTypes["default"].oneOf(['grid'])]),
  'aria-label': _propTypes["default"].string
};
//# sourceMappingURL=OptionsMenuToggleWithText.js.map