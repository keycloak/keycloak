"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.List = exports.ListComponent = exports.ListVariant = exports.OrderType = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _list = _interopRequireDefault(require("@patternfly/react-styles/css/components/List/list"));

var _reactStyles = require("@patternfly/react-styles");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var OrderType;
exports.OrderType = OrderType;

(function (OrderType) {
  OrderType["number"] = "1";
  OrderType["lowercaseLetter"] = "a";
  OrderType["uppercaseLetter"] = "A";
  OrderType["lowercaseRomanNumber"] = "i";
  OrderType["uppercaseRomanNumber"] = "I";
})(OrderType || (exports.OrderType = OrderType = {}));

var ListVariant;
exports.ListVariant = ListVariant;

(function (ListVariant) {
  ListVariant["inline"] = "inline";
})(ListVariant || (exports.ListVariant = ListVariant = {}));

var ListComponent;
exports.ListComponent = ListComponent;

(function (ListComponent) {
  ListComponent["ol"] = "ol";
  ListComponent["ul"] = "ul";
})(ListComponent || (exports.ListComponent = ListComponent = {}));

var List = function List(_ref) {
  var _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$variant = _ref.variant,
      variant = _ref$variant === void 0 ? null : _ref$variant,
      _ref$type = _ref.type,
      type = _ref$type === void 0 ? OrderType.number : _ref$type,
      _ref$ref = _ref.ref,
      ref = _ref$ref === void 0 ? null : _ref$ref,
      _ref$component = _ref.component,
      component = _ref$component === void 0 ? ListComponent.ul : _ref$component,
      props = _objectWithoutProperties(_ref, ["className", "children", "variant", "type", "ref", "component"]);

  return component === ListComponent.ol ? React.createElement("ol", _extends({
    ref: ref,
    type: type
  }, props, {
    className: (0, _reactStyles.css)(_list["default"].list, variant && (0, _reactStyles.getModifier)(_list["default"].modifiers, variant), className)
  }), children) : React.createElement("ul", _extends({
    ref: ref
  }, props, {
    className: (0, _reactStyles.css)(_list["default"].list, variant && (0, _reactStyles.getModifier)(_list["default"].modifiers, variant), className)
  }), children);
};

exports.List = List;
List.propTypes = {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  variant: _propTypes["default"].any,
  type: _propTypes["default"].any,
  component: _propTypes["default"].oneOf(['ol', 'ul'])
};
//# sourceMappingURL=List.js.map