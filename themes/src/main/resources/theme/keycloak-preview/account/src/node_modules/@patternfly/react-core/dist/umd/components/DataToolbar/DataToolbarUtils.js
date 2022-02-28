(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "react", "@patternfly/react-tokens/dist/js/global_breakpoint_md", "@patternfly/react-tokens/dist/js/global_breakpoint_lg", "@patternfly/react-tokens/dist/js/global_breakpoint_xl", "@patternfly/react-tokens/dist/js/global_breakpoint_2xl"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("react"), require("@patternfly/react-tokens/dist/js/global_breakpoint_md"), require("@patternfly/react-tokens/dist/js/global_breakpoint_lg"), require("@patternfly/react-tokens/dist/js/global_breakpoint_xl"), require("@patternfly/react-tokens/dist/js/global_breakpoint_2xl"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.react, global.global_breakpoint_md, global.global_breakpoint_lg, global.global_breakpoint_xl, global.global_breakpoint_2xl);
    global.undefined = mod.exports;
  }
})(this, function (exports, _react, _global_breakpoint_md, _global_breakpoint_lg, _global_breakpoint_xl, _global_breakpoint_2xl) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.globalBreakpoints = exports.DataToolbarContentContext = exports.DataToolbarContext = undefined;

  var React = _interopRequireWildcard(_react);

  var _global_breakpoint_md2 = _interopRequireDefault(_global_breakpoint_md);

  var _global_breakpoint_lg2 = _interopRequireDefault(_global_breakpoint_lg);

  var _global_breakpoint_xl2 = _interopRequireDefault(_global_breakpoint_xl);

  var _global_breakpoint_2xl2 = _interopRequireDefault(_global_breakpoint_2xl);

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

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

  const DataToolbarContext = exports.DataToolbarContext = React.createContext({
    isExpanded: false,
    toggleIsExpanded: () => {},
    chipGroupContentRef: null,
    updateNumberFilters: () => {},
    numberOfFilters: 0
  });
  const DataToolbarContentContext = exports.DataToolbarContentContext = React.createContext({
    expandableContentRef: null,
    expandableContentId: '',
    chipContainerRef: null
  });

  const globalBreakpoints = exports.globalBreakpoints = breakpoint => {
    const breakpoints = {
      md: parseInt(_global_breakpoint_md2.default.value),
      lg: parseInt(_global_breakpoint_lg2.default.value),
      xl: parseInt(_global_breakpoint_xl2.default.value),
      '2xl': parseInt(_global_breakpoint_2xl2.default.value)
    };
    return breakpoints[breakpoint];
  };
});
//# sourceMappingURL=DataToolbarUtils.js.map