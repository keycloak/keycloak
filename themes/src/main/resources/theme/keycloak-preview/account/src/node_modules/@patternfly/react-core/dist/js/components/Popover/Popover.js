"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.Popover = exports.PopoverPosition = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _PopoverBase = _interopRequireDefault(require("../../helpers/PopoverBase/PopoverBase"));

var _constants = require("../../helpers/constants");

var _popover = _interopRequireDefault(require("@patternfly/react-styles/css/components/Popover/popover"));

require("@patternfly/react-styles/css/components/Tooltip/tippy.css");

require("@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css");

var _reactStyles = require("@patternfly/react-styles");

var _PopoverContent = require("./PopoverContent");

var _PopoverBody = require("./PopoverBody");

var _PopoverHeader = require("./PopoverHeader");

var _PopoverFooter = require("./PopoverFooter");

var _PopoverCloseButton = require("./PopoverCloseButton");

var _GenerateId = _interopRequireDefault(require("../../helpers/GenerateId/GenerateId"));

var _c_popover_MaxWidth = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/c_popover_MaxWidth"));

var _helpers = require("../../helpers");

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

var PopoverPosition;
exports.PopoverPosition = PopoverPosition;

(function (PopoverPosition) {
  PopoverPosition["auto"] = "auto";
  PopoverPosition["top"] = "top";
  PopoverPosition["bottom"] = "bottom";
  PopoverPosition["left"] = "left";
  PopoverPosition["right"] = "right";
})(PopoverPosition || (exports.PopoverPosition = PopoverPosition = {}));

var Popover =
/*#__PURE__*/
function (_React$Component) {
  _inherits(Popover, _React$Component);

  function Popover(props) {
    var _this;

    _classCallCheck(this, Popover);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Popover).call(this, props));

    _defineProperty(_assertThisInitialized(_this), "hideOrNotify", function () {
      if (_this.props.isVisible === null) {
        // Handle closing
        _this.tip.hide();
      } else {
        // notify consumer
        _this.props.shouldClose(_this.tip);
      }
    });

    _defineProperty(_assertThisInitialized(_this), "handleEscOrEnterKey", function (event) {
      if (event.keyCode === _constants.KEY_CODES.ESCAPE_KEY && _this.tip.state.isVisible) {
        _this.hideOrNotify();
      } else if (!_this.state.isOpen && event.keyCode === _constants.KEY_CODES.ENTER) {
        _this.setState({
          focusTrapActive: true
        });
      }
    });

    _defineProperty(_assertThisInitialized(_this), "storeTippyInstance", function (tip) {
      if (_this.props.minWidth) {
        tip.popperChildren.tooltip.style.minWidth = _this.props.minWidth;
      }

      tip.popperChildren.tooltip.classList.add(_popover["default"].popover);
      _this.tip = tip;
    });

    _defineProperty(_assertThisInitialized(_this), "closePopover", function () {
      _this.hideOrNotify();

      _this.setState({
        focusTrapActive: false
      });
    });

    _defineProperty(_assertThisInitialized(_this), "hideAllPopovers", function () {
      document.querySelectorAll('.tippy-popper').forEach(function (popper) {
        if (popper._tippy) {
          popper._tippy.hide();
        }
      });
    });

    _defineProperty(_assertThisInitialized(_this), "onHide", function (tip) {
      if (_this.state.isOpen) {
        _this.setState({
          isOpen: false
        });
      }

      return _this.props.onHide(tip);
    });

    _defineProperty(_assertThisInitialized(_this), "onHidden", function (tip) {
      return _this.props.onHidden(tip);
    });

    _defineProperty(_assertThisInitialized(_this), "onMount", function (tip) {
      return _this.props.onMount(tip);
    });

    _defineProperty(_assertThisInitialized(_this), "onShow", function (tip) {
      var _this$props = _this.props,
          hideOnOutsideClick = _this$props.hideOnOutsideClick,
          isVisible = _this$props.isVisible,
          onShow = _this$props.onShow; // hide all other open popovers first if events are managed by us

      if (!hideOnOutsideClick && isVisible === null) {
        _this.hideAllPopovers();
      }

      if (_this.state.isOpen === false) {
        _this.setState({
          isOpen: true
        });
      }

      return onShow(tip);
    });

    _defineProperty(_assertThisInitialized(_this), "onShown", function (tip) {
      return _this.props.onShown(tip);
    });

    _defineProperty(_assertThisInitialized(_this), "onContentMouseDown", function () {
      if (_this.state.focusTrapActive) {
        _this.setState({
          focusTrapActive: false
        });
      }
    });

    _this.state = {
      isOpen: false,
      focusTrapActive: false
    };
    return _this;
  }

  _createClass(Popover, [{
    key: "componentDidMount",
    value: function componentDidMount() {
      document.addEventListener('keydown', this.handleEscOrEnterKey, false);
    }
  }, {
    key: "componentWillUnmount",
    value: function componentWillUnmount() {
      document.removeEventListener('keydown', this.handleEscOrEnterKey, false);
    }
  }, {
    key: "render",
    value: function render() {
      var _this2 = this;

      /* eslint-disable @typescript-eslint/no-unused-vars */
      var _this$props2 = this.props,
          position = _this$props2.position,
          enableFlip = _this$props2.enableFlip,
          children = _this$props2.children,
          className = _this$props2.className,
          ariaLabel = _this$props2['aria-label'],
          headerContent = _this$props2.headerContent,
          bodyContent = _this$props2.bodyContent,
          footerContent = _this$props2.footerContent,
          isVisible = _this$props2.isVisible,
          shouldClose = _this$props2.shouldClose,
          appendTo = _this$props2.appendTo,
          hideOnOutsideClick = _this$props2.hideOnOutsideClick,
          onHide = _this$props2.onHide,
          onHidden = _this$props2.onHidden,
          onShow = _this$props2.onShow,
          onShown = _this$props2.onShown,
          onMount = _this$props2.onMount,
          zIndex = _this$props2.zIndex,
          minWidth = _this$props2.minWidth,
          maxWidth = _this$props2.maxWidth,
          closeBtnAriaLabel = _this$props2.closeBtnAriaLabel,
          distance = _this$props2.distance,
          boundary = _this$props2.boundary,
          flipBehavior = _this$props2.flipBehavior,
          tippyProps = _this$props2.tippyProps,
          rest = _objectWithoutProperties(_this$props2, ["position", "enableFlip", "children", "className", "aria-label", "headerContent", "bodyContent", "footerContent", "isVisible", "shouldClose", "appendTo", "hideOnOutsideClick", "onHide", "onHidden", "onShow", "onShown", "onMount", "zIndex", "minWidth", "maxWidth", "closeBtnAriaLabel", "distance", "boundary", "flipBehavior", "tippyProps"]);
      /* eslint-enable @typescript-eslint/no-unused-vars */


      if (!headerContent && !ariaLabel) {
        return new Error('aria-label is required when header is not used');
      }

      var content = this.state.isOpen ? React.createElement(_GenerateId["default"], null, function (randomId) {
        return React.createElement(_helpers.FocusTrap, {
          active: _this2.state.focusTrapActive,
          focusTrapOptions: {
            clickOutsideDeactivates: true
          }
        }, React.createElement("div", _extends({
          className: (0, _reactStyles.css)(!enableFlip && (0, _reactStyles.getModifier)(_popover["default"], position, _popover["default"].modifiers.top), className),
          role: "dialog",
          "aria-modal": "true",
          "aria-label": headerContent ? undefined : ariaLabel,
          "aria-labelledby": headerContent ? "popover-".concat(randomId, "-header") : undefined,
          "aria-describedby": "popover-".concat(randomId, "-body"),
          onMouseDown: _this2.onContentMouseDown
        }, rest), React.createElement(_PopoverContent.PopoverContent, null, React.createElement(_PopoverCloseButton.PopoverCloseButton, {
          onClose: _this2.closePopover,
          "aria-label": closeBtnAriaLabel
        }), headerContent && React.createElement(_PopoverHeader.PopoverHeader, {
          id: "popover-".concat(randomId, "-header")
        }, headerContent), React.createElement(_PopoverBody.PopoverBody, {
          id: "popover-".concat(randomId, "-body")
        }, bodyContent), footerContent && React.createElement(_PopoverFooter.PopoverFooter, null, footerContent))));
      }) : React.createElement(React.Fragment, null);
      var handleEvents = isVisible === null;

      var shouldHideOnClick = function shouldHideOnClick() {
        if (handleEvents) {
          if (hideOnOutsideClick === true) {
            return true;
          }

          return 'toggle';
        }

        return false;
      };

      return React.createElement(_PopoverBase["default"], _extends({}, tippyProps, {
        arrow: true,
        onCreate: this.storeTippyInstance,
        maxWidth: maxWidth,
        zIndex: zIndex,
        appendTo: appendTo,
        content: content,
        lazy: true,
        trigger: handleEvents ? 'click' : 'manual',
        isVisible: isVisible,
        hideOnClick: shouldHideOnClick(),
        theme: "pf-popover",
        interactive: true,
        interactiveBorder: 0,
        placement: position,
        distance: distance,
        flip: enableFlip,
        flipBehavior: flipBehavior,
        boundary: boundary,
        popperOptions: {
          modifiers: {
            preventOverflow: {
              enabled: enableFlip
            },
            hide: {
              enabled: enableFlip
            }
          }
        },
        onHide: function onHide(tip) {
          return _this2.onHide(tip);
        },
        onHidden: function onHidden(tip) {
          return _this2.onHidden(tip);
        },
        onShow: function onShow(tip) {
          return _this2.onShow(tip);
        },
        onShown: function onShown(tip) {
          return _this2.onShown(tip);
        },
        onMount: function onMount(tip) {
          return _this2.onMount(tip);
        }
      }), children);
    }
  }]);

  return Popover;
}(React.Component);

exports.Popover = Popover;

_defineProperty(Popover, "propTypes", {
  'aria-label': _propTypes["default"].string,
  appendTo: _propTypes["default"].oneOfType([_propTypes["default"].element, _propTypes["default"].func]),
  bodyContent: _propTypes["default"].node.isRequired,
  boundary: _propTypes["default"].oneOfType([_propTypes["default"].oneOf(['scrollParent']), _propTypes["default"].oneOf(['window']), _propTypes["default"].oneOf(['viewport']), _propTypes["default"].any]),
  children: _propTypes["default"].element.isRequired,
  className: _propTypes["default"].string,
  closeBtnAriaLabel: _propTypes["default"].string,
  distance: _propTypes["default"].number,
  enableFlip: _propTypes["default"].bool,
  flipBehavior: _propTypes["default"].oneOfType([_propTypes["default"].oneOf(['flip']), _propTypes["default"].arrayOf(_propTypes["default"].oneOf(['top', 'bottom', 'left', 'right']))]),
  footerContent: _propTypes["default"].node,
  headerContent: _propTypes["default"].node,
  hideOnOutsideClick: _propTypes["default"].bool,
  isVisible: _propTypes["default"].bool,
  minWidth: _propTypes["default"].string,
  maxWidth: _propTypes["default"].string,
  onHidden: _propTypes["default"].func,
  onHide: _propTypes["default"].func,
  onMount: _propTypes["default"].func,
  onShow: _propTypes["default"].func,
  onShown: _propTypes["default"].func,
  position: _propTypes["default"].oneOf(['auto', 'top', 'bottom', 'left', 'right']),
  shouldClose: _propTypes["default"].func,
  zIndex: _propTypes["default"].number,
  tippyProps: _propTypes["default"].any
});

_defineProperty(Popover, "defaultProps", {
  position: 'top',
  enableFlip: true,
  className: '',
  isVisible: null,
  shouldClose: function shouldClose() {
    return null;
  },
  'aria-label': '',
  headerContent: null,
  footerContent: null,
  appendTo: function appendTo() {
    return document.body;
  },
  hideOnOutsideClick: true,
  onHide: function onHide() {
    return null;
  },
  onHidden: function onHidden() {
    return null;
  },
  onShow: function onShow() {
    return null;
  },
  onShown: function onShown() {
    return null;
  },
  onMount: function onMount() {
    return null;
  },
  zIndex: 9999,
  maxWidth: _c_popover_MaxWidth["default"] && _c_popover_MaxWidth["default"].value,
  closeBtnAriaLabel: 'Close',
  distance: 25,
  boundary: 'window',
  // For every initial starting position, there are 3 escape positions
  flipBehavior: ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'],
  tippyProps: {}
});
//# sourceMappingURL=Popover.js.map