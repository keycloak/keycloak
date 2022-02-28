(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "react-dom", "@patternfly/react-styles/css/components/DataToolbar/data-toolbar", "@patternfly/react-styles", "./DataToolbarUtils", "../../components/Button", "@patternfly/react-tokens/dist/js/global_breakpoint_lg", "../../helpers/util"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("react-dom"), require("@patternfly/react-styles/css/components/DataToolbar/data-toolbar"), require("@patternfly/react-styles"), require("./DataToolbarUtils"), require("../../components/Button"), require("@patternfly/react-tokens/dist/js/global_breakpoint_lg"), require("../../helpers/util"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactDom, global.dataToolbar, global.reactStyles, global.DataToolbarUtils, global.Button, global.global_breakpoint_lg, global.util);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactDom, _dataToolbar, _reactStyles, _DataToolbarUtils, _Button, _global_breakpoint_lg, _util) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DataToolbarToggleGroup = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var ReactDOM = _interopRequireWildcard(_reactDom);

  var _dataToolbar2 = _interopRequireDefault(_dataToolbar);

  var _global_breakpoint_lg2 = _interopRequireDefault(_global_breakpoint_lg);

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

  class DataToolbarToggleGroup extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "isContentPopup", () => {
        const viewportSize = window.innerWidth;
        const lgBreakpointValue = parseInt(_global_breakpoint_lg2.default.value);
        return viewportSize < lgBreakpointValue;
      });
    }

    render() {
      const _this$props = this.props,
            {
        toggleIcon,
        breakpoint,
        variant,
        breakpointMods,
        className,
        children
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["toggleIcon", "breakpoint", "variant", "breakpointMods", "className", "children"]);

      return React.createElement(_DataToolbarUtils.DataToolbarContext.Consumer, null, ({
        isExpanded,
        toggleIsExpanded
      }) => React.createElement(_DataToolbarUtils.DataToolbarContentContext.Consumer, null, ({
        expandableContentRef,
        expandableContentId
      }) => {
        if (expandableContentRef.current && expandableContentRef.current.classList) {
          if (isExpanded) {
            expandableContentRef.current.classList.add((0, _reactStyles.getModifier)(_dataToolbar2.default, 'expanded'));
          } else {
            expandableContentRef.current.classList.remove((0, _reactStyles.getModifier)(_dataToolbar2.default, 'expanded'));
          }
        }

        return React.createElement("div", _extends({
          className: (0, _reactStyles.css)(_dataToolbar2.default.dataToolbarGroup, variant && (0, _reactStyles.getModifier)(_dataToolbar2.default, variant), (0, _util.formatBreakpointMods)(breakpointMods, _dataToolbar2.default), (0, _reactStyles.getModifier)(_dataToolbar2.default, 'toggle-group'), (0, _reactStyles.getModifier)(_dataToolbar2.default, `show-on-${breakpoint}`), className)
        }, props), React.createElement("div", {
          className: (0, _reactStyles.css)(_dataToolbar2.default.dataToolbarToggle)
        }, React.createElement(_Button.Button, _extends({
          variant: "plain",
          onClick: toggleIsExpanded,
          "aria-label": "Show Filters"
        }, isExpanded && {
          'aria-expanded': true
        }, {
          "aria-haspopup": isExpanded && this.isContentPopup(),
          "aria-controls": expandableContentId
        }), toggleIcon)), isExpanded ? ReactDOM.createPortal(children, expandableContentRef.current.firstElementChild) : children);
      }));
    }

  }

  exports.DataToolbarToggleGroup = DataToolbarToggleGroup;

  _defineProperty(DataToolbarToggleGroup, "propTypes", {
    toggleIcon: _propTypes2.default.node.isRequired,
    breakpoint: _propTypes2.default.oneOf(['md', 'lg', 'xl']).isRequired,
    breakpointMods: _propTypes2.default.arrayOf(_propTypes2.default.any)
  });

  _defineProperty(DataToolbarToggleGroup, "defaultProps", {
    breakpointMods: []
  });
});
//# sourceMappingURL=DataToolbarToggleGroup.js.map