"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.FocusTrap = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _focusTrap = _interopRequireDefault(require("focus-trap"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var FocusTrap =
/*#__PURE__*/
function (_React$Component) {
  _inherits(FocusTrap, _React$Component);

  function FocusTrap(props) {
    var _this;

    _classCallCheck(this, FocusTrap);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(FocusTrap).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "divRef", React.createRef());

    if (typeof document !== 'undefined') {
      _this.previouslyFocusedElement = document.activeElement;
    }

    return _this;
  }

  _createClass(FocusTrap, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      // We need to hijack the returnFocusOnDeactivate option,
      // because React can move focus into the element before we arrived at
      // this lifecycle hook (e.g. with autoFocus inputs). So the component
      // captures the previouslyFocusedElement in componentWillMount,
      // then (optionally) returns focus to it in componentWillUnmount.
      this.focusTrap = (0, _focusTrap["default"])(this.divRef.current, _objectSpread({}, this.props.focusTrapOptions, {
        returnFocusOnDeactivate: false
      }));

      if (this.props.active) {
        this.focusTrap.activate();
      }

      if (this.props.paused) {
        this.focusTrap.pause();
      }
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate(prevProps) {
      if (prevProps.active && !this.props.active) {
        var returnFocusOnDeactivate = this.props.focusTrapOptions.returnFocusOnDeactivate;
        var returnFocus = returnFocusOnDeactivate || false;
        var config = {
          returnFocus: returnFocus
        };
        this.focusTrap.deactivate(config);
      } else if (!prevProps.active && this.props.active) {
        this.focusTrap.activate();
      }

      if (prevProps.paused && !this.props.paused) {
        this.focusTrap.unpause();
      } else if (!prevProps.paused && this.props.paused) {
        this.focusTrap.pause();
      }
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      this.focusTrap.deactivate();

      if (this.props.focusTrapOptions.returnFocusOnDeactivate !== false && this.previouslyFocusedElement && this.previouslyFocusedElement.focus) {
        this.previouslyFocusedElement.focus();
      }
    }
  }, {
    key: "render",
    value: function render() {
      return React.createElement("div", {
        ref: this.divRef,
        className: this.props.className
      }, this.props.children);
    }
  }]);

  return FocusTrap;
}(React.Component);

exports.FocusTrap = FocusTrap;

_defineProperty(FocusTrap, "propTypes", {
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  active: _propTypes["default"].bool,
  paused: _propTypes["default"].bool,
  focusTrapOptions: _propTypes["default"].any
});

_defineProperty(FocusTrap, "defaultProps", {
  active: true,
  paused: false,
  focusTrapOptions: {}
});
//# sourceMappingURL=FocusTrap.js.map