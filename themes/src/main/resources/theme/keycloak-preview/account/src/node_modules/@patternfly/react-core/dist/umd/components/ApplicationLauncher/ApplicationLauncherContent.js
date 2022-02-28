(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/AppLauncher/app-launcher", "@patternfly/react-styles/css/utilities/Accessibility/accessibility", "./ApplicationLauncherIcon", "./ApplicationLauncherText", "@patternfly/react-icons/dist/js/icons/external-link-alt-icon", "./ApplicationLauncherItemContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"), require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"), require("./ApplicationLauncherIcon"), require("./ApplicationLauncherText"), require("@patternfly/react-icons/dist/js/icons/external-link-alt-icon"), require("./ApplicationLauncherItemContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.appLauncher, global.accessibility, global.ApplicationLauncherIcon, global.ApplicationLauncherText, global.externalLinkAltIcon, global.ApplicationLauncherItemContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _appLauncher, _accessibility, _ApplicationLauncherIcon, _ApplicationLauncherText, _externalLinkAltIcon, _ApplicationLauncherItemContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ApplicationLauncherContent = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _appLauncher2 = _interopRequireDefault(_appLauncher);

  var _accessibility2 = _interopRequireDefault(_accessibility);

  var _externalLinkAltIcon2 = _interopRequireDefault(_externalLinkAltIcon);

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

  const ApplicationLauncherContent = exports.ApplicationLauncherContent = ({
    children
  }) => React.createElement(_ApplicationLauncherItemContext.ApplicationLauncherItemContext.Consumer, null, ({
    isExternal,
    icon
  }) => React.createElement(React.Fragment, null, icon && React.createElement(_ApplicationLauncherIcon.ApplicationLauncherIcon, null, icon), icon ? React.createElement(_ApplicationLauncherText.ApplicationLauncherText, null, children) : children, isExternal && React.createElement(React.Fragment, null, React.createElement("span", {
    className: (0, _reactStyles.css)(_appLauncher2.default.appLauncherMenuItemExternalIcon)
  }, React.createElement(_externalLinkAltIcon2.default, null)), React.createElement("span", {
    className: (0, _reactStyles.css)(_accessibility2.default.screenReader)
  }, "(opens new window)"))));

  ApplicationLauncherContent.propTypes = {
    children: _propTypes2.default.node.isRequired
  };
});
//# sourceMappingURL=ApplicationLauncherContent.js.map