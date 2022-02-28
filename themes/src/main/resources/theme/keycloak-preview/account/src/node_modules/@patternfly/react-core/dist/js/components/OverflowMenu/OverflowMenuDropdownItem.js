"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.OverflowMenuDropdownItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _Dropdown = require("../Dropdown");

var _OverflowMenuContext = require("./OverflowMenuContext");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var OverflowMenuDropdownItem = function OverflowMenuDropdownItem(_ref) {
  var children = _ref.children,
      _ref$isShared = _ref.isShared,
      isShared = _ref$isShared === void 0 ? false : _ref$isShared;
  return React.createElement(_OverflowMenuContext.OverflowMenuContext.Consumer, null, function (value) {
    return (!isShared || value.isBelowBreakpoint) && React.createElement(_Dropdown.DropdownItem, {
      component: "button"
    }, " ", children, " ");
  });
};

exports.OverflowMenuDropdownItem = OverflowMenuDropdownItem;
OverflowMenuDropdownItem.propTypes = {
  children: _propTypes["default"].any,
  isShared: _propTypes["default"].bool
};
//# sourceMappingURL=OverflowMenuDropdownItem.js.map