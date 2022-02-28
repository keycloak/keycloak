(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/OverflowMenu/overflow-menu", "@patternfly/react-styles", "./OverflowMenuContext", "@patternfly/react-tokens/dist/js/global_breakpoint_md", "@patternfly/react-tokens/dist/js/global_breakpoint_lg", "@patternfly/react-tokens/dist/js/global_breakpoint_xl", "../../helpers/util"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/OverflowMenu/overflow-menu"), require("@patternfly/react-styles"), require("./OverflowMenuContext"), require("@patternfly/react-tokens/dist/js/global_breakpoint_md"), require("@patternfly/react-tokens/dist/js/global_breakpoint_lg"), require("@patternfly/react-tokens/dist/js/global_breakpoint_xl"), require("../../helpers/util"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.overflowMenu, global.reactStyles, global.OverflowMenuContext, global.global_breakpoint_md, global.global_breakpoint_lg, global.global_breakpoint_xl, global.util);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _overflowMenu, _reactStyles, _OverflowMenuContext, _global_breakpoint_md, _global_breakpoint_lg, _global_breakpoint_xl, _util) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OverflowMenu = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _overflowMenu2 = _interopRequireDefault(_overflowMenu);

  var _global_breakpoint_md2 = _interopRequireDefault(_global_breakpoint_md);

  var _global_breakpoint_lg2 = _interopRequireDefault(_global_breakpoint_lg);

  var _global_breakpoint_xl2 = _interopRequireDefault(_global_breakpoint_xl);

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

  class OverflowMenu extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "handleResize", () => {
        const breakpoints = {
          /* eslint-disable camelcase */
          md: _global_breakpoint_md2.default,
          lg: _global_breakpoint_lg2.default,
          xl: _global_breakpoint_xl2.default
          /* eslint-enable camelcase */

        };
        const {
          breakpoint
        } = this.props;
        let breakpointWidth = breakpoints[breakpoint].value;
        breakpointWidth = Number(breakpointWidth.split('px')[0]);
        const isBelowBreakpoint = window.innerWidth < breakpointWidth;
        this.state.isBelowBreakpoint !== isBelowBreakpoint && this.setState({
          isBelowBreakpoint
        });
      });

      this.state = {
        isBelowBreakpoint: false
      };
    }

    componentDidMount() {
      this.handleResize();
      window.addEventListener('resize', (0, _util.debounce)(this.handleResize, 250));
    }

    componentWillUnmount() {
      window.removeEventListener('resize', (0, _util.debounce)(this.handleResize, 250));
    }

    render() {
      const _this$props = this.props,
            {
        className,
        breakpoint,
        children
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["className", "breakpoint", "children"]);

      return React.createElement("div", _extends({}, props, {
        className: (0, _reactStyles.css)(_overflowMenu2.default.overflowMenu, (0, _reactStyles.getModifier)(_overflowMenu2.default.modifiers, `showOn ${breakpoint}`), className)
      }), React.createElement(_OverflowMenuContext.OverflowMenuContext.Provider, {
        value: {
          isBelowBreakpoint: this.state.isBelowBreakpoint
        }
      }, children));
    }

  }

  exports.OverflowMenu = OverflowMenu;

  _defineProperty(OverflowMenu, "propTypes", {
    children: _propTypes2.default.any,
    className: _propTypes2.default.string,
    breakpoint: _propTypes2.default.oneOf(['md', 'lg', 'xl']).isRequired
  });

  OverflowMenu.contextType = _OverflowMenuContext.OverflowMenuContext;
});
//# sourceMappingURL=OverflowMenu.js.map