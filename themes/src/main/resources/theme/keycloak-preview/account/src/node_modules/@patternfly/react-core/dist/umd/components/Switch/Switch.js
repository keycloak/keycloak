(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Switch/switch", "@patternfly/react-styles", "@patternfly/react-icons/dist/js/icons/check-icon", "../../helpers/util", "../withOuia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Switch/switch"), require("@patternfly/react-styles"), require("@patternfly/react-icons/dist/js/icons/check-icon"), require("../../helpers/util"), require("../withOuia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global._switch, global.reactStyles, global.checkIcon, global.util, global.withOuia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _switch, _reactStyles, _checkIcon, _util, _withOuia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Switch = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _switch2 = _interopRequireDefault(_switch);

  var _checkIcon2 = _interopRequireDefault(_checkIcon);

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

  class Switch extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "id", '');

      if (!props.id && !props['aria-label']) {
        // eslint-disable-next-line no-console
        console.error('Switch: Switch requires either an id or aria-label to be specified');
      }

      this.id = props.id || (0, _util.getUniqueId)();
    }

    render() {
      const _this$props = this.props,
            {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        id,
        className,
        label,
        labelOff,
        isChecked,
        isDisabled,
        onChange,
        ouiaContext,
        ouiaId
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["id", "className", "label", "labelOff", "isChecked", "isDisabled", "onChange", "ouiaContext", "ouiaId"]);

      const isAriaLabelledBy = props['aria-label'] === '';
      return React.createElement("label", _extends({
        className: (0, _reactStyles.css)(_switch2.default.switch, className),
        htmlFor: this.id
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Switch',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }), React.createElement("input", _extends({
        id: this.id,
        className: (0, _reactStyles.css)(_switch2.default.switchInput),
        type: "checkbox",
        onChange: event => onChange(event.target.checked, event),
        checked: isChecked,
        disabled: isDisabled,
        "aria-labelledby": isAriaLabelledBy ? `${this.id}-on` : null
      }, props)), label !== '' ? React.createElement(React.Fragment, null, React.createElement("span", {
        className: (0, _reactStyles.css)(_switch2.default.switchToggle)
      }), React.createElement("span", {
        className: (0, _reactStyles.css)(_switch2.default.switchLabel, _switch2.default.modifiers.on),
        id: isAriaLabelledBy ? `${this.id}-on` : null,
        "aria-hidden": "true"
      }, label), React.createElement("span", {
        className: (0, _reactStyles.css)(_switch2.default.switchLabel, _switch2.default.modifiers.off),
        id: isAriaLabelledBy ? `${this.id}-off` : null,
        "aria-hidden": "true"
      }, labelOff || label)) : label !== '' && labelOff !== '' ? React.createElement(React.Fragment, null, React.createElement("span", {
        className: (0, _reactStyles.css)(_switch2.default.switchToggle)
      }), React.createElement("span", {
        className: (0, _reactStyles.css)(_switch2.default.switchLabel, _switch2.default.modifiers.on),
        id: isAriaLabelledBy ? `${this.id}-on` : null,
        "aria-hidden": "true"
      }, label), React.createElement("span", {
        className: (0, _reactStyles.css)(_switch2.default.switchLabel, _switch2.default.modifiers.off),
        id: isAriaLabelledBy ? `${this.id}-off` : null,
        "aria-hidden": "true"
      }, labelOff)) : React.createElement("span", {
        className: (0, _reactStyles.css)(_switch2.default.switchToggle)
      }, React.createElement("div", {
        className: (0, _reactStyles.css)(_switch2.default.switchToggleIcon),
        "aria-hidden": "true"
      }, React.createElement(_checkIcon2.default, {
        noVerticalAlign: true
      }))));
    }

  }

  _defineProperty(Switch, "propTypes", {
    id: _propTypes2.default.string,
    className: _propTypes2.default.string,
    label: _propTypes2.default.string,
    labelOff: _propTypes2.default.string,
    isChecked: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    onChange: _propTypes2.default.func,
    'aria-label': _propTypes2.default.string
  });

  _defineProperty(Switch, "defaultProps", {
    id: '',
    className: '',
    label: '',
    labelOff: '',
    isChecked: true,
    isDisabled: false,
    'aria-label': '',
    onChange: () => undefined
  });

  const SwitchWithOuiaContext = (0, _withOuia.withOuiaContext)(Switch);
  exports.Switch = SwitchWithOuiaContext;
});
//# sourceMappingURL=Switch.js.map