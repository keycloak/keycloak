(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Page/page", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/bars-icon", "../../components/Button", "./Page"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Page/page"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/bars-icon"), require("../../components/Button"), require("./Page"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.page, global.reactStyles, global.barsIcon, global.Button, global.Page);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _page, _reactStyles, _barsIcon, _Button, _Page) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.PageHeader = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _page2 = _interopRequireDefault(_page);

  var _barsIcon2 = _interopRequireDefault(_barsIcon);

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

  const PageHeader = exports.PageHeader = _ref => {
    let {
      className = '',
      logo = null,
      logoProps = null,
      logoComponent = 'a',
      toolbar = null,
      avatar = null,
      topNav = null,
      isNavOpen = true,
      role = undefined,
      showNavToggle = false,
      onNavToggle = () => undefined,
      'aria-label': ariaLabel = 'Global navigation'
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "logo", "logoProps", "logoComponent", "toolbar", "avatar", "topNav", "isNavOpen", "role", "showNavToggle", "onNavToggle", "aria-label"]);

    const LogoComponent = logoComponent;
    return React.createElement(_Page.PageContextConsumer, null, ({
      isManagedSidebar,
      onNavToggle: managedOnNavToggle,
      isNavOpen: managedIsNavOpen
    }) => {
      const navToggle = isManagedSidebar ? managedOnNavToggle : onNavToggle;
      const navOpen = isManagedSidebar ? managedIsNavOpen : isNavOpen;
      return React.createElement("header", _extends({
        role: role,
        className: (0, _reactStyles.css)(_page2.default.pageHeader, className)
      }, props), (showNavToggle || logo) && React.createElement("div", {
        className: (0, _reactStyles.css)(_page2.default.pageHeaderBrand)
      }, showNavToggle && React.createElement("div", {
        className: (0, _reactStyles.css)(_page2.default.pageHeaderBrandToggle)
      }, React.createElement(_Button.Button, {
        id: "nav-toggle",
        onClick: navToggle,
        "aria-label": ariaLabel,
        "aria-controls": "page-sidebar",
        "aria-expanded": navOpen ? 'true' : 'false',
        variant: _Button.ButtonVariant.plain
      }, React.createElement(_barsIcon2.default, null))), logo && React.createElement(LogoComponent, _extends({
        className: (0, _reactStyles.css)(_page2.default.pageHeaderBrandLink)
      }, logoProps), logo)), topNav && React.createElement("div", {
        className: (0, _reactStyles.css)(_page2.default.pageHeaderNav)
      }, topNav), (toolbar || avatar) && React.createElement("div", {
        className: (0, _reactStyles.css)(_page2.default.pageHeaderTools)
      }, toolbar, avatar));
    });
  };

  PageHeader.propTypes = {
    className: _propTypes2.default.string,
    logo: _propTypes2.default.node,
    logoProps: _propTypes2.default.object,
    logoComponent: _propTypes2.default.node,
    toolbar: _propTypes2.default.node,
    avatar: _propTypes2.default.node,
    topNav: _propTypes2.default.node,
    showNavToggle: _propTypes2.default.bool,
    isNavOpen: _propTypes2.default.bool,
    isManagedSidebar: _propTypes2.default.bool,
    role: _propTypes2.default.string,
    onNavToggle: _propTypes2.default.func,
    'aria-label': _propTypes2.default.string
  };
});
//# sourceMappingURL=PageHeader.js.map