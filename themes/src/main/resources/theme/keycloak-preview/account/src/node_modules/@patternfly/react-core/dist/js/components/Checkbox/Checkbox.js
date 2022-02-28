"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Checkbox = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _check = _interopRequireDefault(require("@patternfly/react-styles/css/components/Check/check"));

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

// tslint:disable-next-line:no-empty
var defaultOnChange = function defaultOnChange() {};

var Checkbox =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Checkbox, _React$Component);

  function Checkbox(props) {
    var _this;

    _classCallCheck(this, Checkbox);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Checkbox).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "handleChange", function (event) {
      _this.props.onChange(event.currentTarget.checked, event);
    });

    return _this;
  }

  _createClass(Checkbox, [{
    key: "render",
    value: function render() {
      var _this$props = this.props,
          ariaLabel = _this$props['aria-label'],
          className = _this$props.className,
          onChange = _this$props.onChange,
          isValid = _this$props.isValid,
          isDisabled = _this$props.isDisabled,
          isChecked = _this$props.isChecked,
          label = _this$props.label,
          checked = _this$props.checked,
          defaultChecked = _this$props.defaultChecked,
          description = _this$props.description,
          props = _objectWithoutProperties(_this$props, ["aria-label", "className", "onChange", "isValid", "isDisabled", "isChecked", "label", "checked", "defaultChecked", "description"]);

      var checkedProps = {};

      if ([true, false].includes(checked) || isChecked === true) {
        checkedProps.checked = checked || isChecked;
      }

      if (onChange !== defaultOnChange) {
        checkedProps.checked = isChecked;
      }

      if ([false, true].includes(defaultChecked)) {
        checkedProps.defaultChecked = defaultChecked;
      }

      checkedProps.checked = checkedProps.checked === null ? false : checkedProps.checked;
      return React.createElement("div", {
        className: (0, _reactStyles.css)(_check["default"].check, className)
      }, React.createElement("input", _extends({}, props, {
        className: (0, _reactStyles.css)(_check["default"].checkInput),
        type: "checkbox",
        onChange: this.handleChange,
        "aria-invalid": !isValid,
        "aria-label": ariaLabel,
        disabled: isDisabled,
        ref: function ref(elem) {
          return elem && (elem.indeterminate = isChecked === null);
        }
      }, checkedProps)), label && React.createElement("label", {
        className: (0, _reactStyles.css)(_check["default"].checkLabel, isDisabled ? (0, _reactStyles.getModifier)(_check["default"], 'disabled') : ''),
        htmlFor: props.id
      }, label), description && React.createElement("div", {
        className: (0, _reactStyles.css)(_check["default"].checkDescription)
      }, description));
    }
  }]);

  return Checkbox;
}(React.Component);

exports.Checkbox = Checkbox;

_defineProperty(Checkbox, "propTypes", {
  className: _propTypes["default"].string,
  isValid: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isChecked: _propTypes["default"].bool,
  checked: _propTypes["default"].bool,
  onChange: _propTypes["default"].func,
  label: _propTypes["default"].node,
  id: _propTypes["default"].string.isRequired,
  'aria-label': _propTypes["default"].string,
  description: _propTypes["default"].node
});

_defineProperty(Checkbox, "defaultProps", {
  className: '',
  isValid: true,
  isDisabled: false,
  isChecked: false,
  onChange: defaultOnChange
});
//# sourceMappingURL=Checkbox.js.map