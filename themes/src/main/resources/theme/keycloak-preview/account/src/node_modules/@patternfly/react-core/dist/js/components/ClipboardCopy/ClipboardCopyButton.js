"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ClipboardCopyButton = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _clipboardCopy = _interopRequireDefault(require("@patternfly/react-styles/css/components/ClipboardCopy/clipboard-copy"));

var _reactStyles = require("@patternfly/react-styles");

var _copyIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/copy-icon"));

var _Tooltip = require("../Tooltip");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var ClipboardCopyButton = function ClipboardCopyButton(_ref) {
  var onClick = _ref.onClick,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$exitDelay = _ref.exitDelay,
      exitDelay = _ref$exitDelay === void 0 ? 100 : _ref$exitDelay,
      _ref$entryDelay = _ref.entryDelay,
      entryDelay = _ref$entryDelay === void 0 ? 100 : _ref$entryDelay,
      _ref$maxWidth = _ref.maxWidth,
      maxWidth = _ref$maxWidth === void 0 ? '100px' : _ref$maxWidth,
      _ref$position = _ref.position,
      position = _ref$position === void 0 ? 'top' : _ref$position,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? 'Copyable input' : _ref$ariaLabel,
      id = _ref.id,
      textId = _ref.textId,
      children = _ref.children,
      props = _objectWithoutProperties(_ref, ["onClick", "className", "exitDelay", "entryDelay", "maxWidth", "position", "aria-label", "id", "textId", "children"]);

  return React.createElement(_Tooltip.Tooltip, {
    trigger: "mouseenter focus click",
    exitDelay: exitDelay,
    entryDelay: entryDelay,
    maxWidth: maxWidth,
    position: position,
    content: React.createElement("div", null, children)
  }, React.createElement("button", _extends({
    type: "button",
    onClick: onClick,
    className: (0, _reactStyles.css)(_clipboardCopy["default"].clipboardCopyGroupCopy, className),
    "aria-label": ariaLabel,
    id: id,
    "aria-labelledby": "".concat(id, " ").concat(textId)
  }, props), React.createElement(_copyIcon["default"], null)));
};

exports.ClipboardCopyButton = ClipboardCopyButton;
ClipboardCopyButton.propTypes = {
  onClick: _propTypes["default"].func.isRequired,
  children: _propTypes["default"].node.isRequired,
  id: _propTypes["default"].string.isRequired,
  textId: _propTypes["default"].string.isRequired,
  className: _propTypes["default"].string,
  exitDelay: _propTypes["default"].number,
  entryDelay: _propTypes["default"].number,
  maxWidth: _propTypes["default"].string,
  position: _propTypes["default"].oneOf(['auto', 'top', 'bottom', 'left', 'right']),
  'aria-label': _propTypes["default"].string
};
//# sourceMappingURL=ClipboardCopyButton.js.map