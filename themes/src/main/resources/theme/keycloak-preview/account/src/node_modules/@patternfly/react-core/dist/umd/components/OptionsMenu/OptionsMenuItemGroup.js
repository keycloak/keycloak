(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/OptionsMenu/options-menu", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.optionsMenu, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _optionsMenu, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OptionsMenuItemGroup = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _optionsMenu2 = _interopRequireDefault(_optionsMenu);

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

  const OptionsMenuItemGroup = exports.OptionsMenuItemGroup = _ref => {
    let {
      className = '',
      ariaLabel = '',
      groupTitle = '',
      children = null,
      hasSeparator = false
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "ariaLabel", "groupTitle", "children", "hasSeparator"]);

    return React.createElement("section", _extends({}, props, {
      className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuGroup)
    }), groupTitle && React.createElement("h1", {
      className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuGroupTitle)
    }, groupTitle), React.createElement("ul", {
      className: className,
      "aria-label": ariaLabel
    }, children, hasSeparator && React.createElement("li", {
      className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuSeparator),
      role: "separator"
    })));
  };

  OptionsMenuItemGroup.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    ariaLabel: _propTypes2.default.string,
    groupTitle: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.node]),
    hasSeparator: _propTypes2.default.bool
  };
});
//# sourceMappingURL=OptionsMenuItemGroup.js.map