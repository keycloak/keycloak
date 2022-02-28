(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/layouts/Grid/grid", "@patternfly/react-styles", "../../styles/sizes"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/layouts/Grid/grid"), require("@patternfly/react-styles"), require("../../styles/sizes"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.grid, global.reactStyles, global.sizes);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _grid, _reactStyles, _sizes) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Grid = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _grid2 = _interopRequireDefault(_grid);

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

  const Grid = exports.Grid = _ref => {
    let {
      children = null,
      className = '',
      gutter = null,
      span = null
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "className", "gutter", "span"]);

    const classes = [_grid2.default.grid, span && (0, _reactStyles.getModifier)(_grid2.default, `all_${span}Col`)];
    Object.entries(_sizes.DeviceSizes).forEach(([propKey, gridSpanModifier]) => {
      const key = propKey;
      const propValue = props[key];

      if (propValue) {
        classes.push((0, _reactStyles.getModifier)(_grid2.default, `all_${propValue}ColOn${gridSpanModifier}`));
      }

      delete props[key];
    });
    return React.createElement("div", _extends({
      className: (0, _reactStyles.css)(...classes, gutter && _grid2.default.modifiers.gutter, className)
    }, props), children);
  };

  Grid.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    gutter: _propTypes2.default.oneOf(['sm', 'md', 'lg']),
    span: _propTypes2.default.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
    sm: _propTypes2.default.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
    md: _propTypes2.default.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
    lg: _propTypes2.default.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
    xl: _propTypes2.default.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]),
    xl2: _propTypes2.default.oneOf([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
  };
});
//# sourceMappingURL=Grid.js.map