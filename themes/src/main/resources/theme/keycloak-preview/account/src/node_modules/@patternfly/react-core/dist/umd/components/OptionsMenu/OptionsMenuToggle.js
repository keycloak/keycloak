(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "../Dropdown"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("../Dropdown"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.Dropdown);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _Dropdown) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OptionsMenuToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

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

  const OptionsMenuToggle = exports.OptionsMenuToggle = _ref => {
    let {
      isPlain = false,
      isHovered = false,
      isActive = false,
      isFocused = false,
      isDisabled = false,
      isOpen = false,
      parentId = '',
      toggleTemplate = React.createElement(React.Fragment, null),
      hideCaret = false,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      isSplitButton = false,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      type,
      'aria-label': ariaLabel = 'Options menu'
    } = _ref,
        props = _objectWithoutProperties(_ref, ["isPlain", "isHovered", "isActive", "isFocused", "isDisabled", "isOpen", "parentId", "toggleTemplate", "hideCaret", "isSplitButton", "type", "aria-label"]);

    return React.createElement(_Dropdown.DropdownContext.Consumer, null, ({
      id: contextId
    }) => React.createElement(_Dropdown.DropdownToggle, _extends({}, (isPlain || hideCaret) && {
      iconComponent: null
    }, props, {
      isPlain: isPlain,
      isOpen: isOpen,
      isDisabled: isDisabled,
      isHovered: isHovered,
      isActive: isActive,
      isFocused: isFocused,
      id: parentId ? `${parentId}-toggle` : `${contextId}-toggle`,
      ariaHasPopup: "listbox",
      "aria-label": ariaLabel,
      "aria-expanded": isOpen
    }, toggleTemplate ? {
      children: toggleTemplate
    } : {})));
  };

  OptionsMenuToggle.propTypes = {
    parentId: _propTypes2.default.string,
    onToggle: _propTypes2.default.func,
    isOpen: _propTypes2.default.bool,
    isPlain: _propTypes2.default.bool,
    isFocused: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    isSplitButton: _propTypes2.default.bool,
    isActive: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    hideCaret: _propTypes2.default.bool,
    'aria-label': _propTypes2.default.string,
    onEnter: _propTypes2.default.func,
    parentRef: _propTypes2.default.any,
    toggleTemplate: _propTypes2.default.node
  };
});
//# sourceMappingURL=OptionsMenuToggle.js.map