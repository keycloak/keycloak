(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/OptionsMenu/options-menu"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.optionsMenu);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _optionsMenu) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OptionsMenuToggleWithText = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _optionsMenu2 = _interopRequireDefault(_optionsMenu);

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

  const OptionsMenuToggleWithText = exports.OptionsMenuToggleWithText = _ref => {
    let {
      parentId = '',
      toggleText,
      toggleTextClassName = '',
      toggleButtonContents,
      toggleButtonContentsClassName = '',
      onToggle = () => null,
      isOpen = false,
      isPlain = false,
      isHovered = false,
      isActive = false,
      isFocused = false,
      isDisabled = false,

      /* eslint-disable @typescript-eslint/no-unused-vars */
      ariaHasPopup,
      parentRef,
      onEnter,

      /* eslint-enable @typescript-eslint/no-unused-vars */
      'aria-label': ariaLabel = 'Options menu'
    } = _ref,
        props = _objectWithoutProperties(_ref, ["parentId", "toggleText", "toggleTextClassName", "toggleButtonContents", "toggleButtonContentsClassName", "onToggle", "isOpen", "isPlain", "isHovered", "isActive", "isFocused", "isDisabled", "ariaHasPopup", "parentRef", "onEnter", "aria-label"]);

    return React.createElement("div", _extends({
      className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuToggle, (0, _reactStyles.getModifier)(_optionsMenu2.default, 'text'), isPlain && (0, _reactStyles.getModifier)(_optionsMenu2.default, 'plain'), isHovered && (0, _reactStyles.getModifier)(_optionsMenu2.default, 'hover'), isActive && (0, _reactStyles.getModifier)(_optionsMenu2.default, 'active'), isFocused && (0, _reactStyles.getModifier)(_optionsMenu2.default, 'focus'), isDisabled && (0, _reactStyles.getModifier)(_optionsMenu2.default, 'disabled'))
    }, props), React.createElement("span", {
      className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuToggleText, toggleTextClassName)
    }, toggleText), React.createElement("button", {
      className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuToggleButton, toggleButtonContentsClassName),
      id: `${parentId}-toggle`,
      "aria-haspopup": "listbox",
      "aria-label": ariaLabel,
      "aria-expanded": isOpen,
      onClick: () => onToggle(!isOpen)
    }, toggleButtonContents));
  };

  OptionsMenuToggleWithText.propTypes = {
    parentId: _propTypes2.default.string,
    toggleText: _propTypes2.default.node.isRequired,
    toggleTextClassName: _propTypes2.default.string,
    toggleButtonContents: _propTypes2.default.node,
    toggleButtonContentsClassName: _propTypes2.default.string,
    onToggle: _propTypes2.default.func,
    onEnter: _propTypes2.default.func,
    isOpen: _propTypes2.default.bool,
    isPlain: _propTypes2.default.bool,
    isFocused: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    isActive: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    parentRef: _propTypes2.default.any,
    ariaHasPopup: _propTypes2.default.oneOfType([_propTypes2.default.bool, _propTypes2.default.oneOf(['dialog']), _propTypes2.default.oneOf(['menu']), _propTypes2.default.oneOf(['false']), _propTypes2.default.oneOf(['true']), _propTypes2.default.oneOf(['listbox']), _propTypes2.default.oneOf(['tree']), _propTypes2.default.oneOf(['grid'])]),
    'aria-label': _propTypes2.default.string
  };
});
//# sourceMappingURL=OptionsMenuToggleWithText.js.map