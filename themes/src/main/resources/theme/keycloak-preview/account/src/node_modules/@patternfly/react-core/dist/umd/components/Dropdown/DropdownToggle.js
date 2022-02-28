(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-icons/dist/js/icons/caret-down-icon", "./Toggle", "@patternfly/react-styles/css/components/Dropdown/dropdown", "./dropdownConstants", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-icons/dist/js/icons/caret-down-icon"), require("./Toggle"), require("@patternfly/react-styles/css/components/Dropdown/dropdown"), require("./dropdownConstants"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.caretDownIcon, global.Toggle, global.dropdown, global.dropdownConstants, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _caretDownIcon, _Toggle, _dropdown, _dropdownConstants, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DropdownToggle = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _caretDownIcon2 = _interopRequireDefault(_caretDownIcon);

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

  const DropdownToggle = exports.DropdownToggle = _ref => {
    let {
      id = '',
      children = null,
      className = '',
      isOpen = false,
      parentRef = null,
      isFocused = false,
      isHovered = false,
      isActive = false,
      isDisabled = false,
      isPlain = false,
      isPrimary = false,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onToggle = _isOpen => undefined,
      iconComponent: IconComponent = _caretDownIcon2.default,
      splitButtonItems,
      splitButtonVariant = 'checkbox',
      ariaHasPopup,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      ref
    } = _ref,
        props = _objectWithoutProperties(_ref, ["id", "children", "className", "isOpen", "parentRef", "isFocused", "isHovered", "isActive", "isDisabled", "isPlain", "isPrimary", "onToggle", "iconComponent", "splitButtonItems", "splitButtonVariant", "ariaHasPopup", "ref"]);

    const toggle = React.createElement(_dropdownConstants.DropdownContext.Consumer, null, ({
      toggleTextClass,
      toggleIconClass
    }) => React.createElement(_Toggle.Toggle, _extends({}, props, {
      id: id,
      className: className,
      isOpen: isOpen,
      parentRef: parentRef,
      isFocused: isFocused,
      isHovered: isHovered,
      isActive: isActive,
      isDisabled: isDisabled,
      isPlain: isPlain,
      isPrimary: isPrimary,
      onToggle: onToggle,
      ariaHasPopup: ariaHasPopup
    }, splitButtonItems && {
      isSplitButton: true,
      'aria-label': props['aria-label'] || 'Select'
    }), children && React.createElement("span", {
      className: IconComponent && (0, _reactStyles.css)(toggleTextClass)
    }, children), IconComponent && React.createElement(IconComponent, {
      className: (0, _reactStyles.css)(children && toggleIconClass)
    })));

    if (splitButtonItems) {
      return React.createElement("div", {
        className: (0, _reactStyles.css)(_dropdown2.default.dropdownToggle, _dropdown2.default.modifiers.splitButton, splitButtonVariant === 'action' && _dropdown2.default.modifiers.action, isDisabled && _dropdown2.default.modifiers.disabled)
      }, splitButtonItems, toggle);
    }

    return toggle;
  };

  DropdownToggle.propTypes = {
    id: _propTypes2.default.string,
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool,
    onToggle: _propTypes2.default.func,
    parentRef: _propTypes2.default.any,
    isFocused: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    isActive: _propTypes2.default.bool,
    isPlain: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    isPrimary: _propTypes2.default.bool,
    iconComponent: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf([null])]),
    splitButtonItems: _propTypes2.default.arrayOf(_propTypes2.default.node),
    splitButtonVariant: _propTypes2.default.oneOf(['action', 'checkbox']),
    'aria-label': _propTypes2.default.string,
    ariaHasPopup: _propTypes2.default.oneOfType([_propTypes2.default.bool, _propTypes2.default.oneOf(['listbox']), _propTypes2.default.oneOf(['menu']), _propTypes2.default.oneOf(['dialog']), _propTypes2.default.oneOf(['grid']), _propTypes2.default.oneOf(['listbox']), _propTypes2.default.oneOf(['tree'])]),
    type: _propTypes2.default.oneOf(['button', 'submit', 'reset']),
    onEnter: _propTypes2.default.func
  };
});
//# sourceMappingURL=DropdownToggle.js.map