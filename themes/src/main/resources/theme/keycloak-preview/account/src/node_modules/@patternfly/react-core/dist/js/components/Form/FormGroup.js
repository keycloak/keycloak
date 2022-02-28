"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.FormGroup = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _form = _interopRequireDefault(require("@patternfly/react-styles/css/components/Form/form"));

var _htmlConstants = require("../../helpers/htmlConstants");

var _FormContext = require("./FormContext");

var _reactStyles = require("@patternfly/react-styles");

var _constants = require("../../helpers/constants");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var FormGroup = function FormGroup(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      label = _ref.label,
      _ref$isRequired = _ref.isRequired,
      isRequired = _ref$isRequired === void 0 ? false : _ref$isRequired,
      _ref$isValid = _ref.isValid,
      isValid = _ref$isValid === void 0 ? true : _ref$isValid,
      _ref$validated = _ref.validated,
      validated = _ref$validated === void 0 ? 'default' : _ref$validated,
      _ref$isInline = _ref.isInline,
      isInline = _ref$isInline === void 0 ? false : _ref$isInline,
      helperText = _ref.helperText,
      helperTextInvalid = _ref.helperTextInvalid,
      fieldId = _ref.fieldId,
      props = _objectWithoutProperties(_ref, ["children", "className", "label", "isRequired", "isValid", "validated", "isInline", "helperText", "helperTextInvalid", "fieldId"]);

  var validHelperText = React.createElement("div", {
    className: (0, _reactStyles.css)(_form["default"].formHelperText, validated === _constants.ValidatedOptions.success && _form["default"].modifiers.success),
    id: "".concat(fieldId, "-helper"),
    "aria-live": "polite"
  }, helperText);
  var inValidHelperText = React.createElement("div", {
    className: (0, _reactStyles.css)(_form["default"].formHelperText, _form["default"].modifiers.error),
    id: "".concat(fieldId, "-helper"),
    "aria-live": "polite"
  }, helperTextInvalid);
  return React.createElement(_FormContext.FormContext.Consumer, null, function (_ref2) {
    var isHorizontal = _ref2.isHorizontal;
    return React.createElement("div", _extends({}, props, {
      className: (0, _reactStyles.css)(_form["default"].formGroup, isInline ? (0, _reactStyles.getModifier)(_form["default"], 'inline', className) : className)
    }), label && React.createElement("label", {
      className: (0, _reactStyles.css)(_form["default"].formLabel),
      htmlFor: fieldId
    }, React.createElement("span", {
      className: (0, _reactStyles.css)(_form["default"].formLabelText)
    }, label), isRequired && React.createElement("span", {
      className: (0, _reactStyles.css)(_form["default"].formLabelRequired),
      "aria-hidden": "true"
    }, _htmlConstants.ASTERISK)), isHorizontal ? React.createElement("div", {
      className: (0, _reactStyles.css)(_form["default"].formHorizontalGroup)
    }, children) : children, (!isValid || validated === _constants.ValidatedOptions.error) && helperTextInvalid ? inValidHelperText : validated !== _constants.ValidatedOptions.error && helperText ? validHelperText : '');
  });
};

exports.FormGroup = FormGroup;
FormGroup.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  label: _propTypes["default"].node,
  isRequired: _propTypes["default"].bool,
  isValid: _propTypes["default"].bool,
  validated: _propTypes["default"].oneOf(['success', 'error', 'default']),
  isInline: _propTypes["default"].bool,
  helperText: _propTypes["default"].node,
  helperTextInvalid: _propTypes["default"].node,
  fieldId: _propTypes["default"].string.isRequired
};
//# sourceMappingURL=FormGroup.js.map