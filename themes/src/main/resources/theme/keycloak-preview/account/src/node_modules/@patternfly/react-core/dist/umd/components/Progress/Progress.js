(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Progress/progress", "@patternfly/react-styles", "./ProgressContainer", "../../helpers/util"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Progress/progress"), require("@patternfly/react-styles"), require("./ProgressContainer"), require("../../helpers/util"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.progress, global.reactStyles, global.ProgressContainer, global.util);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _progress, _reactStyles, _ProgressContainer, _util) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Progress = exports.ProgressSize = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _progress2 = _interopRequireDefault(_progress);

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

  function ownKeys(object, enumerableOnly) {
    var keys = Object.keys(object);

    if (Object.getOwnPropertySymbols) {
      var symbols = Object.getOwnPropertySymbols(object);
      if (enumerableOnly) symbols = symbols.filter(function (sym) {
        return Object.getOwnPropertyDescriptor(object, sym).enumerable;
      });
      keys.push.apply(keys, symbols);
    }

    return keys;
  }

  function _objectSpread(target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i] != null ? arguments[i] : {};

      if (i % 2) {
        ownKeys(source, true).forEach(function (key) {
          _defineProperty(target, key, source[key]);
        });
      } else if (Object.getOwnPropertyDescriptors) {
        Object.defineProperties(target, Object.getOwnPropertyDescriptors(source));
      } else {
        ownKeys(source).forEach(function (key) {
          Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key));
        });
      }
    }

    return target;
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

  let ProgressSize = exports.ProgressSize = undefined;

  (function (ProgressSize) {
    ProgressSize["sm"] = "sm";
    ProgressSize["md"] = "md";
    ProgressSize["lg"] = "lg";
  })(ProgressSize || (exports.ProgressSize = ProgressSize = {}));

  class Progress extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "id", this.props.id || (0, _util.getUniqueId)());
    }

    render() {
      const _this$props = this.props,
            {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        id,
        className,
        size,
        value,
        title,
        label,
        variant,
        measureLocation,
        min,
        max,
        valueText
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["id", "className", "size", "value", "title", "label", "variant", "measureLocation", "min", "max", "valueText"]);

      const additionalProps = _objectSpread({}, props, {}, valueText ? {
        'aria-valuetext': valueText
      } : {
        'aria-describedby': `${this.id}-description`
      });

      const ariaProps = {
        'aria-describedby': `${this.id}-description`,
        'aria-valuemin': min,
        'aria-valuenow': value,
        'aria-valuemax': max
      };

      if (valueText) {
        ariaProps['aria-valuetext'] = valueText;
      }

      const scaledValue = Math.min(100, Math.max(0, Math.floor((value - min) / (max - min) * 100)));
      return React.createElement("div", _extends({}, additionalProps, {
        className: (0, _reactStyles.css)(_progress2.default.progress, (0, _reactStyles.getModifier)(_progress2.default, variant, ''), (0, _reactStyles.getModifier)(_progress2.default, measureLocation, ''), (0, _reactStyles.getModifier)(_progress2.default, measureLocation === _ProgressContainer.ProgressMeasureLocation.inside ? ProgressSize.lg : size, ''), !title && (0, _reactStyles.getModifier)(_progress2.default, 'singleline', ''), className),
        id: this.id,
        role: "progressbar"
      }), React.createElement(_ProgressContainer.ProgressContainer, {
        parentId: this.id,
        value: scaledValue,
        title: title,
        label: label,
        variant: variant,
        measureLocation: measureLocation,
        ariaProps: ariaProps
      }));
    }

  }

  exports.Progress = Progress;

  _defineProperty(Progress, "propTypes", {
    className: _propTypes2.default.string,
    size: _propTypes2.default.oneOf(['sm', 'md', 'lg']),
    measureLocation: _propTypes2.default.oneOf(['outside', 'inside', 'top', 'none']),
    variant: _propTypes2.default.oneOf(['danger', 'success', 'info']),
    title: _propTypes2.default.string,
    label: _propTypes2.default.node,
    value: _propTypes2.default.number,
    id: _propTypes2.default.string,
    min: _propTypes2.default.number,
    max: _propTypes2.default.number,
    valueText: _propTypes2.default.string
  });

  _defineProperty(Progress, "defaultProps", {
    className: '',
    measureLocation: _ProgressContainer.ProgressMeasureLocation.top,
    variant: _ProgressContainer.ProgressVariant.info,
    id: '',
    title: '',
    min: 0,
    max: 100,
    size: null,
    label: null,
    value: 0,
    valueText: null
  });
});
//# sourceMappingURL=Progress.js.map