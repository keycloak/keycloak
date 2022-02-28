(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Dropdown/dropdown", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Dropdown/dropdown"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.dropdown, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _dropdown, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DropdownToggleCheckbox = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _dropdown2 = _interopRequireDefault(_dropdown);

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

  class DropdownToggleCheckbox extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "handleChange", event => {
        this.props.onChange(event.target.checked, event);
      });

      _defineProperty(this, "calculateChecked", () => {
        const {
          isChecked,
          checked
        } = this.props;
        return isChecked !== undefined ? isChecked : checked;
      });
    }

    render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const _this$props = this.props,
            {
        className,
        onChange,
        isValid,
        isDisabled,
        isChecked,
        ref,
        checked,
        children
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["className", "onChange", "isValid", "isDisabled", "isChecked", "ref", "checked", "children"]);

      const text = children && React.createElement("span", {
        className: (0, _reactStyles.css)(_dropdown2.default.dropdownToggleText, className),
        "aria-hidden": "true",
        id: `${props.id}-text`
      }, children);
      return React.createElement("label", {
        className: (0, _reactStyles.css)(_dropdown2.default.dropdownToggleCheck, className),
        htmlFor: props.id
      }, React.createElement("input", _extends({}, props, this.calculateChecked() !== undefined && {
        onChange: this.handleChange
      }, {
        type: "checkbox",
        ref: ref,
        "aria-invalid": !isValid,
        disabled: isDisabled,
        checked: this.calculateChecked()
      })), text);
    }

  }

  exports.DropdownToggleCheckbox = DropdownToggleCheckbox;

  _defineProperty(DropdownToggleCheckbox, "propTypes", {
    className: _propTypes2.default.string,
    isValid: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    isChecked: _propTypes2.default.oneOfType([_propTypes2.default.bool, _propTypes2.default.oneOf([null])]),
    checked: _propTypes2.default.oneOfType([_propTypes2.default.bool, _propTypes2.default.oneOf([null])]),
    onChange: _propTypes2.default.func,
    children: _propTypes2.default.node,
    id: _propTypes2.default.string.isRequired,
    'aria-label': _propTypes2.default.string.isRequired
  });

  _defineProperty(DropdownToggleCheckbox, "defaultProps", {
    className: '',
    isValid: true,
    isDisabled: false,
    onChange: () => undefined
  });
});
//# sourceMappingURL=DropdownToggleCheckbox.js.map