(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/AppLauncher/app-launcher", "../Dropdown", "./ApplicationLauncherContent", "./ApplicationLauncherContext", "./ApplicationLauncherItemContext", "@patternfly/react-icons/dist/js/icons/star-icon"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"), require("../Dropdown"), require("./ApplicationLauncherContent"), require("./ApplicationLauncherContext"), require("./ApplicationLauncherItemContext"), require("@patternfly/react-icons/dist/js/icons/star-icon"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.appLauncher, global.Dropdown, global.ApplicationLauncherContent, global.ApplicationLauncherContext, global.ApplicationLauncherItemContext, global.starIcon);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _appLauncher, _Dropdown, _ApplicationLauncherContent, _ApplicationLauncherContext, _ApplicationLauncherItemContext, _starIcon) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ApplicationLauncherItem = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _appLauncher2 = _interopRequireDefault(_appLauncher);

  var _starIcon2 = _interopRequireDefault(_starIcon);

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

  const ApplicationLauncherItem = exports.ApplicationLauncherItem = _ref => {
    let {
      className = '',
      id,
      children,
      icon = null,
      isExternal = false,
      href,
      tooltip = null,
      tooltipProps = null,
      component = 'a',
      isFavorite = null,
      ariaIsFavoriteLabel = 'starred',
      ariaIsNotFavoriteLabel = 'not starred',
      customChild,
      enterTriggersArrowDown = false
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "id", "children", "icon", "isExternal", "href", "tooltip", "tooltipProps", "component", "isFavorite", "ariaIsFavoriteLabel", "ariaIsNotFavoriteLabel", "customChild", "enterTriggersArrowDown"]);

    return React.createElement(_ApplicationLauncherItemContext.ApplicationLauncherItemContext.Provider, {
      value: {
        isExternal,
        icon
      }
    }, React.createElement(_ApplicationLauncherContext.ApplicationLauncherContext.Consumer, null, ({
      onFavorite
    }) => React.createElement(_Dropdown.DropdownItem, _extends({
      id: id,
      component: component,
      href: href || null,
      className: (0, _reactStyles.css)(isExternal && _appLauncher2.default.modifiers.external, isFavorite !== null && _appLauncher2.default.modifiers.link, className),
      listItemClassName: (0, _reactStyles.css)(onFavorite && _appLauncher2.default.appLauncherMenuWrapper, isFavorite && _appLauncher2.default.modifiers.favorite),
      tooltip: tooltip,
      tooltipProps: tooltipProps
    }, enterTriggersArrowDown === true && {
      enterTriggersArrowDown
    }, customChild && {
      customChild
    }, isFavorite !== null && {
      additionalChild: React.createElement("button", {
        className: (0, _reactStyles.css)(_appLauncher2.default.appLauncherMenuItem, _appLauncher2.default.modifiers.action),
        "aria-label": isFavorite ? ariaIsFavoriteLabel : ariaIsNotFavoriteLabel,
        onClick: () => {
          onFavorite(id, isFavorite);
        }
      }, React.createElement(_starIcon2.default, null))
    }, props), children && React.createElement(_ApplicationLauncherContent.ApplicationLauncherContent, null, children))));
  };

  ApplicationLauncherItem.propTypes = {
    icon: _propTypes2.default.node,
    isExternal: _propTypes2.default.bool,
    tooltip: _propTypes2.default.node,
    tooltipProps: _propTypes2.default.any,
    component: _propTypes2.default.node,
    isFavorite: _propTypes2.default.bool,
    ariaIsFavoriteLabel: _propTypes2.default.string,
    ariaIsNotFavoriteLabel: _propTypes2.default.string,
    id: _propTypes2.default.string,
    customChild: _propTypes2.default.node,
    enterTriggersArrowDown: _propTypes2.default.bool
  };
});
//# sourceMappingURL=ApplicationLauncherItem.js.map