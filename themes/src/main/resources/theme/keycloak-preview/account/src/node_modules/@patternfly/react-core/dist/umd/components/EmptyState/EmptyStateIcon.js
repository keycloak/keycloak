(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/EmptyState/empty-state"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/EmptyState/empty-state"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.emptyState);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _emptyState) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.EmptyStateIcon = exports.IconSize = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _emptyState2 = _interopRequireDefault(_emptyState);

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

  let IconSize = exports.IconSize = undefined;

  (function (IconSize) {
    IconSize["sm"] = "sm";
    IconSize["md"] = "md";
    IconSize["lg"] = "lg";
    IconSize["xl"] = "xl";
  })(IconSize || (exports.IconSize = IconSize = {}));

  const EmptyStateIcon = exports.EmptyStateIcon = _ref => {
    let {
      className = '',
      icon: IconComponent = null,
      component: AnyComponent = null,
      variant = 'icon'
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "icon", "component", "variant"]);

    const classNames = (0, _reactStyles.css)(_emptyState2.default.emptyStateIcon, className);
    return variant === 'icon' ? React.createElement(IconComponent, _extends({
      className: classNames
    }, props, {
      "aria-hidden": "true"
    })) : React.createElement("div", {
      className: classNames
    }, React.createElement(AnyComponent, null));
  };

  EmptyStateIcon.propTypes = {
    color: _propTypes2.default.string,
    size: _propTypes2.default.oneOf(['sm', 'md', 'lg', 'xl']),
    title: _propTypes2.default.string,
    className: _propTypes2.default.string,
    icon: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.any]),
    component: _propTypes2.default.any,
    variant: _propTypes2.default.oneOf(['icon', 'container'])
  };
});
//# sourceMappingURL=EmptyStateIcon.js.map