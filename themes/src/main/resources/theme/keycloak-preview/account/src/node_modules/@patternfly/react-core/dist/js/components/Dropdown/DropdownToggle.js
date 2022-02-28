"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DropdownToggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _caretDownIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/caret-down-icon"));

var _Toggle = require("./Toggle");

var _dropdown = _interopRequireDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));

var _dropdownConstants = require("./dropdownConstants");

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DropdownToggle = function DropdownToggle(_ref) {
  var _ref$id = _ref.id,
      id = _ref$id === void 0 ? '' : _ref$id,
      _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$isOpen = _ref.isOpen,
      isOpen = _ref$isOpen === void 0 ? false : _ref$isOpen,
      _ref$parentRef = _ref.parentRef,
      parentRef = _ref$parentRef === void 0 ? null : _ref$parentRef,
      _ref$isFocused = _ref.isFocused,
      isFocused = _ref$isFocused === void 0 ? false : _ref$isFocused,
      _ref$isHovered = _ref.isHovered,
      isHovered = _ref$isHovered === void 0 ? false : _ref$isHovered,
      _ref$isActive = _ref.isActive,
      isActive = _ref$isActive === void 0 ? false : _ref$isActive,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      _ref$isPlain = _ref.isPlain,
      isPlain = _ref$isPlain === void 0 ? false : _ref$isPlain,
      _ref$isPrimary = _ref.isPrimary,
      isPrimary = _ref$isPrimary === void 0 ? false : _ref$isPrimary,
      _ref$onToggle = _ref.onToggle,
      onToggle = _ref$onToggle === void 0 ? function (_isOpen) {
    return undefined;
  } : _ref$onToggle,
      _ref$iconComponent = _ref.iconComponent,
      IconComponent = _ref$iconComponent === void 0 ? _caretDownIcon["default"] : _ref$iconComponent,
      splitButtonItems = _ref.splitButtonItems,
      _ref$splitButtonVaria = _ref.splitButtonVariant,
      splitButtonVariant = _ref$splitButtonVaria === void 0 ? 'checkbox' : _ref$splitButtonVaria,
      ariaHasPopup = _ref.ariaHasPopup,
      ref = _ref.ref,
      props = _objectWithoutProperties(_ref, ["id", "children", "className", "isOpen", "parentRef", "isFocused", "isHovered", "isActive", "isDisabled", "isPlain", "isPrimary", "onToggle", "iconComponent", "splitButtonItems", "splitButtonVariant", "ariaHasPopup", "ref"]);

  var toggle = React.createElement(_dropdownConstants.DropdownContext.Consumer, null, function (_ref2) {
    var toggleTextClass = _ref2.toggleTextClass,
        toggleIconClass = _ref2.toggleIconClass;
    return React.createElement(_Toggle.Toggle, _extends({}, props, {
      id: id,
      className: className,
      isOpen: isOpen,
      parentRef: parentRef,
      isFocused: isFocused,
      isHovered: isHovered,
      isActive: isActive,
      isDisabled: isDisabled,
      isPlain: isPlain,
      isPrimary: isPrimary,
      onToggle: onToggle,
      ariaHasPopup: ariaHasPopup
    }, splitButtonItems && {
      isSplitButton: true,
      'aria-label': props['aria-label'] || 'Select'
    }), children && React.createElement("span", {
      className: IconComponent && (0, _reactStyles.css)(toggleTextClass)
    }, children), IconComponent && React.createElement(IconComponent, {
      className: (0, _reactStyles.css)(children && toggleIconClass)
    }));
  });

  if (splitButtonItems) {
    return React.createElement("div", {
      className: (0, _reactStyles.css)(_dropdown["default"].dropdownToggle, _dropdown["default"].modifiers.splitButton, splitButtonVariant === 'action' && _dropdown["default"].modifiers.action, isDisabled && _dropdown["default"].modifiers.disabled)
    }, splitButtonItems, toggle);
  }

  return toggle;
};

exports.DropdownToggle = DropdownToggle;
DropdownToggle.propTypes = {
  id: _propTypes["default"].string,
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  isOpen: _propTypes["default"].bool,
  onToggle: _propTypes["default"].func,
  parentRef: _propTypes["default"].any,
  isFocused: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  isActive: _propTypes["default"].bool,
  isPlain: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isPrimary: _propTypes["default"].bool,
  iconComponent: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf([null])]),
  splitButtonItems: _propTypes["default"].arrayOf(_propTypes["default"].node),
  splitButtonVariant: _propTypes["default"].oneOf(['action', 'checkbox']),
  'aria-label': _propTypes["default"].string,
  ariaHasPopup: _propTypes["default"].oneOfType([_propTypes["default"].bool, _propTypes["default"].oneOf(['listbox']), _propTypes["default"].oneOf(['menu']), _propTypes["default"].oneOf(['dialog']), _propTypes["default"].oneOf(['grid']), _propTypes["default"].oneOf(['listbox']), _propTypes["default"].oneOf(['tree'])]),
  type: _propTypes["default"].oneOf(['button', 'submit', 'reset']),
  onEnter: _propTypes["default"].func
};
//# sourceMappingURL=DropdownToggle.js.map