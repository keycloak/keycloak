"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.OptionsMenuToggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _Dropdown = require("../Dropdown");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var OptionsMenuToggle = function OptionsMenuToggle(_ref) {
  var _ref$isPlain = _ref.isPlain,
      isPlain = _ref$isPlain === void 0 ? false : _ref$isPlain,
      _ref$isHovered = _ref.isHovered,
      isHovered = _ref$isHovered === void 0 ? false : _ref$isHovered,
      _ref$isActive = _ref.isActive,
      isActive = _ref$isActive === void 0 ? false : _ref$isActive,
      _ref$isFocused = _ref.isFocused,
      isFocused = _ref$isFocused === void 0 ? false : _ref$isFocused,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      _ref$isOpen = _ref.isOpen,
      isOpen = _ref$isOpen === void 0 ? false : _ref$isOpen,
      _ref$parentId = _ref.parentId,
      parentId = _ref$parentId === void 0 ? '' : _ref$parentId,
      _ref$toggleTemplate = _ref.toggleTemplate,
      toggleTemplate = _ref$toggleTemplate === void 0 ? React.createElement(React.Fragment, null) : _ref$toggleTemplate,
      _ref$hideCaret = _ref.hideCaret,
      hideCaret = _ref$hideCaret === void 0 ? false : _ref$hideCaret,
      _ref$isSplitButton = _ref.isSplitButton,
      isSplitButton = _ref$isSplitButton === void 0 ? false : _ref$isSplitButton,
      type = _ref.type,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? 'Options menu' : _ref$ariaLabel,
      props = _objectWithoutProperties(_ref, ["isPlain", "isHovered", "isActive", "isFocused", "isDisabled", "isOpen", "parentId", "toggleTemplate", "hideCaret", "isSplitButton", "type", "aria-label"]);

  return React.createElement(_Dropdown.DropdownContext.Consumer, null, function (_ref2) {
    var contextId = _ref2.id;
    return React.createElement(_Dropdown.DropdownToggle, _extends({}, (isPlain || hideCaret) && {
      iconComponent: null
    }, props, {
      isPlain: isPlain,
      isOpen: isOpen,
      isDisabled: isDisabled,
      isHovered: isHovered,
      isActive: isActive,
      isFocused: isFocused,
      id: parentId ? "".concat(parentId, "-toggle") : "".concat(contextId, "-toggle"),
      ariaHasPopup: "listbox",
      "aria-label": ariaLabel,
      "aria-expanded": isOpen
    }, toggleTemplate ? {
      children: toggleTemplate
    } : {}));
  });
};

exports.OptionsMenuToggle = OptionsMenuToggle;
OptionsMenuToggle.propTypes = {
  parentId: _propTypes["default"].string,
  onToggle: _propTypes["default"].func,
  isOpen: _propTypes["default"].bool,
  isPlain: _propTypes["default"].bool,
  isFocused: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  isSplitButton: _propTypes["default"].bool,
  isActive: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  hideCaret: _propTypes["default"].bool,
  'aria-label': _propTypes["default"].string,
  onEnter: _propTypes["default"].func,
  parentRef: _propTypes["default"].any,
  toggleTemplate: _propTypes["default"].node
};
//# sourceMappingURL=OptionsMenuToggle.js.map