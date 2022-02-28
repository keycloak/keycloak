(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Check/check", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Check/check"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.check, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _check, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Checkbox = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _check2 = _interopRequireDefault(_check);

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

  // tslint:disable-next-line:no-empty
  const defaultOnChange = () => {};

  class Checkbox extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "handleChange", event => {
        this.props.onChange(event.currentTarget.checked, event);
      });
    }

    render() {
      const _this$props = this.props,
            {
        'aria-label': ariaLabel,
        className,
        onChange,
        isValid,
        isDisabled,
        isChecked,
        label,
        checked,
        defaultChecked,
        description
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["aria-label", "className", "onChange", "isValid", "isDisabled", "isChecked", "label", "checked", "defaultChecked", "description"]);

      const checkedProps = {};

      if ([true, false].includes(checked) || isChecked === true) {
        checkedProps.checked = checked || isChecked;
      }

      if (onChange !== defaultOnChange) {
        checkedProps.checked = isChecked;
      }

      if ([false, true].includes(defaultChecked)) {
        checkedProps.defaultChecked = defaultChecked;
      }

      checkedProps.checked = checkedProps.checked === null ? false : checkedProps.checked;
      return React.createElement("div", {
        className: (0, _reactStyles.css)(_check2.default.check, className)
      }, React.createElement("input", _extends({}, props, {
        className: (0, _reactStyles.css)(_check2.default.checkInput),
        type: "checkbox",
        onChange: this.handleChange,
        "aria-invalid": !isValid,
        "aria-label": ariaLabel,
        disabled: isDisabled,
        ref: elem => elem && (elem.indeterminate = isChecked === null)
      }, checkedProps)), label && React.createElement("label", {
        className: (0, _reactStyles.css)(_check2.default.checkLabel, isDisabled ? (0, _reactStyles.getModifier)(_check2.default, 'disabled') : ''),
        htmlFor: props.id
      }, label), description && React.createElement("div", {
        className: (0, _reactStyles.css)(_check2.default.checkDescription)
      }, description));
    }

  }

  exports.Checkbox = Checkbox;

  _defineProperty(Checkbox, "propTypes", {
    className: _propTypes2.default.string,
    isValid: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    isChecked: _propTypes2.default.bool,
    checked: _propTypes2.default.bool,
    onChange: _propTypes2.default.func,
    label: _propTypes2.default.node,
    id: _propTypes2.default.string.isRequired,
    'aria-label': _propTypes2.default.string,
    description: _propTypes2.default.node
  });

  _defineProperty(Checkbox, "defaultProps", {
    className: '',
    isValid: true,
    isDisabled: false,
    isChecked: false,
    onChange: defaultOnChange
  });
});
//# sourceMappingURL=Checkbox.js.map