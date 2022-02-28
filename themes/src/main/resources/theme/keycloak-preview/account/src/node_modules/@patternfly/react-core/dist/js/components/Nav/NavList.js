"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.NavList = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _NavVariants = require("./NavVariants");

var _nav = _interopRequireDefault(require("@patternfly/react-styles/css/components/Nav/nav"));

var _reactStyles = require("@patternfly/react-styles");

var _angleLeftIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-left-icon"));

var _angleRightIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/angle-right-icon"));

var _util = require("../../helpers/util");

var _Nav = require("./Nav");

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

var NavList =
/*#__PURE__*/
function (_React$Component) {
  _inherits(NavList, _React$Component);

  function NavList() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, NavList);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(NavList)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "navList", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "handleScrollButtons", function () {
      if (_this.navList.current) {
        var updateScrollButtonState = _this.context.updateScrollButtonState;
        var container = _this.navList.current; // get first element and check if it is in view

        var showLeftScrollButton = !(0, _util.isElementInView)(container, container.firstChild, false); // get last element and check if it is in view

        var showRightScrollButton = !(0, _util.isElementInView)(container, container.lastChild, false);
        updateScrollButtonState({
          showLeftScrollButton: showLeftScrollButton,
          showRightScrollButton: showRightScrollButton
        });
      }
    });

    _defineProperty(_assertThisInitialized(_this), "scrollLeft", function () {
      // find first Element that is fully in view on the left, then scroll to the element before it
      if (_this.navList.current) {
        var container = _this.navList.current;
        var childrenArr = Array.from(container.children);
        var firstElementInView;
        var lastElementOutOfView;

        for (var i = 0; i < childrenArr.length && !firstElementInView; i++) {
          if ((0, _util.isElementInView)(container, childrenArr[i], false)) {
            firstElementInView = childrenArr[i];
            lastElementOutOfView = childrenArr[i - 1];
          }
        }

        if (lastElementOutOfView) {
          container.scrollLeft -= lastElementOutOfView.scrollWidth;
        }

        _this.handleScrollButtons();
      }
    });

    _defineProperty(_assertThisInitialized(_this), "scrollRight", function () {
      // find last Element that is fully in view on the right, then scroll to the element after it
      if (_this.navList.current) {
        var container = _this.navList.current;
        var childrenArr = Array.from(container.children);
        var lastElementInView;
        var firstElementOutOfView;

        for (var i = childrenArr.length - 1; i >= 0 && !lastElementInView; i--) {
          if ((0, _util.isElementInView)(container, childrenArr[i], false)) {
            lastElementInView = childrenArr[i];
            firstElementOutOfView = childrenArr[i + 1];
          }
        }

        if (firstElementOutOfView) {
          container.scrollLeft += firstElementOutOfView.scrollWidth;
        }

        _this.handleScrollButtons();
      }
    });

    return _this;
  }

  _createClass(NavList, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      var variant = this.props.variant;
      var isHorizontal = variant === _NavVariants.NavVariants.horizontal || variant === _NavVariants.NavVariants.tertiary;

      if (isHorizontal) {
        window.addEventListener('resize', this.handleScrollButtons, false); // call the handle resize function to check if scroll buttons should be shown

        this.handleScrollButtons();
      }
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      var variant = this.props.variant;
      var isHorizontal = variant === _NavVariants.NavVariants.horizontal || variant === _NavVariants.NavVariants.tertiary;

      if (isHorizontal) {
        document.removeEventListener('resize', this.handleScrollButtons, false);
      }
    }
  }, {
    key: "render",
    value: function render() {
      var _variantStyle;

      var _this$props = this.props,
          variant = _this$props.variant,
          children = _this$props.children,
          className = _this$props.className,
          ariaLeftScroll = _this$props.ariaLeftScroll,
          ariaRightScroll = _this$props.ariaRightScroll,
          props = _objectWithoutProperties(_this$props, ["variant", "children", "className", "ariaLeftScroll", "ariaRightScroll"]);

      var variantStyle = (_variantStyle = {}, _defineProperty(_variantStyle, _NavVariants.NavVariants["default"], _nav["default"].navList), _defineProperty(_variantStyle, _NavVariants.NavVariants.simple, _nav["default"].navSimpleList), _defineProperty(_variantStyle, _NavVariants.NavVariants.horizontal, _nav["default"].navHorizontalList), _defineProperty(_variantStyle, _NavVariants.NavVariants.tertiary, _nav["default"].navTertiaryList), _variantStyle);
      var isHorizontal = variant === _NavVariants.NavVariants.horizontal || variant === _NavVariants.NavVariants.tertiary;
      return React.createElement(React.Fragment, null, isHorizontal && React.createElement("button", {
        className: (0, _reactStyles.css)(_nav["default"].navScrollButton),
        "aria-label": ariaLeftScroll,
        onClick: this.scrollLeft
      }, React.createElement(_angleLeftIcon["default"], null)), React.createElement("ul", _extends({
        ref: this.navList,
        className: (0, _reactStyles.css)(variantStyle[variant], className)
      }, props), children), isHorizontal && React.createElement("button", {
        className: (0, _reactStyles.css)(_nav["default"].navScrollButton),
        "aria-label": ariaRightScroll,
        onClick: this.scrollRight
      }, React.createElement(_angleRightIcon["default"], null)));
    }
  }]);

  return NavList;
}(React.Component);

exports.NavList = NavList;

_defineProperty(NavList, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  variant: _propTypes["default"].oneOf(['default', 'simple', 'horizontal', 'tertiary']),
  ariaLeftScroll: _propTypes["default"].string,
  ariaRightScroll: _propTypes["default"].string
});

_defineProperty(NavList, "contextType", _Nav.NavContext);

_defineProperty(NavList, "defaultProps", {
  variant: 'default',
  children: null,
  className: '',
  ariaLeftScroll: 'Scroll left',
  ariaRightScroll: 'Scroll right'
});
//# sourceMappingURL=NavList.js.map