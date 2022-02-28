(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/Page/page", "@patternfly/react-styles"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/Page/page"), require("@patternfly/react-styles"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.page, global.reactStyles);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _page, _reactStyles) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.PageSection = exports.PageSectionTypes = exports.PageSectionVariants = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _page2 = _interopRequireDefault(_page);

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

  let PageSectionVariants = exports.PageSectionVariants = undefined;

  (function (PageSectionVariants) {
    PageSectionVariants["default"] = "default";
    PageSectionVariants["light"] = "light";
    PageSectionVariants["dark"] = "dark";
    PageSectionVariants["darker"] = "darker";
  })(PageSectionVariants || (exports.PageSectionVariants = PageSectionVariants = {}));

  let PageSectionTypes = exports.PageSectionTypes = undefined;

  (function (PageSectionTypes) {
    PageSectionTypes["default"] = "default";
    PageSectionTypes["nav"] = "nav";
  })(PageSectionTypes || (exports.PageSectionTypes = PageSectionTypes = {}));

  const PageSection = exports.PageSection = _ref => {
    let {
      className = '',
      children,
      variant = 'default',
      type = 'default',
      noPadding = false,
      noPaddingMobile = false,
      isFilled
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "children", "variant", "type", "noPadding", "noPaddingMobile", "isFilled"]);

    const variantType = {
      [PageSectionTypes.default]: _page2.default.pageMainSection,
      [PageSectionTypes.nav]: _page2.default.pageMainNav
    };
    const variantStyle = {
      [PageSectionVariants.default]: '',
      [PageSectionVariants.light]: _page2.default.modifiers.light,
      [PageSectionVariants.dark]: _page2.default.modifiers.dark_200,
      [PageSectionVariants.darker]: _page2.default.modifiers.dark_100
    };
    return React.createElement("section", _extends({}, props, {
      className: (0, _reactStyles.css)(variantType[type], noPadding && _page2.default.modifiers.noPadding, noPaddingMobile && _page2.default.modifiers.noPaddingMobile, variantStyle[variant], isFilled === false && _page2.default.modifiers.noFill, isFilled === true && _page2.default.modifiers.fill, className)
    }), children);
  };

  PageSection.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    variant: _propTypes2.default.oneOf(['default', 'light', 'dark', 'darker']),
    type: _propTypes2.default.oneOf(['default', 'nav']),
    isFilled: _propTypes2.default.bool,
    noPadding: _propTypes2.default.bool,
    noPaddingMobile: _propTypes2.default.bool
  };
});
//# sourceMappingURL=PageSection.js.map