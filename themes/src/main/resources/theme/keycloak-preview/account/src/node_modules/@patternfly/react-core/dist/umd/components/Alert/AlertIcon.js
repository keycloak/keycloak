(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Alert/alert", "@patternfly/react-icons/dist/js/icons/check-circle-icon", "@patternfly/react-icons/dist/js/icons/exclamation-circle-icon", "@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon", "@patternfly/react-icons/dist/js/icons/info-circle-icon", "@patternfly/react-icons/dist/js/icons/bell-icon"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Alert/alert"), require("@patternfly/react-icons/dist/js/icons/check-circle-icon"), require("@patternfly/react-icons/dist/js/icons/exclamation-circle-icon"), require("@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon"), require("@patternfly/react-icons/dist/js/icons/info-circle-icon"), require("@patternfly/react-icons/dist/js/icons/bell-icon"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.alert, global.checkCircleIcon, global.exclamationCircleIcon, global.exclamationTriangleIcon, global.infoCircleIcon, global.bellIcon);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _alert, _checkCircleIcon, _exclamationCircleIcon, _exclamationTriangleIcon, _infoCircleIcon, _bellIcon) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.AlertIcon = exports.variantIcons = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _alert2 = _interopRequireDefault(_alert);

  var _checkCircleIcon2 = _interopRequireDefault(_checkCircleIcon);

  var _exclamationCircleIcon2 = _interopRequireDefault(_exclamationCircleIcon);

  var _exclamationTriangleIcon2 = _interopRequireDefault(_exclamationTriangleIcon);

  var _infoCircleIcon2 = _interopRequireDefault(_infoCircleIcon);

  var _bellIcon2 = _interopRequireDefault(_bellIcon);

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

  const variantIcons = exports.variantIcons = {
    success: _checkCircleIcon2.default,
    danger: _exclamationCircleIcon2.default,
    warning: _exclamationTriangleIcon2.default,
    info: _infoCircleIcon2.default,
    default: _bellIcon2.default
  };

  const AlertIcon = exports.AlertIcon = _ref => {
    let {
      variant,
      className = ''
    } = _ref,
        props = _objectWithoutProperties(_ref, ["variant", "className"]);

    const Icon = variantIcons[variant];
    return React.createElement("div", _extends({}, props, {
      className: (0, _reactStyles.css)(_alert2.default.alertIcon, className)
    }), React.createElement(Icon, null));
  };

  AlertIcon.propTypes = {
    variant: _propTypes2.default.oneOf(['success', 'danger', 'warning', 'info', 'default']).isRequired,
    className: _propTypes2.default.string
  };
});
//# sourceMappingURL=AlertIcon.js.map