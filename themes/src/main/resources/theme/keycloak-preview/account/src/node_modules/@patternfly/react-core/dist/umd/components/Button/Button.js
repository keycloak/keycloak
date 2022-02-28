(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Button/button", "@patternfly/react-styles", "../withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Button/button"), require("@patternfly/react-styles"), require("../withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.button, global.reactStyles, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _button, _reactStyles, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Button = exports.ButtonType = exports.ButtonVariant = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _button2 = _interopRequireDefault(_button);

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

  let ButtonVariant = exports.ButtonVariant = undefined;

  (function (ButtonVariant) {
    ButtonVariant["primary"] = "primary";
    ButtonVariant["secondary"] = "secondary";
    ButtonVariant["tertiary"] = "tertiary";
    ButtonVariant["danger"] = "danger";
    ButtonVariant["link"] = "link";
    ButtonVariant["plain"] = "plain";
    ButtonVariant["control"] = "control";
  })(ButtonVariant || (exports.ButtonVariant = ButtonVariant = {}));

  let ButtonType = exports.ButtonType = undefined;

  (function (ButtonType) {
    ButtonType["button"] = "button";
    ButtonType["submit"] = "submit";
    ButtonType["reset"] = "reset";
  })(ButtonType || (exports.ButtonType = ButtonType = {}));

  const Button = _ref => {
    let {
      children = null,
      className = '',
      component = 'button',
      isActive = false,
      isBlock = false,
      isDisabled = false,
      isFocus = false,
      isHover = false,
      isInline = false,
      type = ButtonType.button,
      variant = ButtonVariant.primary,
      iconPosition = 'left',
      'aria-label': ariaLabel = null,
      icon = null,
      ouiaContext = null,
      ouiaId = null,
      tabIndex = null
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "className", "component", "isActive", "isBlock", "isDisabled", "isFocus", "isHover", "isInline", "type", "variant", "iconPosition", "aria-label", "icon", "ouiaContext", "ouiaId", "tabIndex"]);

    const Component = component;
    const isButtonElement = Component === 'button';
    return React.createElement(Component, _extends({}, props, {
      "aria-disabled": isButtonElement ? null : isDisabled,
      "aria-label": ariaLabel,
      className: (0, _reactStyles.css)(_button2.default.button, (0, _reactStyles.getModifier)(_button2.default.modifiers, variant), isBlock && _button2.default.modifiers.block, isDisabled && !isButtonElement && _button2.default.modifiers.disabled, isActive && _button2.default.modifiers.active, isFocus && _button2.default.modifiers.focus, isHover && _button2.default.modifiers.hover, isInline && variant === ButtonVariant.link && _button2.default.modifiers.inline, className),
      disabled: isButtonElement ? isDisabled : null,
      tabIndex: isDisabled && !isButtonElement ? -1 : tabIndex,
      type: isButtonElement ? type : null
    }, ouiaContext.isOuia && {
      'data-ouia-component-type': 'Button',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    }), icon && variant === ButtonVariant.link && iconPosition === 'left' && React.createElement("span", {
      className: "pf-c-button__icon"
    }, icon), variant === ButtonVariant.link && React.createElement("span", {
      className: (0, _reactStyles.css)(_button2.default.buttonText)
    }, children), variant !== ButtonVariant.link && children, icon && variant === ButtonVariant.link && iconPosition === 'right' && React.createElement("span", {
      className: "pf-c-button__icon"
    }, icon));
  };

  Button.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    component: _propTypes2.default.any,
    isActive: _propTypes2.default.bool,
    isBlock: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    isFocus: _propTypes2.default.bool,
    isHover: _propTypes2.default.bool,
    isInline: _propTypes2.default.bool,
    type: _propTypes2.default.oneOf(['button', 'submit', 'reset']),
    variant: _propTypes2.default.oneOf(['primary', 'secondary', 'tertiary', 'danger', 'link', 'plain', 'control']),
    iconPosition: _propTypes2.default.oneOf(['left', 'right']),
    'aria-label': _propTypes2.default.string,
    icon: _propTypes2.default.oneOfType([_propTypes2.default.node, _propTypes2.default.oneOf([null])]),
    tabIndex: _propTypes2.default.number
  };
  const ButtonWithOuiaContext = (0, _withOuia.withOuiaContext)(Button);
  exports.Button = ButtonWithOuiaContext;
});
//# sourceMappingURL=Button.js.map