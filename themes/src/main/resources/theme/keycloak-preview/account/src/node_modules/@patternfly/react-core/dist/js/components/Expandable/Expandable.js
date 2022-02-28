"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Expandable = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _expandable = _interopRequireDefault(require("@patternfly/react-styles/css/components/Expandable/expandable"));

var _reactStyles = require("@patternfly/react-styles");

var _angleRightIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-right-icon"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var Expandable =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Expandable, _React$Component);

  function Expandable(props) {
    var _this;

    _classCallCheck(this, Expandable);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Expandable).call(this, props));
    _this.state = {
      isExpanded: props.isExpanded
    };
    return _this;
  }

  _createClass(Expandable, [{
    key: "calculateToggleText",
    value: function calculateToggleText(toggleText, toggleTextExpanded, toggleTextCollapsed, propOrStateIsExpanded) {
      if (propOrStateIsExpanded && toggleTextExpanded !== '') {
        return toggleTextExpanded;
      }

      if (!propOrStateIsExpanded && toggleTextCollapsed !== '') {
        return toggleTextCollapsed;
      }

      return toggleText;
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      var _this$props = this.props,
          onToggleProp = _this$props.onToggle,
          isFocused = _this$props.isFocused,
          isHovered = _this$props.isHovered,
          isActive = _this$props.isActive,
          className = _this$props.className,
          toggleText = _this$props.toggleText,
          toggleTextExpanded = _this$props.toggleTextExpanded,
          toggleTextCollapsed = _this$props.toggleTextCollapsed,
          children = _this$props.children,
          isExpanded = _this$props.isExpanded,
          props = _objectWithoutProperties(_this$props, ["onToggle", "isFocused", "isHovered", "isActive", "className", "toggleText", "toggleTextExpanded", "toggleTextCollapsed", "children", "isExpanded"]);

      var onToggle = onToggleProp;
      var propOrStateIsExpanded = isExpanded; // uncontrolled

      if (isExpanded === undefined) {
        propOrStateIsExpanded = this.state.isExpanded;

        onToggle = function onToggle() {
          onToggleProp();

          _this2.setState(function (prevState) {
            return {
              isExpanded: !prevState.isExpanded
            };
          });
        };
      }

      var computedToggleText = this.calculateToggleText(toggleText, toggleTextExpanded, toggleTextCollapsed, propOrStateIsExpanded);
      return React.createElement("div", _extends({}, props, {
        className: (0, _reactStyles.css)(_expandable["default"].expandable, propOrStateIsExpanded && _expandable["default"].modifiers.expanded, className)
      }), React.createElement("button", {
        className: (0, _reactStyles.css)(_expandable["default"].expandableToggle, isFocused && _expandable["default"].modifiers.focus, isHovered && _expandable["default"].modifiers.hover, isActive && _expandable["default"].modifiers.active),
        type: "button",
        "aria-expanded": propOrStateIsExpanded,
        onClick: onToggle
      }, React.createElement(_angleRightIcon["default"], {
        className: (0, _reactStyles.css)(_expandable["default"].expandableToggleIcon),
        "aria-hidden": true
      }), React.createElement("span", null, computedToggleText)), React.createElement("div", {
        className: (0, _reactStyles.css)(_expandable["default"].expandableContent),
        hidden: !propOrStateIsExpanded
      }, children));
    }
  }]);

  return Expandable;
}(React.Component);

exports.Expandable = Expandable;

_defineProperty(Expandable, "propTypes", {
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  isExpanded: _propTypes["default"].bool,
  toggleText: _propTypes["default"].string,
  toggleTextExpanded: _propTypes["default"].string,
  toggleTextCollapsed: _propTypes["default"].string,
  onToggle: _propTypes["default"].func,
  isFocused: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  isActive: _propTypes["default"].bool
});

_defineProperty(Expandable, "defaultProps", {
  className: '',
  toggleText: '',
  toggleTextExpanded: '',
  toggleTextCollapsed: '',
  onToggle: function onToggle() {
    return undefined;
  },
  isFocused: false,
  isActive: false,
  isHovered: false
});
//# sourceMappingURL=Expandable.js.map