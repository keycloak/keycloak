(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-icons/dist/js/icons/ellipsis-v-icon", "./Toggle"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-icons/dist/js/icons/ellipsis-v-icon"), require("./Toggle"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.ellipsisVIcon, global.Toggle);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _ellipsisVIcon, _Toggle) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.KebabToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _ellipsisVIcon2 = _interopRequireDefault(_ellipsisVIcon);

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

  const KebabToggle = exports.KebabToggle = _ref => {
    let {
      id = '',
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      children = null,
      className = '',
      isOpen = false,
      'aria-label': ariaLabel = 'Actions',
      parentRef = null,
      isFocused = false,
      isHovered = false,
      isActive = false,
      isPlain = false,
      isDisabled = false,
      bubbleEvent = false,
      onToggle = () => undefined,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      ref
    } = _ref,
        props = _objectWithoutProperties(_ref, ["id", "children", "className", "isOpen", "aria-label", "parentRef", "isFocused", "isHovered", "isActive", "isPlain", "isDisabled", "bubbleEvent", "onToggle", "ref"]);

    return React.createElement(_Toggle.Toggle, _extends({
      id: id,
      className: className,
      isOpen: isOpen,
      "aria-label": ariaLabel,
      parentRef: parentRef,
      isFocused: isFocused,
      isHovered: isHovered,
      isActive: isActive,
      isPlain: isPlain,
      isDisabled: isDisabled,
      onToggle: onToggle,
      bubbleEvent: bubbleEvent
    }, props), React.createElement(_ellipsisVIcon2.default, null));
  };

  KebabToggle.propTypes = {
    id: _propTypes2.default.string,
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool,
    'aria-label': _propTypes2.default.string,
    onToggle: _propTypes2.default.func,
    parentRef: _propTypes2.default.any,
    isFocused: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    isActive: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    isPlain: _propTypes2.default.bool,
    type: _propTypes2.default.oneOf(['button', 'submit', 'reset']),
    bubbleEvent: _propTypes2.default.bool
  };
});
//# sourceMappingURL=KebabToggle.js.map