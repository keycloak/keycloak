"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataListCheck = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _dataList = _interopRequireDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DataListCheck = function DataListCheck(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$onChange = _ref.onChange,
      _onChange = _ref$onChange === void 0 ? function (checked, event) {} : _ref$onChange,
      _ref$isValid = _ref.isValid,
      isValid = _ref$isValid === void 0 ? true : _ref$isValid,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      _ref$isChecked = _ref.isChecked,
      isChecked = _ref$isChecked === void 0 ? null : _ref$isChecked,
      _ref$checked = _ref.checked,
      checked = _ref$checked === void 0 ? null : _ref$checked,
      props = _objectWithoutProperties(_ref, ["className", "onChange", "isValid", "isDisabled", "isChecked", "checked"]);

  return React.createElement("div", {
    className: (0, _reactStyles.css)(_dataList["default"].dataListItemControl, className)
  }, React.createElement("div", {
    className: (0, _reactStyles.css)('pf-c-data-list__check')
  }, React.createElement("input", _extends({}, props, {
    type: "checkbox",
    onChange: function onChange(event) {
      return _onChange(event.currentTarget.checked, event);
    },
    "aria-invalid": !isValid,
    disabled: isDisabled,
    checked: isChecked || checked
  }))));
};

exports.DataListCheck = DataListCheck;
DataListCheck.propTypes = {
  className: _propTypes["default"].string,
  isValid: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isChecked: _propTypes["default"].bool,
  checked: _propTypes["default"].bool,
  onChange: _propTypes["default"].func,
  'aria-labelledby': _propTypes["default"].string.isRequired
};
//# sourceMappingURL=DataListCheck.js.map