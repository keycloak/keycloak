"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Progress = exports.ProgressSize = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _progress = _interopRequireDefault(require("@patternfly/react-styles/css/components/Progress/progress"));

var _reactStyles = require("@patternfly/react-styles");

var _ProgressContainer = require("./ProgressContainer");

var _util = require("../../helpers/util");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

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

var ProgressSize;
exports.ProgressSize = ProgressSize;

(function (ProgressSize) {
  ProgressSize["sm"] = "sm";
  ProgressSize["md"] = "md";
  ProgressSize["lg"] = "lg";
})(ProgressSize || (exports.ProgressSize = ProgressSize = {}));

var Progress =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Progress, _React$Component);

  function Progress() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, Progress);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(Progress)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "id", _this.props.id || (0, _util.getUniqueId)());

    return _this;
  }

  _createClass(Progress, [{
    key: "render",
    value: function render() {
      var _this$props = this.props,
          id = _this$props.id,
          className = _this$props.className,
          size = _this$props.size,
          value = _this$props.value,
          title = _this$props.title,
          label = _this$props.label,
          variant = _this$props.variant,
          measureLocation = _this$props.measureLocation,
          min = _this$props.min,
          max = _this$props.max,
          valueText = _this$props.valueText,
          props = _objectWithoutProperties(_this$props, ["id", "className", "size", "value", "title", "label", "variant", "measureLocation", "min", "max", "valueText"]);

      var additionalProps = _objectSpread({}, props, {}, valueText ? {
        'aria-valuetext': valueText
      } : {
        'aria-describedby': "".concat(this.id, "-description")
      });

      var ariaProps = {
        'aria-describedby': "".concat(this.id, "-description"),
        'aria-valuemin': min,
        'aria-valuenow': value,
        'aria-valuemax': max
      };

      if (valueText) {
        ariaProps['aria-valuetext'] = valueText;
      }

      var scaledValue = Math.min(100, Math.max(0, Math.floor((value - min) / (max - min) * 100)));
      return React.createElement("div", _extends({}, additionalProps, {
        className: (0, _reactStyles.css)(_progress["default"].progress, (0, _reactStyles.getModifier)(_progress["default"], variant, ''), (0, _reactStyles.getModifier)(_progress["default"], measureLocation, ''), (0, _reactStyles.getModifier)(_progress["default"], measureLocation === _ProgressContainer.ProgressMeasureLocation.inside ? ProgressSize.lg : size, ''), !title && (0, _reactStyles.getModifier)(_progress["default"], 'singleline', ''), className),
        id: this.id,
        role: "progressbar"
      }), React.createElement(_ProgressContainer.ProgressContainer, {
        parentId: this.id,
        value: scaledValue,
        title: title,
        label: label,
        variant: variant,
        measureLocation: measureLocation,
        ariaProps: ariaProps
      }));
    }
  }]);

  return Progress;
}(React.Component);

exports.Progress = Progress;

_defineProperty(Progress, "propTypes", {
  className: _propTypes["default"].string,
  size: _propTypes["default"].oneOf(['sm', 'md', 'lg']),
  measureLocation: _propTypes["default"].oneOf(['outside', 'inside', 'top', 'none']),
  variant: _propTypes["default"].oneOf(['danger', 'success', 'info']),
  title: _propTypes["default"].string,
  label: _propTypes["default"].node,
  value: _propTypes["default"].number,
  id: _propTypes["default"].string,
  min: _propTypes["default"].number,
  max: _propTypes["default"].number,
  valueText: _propTypes["default"].string
});

_defineProperty(Progress, "defaultProps", {
  className: '',
  measureLocation: _ProgressContainer.ProgressMeasureLocation.top,
  variant: _ProgressContainer.ProgressVariant.info,
  id: '',
  title: '',
  min: 0,
  max: 100,
  size: null,
  label: null,
  value: 0,
  valueText: null
});
//# sourceMappingURL=Progress.js.map