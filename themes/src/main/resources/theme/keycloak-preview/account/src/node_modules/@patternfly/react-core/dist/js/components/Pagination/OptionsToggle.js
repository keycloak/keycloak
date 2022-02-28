"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.OptionsToggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _optionsMenu = _interopRequireDefault(require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));

var _reactStyles = require("@patternfly/react-styles");

var _helpers = require("../../helpers");

var _Dropdown = require("../Dropdown");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var toggleId = 0;

var OptionsToggle = function OptionsToggle(_ref) {
  var _ref$itemsTitle = _ref.itemsTitle,
      itemsTitle = _ref$itemsTitle === void 0 ? 'items' : _ref$itemsTitle,
      _ref$optionsToggle = _ref.optionsToggle,
      optionsToggle = _ref$optionsToggle === void 0 ? 'Select' : _ref$optionsToggle,
      _ref$itemsPerPageTitl = _ref.itemsPerPageTitle,
      itemsPerPageTitle = _ref$itemsPerPageTitl === void 0 ? 'Items per page' : _ref$itemsPerPageTitl,
      _ref$firstIndex = _ref.firstIndex,
      firstIndex = _ref$firstIndex === void 0 ? 0 : _ref$firstIndex,
      _ref$lastIndex = _ref.lastIndex,
      lastIndex = _ref$lastIndex === void 0 ? 0 : _ref$lastIndex,
      _ref$itemCount = _ref.itemCount,
      itemCount = _ref$itemCount === void 0 ? 0 : _ref$itemCount,
      _ref$widgetId = _ref.widgetId,
      widgetId = _ref$widgetId === void 0 ? '' : _ref$widgetId,
      _ref$showToggle = _ref.showToggle,
      showToggle = _ref$showToggle === void 0 ? true : _ref$showToggle,
      _ref$onToggle = _ref.onToggle,
      onToggle = _ref$onToggle === void 0 ? function (_isOpen) {
    return undefined;
  } : _ref$onToggle,
      _ref$isOpen = _ref.isOpen,
      isOpen = _ref$isOpen === void 0 ? false : _ref$isOpen,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      _ref$parentRef = _ref.parentRef,
      parentRef = _ref$parentRef === void 0 ? null : _ref$parentRef,
      _ref$toggleTemplate = _ref.toggleTemplate,
      ToggleTemplate = _ref$toggleTemplate === void 0 ? '' : _ref$toggleTemplate,
      _ref$onEnter = _ref.onEnter,
      onEnter = _ref$onEnter === void 0 ? null : _ref$onEnter;
  return React.createElement("div", {
    className: (0, _reactStyles.css)(_optionsMenu["default"].optionsMenuToggle, isDisabled && _optionsMenu["default"].modifiers.disabled, _optionsMenu["default"].modifiers.plain, _optionsMenu["default"].modifiers.text)
  }, showToggle && React.createElement(React.Fragment, null, React.createElement("span", {
    className: (0, _reactStyles.css)(_optionsMenu["default"].optionsMenuToggleText)
  }, typeof ToggleTemplate === 'string' ? (0, _helpers.fillTemplate)(ToggleTemplate, {
    firstIndex: firstIndex,
    lastIndex: lastIndex,
    itemCount: itemCount,
    itemsTitle: itemsTitle
  }) : React.createElement(ToggleTemplate, {
    firstIndex: firstIndex,
    lastIndex: lastIndex,
    itemCount: itemCount,
    itemsTitle: itemsTitle
  })), React.createElement(_Dropdown.DropdownToggle, {
    onEnter: onEnter,
    "aria-label": optionsToggle,
    onToggle: onToggle,
    isDisabled: isDisabled || itemCount <= 0,
    isOpen: isOpen,
    id: "".concat(widgetId, "-toggle-").concat(toggleId++),
    className: _optionsMenu["default"].optionsMenuToggleButton,
    parentRef: parentRef
  })));
};

exports.OptionsToggle = OptionsToggle;
OptionsToggle.propTypes = {
  itemsTitle: _propTypes["default"].string,
  optionsToggle: _propTypes["default"].string,
  itemsPerPageTitle: _propTypes["default"].string,
  firstIndex: _propTypes["default"].number,
  lastIndex: _propTypes["default"].number,
  itemCount: _propTypes["default"].number,
  widgetId: _propTypes["default"].string,
  showToggle: _propTypes["default"].bool,
  onToggle: _propTypes["default"].func,
  isOpen: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  parentRef: _propTypes["default"].any,
  toggleTemplate: _propTypes["default"].oneOfType([_propTypes["default"].func, _propTypes["default"].string]),
  onEnter: _propTypes["default"].func
};
//# sourceMappingURL=OptionsToggle.js.map