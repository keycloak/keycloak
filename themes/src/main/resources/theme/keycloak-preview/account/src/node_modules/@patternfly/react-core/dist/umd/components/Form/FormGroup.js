(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Form/form", "../../helpers/htmlConstants", "./FormContext", "@patternfly/react-styles", "../../helpers/constants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Form/form"), require("../../helpers/htmlConstants"), require("./FormContext"), require("@patternfly/react-styles"), require("../../helpers/constants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.form, global.htmlConstants, global.FormContext, global.reactStyles, global.constants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _form, _htmlConstants, _FormContext, _reactStyles, _constants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.FormGroup = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _form2 = _interopRequireDefault(_form);

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

  const FormGroup = exports.FormGroup = _ref => {
    let {
      children = null,
      className = '',
      label,
      isRequired = false,
      isValid = true,
      validated = 'default',
      isInline = false,
      helperText,
      helperTextInvalid,
      fieldId
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "className", "label", "isRequired", "isValid", "validated", "isInline", "helperText", "helperTextInvalid", "fieldId"]);

    const validHelperText = React.createElement("div", {
      className: (0, _reactStyles.css)(_form2.default.formHelperText, validated === _constants.ValidatedOptions.success && _form2.default.modifiers.success),
      id: `${fieldId}-helper`,
      "aria-live": "polite"
    }, helperText);
    const inValidHelperText = React.createElement("div", {
      className: (0, _reactStyles.css)(_form2.default.formHelperText, _form2.default.modifiers.error),
      id: `${fieldId}-helper`,
      "aria-live": "polite"
    }, helperTextInvalid);
    return React.createElement(_FormContext.FormContext.Consumer, null, ({
      isHorizontal
    }) => React.createElement("div", _extends({}, props, {
      className: (0, _reactStyles.css)(_form2.default.formGroup, isInline ? (0, _reactStyles.getModifier)(_form2.default, 'inline', className) : className)
    }), label && React.createElement("label", {
      className: (0, _reactStyles.css)(_form2.default.formLabel),
      htmlFor: fieldId
    }, React.createElement("span", {
      className: (0, _reactStyles.css)(_form2.default.formLabelText)
    }, label), isRequired && React.createElement("span", {
      className: (0, _reactStyles.css)(_form2.default.formLabelRequired),
      "aria-hidden": "true"
    }, _htmlConstants.ASTERISK)), isHorizontal ? React.createElement("div", {
      className: (0, _reactStyles.css)(_form2.default.formHorizontalGroup)
    }, children) : children, (!isValid || validated === _constants.ValidatedOptions.error) && helperTextInvalid ? inValidHelperText : validated !== _constants.ValidatedOptions.error && helperText ? validHelperText : ''));
  };

  FormGroup.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    label: _propTypes2.default.node,
    isRequired: _propTypes2.default.bool,
    isValid: _propTypes2.default.bool,
    validated: _propTypes2.default.oneOf(['success', 'error', 'default']),
    isInline: _propTypes2.default.bool,
    helperText: _propTypes2.default.node,
    helperTextInvalid: _propTypes2.default.node,
    fieldId: _propTypes2.default.string.isRequired
  };
});
//# sourceMappingURL=FormGroup.js.map