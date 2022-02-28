"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.SelectOption = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _select = _interopRequireDefault(require("@patternfly/react-styles/css/components/Select/select"));

var _check = _interopRequireDefault(require("@patternfly/react-styles/css/components/Check/check"));

var _reactStyles = require("@patternfly/react-styles");

var _checkIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/check-icon"));

var _selectConstants = require("./selectConstants");

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

var SelectOption =
/*#__PURE__*/
function (_React$Component) {
  _inherits(SelectOption, _React$Component);

  function SelectOption() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, SelectOption);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(SelectOption)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "ref", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "onKeyDown", function (event) {
      if (event.key === _selectConstants.KeyTypes.Tab) {
        return;
      }

      event.preventDefault();

      if (event.key === _selectConstants.KeyTypes.ArrowUp) {
        _this.props.keyHandler(_this.props.index, 'up');
      } else if (event.key === _selectConstants.KeyTypes.ArrowDown) {
        _this.props.keyHandler(_this.props.index, 'down');
      } else if (event.key === _selectConstants.KeyTypes.Enter) {
        _this.ref.current.click();

        if (_this.context.variant === _selectConstants.SelectVariant.checkbox) {
          _this.ref.current.focus();
        }
      }
    });

    return _this;
  }

  _createClass(SelectOption, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      this.props.sendRef(this.props.isDisabled ? null : this.ref.current, this.props.index);
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate() {
      this.props.sendRef(this.props.isDisabled ? null : this.ref.current, this.props.index);
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      /* eslint-disable @typescript-eslint/no-unused-vars */
      var _this$props = this.props,
          children = _this$props.children,
          className = _this$props.className,
          value = _this$props.value,
          _onClick = _this$props.onClick,
          isDisabled = _this$props.isDisabled,
          isPlaceholder = _this$props.isPlaceholder,
          isNoResultsOption = _this$props.isNoResultsOption,
          isSelected = _this$props.isSelected,
          isChecked = _this$props.isChecked,
          isFocused = _this$props.isFocused,
          sendRef = _this$props.sendRef,
          keyHandler = _this$props.keyHandler,
          index = _this$props.index,
          component = _this$props.component,
          props = _objectWithoutProperties(_this$props, ["children", "className", "value", "onClick", "isDisabled", "isPlaceholder", "isNoResultsOption", "isSelected", "isChecked", "isFocused", "sendRef", "keyHandler", "index", "component"]);
      /* eslint-enable @typescript-eslint/no-unused-vars */


      var Component = component;
      return React.createElement(_selectConstants.SelectConsumer, null, function (_ref) {
        var onSelect = _ref.onSelect,
            onClose = _ref.onClose,
            variant = _ref.variant;
        return React.createElement(React.Fragment, null, variant !== _selectConstants.SelectVariant.checkbox && React.createElement("li", {
          role: "presentation"
        }, React.createElement(Component, _extends({}, props, {
          className: (0, _reactStyles.css)(_select["default"].selectMenuItem, isSelected && _select["default"].modifiers.selected, isDisabled && _select["default"].modifiers.disabled, isFocused && _select["default"].modifiers.focus, className),
          onClick: function onClick(event) {
            if (!isDisabled) {
              _onClick(event);

              onSelect(event, value, isPlaceholder);
              onClose();
            }
          },
          role: "option",
          "aria-selected": isSelected || null,
          ref: _this2.ref,
          onKeyDown: _this2.onKeyDown,
          type: "button"
        }), children || value.toString(), isSelected && React.createElement(_checkIcon["default"], {
          className: (0, _reactStyles.css)(_select["default"].selectMenuItemIcon),
          "aria-hidden": true
        }))), variant === _selectConstants.SelectVariant.checkbox && !isNoResultsOption && React.createElement("label", _extends({}, props, {
          className: (0, _reactStyles.css)(_check["default"].check, _select["default"].selectMenuItem, isDisabled && _select["default"].modifiers.disabled, className),
          onKeyDown: _this2.onKeyDown
        }), React.createElement("input", {
          id: value.toString(),
          className: (0, _reactStyles.css)(_check["default"].checkInput),
          type: "checkbox",
          onChange: function onChange(event) {
            if (!isDisabled) {
              _onClick(event);

              onSelect(event, value);
            }
          },
          ref: _this2.ref,
          checked: isChecked || false,
          disabled: isDisabled
        }), React.createElement("span", {
          className: (0, _reactStyles.css)(_check["default"].checkLabel, isDisabled && _select["default"].modifiers.disabled)
        }, children || value.toString())), variant === _selectConstants.SelectVariant.checkbox && isNoResultsOption && React.createElement("div", null, React.createElement(Component, _extends({}, props, {
          className: (0, _reactStyles.css)(_select["default"].selectMenuItem, isSelected && _select["default"].modifiers.selected, isDisabled && _select["default"].modifiers.disabled, isFocused && _select["default"].modifiers.focus, className),
          role: "option",
          "aria-selected": isSelected || null,
          ref: _this2.ref,
          onKeyDown: _this2.onKeyDown,
          type: "button"
        }), children || value.toString())));
      });
    }
  }]);

  return SelectOption;
}(React.Component);

exports.SelectOption = SelectOption;

_defineProperty(SelectOption, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  index: _propTypes["default"].number,
  component: _propTypes["default"].node,
  value: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].shape({})]),
  isDisabled: _propTypes["default"].bool,
  isPlaceholder: _propTypes["default"].bool,
  isNoResultsOption: _propTypes["default"].bool,
  isSelected: _propTypes["default"].bool,
  isChecked: _propTypes["default"].bool,
  isFocused: _propTypes["default"].bool,
  sendRef: _propTypes["default"].func,
  keyHandler: _propTypes["default"].func,
  onClick: _propTypes["default"].func
});

_defineProperty(SelectOption, "defaultProps", {
  className: '',
  value: '',
  index: 0,
  isDisabled: false,
  isPlaceholder: false,
  isSelected: false,
  isChecked: false,
  isFocused: false,
  isNoResultsOption: false,
  component: 'button',
  onClick: function onClick() {},
  sendRef: function sendRef() {},
  keyHandler: function keyHandler() {}
});
//# sourceMappingURL=SelectOption.js.map