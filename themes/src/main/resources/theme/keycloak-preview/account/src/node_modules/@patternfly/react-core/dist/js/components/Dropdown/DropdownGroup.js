"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DropdownGroup = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _dropdownConstants = require("./dropdownConstants");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var DropdownGroup = function DropdownGroup(_ref) {
  var _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$label = _ref.label,
      label = _ref$label === void 0 ? '' : _ref$label;
  return React.createElement(_dropdownConstants.DropdownContext.Consumer, null, function (_ref2) {
    var sectionClass = _ref2.sectionClass,
        sectionTitleClass = _ref2.sectionTitleClass,
        sectionComponent = _ref2.sectionComponent;
    var SectionComponent = sectionComponent;
    return React.createElement(SectionComponent, {
      className: (0, _reactStyles.css)(sectionClass, className)
    }, label && React.createElement("h1", {
      className: (0, _reactStyles.css)(sectionTitleClass),
      "aria-hidden": true
    }, label), React.createElement("ul", {
      role: "none"
    }, children));
  });
};

exports.DropdownGroup = DropdownGroup;
DropdownGroup.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  label: _propTypes["default"].node
};
//# sourceMappingURL=DropdownGroup.js.map