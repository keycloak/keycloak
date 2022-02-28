"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Switch = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _switch = _interopRequireDefault(require("@patternfly/react-styles/css/components/Switch/switch"));

var _reactStyles = require("@patternfly/react-styles");

var _checkIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/check-icon"));

var _util = require("../../helpers/util");

var _withOuia = require("../withOuia");

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

var Switch =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Switch, _React$Component);

  function Switch(props) {
    var _this;

    _classCallCheck(this, Switch);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Switch).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "id", '');

    if (!props.id && !props['aria-label']) {
      // eslint-disable-next-line no-console
      console.error('Switch: Switch requires either an id or aria-label to be specified');
    }

    _this.id = props.id || (0, _util.getUniqueId)();
    return _this;
  }

  _createClass(Switch, [{
    key: "render",
    value: function render() {
      var _this$props = this.props,
          id = _this$props.id,
          className = _this$props.className,
          label = _this$props.label,
          labelOff = _this$props.labelOff,
          isChecked = _this$props.isChecked,
          isDisabled = _this$props.isDisabled,
          _onChange = _this$props.onChange,
          ouiaContext = _this$props.ouiaContext,
          ouiaId = _this$props.ouiaId,
          props = _objectWithoutProperties(_this$props, ["id", "className", "label", "labelOff", "isChecked", "isDisabled", "onChange", "ouiaContext", "ouiaId"]);

      var isAriaLabelledBy = props['aria-label'] === '';
      return React.createElement("label", _extends({
        className: (0, _reactStyles.css)(_switch["default"]["switch"], className),
        htmlFor: this.id
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Switch',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }), React.createElement("input", _extends({
        id: this.id,
        className: (0, _reactStyles.css)(_switch["default"].switchInput),
        type: "checkbox",
        onChange: function onChange(event) {
          return _onChange(event.target.checked, event);
        },
        checked: isChecked,
        disabled: isDisabled,
        "aria-labelledby": isAriaLabelledBy ? "".concat(this.id, "-on") : null
      }, props)), label !== '' ? React.createElement(React.Fragment, null, React.createElement("span", {
        className: (0, _reactStyles.css)(_switch["default"].switchToggle)
      }), React.createElement("span", {
        className: (0, _reactStyles.css)(_switch["default"].switchLabel, _switch["default"].modifiers.on),
        id: isAriaLabelledBy ? "".concat(this.id, "-on") : null,
        "aria-hidden": "true"
      }, label), React.createElement("span", {
        className: (0, _reactStyles.css)(_switch["default"].switchLabel, _switch["default"].modifiers.off),
        id: isAriaLabelledBy ? "".concat(this.id, "-off") : null,
        "aria-hidden": "true"
      }, labelOff || label)) : label !== '' && labelOff !== '' ? React.createElement(React.Fragment, null, React.createElement("span", {
        className: (0, _reactStyles.css)(_switch["default"].switchToggle)
      }), React.createElement("span", {
        className: (0, _reactStyles.css)(_switch["default"].switchLabel, _switch["default"].modifiers.on),
        id: isAriaLabelledBy ? "".concat(this.id, "-on") : null,
        "aria-hidden": "true"
      }, label), React.createElement("span", {
        className: (0, _reactStyles.css)(_switch["default"].switchLabel, _switch["default"].modifiers.off),
        id: isAriaLabelledBy ? "".concat(this.id, "-off") : null,
        "aria-hidden": "true"
      }, labelOff)) : React.createElement("span", {
        className: (0, _reactStyles.css)(_switch["default"].switchToggle)
      }, React.createElement("div", {
        className: (0, _reactStyles.css)(_switch["default"].switchToggleIcon),
        "aria-hidden": "true"
      }, React.createElement(_checkIcon["default"], {
        noVerticalAlign: true
      }))));
    }
  }]);

  return Switch;
}(React.Component);

_defineProperty(Switch, "propTypes", {
  id: _propTypes["default"].string,
  className: _propTypes["default"].string,
  label: _propTypes["default"].string,
  labelOff: _propTypes["default"].string,
  isChecked: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  onChange: _propTypes["default"].func,
  'aria-label': _propTypes["default"].string
});

_defineProperty(Switch, "defaultProps", {
  id: '',
  className: '',
  label: '',
  labelOff: '',
  isChecked: true,
  isDisabled: false,
  'aria-label': '',
  onChange: function onChange() {
    return undefined;
  }
});

var SwitchWithOuiaContext = (0, _withOuia.withOuiaContext)(Switch);
exports.Switch = SwitchWithOuiaContext;
//# sourceMappingURL=Switch.js.map