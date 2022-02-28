"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ContextSelectorToggle = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _caretDownIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/caret-down-icon"));

var _contextSelector = _interopRequireDefault(require("@patternfly/react-styles/css/components/ContextSelector/context-selector"));

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

var ContextSelectorToggle =
/*#__PURE__*/
function (_React$Component) {
  _inherits(ContextSelectorToggle, _React$Component);

  function ContextSelectorToggle() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, ContextSelectorToggle);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(ContextSelectorToggle)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "toggle", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "componentDidMount", function () {
      document.addEventListener('mousedown', _this.onDocClick);
      document.addEventListener('touchstart', _this.onDocClick);
      document.addEventListener('keydown', _this.onEscPress);
    });

    _defineProperty(_assertThisInitialized(_this), "componentWillUnmount", function () {
      document.removeEventListener('mousedown', _this.onDocClick);
      document.removeEventListener('touchstart', _this.onDocClick);
      document.removeEventListener('keydown', _this.onEscPress);
    });

    _defineProperty(_assertThisInitialized(_this), "onDocClick", function (event) {
      var _this$props = _this.props,
          isOpen = _this$props.isOpen,
          parentRef = _this$props.parentRef,
          onToggle = _this$props.onToggle;

      if (isOpen && parentRef && !parentRef.contains(event.target)) {
        onToggle(null, false);

        _this.toggle.current.focus();
      }
    });

    _defineProperty(_assertThisInitialized(_this), "onEscPress", function (event) {
      var _this$props2 = _this.props,
          isOpen = _this$props2.isOpen,
          parentRef = _this$props2.parentRef,
          onToggle = _this$props2.onToggle;
      var keyCode = event.keyCode || event.which;

      if (isOpen && keyCode === _constants.KEY_CODES.ESCAPE_KEY && parentRef && parentRef.contains(event.target)) {
        onToggle(null, false);

        _this.toggle.current.focus();
      }
    });

    _defineProperty(_assertThisInitialized(_this), "onKeyDown", function (event) {
      var _this$props3 = _this.props,
          isOpen = _this$props3.isOpen,
          onToggle = _this$props3.onToggle,
          onEnter = _this$props3.onEnter;

      if (event.keyCode === _constants.KEY_CODES.TAB && !isOpen || event.key !== _constants.KEY_CODES.ENTER) {
        return;
      }

      event.preventDefault();

      if ((event.keyCode === _constants.KEY_CODES.TAB || event.keyCode === _constants.KEY_CODES.ENTER || event.key !== _constants.KEY_CODES.SPACE) && isOpen) {
        onToggle(null, !isOpen);
      } else if ((event.keyCode === _constants.KEY_CODES.ENTER || event.key === ' ') && !isOpen) {
        onToggle(null, !isOpen);
        onEnter();
      }
    });

    return _this;
  }

  _createClass(ContextSelectorToggle, [{
    key: "render",
    value: function render() {
      var _this$props4 = this.props,
          className = _this$props4.className,
          toggleText = _this$props4.toggleText,
          isOpen = _this$props4.isOpen,
          isFocused = _this$props4.isFocused,
          isActive = _this$props4.isActive,
          isHovered = _this$props4.isHovered,
          onToggle = _this$props4.onToggle,
          id = _this$props4.id,
          onEnter = _this$props4.onEnter,
          parentRef = _this$props4.parentRef,
          props = _objectWithoutProperties(_this$props4, ["className", "toggleText", "isOpen", "isFocused", "isActive", "isHovered", "onToggle", "id", "onEnter", "parentRef"]);

      return React.createElement("button", _extends({}, props, {
        id: id,
        ref: this.toggle,
        className: (0, _reactStyles.css)(_contextSelector["default"].contextSelectorToggle, isFocused && _contextSelector["default"].modifiers.focus, isHovered && _contextSelector["default"].modifiers.hover, isActive && _contextSelector["default"].modifiers.active, className),
        type: "button",
        onClick: function onClick(event) {
          return onToggle(event, !isOpen);
        },
        "aria-expanded": isOpen,
        onKeyDown: this.onKeyDown
      }), React.createElement("span", {
        className: (0, _reactStyles.css)(_contextSelector["default"].contextSelectorToggleText)
      }, toggleText), React.createElement(_caretDownIcon["default"], {
        className: (0, _reactStyles.css)(_contextSelector["default"].contextSelectorToggleIcon),
        "aria-hidden": true
      }));
    }
  }]);

  return ContextSelectorToggle;
}(React.Component);

exports.ContextSelectorToggle = ContextSelectorToggle;

_defineProperty(ContextSelectorToggle, "propTypes", {
  id: _propTypes["default"].string.isRequired,
  className: _propTypes["default"].string,
  toggleText: _propTypes["default"].string,
  isOpen: _propTypes["default"].bool,
  onToggle: _propTypes["default"].func,
  onEnter: _propTypes["default"].func,
  parentRef: _propTypes["default"].any,
  isFocused: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  isActive: _propTypes["default"].bool
});

_defineProperty(ContextSelectorToggle, "defaultProps", {
  className: '',
  toggleText: '',
  isOpen: false,
  onEnter: function onEnter() {
    return undefined;
  },
  parentRef: null,
  isFocused: false,
  isHovered: false,
  isActive: false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onToggle: function onToggle(event, value) {
    return undefined;
  }
});
//# sourceMappingURL=ContextSelectorToggle.js.map