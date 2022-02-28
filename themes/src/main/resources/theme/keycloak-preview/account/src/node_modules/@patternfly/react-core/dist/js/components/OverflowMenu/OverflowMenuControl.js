"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.OverflowMenuControl = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _overflowMenu = _interopRequireDefault(require("@patternfly/react-styles/css/components/OverflowMenu/overflow-menu"));

var _OverflowMenuContext = require("./OverflowMenuContext");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var OverflowMenuControl = function OverflowMenuControl(_ref) {
  var className = _ref.className,
      children = _ref.children,
      hasAdditionalOptions = _ref.hasAdditionalOptions;
  return React.createElement(_OverflowMenuContext.OverflowMenuContext.Consumer, null, function (value) {
    return (value.isBelowBreakpoint || hasAdditionalOptions) && React.createElement("div", {
      className: (0, _reactStyles.css)(_overflowMenu["default"].overflowMenuControl, className)
    }, " ", children, " ");
  });
};

exports.OverflowMenuControl = OverflowMenuControl;
OverflowMenuControl.propTypes = {
  children: _propTypes["default"].any,
  className: _propTypes["default"].string,
  hasAdditionalOptions: _propTypes["default"].bool
};
//# sourceMappingURL=OverflowMenuControl.js.map