(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "./InternalDropdownItem", "./dropdownConstants"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("./InternalDropdownItem"), require("./dropdownConstants"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.InternalDropdownItem, global.dropdownConstants);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _InternalDropdownItem, _dropdownConstants) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.DropdownItem = undefined;

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

  const DropdownItem = exports.DropdownItem = _ref => {
    let {
      children = null,
      className = '',
      component = 'a',
      variant = 'item',
      isDisabled = false,
      isHovered = false,
      href,
      tooltip = null,
      tooltipProps = {},
      listItemClassName,
      onClick,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      ref,
      // Types of Ref are different for React.FC vs React.Component
      additionalChild,
      customChild
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "className", "component", "variant", "isDisabled", "isHovered", "href", "tooltip", "tooltipProps", "listItemClassName", "onClick", "ref", "additionalChild", "customChild"]);

    return React.createElement(_dropdownConstants.DropdownArrowContext.Consumer, null, context => React.createElement(_InternalDropdownItem.InternalDropdownItem, _extends({
      context: context,
      role: "menuitem",
      tabIndex: -1,
      className: className,
      component: component,
      variant: variant,
      isDisabled: isDisabled,
      isHovered: isHovered,
      href: href,
      tooltip: tooltip,
      tooltipProps: tooltipProps,
      listItemClassName: listItemClassName,
      onClick: onClick,
      additionalChild: additionalChild,
      customChild: customChild
    }, props), children));
  };

  DropdownItem.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    listItemClassName: _propTypes2.default.string,
    component: _propTypes2.default.node,
    variant: _propTypes2.default.oneOf(['item', 'icon']),
    isDisabled: _propTypes2.default.bool,
    isHovered: _propTypes2.default.bool,
    href: _propTypes2.default.string,
    tooltip: _propTypes2.default.node,
    tooltipProps: _propTypes2.default.any,
    additionalChild: _propTypes2.default.node,
    customChild: _propTypes2.default.node
  };
});
//# sourceMappingURL=DropdownItem.js.map