(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/List/list", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/List/list"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.list, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _list, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.List = exports.ListComponent = exports.ListVariant = exports.OrderType = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _list2 = _interopRequireDefault(_list);

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

  let OrderType = exports.OrderType = undefined;

  (function (OrderType) {
    OrderType["number"] = "1";
    OrderType["lowercaseLetter"] = "a";
    OrderType["uppercaseLetter"] = "A";
    OrderType["lowercaseRomanNumber"] = "i";
    OrderType["uppercaseRomanNumber"] = "I";
  })(OrderType || (exports.OrderType = OrderType = {}));

  let ListVariant = exports.ListVariant = undefined;

  (function (ListVariant) {
    ListVariant["inline"] = "inline";
  })(ListVariant || (exports.ListVariant = ListVariant = {}));

  let ListComponent = exports.ListComponent = undefined;

  (function (ListComponent) {
    ListComponent["ol"] = "ol";
    ListComponent["ul"] = "ul";
  })(ListComponent || (exports.ListComponent = ListComponent = {}));

  const List = exports.List = _ref => {
    let {
      className = '',
      children = null,
      variant = null,
      type = OrderType.number,
      ref = null,
      component = ListComponent.ul
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "children", "variant", "type", "ref", "component"]);

    return component === ListComponent.ol ? React.createElement("ol", _extends({
      ref: ref,
      type: type
    }, props, {
      className: (0, _reactStyles.css)(_list2.default.list, variant && (0, _reactStyles.getModifier)(_list2.default.modifiers, variant), className)
    }), children) : React.createElement("ul", _extends({
      ref: ref
    }, props, {
      className: (0, _reactStyles.css)(_list2.default.list, variant && (0, _reactStyles.getModifier)(_list2.default.modifiers, variant), className)
    }), children);
  };

  List.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    variant: _propTypes2.default.any,
    type: _propTypes2.default.any,
    component: _propTypes2.default.oneOf(['ol', 'ul'])
  };
});
//# sourceMappingURL=List.js.map