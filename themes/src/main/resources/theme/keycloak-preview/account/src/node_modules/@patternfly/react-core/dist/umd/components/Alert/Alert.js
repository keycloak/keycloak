(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Alert/alert", "@patternfly/react-styles/css/utilities/Accessibility/accessibility", "./AlertIcon", "../../helpers/util", "../withOuia", "./AlertContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Alert/alert"), require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"), require("./AlertIcon"), require("../../helpers/util"), require("../withOuia"), require("./AlertContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.alert, global.accessibility, global.AlertIcon, global.util, global.withOuia, global.AlertContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _alert, _accessibility, _AlertIcon, _util, _withOuia, _AlertContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Alert = exports.AlertVariant = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _alert2 = _interopRequireDefault(_alert);

  var _accessibility2 = _interopRequireDefault(_accessibility);

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

  let AlertVariant = exports.AlertVariant = undefined;

  (function (AlertVariant) {
    AlertVariant["success"] = "success";
    AlertVariant["danger"] = "danger";
    AlertVariant["warning"] = "warning";
    AlertVariant["info"] = "info";
    AlertVariant["default"] = "default";
  })(AlertVariant || (exports.AlertVariant = AlertVariant = {}));

  const Alert = _ref => {
    let {
      variant = AlertVariant.info,
      isInline = false,
      isLiveRegion = false,
      variantLabel = `${(0, _util.capitalize)(variant)} alert:`,
      'aria-label': ariaLabel = `${(0, _util.capitalize)(variant)} Alert`,
      action = null,
      title,
      children = '',
      className = '',
      ouiaContext = null,
      ouiaId = null
    } = _ref,
        props = _objectWithoutProperties(_ref, ["variant", "isInline", "isLiveRegion", "variantLabel", "aria-label", "action", "title", "children", "className", "ouiaContext", "ouiaId"]);

    const getHeadingContent = React.createElement(React.Fragment, null, React.createElement("span", {
      className: (0, _reactStyles.css)(_accessibility2.default.screenReader)
    }, variantLabel), title);
    const customClassName = (0, _reactStyles.css)(_alert2.default.alert, isInline && _alert2.default.modifiers.inline, variant !== AlertVariant.default && (0, _reactStyles.getModifier)(_alert2.default, variant, _alert2.default.modifiers.info), className);
    return React.createElement("div", _extends({}, props, {
      className: customClassName,
      "aria-label": ariaLabel
    }, ouiaContext.isOuia && {
      'data-ouia-component-type': 'Alert',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    }, isLiveRegion && {
      'aria-live': 'polite',
      'aria-atomic': 'false'
    }), React.createElement(_AlertIcon.AlertIcon, {
      variant: variant
    }), React.createElement("h4", {
      className: (0, _reactStyles.css)(_alert2.default.alertTitle)
    }, getHeadingContent), children && React.createElement("div", {
      className: (0, _reactStyles.css)(_alert2.default.alertDescription)
    }, children), React.createElement(_AlertContext.AlertContext.Provider, {
      value: {
        title,
        variantLabel
      }
    }, action && (typeof action === 'object' || typeof action === 'string') && React.createElement("div", {
      className: (0, _reactStyles.css)(_alert2.default.alertAction)
    }, action)));
  };

  Alert.propTypes = {
    variant: _propTypes2.default.oneOf(['success', 'danger', 'warning', 'info', 'default']),
    isInline: _propTypes2.default.bool,
    title: _propTypes2.default.node.isRequired,
    action: _propTypes2.default.node,
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    'aria-label': _propTypes2.default.string,
    variantLabel: _propTypes2.default.string,
    isLiveRegion: _propTypes2.default.bool
  };
  const AlertWithOuiaContext = (0, _withOuia.withOuiaContext)(Alert);
  exports.Alert = AlertWithOuiaContext;
});
//# sourceMappingURL=Alert.js.map