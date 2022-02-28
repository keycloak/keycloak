"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ProgressContainer = exports.ProgressVariant = exports.ProgressMeasureLocation = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _progress = _interopRequireDefault(require("@patternfly/react-styles/css/components/Progress/progress"));

var _reactStyles = require("@patternfly/react-styles");

var _checkCircleIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/check-circle-icon"));

var _timesCircleIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/times-circle-icon"));

var _ProgressBar = require("./ProgressBar");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var ProgressMeasureLocation;
exports.ProgressMeasureLocation = ProgressMeasureLocation;

(function (ProgressMeasureLocation) {
  ProgressMeasureLocation["outside"] = "outside";
  ProgressMeasureLocation["inside"] = "inside";
  ProgressMeasureLocation["top"] = "top";
  ProgressMeasureLocation["none"] = "none";
})(ProgressMeasureLocation || (exports.ProgressMeasureLocation = ProgressMeasureLocation = {}));

var ProgressVariant;
exports.ProgressVariant = ProgressVariant;

(function (ProgressVariant) {
  ProgressVariant["danger"] = "danger";
  ProgressVariant["success"] = "success";
  ProgressVariant["info"] = "info";
})(ProgressVariant || (exports.ProgressVariant = ProgressVariant = {}));

var variantToIcon = {
  danger: _timesCircleIcon["default"],
  success: _checkCircleIcon["default"]
};

var ProgressContainer = function ProgressContainer(_ref) {
  var ariaProps = _ref.ariaProps,
      value = _ref.value,
      _ref$title = _ref.title,
      title = _ref$title === void 0 ? '' : _ref$title,
      parentId = _ref.parentId,
      _ref$label = _ref.label,
      label = _ref$label === void 0 ? null : _ref$label,
      _ref$variant = _ref.variant,
      variant = _ref$variant === void 0 ? ProgressVariant.info : _ref$variant,
      _ref$measureLocation = _ref.measureLocation,
      measureLocation = _ref$measureLocation === void 0 ? ProgressMeasureLocation.top : _ref$measureLocation;
  var StatusIcon = variantToIcon.hasOwnProperty(variant) && variantToIcon[variant];
  return React.createElement(React.Fragment, null, React.createElement("div", {
    className: (0, _reactStyles.css)(_progress["default"].progressDescription),
    id: "".concat(parentId, "-description")
  }, title), React.createElement("div", {
    className: (0, _reactStyles.css)(_progress["default"].progressStatus)
  }, (measureLocation === ProgressMeasureLocation.top || measureLocation === ProgressMeasureLocation.outside) && React.createElement("span", {
    className: (0, _reactStyles.css)(_progress["default"].progressMeasure)
  }, label || "".concat(value, "%")), variantToIcon.hasOwnProperty(variant) && React.createElement("span", {
    className: (0, _reactStyles.css)(_progress["default"].progressStatusIcon)
  }, React.createElement(StatusIcon, null))), React.createElement(_ProgressBar.ProgressBar, {
    ariaProps: ariaProps,
    value: value
  }, measureLocation === ProgressMeasureLocation.inside && "".concat(value, "%")));
};

exports.ProgressContainer = ProgressContainer;
ProgressContainer.propTypes = {
  ariaProps: _propTypes["default"].any,
  parentId: _propTypes["default"].string.isRequired,
  title: _propTypes["default"].string,
  label: _propTypes["default"].node,
  variant: _propTypes["default"].oneOf(['danger', 'success', 'info']),
  measureLocation: _propTypes["default"].oneOf(['outside', 'inside', 'top', 'none']),
  value: _propTypes["default"].number.isRequired
};
//# sourceMappingURL=ProgressContainer.js.map