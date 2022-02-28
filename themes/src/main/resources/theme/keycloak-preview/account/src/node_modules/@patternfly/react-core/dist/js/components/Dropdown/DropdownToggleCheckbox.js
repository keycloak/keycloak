"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DropdownToggleCheckbox = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _dropdown = _interopRequireDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));

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

var DropdownToggleCheckbox =
/*#__PURE__*/
function (_React$Component) {
  _inherits(DropdownToggleCheckbox, _React$Component);

  function DropdownToggleCheckbox() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, DropdownToggleCheckbox);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(DropdownToggleCheckbox)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "handleChange", function (event) {
      _this.props.onChange(event.target.checked, event);
    });

    _defineProperty(_assertThisInitialized(_this), "calculateChecked", function () {
      var _this$props = _this.props,
          isChecked = _this$props.isChecked,
          checked = _this$props.checked;
      return isChecked !== undefined ? isChecked : checked;
    });

    return _this;
  }

  _createClass(DropdownToggleCheckbox, [{
    key: "render",
    value: function render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      var _this$props2 = this.props,
          className = _this$props2.className,
          onChange = _this$props2.onChange,
          isValid = _this$props2.isValid,
          isDisabled = _this$props2.isDisabled,
          isChecked = _this$props2.isChecked,
          ref = _this$props2.ref,
          checked = _this$props2.checked,
          children = _this$props2.children,
          props = _objectWithoutProperties(_this$props2, ["className", "onChange", "isValid", "isDisabled", "isChecked", "ref", "checked", "children"]);

      var text = children && React.createElement("span", {
        className: (0, _reactStyles.css)(_dropdown["default"].dropdownToggleText, className),
        "aria-hidden": "true",
        id: "".concat(props.id, "-text")
      }, children);
      return React.createElement("label", {
        className: (0, _reactStyles.css)(_dropdown["default"].dropdownToggleCheck, className),
        htmlFor: props.id
      }, React.createElement("input", _extends({}, props, this.calculateChecked() !== undefined && {
        onChange: this.handleChange
      }, {
        type: "checkbox",
        ref: ref,
        "aria-invalid": !isValid,
        disabled: isDisabled,
        checked: this.calculateChecked()
      })), text);
    }
  }]);

  return DropdownToggleCheckbox;
}(React.Component);

exports.DropdownToggleCheckbox = DropdownToggleCheckbox;

_defineProperty(DropdownToggleCheckbox, "propTypes", {
  className: _propTypes["default"].string,
  isValid: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isChecked: _propTypes["default"].oneOfType([_propTypes["default"].bool, _propTypes["default"].oneOf([null])]),
  checked: _propTypes["default"].oneOfType([_propTypes["default"].bool, _propTypes["default"].oneOf([null])]),
  onChange: _propTypes["default"].func,
  children: _propTypes["default"].node,
  id: _propTypes["default"].string.isRequired,
  'aria-label': _propTypes["default"].string.isRequired
});

_defineProperty(DropdownToggleCheckbox, "defaultProps", {
  className: '',
  isValid: true,
  isDisabled: false,
  onChange: function onChange() {
    return undefined;
  }
});
//# sourceMappingURL=DropdownToggleCheckbox.js.map