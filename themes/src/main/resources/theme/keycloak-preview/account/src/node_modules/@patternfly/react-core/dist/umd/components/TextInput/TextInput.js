(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/FormControl/form-control", "@patternfly/react-styles", "../../helpers/constants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/FormControl/form-control"), require("@patternfly/react-styles"), require("../../helpers/constants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.formControl, global.reactStyles, global.constants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _formControl, _reactStyles, _constants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.TextInput = exports.TextInputBase = exports.TextInputTypes = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _formControl2 = _interopRequireDefault(_formControl);

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

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  let TextInputTypes = exports.TextInputTypes = undefined;

  (function (TextInputTypes) {
    TextInputTypes["text"] = "text";
    TextInputTypes["date"] = "date";
    TextInputTypes["datetimeLocal"] = "datetime-local";
    TextInputTypes["email"] = "email";
    TextInputTypes["month"] = "month";
    TextInputTypes["number"] = "number";
    TextInputTypes["password"] = "password";
    TextInputTypes["search"] = "search";
    TextInputTypes["tel"] = "tel";
    TextInputTypes["time"] = "time";
    TextInputTypes["url"] = "url";
  })(TextInputTypes || (exports.TextInputTypes = TextInputTypes = {}));

  class TextInputBase extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "handleChange", event => {
        if (this.props.onChange) {
          this.props.onChange(event.currentTarget.value, event);
        }
      });

      if (!props.id && !props['aria-label'] && !props['aria-labelledby']) {
        // eslint-disable-next-line no-console
        console.error('Text input:', 'Text input requires either an id or aria-label to be specified');
      }
    }

    render() {
      const _this$props = this.props,
            {
        innerRef,
        className,
        type,
        value,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onChange,
        isValid,
        validated,
        isReadOnly,
        isRequired,
        isDisabled
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["innerRef", "className", "type", "value", "onChange", "isValid", "validated", "isReadOnly", "isRequired", "isDisabled"]);

      return React.createElement("input", _extends({}, props, {
        className: (0, _reactStyles.css)(_formControl2.default.formControl, validated === _constants.ValidatedOptions.success && _formControl2.default.modifiers.success, className),
        onChange: this.handleChange,
        type: type,
        value: value,
        "aria-invalid": !isValid || validated === _constants.ValidatedOptions.error,
        required: isRequired,
        disabled: isDisabled,
        readOnly: isReadOnly,
        ref: innerRef
      }));
    }

  }

  exports.TextInputBase = TextInputBase;

  _defineProperty(TextInputBase, "propTypes", {
    className: _propTypes2.default.string,
    isDisabled: _propTypes2.default.bool,
    isReadOnly: _propTypes2.default.bool,
    isRequired: _propTypes2.default.bool,
    isValid: _propTypes2.default.bool,
    validated: _propTypes2.default.oneOf(['success', 'error', 'default']),
    onChange: _propTypes2.default.func,
    type: _propTypes2.default.oneOf(['text', 'date', 'datetime-local', 'email', 'month', 'number', 'password', 'search', 'tel', 'time', 'url']),
    value: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]),
    'aria-label': _propTypes2.default.string,
    innerRef: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.func, _propTypes2.default.object])
  });

  _defineProperty(TextInputBase, "defaultProps", {
    'aria-label': null,
    className: '',
    isRequired: false,
    isValid: true,
    validated: 'default',
    isDisabled: false,
    isReadOnly: false,
    type: TextInputTypes.text,
    onChange: () => undefined
  });

  const TextInput = exports.TextInput = React.forwardRef((props, ref) => React.createElement(TextInputBase, _extends({}, props, {
    innerRef: ref
  })));
});
//# sourceMappingURL=TextInput.js.map