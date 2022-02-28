"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Toggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _dropdown = _interopRequireDefault(require("@patternfly/react-styles/css/components/Dropdown/dropdown"));

var _dropdownConstants = require("./dropdownConstants");

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

var Toggle =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Toggle, _React$Component);

  function Toggle() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, Toggle);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(Toggle)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "buttonRef", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "componentDidMount", function () {
      document.addEventListener('mousedown', function (event) {
        return _this.onDocClick(event);
      });
      document.addEventListener('touchstart', function (event) {
        return _this.onDocClick(event);
      });
      document.addEventListener('keydown', function (event) {
        return _this.onEscPress(event);
      });
    });

    _defineProperty(_assertThisInitialized(_this), "componentWillUnmount", function () {
      document.removeEventListener('mousedown', function (event) {
        return _this.onDocClick(event);
      });
      document.removeEventListener('touchstart', function (event) {
        return _this.onDocClick(event);
      });
      document.removeEventListener('keydown', function (event) {
        return _this.onEscPress(event);
      });
    });

    _defineProperty(_assertThisInitialized(_this), "onDocClick", function (event) {
      if (_this.props.isOpen && _this.props.parentRef && _this.props.parentRef.current && !_this.props.parentRef.current.contains(event.target)) {
        _this.props.onToggle(false, event);

        _this.buttonRef.current.focus();
      }
    });

    _defineProperty(_assertThisInitialized(_this), "onEscPress", function (event) {
      var parentRef = _this.props.parentRef;
      var keyCode = event.keyCode || event.which;

      if (_this.props.isOpen && (keyCode === _constants.KEY_CODES.ESCAPE_KEY || event.key === 'Tab') && parentRef && parentRef.current && parentRef.current.contains(event.target)) {
        _this.props.onToggle(false, event);

        _this.buttonRef.current.focus();
      }
    });

    _defineProperty(_assertThisInitialized(_this), "onKeyDown", function (event) {
      if (event.key === 'Tab' && !_this.props.isOpen) {
        return;
      }

      if (!_this.props.bubbleEvent) {
        event.stopPropagation();
      }

      event.preventDefault();

      if ((event.key === 'Tab' || event.key === 'Enter' || event.key === ' ') && _this.props.isOpen) {
        _this.props.onToggle(!_this.props.isOpen, event);
      } else if ((event.key === 'Enter' || event.key === ' ') && !_this.props.isOpen) {
        _this.props.onToggle(!_this.props.isOpen, event);

        _this.props.onEnter();
      }
    });

    return _this;
  }

  _createClass(Toggle, [{
    key: "render",
    value: function render() {
      var _this2 = this;

      var _this$props = this.props,
          className = _this$props.className,
          children = _this$props.children,
          isOpen = _this$props.isOpen,
          isFocused = _this$props.isFocused,
          isActive = _this$props.isActive,
          isHovered = _this$props.isHovered,
          isDisabled = _this$props.isDisabled,
          isPlain = _this$props.isPlain,
          isPrimary = _this$props.isPrimary,
          isSplitButton = _this$props.isSplitButton,
          ariaHasPopup = _this$props.ariaHasPopup,
          bubbleEvent = _this$props.bubbleEvent,
          onToggle = _this$props.onToggle,
          onEnter = _this$props.onEnter,
          parentRef = _this$props.parentRef,
          id = _this$props.id,
          type = _this$props.type,
          props = _objectWithoutProperties(_this$props, ["className", "children", "isOpen", "isFocused", "isActive", "isHovered", "isDisabled", "isPlain", "isPrimary", "isSplitButton", "ariaHasPopup", "bubbleEvent", "onToggle", "onEnter", "parentRef", "id", "type"]);

      return React.createElement(_dropdownConstants.DropdownContext.Consumer, null, function (_ref) {
        var toggleClass = _ref.toggleClass;
        return React.createElement("button", _extends({}, props, {
          id: id,
          ref: _this2.buttonRef,
          className: (0, _reactStyles.css)(isSplitButton ? _dropdown["default"].dropdownToggleButton : toggleClass || _dropdown["default"].dropdownToggle, isFocused && _dropdown["default"].modifiers.focus, isHovered && _dropdown["default"].modifiers.hover, isActive && _dropdown["default"].modifiers.active, isPlain && _dropdown["default"].modifiers.plain, isPrimary && _dropdown["default"].modifiers.primary, className),
          type: type || 'button',
          onClick: function onClick(event) {
            return onToggle(!isOpen, event);
          },
          "aria-expanded": isOpen,
          "aria-haspopup": ariaHasPopup,
          onKeyDown: function onKeyDown(event) {
            return _this2.onKeyDown(event);
          },
          disabled: isDisabled
        }), children);
      });
    }
  }]);

  return Toggle;
}(React.Component);

exports.Toggle = Toggle;

_defineProperty(Toggle, "propTypes", {
  id: _propTypes["default"].string.isRequired,
  type: _propTypes["default"].oneOf(['button', 'submit', 'reset']),
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  isOpen: _propTypes["default"].bool,
  onToggle: _propTypes["default"].func,
  onEnter: _propTypes["default"].func,
  parentRef: _propTypes["default"].any,
  isFocused: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  isActive: _propTypes["default"].bool,
  isDisabled: _propTypes["default"].bool,
  isPlain: _propTypes["default"].bool,
  isPrimary: _propTypes["default"].bool,
  isSplitButton: _propTypes["default"].bool,
  ariaHasPopup: _propTypes["default"].oneOfType([_propTypes["default"].bool, _propTypes["default"].oneOf(['listbox']), _propTypes["default"].oneOf(['menu']), _propTypes["default"].oneOf(['dialog']), _propTypes["default"].oneOf(['grid']), _propTypes["default"].oneOf(['listbox']), _propTypes["default"].oneOf(['tree'])]),
  bubbleEvent: _propTypes["default"].bool
});

_defineProperty(Toggle, "defaultProps", {
  className: '',
  isOpen: false,
  isFocused: false,
  isHovered: false,
  isActive: false,
  isDisabled: false,
  isPlain: false,
  isPrimary: false,
  isSplitButton: false,
  onToggle: function onToggle() {},
  onEnter: function onEnter() {},
  bubbleEvent: false
});
//# sourceMappingURL=Toggle.js.map