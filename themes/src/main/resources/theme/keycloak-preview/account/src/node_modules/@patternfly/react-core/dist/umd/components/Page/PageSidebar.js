(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Page/page", "@patternfly/react-styles", "./Page"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Page/page"), require("@patternfly/react-styles"), require("./Page"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.page, global.reactStyles, global.Page);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _page, _reactStyles, _Page) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.PageSidebar = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _page2 = _interopRequireDefault(_page);

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

  const PageSidebar = exports.PageSidebar = _ref => {
    let {
      className = '',
      nav,
      isNavOpen = true,
      theme = 'light'
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "nav", "isNavOpen", "theme"]);

    return React.createElement(_Page.PageContextConsumer, null, ({
      isManagedSidebar,
      isNavOpen: managedIsNavOpen
    }) => {
      const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
      return React.createElement("div", _extends({
        id: "page-sidebar",
        className: (0, _reactStyles.css)(_page2.default.pageSidebar, theme === 'dark' && _page2.default.modifiers.dark, navOpen && _page2.default.modifiers.expanded, !navOpen && _page2.default.modifiers.collapsed, className)
      }, props), React.createElement("div", {
        className: (0, _reactStyles.css)(_page2.default.pageSidebarBody)
      }, nav));
    });
  };

  PageSidebar.propTypes = {
    className: _propTypes2.default.string,
    nav: _propTypes2.default.node,
    isManagedSidebar: _propTypes2.default.bool,
    isNavOpen: _propTypes2.default.bool,
    theme: _propTypes2.default.oneOf(['dark', 'light'])
  };
});
//# sourceMappingURL=PageSidebar.js.map