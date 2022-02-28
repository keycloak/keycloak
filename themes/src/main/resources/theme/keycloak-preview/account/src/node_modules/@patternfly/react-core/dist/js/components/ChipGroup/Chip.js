"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Chip = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _ChipButton = require("./ChipButton");

var _Tooltip = require("../Tooltip");

var _timesCircleIcon = _interopRequireDefault(require("@patternfly/react-icons/dist/js/icons/times-circle-icon"));

var _chip = _interopRequireDefault(require("@patternfly/react-styles/css/components/Chip/chip"));

var _GenerateId = _interopRequireDefault(require("../../helpers/GenerateId/GenerateId"));

var _withOuia = require("../withOuia");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _typeof(obj) { if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

var Chip =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Chip, _React$Component);

  function Chip(props) {
    var _this;

    _classCallCheck(this, Chip);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Chip).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "span", React.createRef());

    _defineProperty(_assertThisInitialized(_this), "renderOverflowChip", function () {
      var _this$props = _this.props,
          children = _this$props.children,
          className = _this$props.className,
          onClick = _this$props.onClick,
          ouiaContext = _this$props.ouiaContext,
          ouiaId = _this$props.ouiaId;
      var Component = _this.props.component;
      return React.createElement(Component, _extends({
        className: (0, _reactStyles.css)(_chip["default"].chip, _chip["default"].modifiers.overflow, className)
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'OverflowChip',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }), React.createElement(_ChipButton.ChipButton, {
        onClick: onClick
      }, React.createElement("span", {
        className: (0, _reactStyles.css)(_chip["default"].chipText)
      }, children)));
    });

    _defineProperty(_assertThisInitialized(_this), "renderChip", function (randomId) {
      var _this$props2 = _this.props,
          children = _this$props2.children,
          closeBtnAriaLabel = _this$props2.closeBtnAriaLabel,
          tooltipPosition = _this$props2.tooltipPosition,
          className = _this$props2.className,
          onClick = _this$props2.onClick,
          isReadOnly = _this$props2.isReadOnly,
          ouiaContext = _this$props2.ouiaContext,
          ouiaId = _this$props2.ouiaId;
      var Component = _this.props.component;

      if (_this.state.isTooltipVisible) {
        return React.createElement(_Tooltip.Tooltip, {
          position: tooltipPosition,
          content: children
        }, React.createElement(Component, _extends({
          className: (0, _reactStyles.css)(_chip["default"].chip, isReadOnly && _chip["default"].modifiers.readOnly, className),
          tabIndex: "0"
        }, ouiaContext.isOuia && {
          'data-ouia-component-type': 'Chip',
          'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
        }), React.createElement("span", {
          ref: _this.span,
          className: (0, _reactStyles.css)(_chip["default"].chipText),
          id: randomId
        }, children), !isReadOnly && React.createElement(_ChipButton.ChipButton, {
          onClick: onClick,
          ariaLabel: closeBtnAriaLabel,
          id: "remove_".concat(randomId),
          "aria-labelledby": "remove_".concat(randomId, " ").concat(randomId)
        }, React.createElement(_timesCircleIcon["default"], {
          "aria-hidden": "true"
        }))));
      }

      return React.createElement(Component, _extends({
        className: (0, _reactStyles.css)(_chip["default"].chip, isReadOnly && _chip["default"].modifiers.readOnly, className)
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Chip',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }), React.createElement("span", {
        ref: _this.span,
        className: (0, _reactStyles.css)(_chip["default"].chipText),
        id: randomId
      }, children), !isReadOnly && React.createElement(_ChipButton.ChipButton, {
        onClick: onClick,
        ariaLabel: closeBtnAriaLabel,
        id: "remove_".concat(randomId),
        "aria-labelledby": "remove_".concat(randomId, " ").concat(randomId)
      }, React.createElement(_timesCircleIcon["default"], {
        "aria-hidden": "true"
      })));
    });

    _this.state = {
      isTooltipVisible: false
    };
    return _this;
  }

  _createClass(Chip, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      this.setState({
        isTooltipVisible: Boolean(this.span.current && this.span.current.offsetWidth < this.span.current.scrollWidth)
      });
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      var isOverflowChip = this.props.isOverflowChip;
      return React.createElement(_GenerateId["default"], null, function (randomId) {
        return isOverflowChip ? _this2.renderOverflowChip() : _this2.renderChip(randomId);
      });
    }
  }]);

  return Chip;
}(React.Component);

_defineProperty(Chip, "propTypes", {
  children: _propTypes["default"].node,
  closeBtnAriaLabel: _propTypes["default"].string,
  className: _propTypes["default"].string,
  isOverflowChip: _propTypes["default"].bool,
  isReadOnly: _propTypes["default"].bool,
  onClick: _propTypes["default"].func,
  component: _propTypes["default"].node,
  tooltipPosition: _propTypes["default"].oneOf(['auto', 'top', 'bottom', 'left', 'right'])
});

_defineProperty(Chip, "defaultProps", {
  closeBtnAriaLabel: 'close',
  className: '',
  isOverflowChip: false,
  isReadOnly: false,
  tooltipPosition: 'top',
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onClick: function onClick(_e) {
    return undefined;
  },
  component: 'div'
});

var ChipWithOuiaContext = (0, _withOuia.withOuiaContext)(Chip);
exports.Chip = ChipWithOuiaContext;
//# sourceMappingURL=Chip.js.map