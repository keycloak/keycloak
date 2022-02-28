(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "../../helpers/PopoverBase/PopoverBase", "@patternfly/react-styles/css/components/Tooltip/tooltip", "@patternfly/react-styles", "./TooltipContent", "../../helpers/constants", "@patternfly/react-tokens/dist/js/c_tooltip_MaxWidth", "@patternfly/react-styles/css/components/Tooltip/tippy.css", "@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("../../helpers/PopoverBase/PopoverBase"), require("@patternfly/react-styles/css/components/Tooltip/tooltip"), require("@patternfly/react-styles"), require("./TooltipContent"), require("../../helpers/constants"), require("@patternfly/react-tokens/dist/js/c_tooltip_MaxWidth"), require("@patternfly/react-styles/css/components/Tooltip/tippy.css"), require("@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.PopoverBase, global.tooltip, global.reactStyles, global.TooltipContent, global.constants, global.c_tooltip_MaxWidth, global.tippy, global.tippyOverrides);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _PopoverBase, _tooltip, _reactStyles, _TooltipContent, _constants, _c_tooltip_MaxWidth) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Tooltip = exports.TooltipPosition = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _PopoverBase2 = _interopRequireDefault(_PopoverBase);

  var _tooltip2 = _interopRequireDefault(_tooltip);

  var _c_tooltip_MaxWidth2 = _interopRequireDefault(_c_tooltip_MaxWidth);

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

  let TooltipPosition = exports.TooltipPosition = undefined;

  (function (TooltipPosition) {
    TooltipPosition["auto"] = "auto";
    TooltipPosition["top"] = "top";
    TooltipPosition["bottom"] = "bottom";
    TooltipPosition["left"] = "left";
    TooltipPosition["right"] = "right";
  })(TooltipPosition || (exports.TooltipPosition = TooltipPosition = {}));

  class Tooltip extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "storeTippyInstance", tip => {
        tip.popperChildren.tooltip.classList.add(_tooltip2.default.tooltip);
        this.tip = tip;
      });

      _defineProperty(this, "handleEscKeyClick", event => {
        if (event.keyCode === _constants.KEY_CODES.ESCAPE_KEY && this.tip.state.isVisible) {
          this.tip.hide();
        }
      });
    }

    componentDidMount() {
      document.addEventListener('keydown', this.handleEscKeyClick, false);
    }

    componentWillUnmount() {
      document.removeEventListener('keydown', this.handleEscKeyClick, false);
    }

    extendChildren() {
      return React.cloneElement(this.props.children, {
        isAppLauncher: this.props.isAppLauncher
      });
    }

    render() {
      const _this$props = this.props,
            {
        position,
        trigger,
        isContentLeftAligned,
        isVisible,
        enableFlip,
        children,
        className,
        content: bodyContent,
        entryDelay,
        exitDelay,
        appendTo,
        zIndex,
        maxWidth,
        isAppLauncher,
        distance,
        aria,
        boundary,
        flipBehavior,
        tippyProps,
        id
      } = _this$props,
            rest = _objectWithoutProperties(_this$props, ["position", "trigger", "isContentLeftAligned", "isVisible", "enableFlip", "children", "className", "content", "entryDelay", "exitDelay", "appendTo", "zIndex", "maxWidth", "isAppLauncher", "distance", "aria", "boundary", "flipBehavior", "tippyProps", "id"]);

      const content = React.createElement("div", _extends({
        className: (0, _reactStyles.css)(!enableFlip && (0, _reactStyles.getModifier)(_tooltip2.default, position, _tooltip2.default.modifiers.top), className),
        role: "tooltip",
        id: id
      }, rest), React.createElement(_TooltipContent.TooltipContent, {
        isLeftAligned: isContentLeftAligned
      }, bodyContent));
      return React.createElement(_PopoverBase2.default, _extends({}, tippyProps, {
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

  }

  exports.Tooltip = Tooltip;

  _defineProperty(Tooltip, "propTypes", {
    appendTo: _propTypes2.default.oneOfType([_propTypes2.default.element, _propTypes2.default.func]),
    aria: _propTypes2.default.oneOf(['describedby', 'labelledby']),
    boundary: _propTypes2.default.oneOfType([_propTypes2.default.oneOf(['scrollParent']), _propTypes2.default.oneOf(['window']), _propTypes2.default.oneOf(['viewport']), _propTypes2.default.any]),
    children: _propTypes2.default.element.isRequired,
    className: _propTypes2.default.string,
    content: _propTypes2.default.node.isRequired,
    distance: _propTypes2.default.number,
    enableFlip: _propTypes2.default.bool,
    entryDelay: _propTypes2.default.number,
    exitDelay: _propTypes2.default.number,
    flipBehavior: _propTypes2.default.oneOfType([_propTypes2.default.oneOf(['flip']), _propTypes2.default.arrayOf(_propTypes2.default.oneOf(['top', 'bottom', 'left', 'right']))]),
    isAppLauncher: _propTypes2.default.bool,
    maxWidth: _propTypes2.default.string,
    position: _propTypes2.default.oneOf(['auto', 'top', 'bottom', 'left', 'right']),
    trigger: _propTypes2.default.string,
    isContentLeftAligned: _propTypes2.default.bool,
    isVisible: _propTypes2.default.bool,
    zIndex: _propTypes2.default.number,
    tippyProps: _propTypes2.default.any,
    id: _propTypes2.default.string
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
    appendTo: () => document.body,
    zIndex: 9999,
    maxWidth: _c_tooltip_MaxWidth2.default && _c_tooltip_MaxWidth2.default.value,
    isAppLauncher: false,
    distance: 15,
    aria: 'describedby',
    boundary: 'window',
    // For every initial starting position, there are 3 escape positions
    flipBehavior: ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'],
    tippyProps: {},
    id: ''
  });
});
//# sourceMappingURL=Tooltip.js.map