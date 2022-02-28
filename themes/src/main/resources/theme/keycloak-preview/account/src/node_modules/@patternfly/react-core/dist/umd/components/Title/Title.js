(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Title/title"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Title/title"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.title);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _title) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Title = exports.TitleLevel = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _title2 = _interopRequireDefault(_title);

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

  let TitleLevel = exports.TitleLevel = undefined;

  (function (TitleLevel) {
    TitleLevel["h1"] = "h1";
    TitleLevel["h2"] = "h2";
    TitleLevel["h3"] = "h3";
    TitleLevel["h4"] = "h4";
    TitleLevel["h5"] = "h5";
    TitleLevel["h6"] = "h6";
  })(TitleLevel || (exports.TitleLevel = TitleLevel = {}));

  const Title = exports.Title = _ref => {
    let {
      size,
      className = '',
      children = '',
      headingLevel: HeadingLevel = 'h1'
    } = _ref,
        props = _objectWithoutProperties(_ref, ["size", "className", "children", "headingLevel"]);

    return React.createElement(HeadingLevel, _extends({}, props, {
      className: (0, _reactStyles.css)(_title2.default.title, (0, _reactStyles.getModifier)(_title2.default, size), className)
    }), children);
  };

  Title.propTypes = {
    size: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.oneOf(['xs']), _propTypes2.default.oneOf(['sm']), _propTypes2.default.oneOf(['md']), _propTypes2.default.oneOf(['lg']), _propTypes2.default.oneOf(['xl']), _propTypes2.default.oneOf(['2xl']), _propTypes2.default.oneOf(['3xl']), _propTypes2.default.oneOf(['4xl'])]).isRequired,
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    headingLevel: _propTypes2.default.oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6'])
  };
});
//# sourceMappingURL=Title.js.map