(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Radio/radio", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Radio/radio"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.radio, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _radio, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Radio = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _radio2 = _interopRequireDefault(_radio);

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

  class Radio extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "handleChange", event => {
        this.props.onChange(event.currentTarget.checked, event);
      });

      if (!props.label && !props['aria-label']) {
        // eslint-disable-next-line no-console
        console.error('Radio:', 'Radio requires an aria-label to be specified');
      }
    }

    render() {
      const _this$props = this.props,
            {
        'aria-label': ariaLabel,
        checked,
        className,
        defaultChecked,
        isLabelWrapped,
        isLabelBeforeButton,
        isChecked,
        isDisabled,
        isValid,
        label,
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        onChange,
        description
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["aria-label", "checked", "className", "defaultChecked", "isLabelWrapped", "isLabelBeforeButton", "isChecked", "isDisabled", "isValid", "label", "onChange", "description"]);

      const inputRendered = React.createElement("input", _extends({}, props, {
        className: (0, _reactStyles.css)(_radio2.default.radioInput),
        type: "radio",
        onChange: this.handleChange,
        "aria-invalid": !isValid,
        disabled: isDisabled,
        checked: checked || isChecked
      }, checked === undefined && {
        defaultChecked
      }, !label && {
        'aria-label': ariaLabel
      }));
      const labelRendered = !label ? null : isLabelWrapped ? React.createElement("span", {
        className: (0, _reactStyles.css)(_radio2.default.radioLabel, (0, _reactStyles.getModifier)(_radio2.default, isDisabled && 'disabled'))
      }, label) : React.createElement("label", {
        className: (0, _reactStyles.css)(_radio2.default.radioLabel, (0, _reactStyles.getModifier)(_radio2.default, isDisabled && 'disabled')),
        htmlFor: props.id
      }, label);
      const descRender = description ? React.createElement("div", {
        className: (0, _reactStyles.css)(_radio2.default.radioDescription)
      }, description) : null;
      const childrenRendered = isLabelBeforeButton ? React.createElement(React.Fragment, null, labelRendered, inputRendered, descRender) : React.createElement(React.Fragment, null, inputRendered, labelRendered, descRender);
      return isLabelWrapped ? React.createElement("label", {
        className: (0, _reactStyles.css)(_radio2.default.radio, className),
        htmlFor: props.id
      }, childrenRendered) : React.createElement("div", {
        className: (0, _reactStyles.css)(_radio2.default.radio, className)
      }, childrenRendered);
    }

  }

  exports.Radio = Radio;

  _defineProperty(Radio, "propTypes", {
    className: _propTypes2.default.string,
    id: _propTypes2.default.string.isRequired,
    isLabelWrapped: _propTypes2.default.bool,
    isLabelBeforeButton: _propTypes2.default.bool,
    checked: _propTypes2.default.bool,
    isChecked: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    isValid: _propTypes2.default.bool,
    label: _propTypes2.default.node,
    name: _propTypes2.default.string.isRequired,
    onChange: _propTypes2.default.func,
    'aria-label': _propTypes2.default.string,
    description: _propTypes2.default.node
  });

  _defineProperty(Radio, "defaultProps", {
    className: '',
    isDisabled: false,
    isValid: true,
    onChange: () => {}
  });
});
//# sourceMappingURL=Radio.js.map