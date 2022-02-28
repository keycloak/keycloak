(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "../../helpers/PopoverBase/PopoverBase", "../../helpers/constants", "@patternfly/react-styles/css/components/Popover/popover", "@patternfly/react-styles", "./PopoverContent", "./PopoverBody", "./PopoverHeader", "./PopoverFooter", "./PopoverCloseButton", "../../helpers/GenerateId/GenerateId", "@patternfly/react-tokens/dist/js/c_popover_MaxWidth", "../../helpers", "@patternfly/react-styles/css/components/Tooltip/tippy.css", "@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("../../helpers/PopoverBase/PopoverBase"), require("../../helpers/constants"), require("@patternfly/react-styles/css/components/Popover/popover"), require("@patternfly/react-styles"), require("./PopoverContent"), require("./PopoverBody"), require("./PopoverHeader"), require("./PopoverFooter"), require("./PopoverCloseButton"), require("../../helpers/GenerateId/GenerateId"), require("@patternfly/react-tokens/dist/js/c_popover_MaxWidth"), require("../../helpers"), require("@patternfly/react-styles/css/components/Tooltip/tippy.css"), require("@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.PopoverBase, global.constants, global.popover, global.reactStyles, global.PopoverContent, global.PopoverBody, global.PopoverHeader, global.PopoverFooter, global.PopoverCloseButton, global.GenerateId, global.c_popover_MaxWidth, global.helpers, global.tippy, global.tippyOverrides);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _PopoverBase, _constants, _popover, _reactStyles, _PopoverContent, _PopoverBody, _PopoverHeader, _PopoverFooter, _PopoverCloseButton, _GenerateId, _c_popover_MaxWidth, _helpers) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Popover = exports.PopoverPosition = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _PopoverBase2 = _interopRequireDefault(_PopoverBase);

  var _popover2 = _interopRequireDefault(_popover);

  var _GenerateId2 = _interopRequireDefault(_GenerateId);

  var _c_popover_MaxWidth2 = _interopRequireDefault(_c_popover_MaxWidth);

  function _getRequireWildcardCache() {
    if (typeof WeakMap !== "function") return null;
    var cache = new WeakMap();

    _getRequireWildcardCache = function () {
      return cache;
    };

    return cache;
  }

  function _interopRequireWildcard(obj) {
    if (obj && obj.__esModule) {
      return obj;
    }

    var cache = _getRequireWildcardCache();

    if (cache && cache.has(obj)) {
      return cache.get(obj);
    }

    var newObj = {};

    if (obj != null) {
      var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor;

      for (var key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null;

          if (desc && (desc.get || desc.set)) {
            Object.defineProperty(newObj, key, desc);
          } else {
            newObj[key] = obj[key];
          }
        }
      }
    }

    newObj.default = obj;

    if (cache) {
      cache.set(obj, newObj);
    }

    return newObj;
  }

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  function _extends() {
    _extends = Object.assign || function (target) {
      for (var i = 1; i < arguments.length; i++) {
        var source = arguments[i];

        for (var key in source) {
          if (Object.prototype.hasOwnProperty.call(source, key)) {
            target[key] = source[key];
          }
        }
      }

      return target;
    };

    return _extends.apply(this, arguments);
  }

  function _objectWithoutProperties(source, excluded) {
    if (source == null) return {};

    var target = _objectWithoutPropertiesLoose(source, excluded);

    var key, i;

    if (Object.getOwnPropertySymbols) {
      var sourceSymbolKeys = Object.getOwnPropertySymbols(source);

      for (i = 0; i < sourceSymbolKeys.length; i++) {
        key = sourceSymbolKeys[i];
        if (excluded.indexOf(key) >= 0) continue;
        if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue;
        target[key] = source[key];
      }
    }

    return target;
  }

  function _objectWithoutPropertiesLoose(source, excluded) {
    if (source == null) return {};
    var target = {};
    var sourceKeys = Object.keys(source);
    var key, i;

    for (i = 0; i < sourceKeys.length; i++) {
      key = sourceKeys[i];
      if (excluded.indexOf(key) >= 0) continue;
      target[key] = source[key];
    }

    return target;
  }

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  let PopoverPosition = exports.PopoverPosition = undefined;

  (function (PopoverPosition) {
    PopoverPosition["auto"] = "auto";
    PopoverPosition["top"] = "top";
    PopoverPosition["bottom"] = "bottom";
    PopoverPosition["left"] = "left";
    PopoverPosition["right"] = "right";
  })(PopoverPosition || (exports.PopoverPosition = PopoverPosition = {}));

  class Popover extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "hideOrNotify", () => {
        if (this.props.isVisible === null) {
          // Handle closing
          this.tip.hide();
        } else {
          // notify consumer
          this.props.shouldClose(this.tip);
        }
      });

      _defineProperty(this, "handleEscOrEnterKey", event => {
        if (event.keyCode === _constants.KEY_CODES.ESCAPE_KEY && this.tip.state.isVisible) {
          this.hideOrNotify();
        } else if (!this.state.isOpen && event.keyCode === _constants.KEY_CODES.ENTER) {
          this.setState({
            focusTrapActive: true
          });
        }
      });

      _defineProperty(this, "storeTippyInstance", tip => {
        if (this.props.minWidth) {
          tip.popperChildren.tooltip.style.minWidth = this.props.minWidth;
        }

        tip.popperChildren.tooltip.classList.add(_popover2.default.popover);
        this.tip = tip;
      });

      _defineProperty(this, "closePopover", () => {
        this.hideOrNotify();
        this.setState({
          focusTrapActive: false
        });
      });

      _defineProperty(this, "hideAllPopovers", () => {
        document.querySelectorAll('.tippy-popper').forEach(popper => {
          if (popper._tippy) {
            popper._tippy.hide();
          }
        });
      });

      _defineProperty(this, "onHide", tip => {
        if (this.state.isOpen) {
          this.setState({
            isOpen: false
          });
        }

        return this.props.onHide(tip);
      });

      _defineProperty(this, "onHidden", tip => this.props.onHidden(tip));

      _defineProperty(this, "onMount", tip => this.props.onMount(tip));

      _defineProperty(this, "onShow", tip => {
        const {
          hideOnOutsideClick,
          isVisible,
          onShow
        } = this.props; // hide all other open popovers first if events are managed by us

        if (!hideOnOutsideClick && isVisible === null) {
          this.hideAllPopovers();
        }

        if (this.state.isOpen === false) {
          this.setState({
            isOpen: true
          });
        }

        return onShow(tip);
      });

      _defineProperty(this, "onShown", tip => this.props.onShown(tip));

      _defineProperty(this, "onContentMouseDown", () => {
        if (this.state.focusTrapActive) {
          this.setState({
            focusTrapActive: false
          });
        }
      });

      this.state = {
        isOpen: false,
        focusTrapActive: false
      };
    }

    componentDidMount() {
      document.addEventListener('keydown', this.handleEscOrEnterKey, false);
    }

    componentWillUnmount() {
      document.removeEventListener('keydown', this.handleEscOrEnterKey, false);
    }

    render() {
      /* eslint-disable @typescript-eslint/no-unused-vars */
      const _this$props = this.props,
            {
        position,
        enableFlip,
        children,
        className,
        'aria-label': ariaLabel,
        headerContent,
        bodyContent,
        footerContent,
        isVisible,
        shouldClose,
        appendTo,
        hideOnOutsideClick,
        onHide,
        onHidden,
        onShow,
        onShown,
        onMount,
        zIndex,
        minWidth,
        maxWidth,
        closeBtnAriaLabel,
        distance,
        boundary,
        flipBehavior,
        tippyProps
      } = _this$props,
            rest = _objectWithoutProperties(_this$props, ["position", "enableFlip", "children", "className", "aria-label", "headerContent", "bodyContent", "footerContent", "isVisible", "shouldClose", "appendTo", "hideOnOutsideClick", "onHide", "onHidden", "onShow", "onShown", "onMount", "zIndex", "minWidth", "maxWidth", "closeBtnAriaLabel", "distance", "boundary", "flipBehavior", "tippyProps"]);
      /* eslint-enable @typescript-eslint/no-unused-vars */


      if (!headerContent && !ariaLabel) {
        return new Error('aria-label is required when header is not used');
      }

      const content = this.state.isOpen ? React.createElement(_GenerateId2.default, null, randomId => React.createElement(_helpers.FocusTrap, {
        active: this.state.focusTrapActive,
        focusTrapOptions: {
          clickOutsideDeactivates: true
        }
      }, React.createElement("div", _extends({
        className: (0, _reactStyles.css)(!enableFlip && (0, _reactStyles.getModifier)(_popover2.default, position, _popover2.default.modifiers.top), className),
        role: "dialog",
        "aria-modal": "true",
        "aria-label": headerContent ? undefined : ariaLabel,
        "aria-labelledby": headerContent ? `popover-${randomId}-header` : undefined,
        "aria-describedby": `popover-${randomId}-body`,
        onMouseDown: this.onContentMouseDown
      }, rest), React.createElement(_PopoverContent.PopoverContent, null, React.createElement(_PopoverCloseButton.PopoverCloseButton, {
        onClose: this.closePopover,
        "aria-label": closeBtnAriaLabel
      }), headerContent && React.createElement(_PopoverHeader.PopoverHeader, {
        id: `popover-${randomId}-header`
      }, headerContent), React.createElement(_PopoverBody.PopoverBody, {
        id: `popover-${randomId}-body`
      }, bodyContent), footerContent && React.createElement(_PopoverFooter.PopoverFooter, null, footerContent))))) : React.createElement(React.Fragment, null);
      const handleEvents = isVisible === null;

      const shouldHideOnClick = () => {
        if (handleEvents) {
          if (hideOnOutsideClick === true) {
            return true;
          }

          return 'toggle';
        }

        return false;
      };

      return React.createElement(_PopoverBase2.default, _extends({}, tippyProps, {
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
        onHide: tip => this.onHide(tip),
        onHidden: tip => this.onHidden(tip),
        onShow: tip => this.onShow(tip),
        onShown: tip => this.onShown(tip),
        onMount: tip => this.onMount(tip)
      }), children);
    }

  }

  exports.Popover = Popover;

  _defineProperty(Popover, "propTypes", {
    'aria-label': _propTypes2.default.string,
    appendTo: _propTypes2.default.oneOfType([_propTypes2.default.element, _propTypes2.default.func]),
    bodyContent: _propTypes2.default.node.isRequired,
    boundary: _propTypes2.default.oneOfType([_propTypes2.default.oneOf(['scrollParent']), _propTypes2.default.oneOf(['window']), _propTypes2.default.oneOf(['viewport']), _propTypes2.default.any]),
    children: _propTypes2.default.element.isRequired,
    className: _propTypes2.default.string,
    closeBtnAriaLabel: _propTypes2.default.string,
    distance: _propTypes2.default.number,
    enableFlip: _propTypes2.default.bool,
    flipBehavior: _propTypes2.default.oneOfType([_propTypes2.default.oneOf(['flip']), _propTypes2.default.arrayOf(_propTypes2.default.oneOf(['top', 'bottom', 'left', 'right']))]),
    footerContent: _propTypes2.default.node,
    headerContent: _propTypes2.default.node,
    hideOnOutsideClick: _propTypes2.default.bool,
    isVisible: _propTypes2.default.bool,
    minWidth: _propTypes2.default.string,
    maxWidth: _propTypes2.default.string,
    onHidden: _propTypes2.default.func,
    onHide: _propTypes2.default.func,
    onMount: _propTypes2.default.func,
    onShow: _propTypes2.default.func,
    onShown: _propTypes2.default.func,
    position: _propTypes2.default.oneOf(['auto', 'top', 'bottom', 'left', 'right']),
    shouldClose: _propTypes2.default.func,
    zIndex: _propTypes2.default.number,
    tippyProps: _propTypes2.default.any
  });

  _defineProperty(Popover, "defaultProps", {
    position: 'top',
    enableFlip: true,
    className: '',
    isVisible: null,
    shouldClose: () => null,
    'aria-label': '',
    headerContent: null,
    footerContent: null,
    appendTo: () => document.body,
    hideOnOutsideClick: true,
    onHide: () => null,
    onHidden: () => null,
    onShow: () => null,
    onShown: () => null,
    onMount: () => null,
    zIndex: 9999,
    maxWidth: _c_popover_MaxWidth2.default && _c_popover_MaxWidth2.default.value,
    closeBtnAriaLabel: 'Close',
    distance: 25,
    boundary: 'window',
    // For every initial starting position, there are 3 escape positions
    flipBehavior: ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'],
    tippyProps: {}
  });
});
//# sourceMappingURL=Popover.js.map