"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.TextArea = exports.TextAreResizeOrientation = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _formControl = _interopRequireDefault(require("@patternfly/react-styles/css/components/FormControl/form-control"));

var _reactStyles = require("@patternfly/react-styles");

var _constants = require("../../helpers/constants");

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

var TextAreResizeOrientation;
exports.TextAreResizeOrientation = TextAreResizeOrientation;

(function (TextAreResizeOrientation) {
  TextAreResizeOrientation["horizontal"] = "horizontal";
  TextAreResizeOrientation["vertical"] = "vertical";
  TextAreResizeOrientation["both"] = "both";
})(TextAreResizeOrientation || (exports.TextAreResizeOrientation = TextAreResizeOrientation = {}));

var TextArea =
/*#__PURE__*/
function (_React$Component) {
  _inherits(TextArea, _React$Component);

  function TextArea(props) {
    var _this;

    _classCallCheck(this, TextArea);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(TextArea).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "handleChange", function (event) {
      if (_this.props.onChange) {
        _this.props.onChange(event.currentTarget.value, event);
      }
    });

    if (!props.id && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('TextArea: TextArea requires either an id or aria-label to be specified');
    }

    return _this;
  }

  _createClass(TextArea, [{
    key: "render",
    value: function render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      var _this$props = this.props,
          className = _this$props.className,
          value = _this$props.value,
          onChange = _this$props.onChange,
          isValid = _this$props.isValid,
          validated = _this$props.validated,
          isRequired = _this$props.isRequired,
          resizeOrientation = _this$props.resizeOrientation,
          props = _objectWithoutProperties(_this$props, ["className", "value", "onChange", "isValid", "validated", "isRequired", "resizeOrientation"]);

      var orientation = 'resize' + resizeOrientation.charAt(0).toUpperCase() + resizeOrientation.slice(1);
      return React.createElement("textarea", _extends({
        className: (0, _reactStyles.css)(_formControl["default"].formControl, className, resizeOrientation !== TextAreResizeOrientation.both && (0, _reactStyles.getModifier)(_formControl["default"], orientation), validated === _constants.ValidatedOptions.success && _formControl["default"].modifiers.success),
        onChange: this.handleChange
      }, typeof this.props.defaultValue !== 'string' && {
        value: value
      }, {
        "aria-invalid": !isValid || validated === _constants.ValidatedOptions.error,
        required: isRequired
      }, props));
    }
  }]);

  return TextArea;
}(React.Component);

exports.TextArea = TextArea;

_defineProperty(TextArea, "propTypes", {
  className: _propTypes["default"].string,
  isRequired: _propTypes["default"].bool,
  isValid: _propTypes["default"].bool,
  validated: _propTypes["default"].oneOf(['success', 'error', 'default']),
  value: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].number]),
  onChange: _propTypes["default"].func,
  resizeOrientation: _propTypes["default"].oneOf(['horizontal', 'vertical', 'both']),
  'aria-label': _propTypes["default"].string
});

_defineProperty(TextArea, "defaultProps", {
  className: '',
  isRequired: false,
  isValid: true,
  validated: 'default',
  resizeOrientation: 'both',
  'aria-label': null
});
//# sourceMappingURL=TextArea.js.map