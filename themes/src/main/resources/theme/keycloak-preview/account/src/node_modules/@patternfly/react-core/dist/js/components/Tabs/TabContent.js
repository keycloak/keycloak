"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.TabContent = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var TabContentBase = function TabContentBase(_ref) {
  var id = _ref.id,
      activeKey = _ref.activeKey,
      ariaLabel = _ref['aria-label'],
      child = _ref.child,
      children = _ref.children,
      className = _ref.className,
      eventKey = _ref.eventKey,
      innerRef = _ref.innerRef,
      props = _objectWithoutProperties(_ref, ["id", "activeKey", "aria-label", "child", "children", "className", "eventKey", "innerRef"]);

  if (children || child) {
    var labelledBy;

    if (ariaLabel) {
      labelledBy = null;
    } else {
      labelledBy = children ? "pf-tab-".concat(eventKey, "-").concat(id) : "pf-tab-".concat(child.props.eventKey, "-").concat(id);
    }

    return React.createElement("section", _extends({
      ref: innerRef,
      hidden: children ? null : child.props.eventKey !== activeKey,
      className: children ? (0, _reactStyles.css)('pf-c-tab-content', className) : (0, _reactStyles.css)('pf-c-tab-content', child.props.className),
      id: children ? id : "pf-tab-section-".concat(child.props.eventKey, "-").concat(id),
      "aria-label": ariaLabel,
      "aria-labelledby": labelledBy,
      role: "tabpanel",
      tabIndex: 0
    }, props), children || child.props.children);
  }

  return null;
};

TabContentBase.propTypes = {
  children: _propTypes["default"].any,
  child: _propTypes["default"].element,
  className: _propTypes["default"].string,
  activeKey: _propTypes["default"].oneOfType([_propTypes["default"].number, _propTypes["default"].string]),
  eventKey: _propTypes["default"].oneOfType([_propTypes["default"].number, _propTypes["default"].string]),
  innerRef: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].func, _propTypes["default"].object]),
  id: _propTypes["default"].string.isRequired,
  'aria-label': _propTypes["default"].string
};
var TabContent = React.forwardRef(function (props, ref) {
  return React.createElement(TabContentBase, _extends({}, props, {
    innerRef: ref
  }));
});
exports.TabContent = TabContent;
//# sourceMappingURL=TabContent.js.map