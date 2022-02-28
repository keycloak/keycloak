"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataListToggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _angleRightIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-right-icon"));

var _dataList = _interopRequireDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));

var _Button = require("../Button");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DataListToggle = function DataListToggle(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$isExpanded = _ref.isExpanded,
      isExpanded = _ref$isExpanded === void 0 ? false : _ref$isExpanded,
      _ref$ariaControls = _ref['aria-controls'],
      ariaControls = _ref$ariaControls === void 0 ? '' : _ref$ariaControls,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? 'Details' : _ref$ariaLabel,
      _ref$ariaLabelledby = _ref['aria-labelledby'],
      ariaLabelledBy = _ref$ariaLabelledby === void 0 ? '' : _ref$ariaLabelledby,
      _ref$rowid = _ref.rowid,
      rowid = _ref$rowid === void 0 ? '' : _ref$rowid,
      id = _ref.id,
      props = _objectWithoutProperties(_ref, ["className", "isExpanded", "aria-controls", "aria-label", "aria-labelledby", "rowid", "id"]);

  return React.createElement("div", _extends({
    className: (0, _reactStyles.css)(_dataList["default"].dataListItemControl, className)
  }, props), React.createElement("div", {
    className: (0, _reactStyles.css)(_dataList["default"].dataListToggle)
  }, React.createElement(_Button.Button, {
    id: id,
    variant: _Button.ButtonVariant.plain,
    "aria-controls": ariaControls !== '' && ariaControls,
    "aria-label": ariaLabel,
    "aria-labelledby": ariaLabel !== 'Details' ? null : "".concat(rowid, " ").concat(id),
    "aria-expanded": isExpanded
  }, React.createElement(_angleRightIcon["default"], null))));
};

exports.DataListToggle = DataListToggle;
DataListToggle.propTypes = {
  className: _propTypes["default"].string,
  isExpanded: _propTypes["default"].bool,
  id: _propTypes["default"].string.isRequired,
  rowid: _propTypes["default"].string,
  'aria-labelledby': _propTypes["default"].string,
  'aria-label': _propTypes["default"].string,
  'aria-controls': _propTypes["default"].string
};
//# sourceMappingURL=DataListToggle.js.map