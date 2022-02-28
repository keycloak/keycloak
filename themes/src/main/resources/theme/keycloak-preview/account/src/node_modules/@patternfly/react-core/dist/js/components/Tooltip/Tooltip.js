"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Tooltip = exports.TooltipPosition = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _PopoverBase = _interopRequireDefault(require("../../helpers/PopoverBase/PopoverBase"));

var _tooltip = _interopRequireDefault(require("@patternfly/react-styles/css/components/Tooltip/tooltip"));

require("@patternfly/react-styles/css/components/Tooltip/tippy.css");

require("@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css");

var _reactStyles = require("@patternfly/react-styles");

var _TooltipContent = require("./TooltipContent");

var _constants = require("../../helpers/constants");

var _c_tooltip_MaxWidth = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/c_tooltip_MaxWidth"));

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

var TooltipPosition;
exports.TooltipPosition = TooltipPosition;

(function (TooltipPosition) {
  TooltipPosition["auto"] = "auto";
  TooltipPosition["top"] = "top";
  TooltipPosition["bottom"] = "bottom";
  TooltipPosition["left"] = "left";
  TooltipPosition["right"] = "right";
})(TooltipPosition || (exports.TooltipPosition = TooltipPosition = {}));

var Tooltip =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Tooltip, _React$Component);

  function Tooltip() {
    var _getPrototypeOf2;

    var _this;

    _classCallCheck(this, Tooltip);

    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    _this = _possibleConstructorReturn(this, (_getPrototypeOf2 = _getPrototypeOf(Tooltip)).call.apply(_getPrototypeOf2, [this].concat(args)));

    _defineProperty(_assertThisInitialized(_this), "storeTippyInstance", function (tip) {
      tip.popperChildren.tooltip.classList.add(_tooltip["default"].tooltip);
      _this.tip = tip;
    });

    _defineProperty(_assertThisInitialized(_this), "handleEscKeyClick", function (event) {
      if (event.keyCode === _constants.KEY_CODES.ESCAPE_KEY && _this.tip.state.isVisible) {
        _this.tip.hide();
      }
    });

    return _this;
  }

  _createClass(Tooltip, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      document.addEventListener('keydown', this.handleEscKeyClick, false);
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      document.removeEventListener('keydown', this.handleEscKeyClick, false);
    }
  }, {
    key: "extendChildren",
    value: function extendChildren() {
      return React.cloneElement(this.props.children, {
        isAppLauncher: this.props.isAppLauncher
      });
    }
  }, {
    key: "render",
    value: function render() {
      var _this$props = this.props,
          position = _this$props.position,
          trigger = _this$props.trigger,
          isContentLeftAligned = _this$props.isContentLeftAligned,
          isVisible = _this$props.isVisible,
          enableFlip = _this$props.enableFlip,
          children = _this$props.children,
          className = _this$props.className,
          bodyContent = _this$props.content,
          entryDelay = _this$props.entryDelay,
          exitDelay = _this$props.exitDelay,
          appendTo = _this$props.appendTo,
          zIndex = _this$props.zIndex,
          maxWidth = _this$props.maxWidth,
          isAppLauncher = _this$props.isAppLauncher,
          distance = _this$props.distance,
          aria = _this$props.aria,
          boundary = _this$props.boundary,
          flipBehavior = _this$props.flipBehavior,
          tippyProps = _this$props.tippyProps,
          id = _this$props.id,
          rest = _objectWithoutProperties(_this$props, ["position", "trigger", "isContentLeftAligned", "isVisible", "enableFlip", "children", "className", "content", "entryDelay", "exitDelay", "appendTo", "zIndex", "maxWidth", "isAppLauncher", "distance", "aria", "boundary", "flipBehavior", "tippyProps", "id"]);

      var content = React.createElement("div", _extends({
        className: (0, _reactStyles.css)(!enableFlip && (0, _reactStyles.getModifier)(_tooltip["default"], position, _tooltip["default"].modifiers.top), className),
        role: "tooltip",
        id: id
      }, rest), React.createElement(_TooltipContent.TooltipContent, {
        isLeftAligned: isContentLeftAligned
      }, bodyContent));
      return React.createElement(_PopoverBase["default"], _extends({}, tippyProps, {
        arrow: true,
        aria: aria,
        onCreate: this.storeTippyInstance,
        maxWidth: maxWidth,
        zIndex: zIndex,
        appendTo: appendTo,
        content: content,
        lazy: true,
        theme: "pf-tooltip",
        placement: position,
        trigger: trigger,
        delay: [entryDelay, exitDelay],
        distance: distance,
        flip: enableFlip,
        flipBehavior: flipBehavior,
        boundary: boundary,
        isVisible: isVisible,
        popperOptions: {
          modifiers: {
            preventOverflow: {
              enabled: enableFlip
            },
            hide: {
              enabled: enableFlip
            }
          }
        }
      }), isAppLauncher ? this.extendChildren() : children);
    }
  }]);

  return Tooltip;
}(React.Component);

exports.Tooltip = Tooltip;

_defineProperty(Tooltip, "propTypes", {
  appendTo: _propTypes["default"].oneOfType([_propTypes["default"].element, _propTypes["default"].func]),
  aria: _propTypes["default"].oneOf(['describedby', 'labelledby']),
  boundary: _propTypes["default"].oneOfType([_propTypes["default"].oneOf(['scrollParent']), _propTypes["default"].oneOf(['window']), _propTypes["default"].oneOf(['viewport']), _propTypes["default"].any]),
  children: _propTypes["default"].element.isRequired,
  className: _propTypes["default"].string,
  content: _propTypes["default"].node.isRequired,
  distance: _propTypes["default"].number,
  enableFlip: _propTypes["default"].bool,
  entryDelay: _propTypes["default"].number,
  exitDelay: _propTypes["default"].number,
  flipBehavior: _propTypes["default"].oneOfType([_propTypes["default"].oneOf(['flip']), _propTypes["default"].arrayOf(_propTypes["default"].oneOf(['top', 'bottom', 'left', 'right']))]),
  isAppLauncher: _propTypes["default"].bool,
  maxWidth: _propTypes["default"].string,
  position: _propTypes["default"].oneOf(['auto', 'top', 'bottom', 'left', 'right']),
  trigger: _propTypes["default"].string,
  isContentLeftAligned: _propTypes["default"].bool,
  isVisible: _propTypes["default"].bool,
  zIndex: _propTypes["default"].number,
  tippyProps: _propTypes["default"].any,
  id: _propTypes["default"].string
});

_defineProperty(Tooltip, "defaultProps", {
  position: 'top',
  trigger: 'mouseenter focus',
  isVisible: false,
  isContentLeftAligned: false,
  enableFlip: true,
  className: '',
  entryDelay: 500,
  exitDelay: 500,
  appendTo: function appendTo() {
    return document.body;
  },
  zIndex: 9999,
  maxWidth: _c_tooltip_MaxWidth["default"] && _c_tooltip_MaxWidth["default"].value,
  isAppLauncher: false,
  distance: 15,
  aria: 'describedby',
  boundary: 'window',
  // For every initial starting position, there are 3 escape positions
  flipBehavior: ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'],
  tippyProps: {},
  id: ''
});
//# sourceMappingURL=Tooltip.js.map