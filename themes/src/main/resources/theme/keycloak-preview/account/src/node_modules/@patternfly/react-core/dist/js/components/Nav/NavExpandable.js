"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.NavExpandable = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _nav = _interopRequireDefault(require("@patternfly/react-styles/css/components/Nav/nav"));

var _accessibility = _interopRequireDefault(require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"));

var _reactStyles = require("@patternfly/react-styles");

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

var NavExpandable =
/*#__PURE__*/
function (_React$Component) {
  _inherits(NavExpandable, _React$Component);

  function NavExpandable() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, NavExpandable);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(NavExpandable)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "id", _this.props.id || (0, _util.getUniqueId)());

    _defineProperty(_assertThisInitialized(_this), "state", {
      expandedState: _this.props.isExpanded
    });

    _defineProperty(_assertThisInitialized(_this), "onExpand", function (e, val) {
      if (_this.props.onExpand) {
        _this.props.onExpand(e, val);
      } else {
        _this.setState({
          expandedState: val
        });
      }
    });

    _defineProperty(_assertThisInitialized(_this), "handleToggle", function (e, onToggle) {
      // Item events can bubble up, ignore those
      if (e.target.getAttribute('data-component') !== 'pf-nav-expandable') {
        return;
      }

      var groupId = _this.props.groupId;
      var expandedState = _this.state.expandedState;
      onToggle(e, groupId, !expandedState);

      _this.onExpand(e, !expandedState);
    });

    return _this;
  }

  _createClass(NavExpandable, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      this.setState({
        expandedState: this.props.isExpanded
      });
    }
  }, {
    key: "componentDidUpdate",
    value: function componentDidUpdate(prevProps) {
      if (this.props.isExpanded !== prevProps.isExpanded) {
        this.setState({
          expandedState: this.props.isExpanded
        });
      }
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      var _this$props = this.props,
          id = _this$props.id,
          title = _this$props.title,
          srText = _this$props.srText,
          children = _this$props.children,
          className = _this$props.className,
          isActive = _this$props.isActive,
          groupId = _this$props.groupId,
          isExpanded = _this$props.isExpanded,
          onExpand = _this$props.onExpand,
          props = _objectWithoutProperties(_this$props, ["id", "title", "srText", "children", "className", "isActive", "groupId", "isExpanded", "onExpand"]);

      var expandedState = this.state.expandedState;
      return React.createElement(_Nav.NavContext.Consumer, null, function (context) {
        return React.createElement("li", _extends({
          className: (0, _reactStyles.css)(_nav["default"].navItem, expandedState && _nav["default"].modifiers.expanded, isActive && _nav["default"].modifiers.current, className),
          onClick: function onClick(e) {
            return _this2.handleToggle(e, context.onToggle);
          }
        }, props), React.createElement("a", {
          "data-component": "pf-nav-expandable",
          className: (0, _reactStyles.css)(_nav["default"].navLink),
          id: srText ? null : _this2.id,
          href: "#",
          onClick: function onClick(e) {
            return e.preventDefault();
          },
          onMouseDown: function onMouseDown(e) {
            return e.preventDefault();
          },
          "aria-expanded": expandedState
        }, title, React.createElement("span", {
          className: (0, _reactStyles.css)(_nav["default"].navToggle)
        }, React.createElement(_angleRightIcon["default"], {
          "aria-hidden": "true"
        }))), React.createElement("section", {
          className: (0, _reactStyles.css)(_nav["default"].navSubnav),
          "aria-labelledby": _this2.id,
          hidden: expandedState ? null : true
        }, srText && React.createElement("h2", {
          className: (0, _reactStyles.css)(_accessibility["default"].screenReader),
          id: _this2.id
        }, srText), React.createElement("ul", {
          className: (0, _reactStyles.css)(_nav["default"].navSimpleList)
        }, children)));
      });
    }
  }]);

  return NavExpandable;
}(React.Component);

exports.NavExpandable = NavExpandable;

_defineProperty(NavExpandable, "propTypes", {
  title: _propTypes["default"].string.isRequired,
  srText: _propTypes["default"].string,
  isExpanded: _propTypes["default"].bool,
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  groupId: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].number]),
  isActive: _propTypes["default"].bool,
  id: _propTypes["default"].string,
  onExpand: _propTypes["default"].func
});

_defineProperty(NavExpandable, "defaultProps", {
  srText: '',
  isExpanded: false,
  children: '',
  className: '',
  groupId: null,
  isActive: false,
  id: ''
});
//# sourceMappingURL=NavExpandable.js.map