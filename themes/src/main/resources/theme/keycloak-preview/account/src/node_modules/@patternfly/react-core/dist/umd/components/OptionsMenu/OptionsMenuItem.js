(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/OptionsMenu/options-menu", "../Dropdown", "@patternfly/react-icons/dist/js/icons/check-icon"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"), require("../Dropdown"), require("@patternfly/react-icons/dist/js/icons/check-icon"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.optionsMenu, global.Dropdown, global.checkIcon);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _optionsMenu, _Dropdown, _checkIcon) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OptionsMenuItem = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _optionsMenu2 = _interopRequireDefault(_optionsMenu);

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

  const OptionsMenuItem = exports.OptionsMenuItem = _ref => {
    let {
      children = null,
      isSelected = false,
      onSelect = () => null,
      id = '',
      isDisabled
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "isSelected", "onSelect", "id", "isDisabled"]);

    return React.createElement(_Dropdown.DropdownItem, _extends({
      id: id,
      component: "button",
      isDisabled: isDisabled,
      onClick: event => onSelect(event)
    }, isDisabled && {
      'aria-disabled': true
    }, props), children, isSelected && React.createElement(_checkIcon2.default, {
      className: (0, _reactStyles.css)(_optionsMenu2.default.optionsMenuMenuItemIcon),
      "aria-hidden": isSelected
    }));
  };

  OptionsMenuItem.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    isSelected: _propTypes2.default.bool,
    isDisabled: _propTypes2.default.bool,
    onSelect: _propTypes2.default.func,
    id: _propTypes2.default.string
  };
});
//# sourceMappingURL=OptionsMenuItem.js.map