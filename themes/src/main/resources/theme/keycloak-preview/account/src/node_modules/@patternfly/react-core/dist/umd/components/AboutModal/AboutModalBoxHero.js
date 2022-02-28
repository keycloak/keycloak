(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/AboutModalBox/about-modal-box", "@patternfly/react-tokens/dist/js/c_about_modal_box__hero_sm_BackgroundImage"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/AboutModalBox/about-modal-box"), require("@patternfly/react-tokens/dist/js/c_about_modal_box__hero_sm_BackgroundImage"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.aboutModalBox, global.c_about_modal_box__hero_sm_BackgroundImage);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _aboutModalBox, _c_about_modal_box__hero_sm_BackgroundImage) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.AboutModalBoxHero = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _aboutModalBox2 = _interopRequireDefault(_aboutModalBox);

  var _c_about_modal_box__hero_sm_BackgroundImage2 = _interopRequireDefault(_c_about_modal_box__hero_sm_BackgroundImage);

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

  const AboutModalBoxHero = exports.AboutModalBoxHero = _ref => {
    let {
      className,
      backgroundImageSrc
    } = _ref,
        props = _objectWithoutProperties(_ref, ["className", "backgroundImageSrc"]);

    return React.createElement("div", _extends({
      style:
      /* eslint-disable camelcase */
      backgroundImageSrc !== '' ? {
        [_c_about_modal_box__hero_sm_BackgroundImage2.default.name]: `url(${backgroundImageSrc})`
      } : {}
      /* eslint-enable camelcase */
      ,
      className: (0, _reactStyles.css)(_aboutModalBox2.default.aboutModalBoxHero, className)
    }, props));
  };

  AboutModalBoxHero.propTypes = {
    className: _propTypes2.default.string,
    backgroundImageSrc: _propTypes2.default.string
  };
});
//# sourceMappingURL=AboutModalBoxHero.js.map