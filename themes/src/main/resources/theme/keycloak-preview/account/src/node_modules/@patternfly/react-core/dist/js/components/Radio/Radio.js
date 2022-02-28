"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Radio = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _radio = _interopRequireDefault(require("@patternfly/react-styles/css/components/Radio/radio"));

var _reactStyles = require("@patternfly/react-styles");

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

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var Radio =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Radio, _React$Component);

  function Radio(props) {
    var _this;

    _classCallCheck(this, Radio);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Radio).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "handleChange", function (event) {
      _this.props.onChange(event.currentTarget.checked, event);
    });

    if (!props.label && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('Radio:', 'Radio requires an aria-label to be specified');
    }

    return _this;
  }

  _createClass(Radio, [{
    key: "render",
    value: function render() {
      var _this$props = this.props,
          ariaLabel = _this$props['aria-label'],
          checked = _this$props.checked,
          className = _this$props.className,
          defaultChecked = _this$props.defaultChecked,
          isLabelWrapped = _this$props.isLabelWrapped,
          isLabelBeforeButton = _this$props.isLabelBeforeButton,
          isChecked = _this$props.isChecked,
          isDisabled = _this$props.isDisabled,
          isValid = _this$props.isValid,
          label = _this$props.label,
          onChange = _this$props.onChange,
          description = _this$props.description,
          props = _objectWithoutProperties(_this$props, ["aria-label", "checked", "className", "defaultChecked", "isLabelWrapped", "isLabelBeforeButton", "isChecked", "isDisabled", "isValid", "label", "onChange", "description"]);

      var inputRendered = React.createElement("input", _extends({}, props, {
        className: (0, _reactStyles.css)(_radio["default"].radioInput),
        type: "radio",
        onChange: this.handleChange,
        "aria-invalid": !isValid,
        disabled: isDisabled,
        checked: checked || isChecked
      }, checked === undefined && {
        defaultChecked: defaultChecked
      }, !label && {
        'aria-label': ariaLabel
      }));
      var labelRendered = !label ? null : isLabelWrapped ? React.createElement("span", {
        className: (0, _reactStyles.css)(_radio["default"].radioLabel, (0, _reactStyles.getModifier)(_radio["default"], isDisabled && 'disabled'))
      }, label) : React.createElement("label", {
        className: (0, _reactStyles.css)(_radio["default"].radioLabel, (0, _reactStyles.getModifier)(_radio["default"], isDisabled && 'disabled')),
        htmlFor: props.id
      }, label);
      var descRender = description ? React.createElement("div", {
        className: (0, _reactStyles.css)(_radio["default"].radioDescription)
      }, description) : null;
      var childrenRendered = isLabelBeforeButton ? React.createElement(React.Fragment, null, labelRendered, inputRendered, descRender) : React.createElement(React.Fragment, null, inputRendered, labelRendered, descRender);
      return isLabelWrapped ? React.createElement("label", {
        className: (0, _reactStyles.css)(_radio["default"].radio, className),
        htmlFor: props.id
      }, childrenRendered) : React.createElement("div", {
        className: (0, _reactStyles.css)(_radio["default"].radio, className)
      }, childrenRendered);
    }
  }]);

  return Radio;
}(React.Component);

exports.Radio = Radio;

_defineProperty(Radio, "propTypes", {
  className: _propTypes["default"].string,
  id: _propTypes["default"].string.isRequired,
  isLabelWrapped: _propTypes["default"].bool,
  isLabelBeforeButton: _propTypes["default"].bool,
  checked: _propTypes["default"].bool,
  isChecked: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isValid: _propTypes["default"].bool,
  label: _propTypes["default"].node,
  name: _propTypes["default"].string.isRequired,
  onChange: _propTypes["default"].func,
  'aria-label': _propTypes["default"].string,
  description: _propTypes["default"].node
});

_defineProperty(Radio, "defaultProps", {
  className: '',
  isDisabled: false,
  isValid: true,
  onChange: function onChange() {}
});
//# sourceMappingURL=Radio.js.map