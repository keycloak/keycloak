"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Nav = exports.NavContext = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _nav = _interopRequireDefault(require("@patternfly/react-styles/css/components/Nav/nav"));

var _reactStyles = require("@patternfly/react-styles");

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

var NavContext = React.createContext({});
exports.NavContext = NavContext;

var Nav =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Nav, _React$Component);

  function Nav() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, Nav);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(Nav)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "state", {
      showLeftScrollButton: false,
      showRightScrollButton: false
    });

    _defineProperty(_assertThisInitialized(_this), "updateScrollButtonState", function (state) {
      var showLeftScrollButton = state.showLeftScrollButton,
          showRightScrollButton = state.showRightScrollButton;

      _this.setState({
        showLeftScrollButton: showLeftScrollButton,
        showRightScrollButton: showRightScrollButton
      });
    });

    return _this;
  }

  _createClass(Nav, [{
    key: "onSelect",
    // Callback from NavItem
    value: function onSelect(event, groupId, itemId, to, preventDefault, onClick) {
      if (preventDefault) {
        event.preventDefault();
      }

      this.props.onSelect({
        groupId: groupId,
        itemId: itemId,
        event: event,
        to: to
      });

      if (onClick) {
        onClick(event, itemId, groupId, to);
      }
    } // Callback from NavExpandable

  }, {
    key: "onToggle",
    value: function onToggle(event, groupId, toggleValue) {
      this.props.onToggle({
        event: event,
        groupId: groupId,
        isExpanded: toggleValue
      });
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      var _this$props = this.props,
          ariaLabel = _this$props['aria-label'],
          children = _this$props.children,
          className = _this$props.className,
          onSelect = _this$props.onSelect,
          onToggle = _this$props.onToggle,
          theme = _this$props.theme,
          ouiaContext = _this$props.ouiaContext,
          ouiaId = _this$props.ouiaId,
          props = _objectWithoutProperties(_this$props, ["aria-label", "children", "className", "onSelect", "onToggle", "theme", "ouiaContext", "ouiaId"]);

      var _this$state = this.state,
          showLeftScrollButton = _this$state.showLeftScrollButton,
          showRightScrollButton = _this$state.showRightScrollButton;
      var childrenProps = children.props;
      return React.createElement(NavContext.Provider, {
        value: {
          onSelect: function onSelect(event, groupId, itemId, to, preventDefault, onClick) {
            return _this2.onSelect(event, groupId, itemId, to, preventDefault, onClick);
          },
          onToggle: function onToggle(event, groupId, expanded) {
            return _this2.onToggle(event, groupId, expanded);
          },
          updateScrollButtonState: this.updateScrollButtonState
        }
      }, React.createElement("nav", _extends({
        className: (0, _reactStyles.css)(_nav["default"].nav, theme === 'dark' && _nav["default"].modifiers.dark, showLeftScrollButton && _nav["default"].modifiers.start, showRightScrollButton && _nav["default"].modifiers.end, className),
        "aria-label": ariaLabel === '' ? typeof childrenProps !== 'undefined' && childrenProps.variant === 'tertiary' ? 'Local' : 'Global' : ariaLabel
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Nav',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }, props), children));
    }
  }]);

  return Nav;
}(React.Component);

_defineProperty(Nav, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  onSelect: _propTypes["default"].func,
  onToggle: _propTypes["default"].func,
  'aria-label': _propTypes["default"].string,
  theme: _propTypes["default"].oneOf(['dark', 'light'])
});

_defineProperty(Nav, "defaultProps", {
  'aria-label': '',
  children: null,
  className: '',
  onSelect: function onSelect() {
    return undefined;
  },
  onToggle: function onToggle() {
    return undefined;
  },
  theme: 'light'
});

var NavWithOuiaContext = (0, _withOuia.withOuiaContext)(Nav);
exports.Nav = NavWithOuiaContext;
//# sourceMappingURL=Nav.js.map