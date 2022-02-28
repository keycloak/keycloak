"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ToggleTemplate = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

var ToggleTemplate = function ToggleTemplate(_ref) {
  var _ref$firstIndex = _ref.firstIndex,
      firstIndex = _ref$firstIndex === void 0 ? 0 : _ref$firstIndex,
      _ref$lastIndex = _ref.lastIndex,
      lastIndex = _ref$lastIndex === void 0 ? 0 : _ref$lastIndex,
      _ref$itemCount = _ref.itemCount,
      itemCount = _ref$itemCount === void 0 ? 0 : _ref$itemCount,
      _ref$itemsTitle = _ref.itemsTitle,
      itemsTitle = _ref$itemsTitle === void 0 ? 'items' : _ref$itemsTitle;
  return React.createElement(React.Fragment, null, React.createElement("b", null, firstIndex, " - ", lastIndex), ' ', "of ", React.createElement("b", null, itemCount), " ", itemsTitle);
};

exports.ToggleTemplate = ToggleTemplate;
ToggleTemplate.propTypes = {
  firstIndex: _propTypes["default"].number,
  lastIndex: _propTypes["default"].number,
  itemCount: _propTypes["default"].number,
  itemsTitle: _propTypes["default"].string
};
//# sourceMappingURL=ToggleTemplate.js.map