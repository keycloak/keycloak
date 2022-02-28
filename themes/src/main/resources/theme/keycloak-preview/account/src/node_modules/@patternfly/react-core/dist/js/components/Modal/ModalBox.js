"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ModalBox = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _modalBox = _interopRequireDefault(require("@patternfly/react-styles/css/components/ModalBox/modal-box"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var ModalBox = function ModalBox(_ref) {
  var children = _ref.children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$isLarge = _ref.isLarge,
      isLarge = _ref$isLarge === void 0 ? false : _ref$isLarge,
      _ref$isSmall = _ref.isSmall,
      isSmall = _ref$isSmall === void 0 ? false : _ref$isSmall,
      title = _ref.title,
      id = _ref.id,
      props = _objectWithoutProperties(_ref, ["children", "className", "isLarge", "isSmall", "title", "id"]);

  return React.createElement("div", _extends({}, props, {
    role: "dialog",
    "aria-label": title,
    "aria-describedby": id,
    "aria-modal": "true",
    className: (0, _reactStyles.css)(_modalBox["default"].modalBox, className, isLarge && _modalBox["default"].modifiers.lg, isSmall && _modalBox["default"].modifiers.sm)
  }), children);
};

exports.ModalBox = ModalBox;
ModalBox.propTypes = {
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  isLarge: _propTypes["default"].bool,
  isSmall: _propTypes["default"].bool,
  title: _propTypes["default"].string.isRequired,
  id: _propTypes["default"].string.isRequired
};
//# sourceMappingURL=ModalBox.js.map