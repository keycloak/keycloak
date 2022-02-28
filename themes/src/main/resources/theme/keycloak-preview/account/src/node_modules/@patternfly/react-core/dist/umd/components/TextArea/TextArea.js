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
  exports.TextArea = exports.TextAreResizeOrientation = undefined;

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

  let TextAreResizeOrientation = exports.TextAreResizeOrientation = undefined;

  (function (TextAreResizeOrientation) {
    TextAreResizeOrientation["horizontal"] = "horizontal";
    TextAreResizeOrientation["vertical"] = "vertical";
    TextAreResizeOrientation["both"] = "both";
  })(TextAreResizeOrientation || (exports.TextAreResizeOrientation = TextAreResizeOrientation = {}));

  class TextArea extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "handleChange", event => {
        if (this.props.onChange) {
          this.props.onChange(event.currentTarget.value, event);
        }
      });

      if (!props.id && !props['aria-label']) {
        // eslint-disable-next-line no-console
        console.error('TextArea: TextArea requires either an id or aria-label to be specified');
      }
    }

    render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const _this$props = this.props,
            {
        className,
        value,
        onChange,
        isValid,
        validated,
        isRequired,
        resizeOrientation
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["className", "value", "onChange", "isValid", "validated", "isRequired", "resizeOrientation"]);

      const orientation = 'resize' + resizeOrientation.charAt(0).toUpperCase() + resizeOrientation.slice(1);
      return React.createElement("textarea", _extends({
        className: (0, _reactStyles.css)(_formControl2.default.formControl, className, resizeOrientation !== TextAreResizeOrientation.both && (0, _reactStyles.getModifier)(_formControl2.default, orientation), validated === _constants.ValidatedOptions.success && _formControl2.default.modifiers.success),
        onChange: this.handleChange
      }, typeof this.props.defaultValue !== 'string' && {
        value
      }, {
        "aria-invalid": !isValid || validated === _constants.ValidatedOptions.error,
        required: isRequired
      }, props));
    }

  }

  exports.TextArea = TextArea;

  _defineProperty(TextArea, "propTypes", {
    className: _propTypes2.default.string,
    isRequired: _propTypes2.default.bool,
    isValid: _propTypes2.default.bool,
    validated: _propTypes2.default.oneOf(['success', 'error', 'default']),
    value: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]),
    onChange: _propTypes2.default.func,
    resizeOrientation: _propTypes2.default.oneOf(['horizontal', 'vertical', 'both']),
    'aria-label': _propTypes2.default.string
  });

  _defineProperty(TextArea, "defaultProps", {
    className: '',
    isRequired: false,
    isValid: true,
    validated: 'default',
    resizeOrientation: 'both',
    'aria-label': null
  });
});
//# sourceMappingURL=TextArea.js.map