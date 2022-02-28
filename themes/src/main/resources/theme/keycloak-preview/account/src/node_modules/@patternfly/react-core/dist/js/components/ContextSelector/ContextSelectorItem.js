"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ContextSelectorItem = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _contextSelector = _interopRequireDefault(require("@patternfly/react-styles/css/components/ContextSelector/context-selector"));

var _reactStyles = require("@patternfly/react-styles");

var _contextSelectorConstants = require("./contextSelectorConstants");

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

var ContextSelectorItem =
/*#__PURE__*/
function (_React$Component) {
  _inherits(ContextSelectorItem, _React$Component);

  function ContextSelectorItem() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, ContextSelectorItem);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(ContextSelectorItem)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "ref", React.createRef());

    return _this;
  }

  _createClass(ContextSelectorItem, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      /* eslint-disable-next-line */
      this.props.sendRef(this.props.index, this.ref.current);
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      var _this$props = this.props,
          className = _this$props.className,
          children = _this$props.children,
          isHovered = _this$props.isHovered,
          _onClick = _this$props.onClick,
          isDisabled = _this$props.isDisabled,
          index = _this$props.index,
          sendRef = _this$props.sendRef,
          props = _objectWithoutProperties(_this$props, ["className", "children", "isHovered", "onClick", "isDisabled", "index", "sendRef"]);

      return React.createElement(_contextSelectorConstants.ContextSelectorContext.Consumer, null, function (_ref) {
        var onSelect = _ref.onSelect;
        return React.createElement("li", {
          role: "none"
        }, React.createElement("button", _extends({
          className: (0, _reactStyles.css)(_contextSelector["default"].contextSelectorMenuListItem, isDisabled && _contextSelector["default"].modifiers.disabled, isHovered && _contextSelector["default"].modifiers.hover, className),
          ref: _this2.ref,
          onClick: function onClick(event) {
            if (!isDisabled) {
              _onClick(event);

              onSelect(event, children);
            }
          }
        }, props), children));
      });
    }
  }]);

  return ContextSelectorItem;
}(React.Component);

exports.ContextSelectorItem = ContextSelectorItem;

_defineProperty(ContextSelectorItem, "propTypes", {
  children: _propTypes["default"].node,
  className: _propTypes["default"].string,
  isDisabled: _propTypes["default"].bool,
  isHovered: _propTypes["default"].bool,
  onClick: _propTypes["default"].func,
  index: _propTypes["default"].number,
  sendRef: _propTypes["default"].func
});

_defineProperty(ContextSelectorItem, "defaultProps", {
  children: null,
  className: '',
  isHovered: false,
  isDisabled: false,
  onClick: function onClick() {
    return undefined;
  },
  index: undefined,
  sendRef: function sendRef() {}
});
//# sourceMappingURL=ContextSelectorItem.js.map