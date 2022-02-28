(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/BackgroundImage/background-image", "@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage", "@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_2x", "@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm", "@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm_2x", "@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_lg"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/BackgroundImage/background-image"), require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage"), require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_2x"), require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm"), require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_sm_2x"), require("@patternfly/react-tokens/dist/js/c_background_image_BackgroundImage_lg"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.backgroundImage, global.c_background_image_BackgroundImage, global.c_background_image_BackgroundImage_2x, global.c_background_image_BackgroundImage_sm, global.c_background_image_BackgroundImage_sm_2x, global.c_background_image_BackgroundImage_lg);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _backgroundImage, _c_background_image_BackgroundImage, _c_background_image_BackgroundImage_2x, _c_background_image_BackgroundImage_sm, _c_background_image_BackgroundImage_sm_2x, _c_background_image_BackgroundImage_lg) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.BackgroundImage = exports.BackgroundImageSrc = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _backgroundImage2 = _interopRequireDefault(_backgroundImage);

  var _c_background_image_BackgroundImage2 = _interopRequireDefault(_c_background_image_BackgroundImage);

  var _c_background_image_BackgroundImage_2x2 = _interopRequireDefault(_c_background_image_BackgroundImage_2x);

  var _c_background_image_BackgroundImage_sm2 = _interopRequireDefault(_c_background_image_BackgroundImage_sm);

  var _c_background_image_BackgroundImage_sm_2x2 = _interopRequireDefault(_c_background_image_BackgroundImage_sm_2x);

  var _c_background_image_BackgroundImage_lg2 = _interopRequireDefault(_c_background_image_BackgroundImage_lg);

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

  let BackgroundImageSrc = exports.BackgroundImageSrc = undefined;

  (function (BackgroundImageSrc) {
    BackgroundImageSrc["xs"] = "xs";
    BackgroundImageSrc["xs2x"] = "xs2x";
    BackgroundImageSrc["sm"] = "sm";
    BackgroundImageSrc["sm2x"] = "sm2x";
    BackgroundImageSrc["lg"] = "lg";
    BackgroundImageSrc["filter"] = "filter";
  })(BackgroundImageSrc || (exports.BackgroundImageSrc = BackgroundImageSrc = {}));

  const cssVariables = {
    [BackgroundImageSrc.xs]: _c_background_image_BackgroundImage2.default && _c_background_image_BackgroundImage2.default.name,
    [BackgroundImageSrc.xs2x]: _c_background_image_BackgroundImage_2x2.default && _c_background_image_BackgroundImage_2x2.default.name,
    [BackgroundImageSrc.sm]: _c_background_image_BackgroundImage_sm2.default && _c_background_image_BackgroundImage_sm2.default.name,
    [BackgroundImageSrc.sm2x]: _c_background_image_BackgroundImage_sm_2x2.default && _c_background_image_BackgroundImage_sm_2x2.default.name,
    [BackgroundImageSrc.lg]: _c_background_image_BackgroundImage_lg2.default && _c_background_image_BackgroundImage_lg2.default.name
  };

  const BackgroundImage = exports.BackgroundImage = _ref => {
    let {
      className = '',
      src
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "src"]);

    let srcMap = src; // Default string value to handle all sizes

    if (typeof src === 'string') {
      srcMap = {
        [BackgroundImageSrc.xs]: src,
        [BackgroundImageSrc.xs2x]: src,
        [BackgroundImageSrc.sm]: src,
        [BackgroundImageSrc.sm2x]: src,
        [BackgroundImageSrc.lg]: src,
        [BackgroundImageSrc.filter]: '' // unused

      };
    } // Build stylesheet string based on cssVariables


    let cssSheet = '';
    Object.keys(cssVariables).forEach(size => {
      cssSheet += `${cssVariables[size]}: url('${srcMap[size]}');`;
    }); // Create emotion stylesheet to inject new css

    const bgStyles = _reactStyles.StyleSheet.create({
      bgOverrides: `&.pf-c-background-image {
      ${cssSheet}
    }`
    });

    return React.createElement("div", _extends({
      className: (0, _reactStyles.css)(_backgroundImage2.default.backgroundImage, bgStyles.bgOverrides, className)
    }, props), React.createElement("svg", {
      xmlns: "http://www.w3.org/2000/svg",
      className: "pf-c-background-image__filter",
      width: "0",
      height: "0"
    }, React.createElement("filter", {
      id: "image_overlay"
    }, React.createElement("feColorMatrix", {
      type: "matrix",
      values: "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 0 0 0 1 0"
    }), React.createElement("feComponentTransfer", {
      colorInterpolationFilters: "sRGB",
      result: "duotone"
    }, React.createElement("feFuncR", {
      type: "table",
      tableValues: "0.086274509803922 0.43921568627451"
    }), React.createElement("feFuncG", {
      type: "table",
      tableValues: "0.086274509803922 0.43921568627451"
    }), React.createElement("feFuncB", {
      type: "table",
      tableValues: "0.086274509803922 0.43921568627451"
    }), React.createElement("feFuncA", {
      type: "table",
      tableValues: "0 1"
    })))));
  };

  BackgroundImage.propTypes = {
    className: _propTypes2.default.string,
    src: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.shape({
      xs: _propTypes2.default.string.isRequired,
      xs2x: _propTypes2.default.string.isRequired,
      sm: _propTypes2.default.string.isRequired,
      sm2x: _propTypes2.default.string.isRequired,
      lg: _propTypes2.default.string.isRequired,
      filter: _propTypes2.default.string
    })]).isRequired
  };
});
//# sourceMappingURL=BackgroundImage.js.map