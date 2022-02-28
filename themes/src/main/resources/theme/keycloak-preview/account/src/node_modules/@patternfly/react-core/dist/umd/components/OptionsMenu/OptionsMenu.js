(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/OptionsMenu/options-menu", "../Dropdown", "../Dropdown/DropdownWithContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"), require("../Dropdown"), require("../Dropdown/DropdownWithContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.optionsMenu, global.Dropdown, global.DropdownWithContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _optionsMenu, _Dropdown, _DropdownWithContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OptionsMenu = exports.OptionsMenuDirection = exports.OptionsMenuPosition = undefined;

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

  let OptionsMenuPosition = exports.OptionsMenuPosition = undefined;

  (function (OptionsMenuPosition) {
    OptionsMenuPosition["right"] = "right";
    OptionsMenuPosition["left"] = "left";
  })(OptionsMenuPosition || (exports.OptionsMenuPosition = OptionsMenuPosition = {}));

  let OptionsMenuDirection = exports.OptionsMenuDirection = undefined;

  (function (OptionsMenuDirection) {
    OptionsMenuDirection["up"] = "up";
    OptionsMenuDirection["down"] = "down";
  })(OptionsMenuDirection || (exports.OptionsMenuDirection = OptionsMenuDirection = {}));

  const OptionsMenu = exports.OptionsMenu = _ref => {
    let {
      className = '',
      menuItems,
      toggle,
      isText = false,
      isGrouped = false,
      id,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      ref
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "menuItems", "toggle", "isText", "isGrouped", "id", "ref"]);

    return React.createElement(_Dropdown.DropdownContext.Provider, {
      value: {
        id,
        onSelect: () => undefined,
        toggleIconClass: _optionsMenu2.default.optionsMenuToggleIcon,
        toggleTextClass: _optionsMenu2.default.optionsMenuToggleText,
        menuClass: _optionsMenu2.default.optionsMenuMenu,
        itemClass: _optionsMenu2.default.optionsMenuMenuItem,
        toggleClass: isText ? _optionsMenu2.default.optionsMenuToggleButton : _optionsMenu2.default.optionsMenuToggle,
        baseClass: _optionsMenu2.default.optionsMenu,
        disabledClass: _optionsMenu2.default.modifiers.disabled,
        menuComponent: isGrouped ? 'div' : 'ul',
        baseComponent: 'div'
      }
    }, React.createElement(_DropdownWithContext.DropdownWithContext, _extends({}, props, {
      id: id,
      dropdownItems: menuItems,
      className: className,
      isGrouped: isGrouped,
      toggle: toggle
    })));
  };

  OptionsMenu.propTypes = {
    className: _propTypes2.default.string,
    id: _propTypes2.default.string.isRequired,
    menuItems: _propTypes2.default.arrayOf(_propTypes2.default.node).isRequired,
    toggle: _propTypes2.default.element.isRequired,
    isPlain: _propTypes2.default.bool,
    isOpen: _propTypes2.default.bool,
    isText: _propTypes2.default.bool,
    isGrouped: _propTypes2.default.bool,
    ariaLabelMenu: _propTypes2.default.string,
    position: _propTypes2.default.oneOf(['right', 'left']),
    direction: _propTypes2.default.oneOf(['up', 'down'])
  };
});
//# sourceMappingURL=OptionsMenu.js.map