"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DataToolbarItem = exports.DataToolbarItemVariant = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _dataToolbar = _interopRequireDefault(require("@patternfly/react-styles/css/components/DataToolbar/data-toolbar"));

var _reactStyles = require("@patternfly/react-styles");

var _util = require("../../helpers/util");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var DataToolbarItemVariant;
exports.DataToolbarItemVariant = DataToolbarItemVariant;

(function (DataToolbarItemVariant) {
  DataToolbarItemVariant["separator"] = "separator";
  DataToolbarItemVariant["bulk-select"] = "bulk-select";
  DataToolbarItemVariant["overflow-menu"] = "overflow-menu";
  DataToolbarItemVariant["pagination"] = "pagination";
  DataToolbarItemVariant["search-filter"] = "search-filter";
  DataToolbarItemVariant["label"] = "label";
  DataToolbarItemVariant["chip-group"] = "chip-group";
})(DataToolbarItemVariant || (exports.DataToolbarItemVariant = DataToolbarItemVariant = {}));

var DataToolbarItem = function DataToolbarItem(_ref) {
  var className = _ref.className,
      variant = _ref.variant,
      _ref$breakpointMods = _ref.breakpointMods,
      breakpointMods = _ref$breakpointMods === void 0 ? [] : _ref$breakpointMods,
      id = _ref.id,
      children = _ref.children,
      props = _objectWithoutProperties(_ref, ["className", "variant", "breakpointMods", "id", "children"]);

  var labelVariant = variant === 'label';
  return React.createElement("div", _extends({
    className: (0, _reactStyles.css)(_dataToolbar["default"].dataToolbarItem, variant && (0, _reactStyles.getModifier)(_dataToolbar["default"], variant), (0, _util.formatBreakpointMods)(breakpointMods, _dataToolbar["default"]), className)
  }, labelVariant && {
    'aria-hidden': true
  }, {
    id: id
  }, props), children);
};

exports.DataToolbarItem = DataToolbarItem;
DataToolbarItem.propTypes = {
  className: _propTypes["default"].string,
  variant: _propTypes["default"].oneOfType([_propTypes["default"].any, _propTypes["default"].oneOf(['separator']), _propTypes["default"].oneOf(['bulk-select']), _propTypes["default"].oneOf(['overflow-menu']), _propTypes["default"].oneOf(['pagination']), _propTypes["default"].oneOf(['search-filter']), _propTypes["default"].oneOf(['label']), _propTypes["default"].oneOf(['chip-group'])]),
  breakpointMods: _propTypes["default"].arrayOf(_propTypes["default"].any),
  id: _propTypes["default"].string,
  children: _propTypes["default"].node
};
//# sourceMappingURL=DataToolbarItem.js.map