(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Dropdown/dropdown", "./dropdownConstants", "./DropdownWithContext"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Dropdown/dropdown"), require("./dropdownConstants"), require("./DropdownWithContext"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.dropdown, global.dropdownConstants, global.DropdownWithContext);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _dropdown, _dropdownConstants, _DropdownWithContext) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Dropdown = undefined;

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

  const Dropdown = exports.Dropdown = _ref => {
    let {
      onSelect,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      ref
    } = _ref,
        props = _objectWithoutProperties(_ref, ["onSelect", "ref"]);

    return React.createElement(_dropdownConstants.DropdownContext.Provider, {
      value: {
        onSelect: event => onSelect && onSelect(event),
        toggleTextClass: _dropdown2.default.dropdownToggleText,
        toggleIconClass: _dropdown2.default.dropdownToggleIcon,
        menuClass: _dropdown2.default.dropdownMenu,
        itemClass: _dropdown2.default.dropdownMenuItem,
        toggleClass: _dropdown2.default.dropdownToggle,
        baseClass: _dropdown2.default.dropdown,
        baseComponent: 'div',
        sectionClass: _dropdown2.default.dropdownGroup,
        sectionTitleClass: _dropdown2.default.dropdownGroupTitle,
        sectionComponent: 'section',
        disabledClass: _dropdown2.default.modifiers.disabled,
        hoverClass: _dropdown2.default.modifiers.hover,
        separatorClass: _dropdown2.default.dropdownSeparator
      }
    }, React.createElement(_DropdownWithContext.DropdownWithContext, props));
  };

  Dropdown.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    dropdownItems: _propTypes2.default.arrayOf(_propTypes2.default.any),
    isOpen: _propTypes2.default.bool,
    isPlain: _propTypes2.default.bool,
    position: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['right']), _propTypes2.default.oneOf(['left'])]),
    direction: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['up']), _propTypes2.default.oneOf(['down'])]),
    isGrouped: _propTypes2.default.bool,
    toggle: _propTypes2.default.element.isRequired,
    onSelect: _propTypes2.default.func,
    autoFocus: _propTypes2.default.bool,
    ouiaComponentType: _propTypes2.default.string
  };
});
//# sourceMappingURL=Dropdown.js.map